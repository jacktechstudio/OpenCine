package com.opencapture.opencine.appkit

import com.opencapture.monitorui.MonitorConnectFailure
import com.opencapture.monitorui.MonitorConnectStep
import com.opencapture.monitorui.MonitorPairingPresentation
import com.opencapture.monitorui.MonitorPairingStep
import com.opencapture.monitorui.MonitorSetupChip
import com.opencapture.opencine.core.Availability
import com.opencapture.opencine.core.ConnectionPhase
import com.opencapture.opencine.core.IntegrationID
import com.opencapture.opencine.core.NearbyCamera
import com.opencapture.opencine.core.RecoveryAction
import com.opencapture.opencine.core.RouteKind
import com.opencapture.opencine.core.SavedRoute
import com.opencapture.opencine.core.UnavailableReason
import com.opencapture.opencine.core.testing.FakeIntegration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Swift suite `CamerasMappingTests`: the same golden values. */
@OptIn(ExperimentalCoroutinesApi::class)
class CamerasMappingTest {
    @Test fun editSetupReopensOnlyAnAdvertisedCurrentRoute() = runTest {
        val fake = FakeIntegration(id = IntegrationID("test.cameras"))
        val model = Values.model(listOf(fake), backgroundScope)
        val chooser = AddSetupChooser("添加连接方案", "", listOf(
            AddSetupOption("wifi", RouteKind.INFRASTRUCTURE_NETWORK, "Wi-Fi", "", "Choose"),
            AddSetupOption("hotspot", RouteKind.PHONE_HOTSPOT, "热点", "", "Choose"),
        ))
        model.connect(Values.camera, Values.wifi.id)
        fake.attempts[0].fail(Values.failure)
        runCurrent()
        assertEquals("wifi", CamerasMapping.addSetupSelection(model, Values.camera, chooser))
        val removed = Values.camera.copy(routes = Values.camera.routes.filterNot { it.kind == RouteKind.INFRASTRUCTURE_NETWORK })
        assertEquals(null, CamerasMapping.addSetupSelection(model, removed, chooser))
        val unsupported = chooser.copy(options = chooser.options.filterNot { it.kind == RouteKind.INFRASTRUCTURE_NETWORK })
        assertEquals(null, CamerasMapping.addSetupSelection(model, Values.camera, unsupported))
    }

    @Test
    fun pairedCardsShowRoutesProgressAndFailures() = runTest {
        val fake = FakeIntegration(id = IntegrationID("test.cameras"))
        val older = Values.camera.copy(id = "camera-0", name = "B cam", lastConnected = 500_000)
        val model = Values.model(listOf(fake), backgroundScope, listOf(older, Values.camera))
        fake.publish(listOf(NearbyCamera("camera-1", name = "CAM-1", model = "Test Body", routes = setOf(RouteKind.CAMERA_ACCESS_POINT))))
        runCurrent()
        model.connect(Values.camera)
        fake.attempts[0].progress(ConnectionPhase.JOINING_NETWORK, 2, note = "Joining CAM-1")
        model.connect(older, Values.wifi.id)
        fake.attempts[1].fail(Values.failure)
        runCurrent()

        val items = CamerasMapping.paired(model)
        assertEquals(
            CameraItem(
                id = "camera-1", name = "CAM-1", subtitle = "Test Body · CAM-1", badge = "正在连接",
                status = "Joining CAM-1", actionTitle = "连接", isPrimary = true, isBusy = true, isAvailable = true,
                setups = listOf(
                    MonitorSetupChip(Values.ap.id.text, "相机 Wi-Fi", active = true, kind = "cameraWiFi"),
                    MonitorSetupChip(Values.wifi.id.text, "Wi-Fi · studio", active = false, canForget = true, kind = "wifi"),
                ),
                canAddSetup = false,
                steps = listOf(
                    MonitorConnectStep("Link", "", MonitorConnectStep.State.DONE),
                    MonitorConnectStep("Approve", "", MonitorConnectStep.State.DONE),
                    MonitorConnectStep("Join network", "Joining CAM-1", MonitorConnectStep.State.ACTIVE),
                    MonitorConnectStep("Video", "", MonitorConnectStep.State.WAITING),
                ),
            ),
            items[0],
        )
        assertEquals(
            CameraItem(
                id = "camera-0", name = "B cam", subtitle = "Test Body · studio", badge = "未连接",
                status = "未找到相机。请开机后重新连接", actionTitle = "重新连接", isAvailable = false,
                setups = listOf(
                    MonitorSetupChip(Values.ap.id.text, "相机 Wi-Fi", active = false, kind = "cameraWiFi"),
                    MonitorSetupChip(Values.wifi.id.text, "Wi-Fi · studio", active = true, canForget = true, kind = "wifi"),
                ),
                canAddSetup = false,
                failure = MonitorConnectFailure(
                    "Bluetooth is off", "Turn on Bluetooth for this app.",
                    listOf(
                        MonitorConnectFailure.Action("settings", "打开设置"),
                        MonitorConnectFailure.Action("edit:${Values.wifi.id.text}", "编辑连接方案"),
                        MonitorConnectFailure.Action("retry", "重试", primary = true),
                    ),
                ),
            ),
            items[1],
        )
        assertEquals(RecoveryAction.EditRoute(Values.wifi.id), CamerasMapping.action("edit:${Values.wifi.id.text}", "camera-0", model))
    }

    @Test
    fun aCameraWithoutModelOrSpareRoute() = runTest {
        val fake = FakeIntegration(id = IntegrationID("test.cameras"))
        val camera = Values.camera.copy(model = "", routes = listOf(Values.wifi), preferredRoute = Values.wifi.id)
        val model = Values.model(listOf(fake), backgroundScope, listOf(camera))
        val item = CamerasMapping.paired(model)[0]
        assertTrue(item.subtitle == "studio" && item.setups.map { it.canForget } == listOf(false))
        fake.publish(listOf(NearbyCamera("n9", name = "CAM-9", model = "", routes = setOf(RouteKind.INFRASTRUCTURE_NETWORK))))
        runCurrent()
        assertEquals(listOf("附近"), CamerasMapping.nearby(model).map { it.subtitle })
    }

    @Test
    fun addIsOfferedOnlyWhileAnAddableKindIsMissing() = runTest {
        val fake = FakeIntegration(id = IntegrationID("test.cameras"))
        val missing = Values.camera.copy(routes = listOf(Values.ap), preferredRoute = Values.ap.id)
        assertTrue(CamerasMapping.paired(Values.model(listOf(fake), backgroundScope, listOf(missing)))[0].canAddSetup)
        val full = Values.camera.copy(routes = listOf(Values.ap, Values.wifi), preferredRoute = Values.ap.id)
        assertFalse(CamerasMapping.paired(Values.model(listOf(fake), backgroundScope, listOf(full)))[0].canAddSetup)
    }

    @Test
    fun nearbyCamerasAreUnsavedSightings() = runTest {
        val fake = FakeIntegration(id = IntegrationID("test.cameras"))
        val model = Values.model(listOf(fake), backgroundScope)
        fake.publish(
            listOf(
                NearbyCamera("x", savedID = "camera-1", name = "CAM-1", model = "Test Body", routes = setOf(RouteKind.CAMERA_ACCESS_POINT)),
                NearbyCamera("n2", name = "CAM-2", model = "Test Body", routes = setOf(RouteKind.CAMERA_ACCESS_POINT), signalBars = 3),
                NearbyCamera(
                    "n3", name = "CAM-3", model = "Test Body", routes = setOf(RouteKind.CAMERA_ACCESS_POINT),
                    availability = Availability.Unavailable(UnavailableReason.Busy),
                ),
            ),
        )
        runCurrent()
        assertEquals(
            listOf(
                CameraItem(
                    id = "n2", name = "CAM-2", subtitle = "Test Body · 附近", badge = "新设备", status = "可以配对",
                    actionTitle = "配对", signalBars = 3,
                ),
                CameraItem(
                    id = "n3", name = "CAM-3", subtitle = "Test Body · 附近", badge = "使用中",
                    status = "其他设备正在使用", actionTitle = "配对", isAvailable = false,
                ),
            ),
            CamerasMapping.nearby(model),
        )
    }

    @Test
    fun thePairPageFollowsChoosingAttemptAndFailure() = runTest {
        val fake = FakeIntegration(id = IntegrationID("test.cameras"))
        val model = Values.model(listOf(fake), backgroundScope, emptyList())
        model.beginPairing()
        runCurrent()
        assertEquals(
            MonitorPairingPresentation(
                steps = listOf(MonitorPairingStep("查找相机", "附近的相机")), currentStep = 0,
                title = "查找相机", body = "请打开相机，并将手机靠近相机。",
                target = "尚未选择", hint = "选择与相机屏幕显示一致的设备",
                emptyTitle = "正在搜索相机", primaryAction = "继续",
            ),
            CamerasMapping.pairing(model),
        )

        fake.publish(listOf(Values.sighting))
        runCurrent()
        model.select("n2")
        val chosen = CamerasMapping.pairing(model)
        assertTrue(chosen.target == "CAM-2" && chosen.hint == "已选择 CAM-2" && chosen.primaryActionEnabled)
        assertTrue(chosen.devices.map { it.id } == listOf("n2") && chosen.devices[0].selected && chosen.emptyTitle == null)

        model.connect(model.state.value.nearby.first())
        fake.attempts[0].progress(ConnectionPhase.LINK, 0)
        runCurrent()
        val linking = CamerasMapping.pairing(model)
        assertEquals(listOf("查找相机", "Link", "Approve", "Join network", "Video"), linking.steps.map { it.title })
        assertTrue(linking.currentStep == 1 && linking.devices.isEmpty() && linking.progress == "Link")
        assertTrue(linking.primaryAction == null && linking.backAction == "取消")

        fake.attempts[0].fail(Values.failure)
        runCurrent()
        val failed = CamerasMapping.pairing(model)
        assertTrue(failed.title == "Bluetooth is off" && failed.body == "Turn on Bluetooth for this app." && failed.error == null)
        assertTrue(failed.primaryAction == "打开设置" && failed.primaryActionEnabled && failed.currentStep == 0)
    }

    @Test
    fun routeTitlesFollowTheKind() {
        assertEquals(
            listOf("相机 Wi-Fi", "Wi-Fi", "热点", "USB-C", "HDMI"),
            RouteKind.entries.map { CamerasMapping.title(SavedRoute(key = "k", kind = it)) },
        )
        assertEquals("Desk", CamerasMapping.title(SavedRoute(key = "k", kind = RouteKind.USB, title = "Desk")))
    }
}
