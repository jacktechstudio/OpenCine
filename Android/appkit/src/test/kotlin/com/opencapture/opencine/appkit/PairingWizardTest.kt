package com.opencapture.opencine.appkit

import com.opencapture.monitorui.MonitorPairingCheck
import com.opencapture.monitorui.MonitorPairingCheckState
import com.opencapture.monitorui.MonitorPairingInstruction
import com.opencapture.monitorui.MonitorPairingInstructionIcon
import com.opencapture.monitorui.MonitorPairingPresentation
import com.opencapture.monitorui.MonitorPairingStep
import com.opencapture.opencine.core.Availability
import com.opencapture.opencine.core.ConnectionPhase
import com.opencapture.opencine.core.ConnectionPrompt
import com.opencapture.opencine.core.IntegrationID
import com.opencapture.opencine.core.LocalVPNFilter
import com.opencapture.opencine.core.NearbyCamera
import com.opencapture.opencine.core.RecoveryAction
import com.opencapture.opencine.core.RouteKind
import com.opencapture.opencine.core.SavedCamera
import com.opencapture.opencine.core.UnavailableReason
import com.opencapture.opencine.core.testing.FakeIntegration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Swift suite `PairingWizardTests`, same method names and values, with Android main's words ("On
 * this phone", "Android asks"), no Back while idle, and the discovery blocker as the primary action.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PairingWizardTest {
    private val wizard = PairingWizard(
        bodies = listOf("Find body", "Approve body", "Wi-Fi body", "Video body"),
        approveLines = listOf("Tap Allow"), joinLines = listOf("Leave it on"),
        choosing = MonitorPairingInstruction("Check first", MonitorPairingInstructionIcon.PHONE, listOf("Quit the other app.")),
        label = "Live label",
    )

    private fun model(
        fake: FakeIntegration,
        scope: CoroutineScope,
        saved: List<SavedCamera> = emptyList(),
        copy: ((NearbyCamera) -> NearbyCardCopy)? = null,
    ): CamerasModel {
        val store = SavedCamerasStore(Defaults(emptyMap()).at("document"))
        store.save(saved)
        return CamerasModel(listOf(AppIntegration(fake, IntegrationSlots(nearbyCard = copy))), store, scope)
    }

    @Test
    fun choosingShowsTheFourPagesThePreCheckAndTheScanLabel() = runTest {
        val fake = FakeIntegration(id = IntegrationID("test.cameras"))
        val model = model(fake, backgroundScope)
        model.beginPairing()
        runCurrent()
        assertEquals(
            MonitorPairingPresentation(
                steps = listOf(
                    MonitorPairingStep("查找相机", "搜索蓝牙设备"), MonitorPairingStep("在相机上确认", "相机提示"),
                    MonitorPairingStep("加入相机 Wi-Fi", "相机网络"), MonitorPairingStep("建立图传连接", "图传连接"),
                ),
                currentStep = 0, title = "查找相机", body = "Find body", target = "尚未选择",
                hint = "选择与相机屏幕显示一致的设备", progress = "Live label",
                instructions = listOf(MonitorPairingInstruction("Check first", MonitorPairingInstructionIcon.PHONE, listOf("Quit the other app."))),
                emptyTitle = "正在搜索相机", primaryAction = "继续",
            ),
            CamerasMapping.wizard(model, wizard),
        )

        fake.publish(listOf(Values.sighting))
        runCurrent()
        assertFalse(CamerasMapping.wizard(model, wizard).primaryActionEnabled)
        model.select("n2")
        val chosen = CamerasMapping.wizard(model, wizard)
        assertTrue(chosen.target == "CAM-2" && chosen.hint == "已选择 CAM-2" && chosen.primaryActionEnabled)
        assertTrue(chosen.devices.map { it.id } == listOf("n2") && chosen.devices[0].selected && chosen.emptyTitle == null)
        assertEquals(null, chosen.backAction)

        val blocked = CamerasMapping.wizard(model, wizard.copy(blocker = "Turn Bluetooth on"))
        assertTrue(blocked.primaryAction == "Turn Bluetooth on" && blocked.primaryActionEnabled && blocked.progress == null)
    }

    @Test
    fun aConnectAdvancesTheRailFromTheReportedSteps() = runTest {
        val fake = FakeIntegration(id = IntegrationID("test.cameras"))
        val model = model(fake, backgroundScope, saved = listOf(Values.camera))
        model.beginPairing()
        fake.publish(listOf(Values.sighting))
        runCurrent()
        model.select("n2")
        model.connect(model.state.value.nearby.first())
        runCurrent()
        val starting = CamerasMapping.wizard(model, wizard)
        assertTrue(starting.currentStep == 1 && starting.devices.isEmpty() && starting.progress == "Live label")
        assertTrue(starting.primaryAction == null && !starting.primaryActionEnabled && starting.backAction == "取消")

        fake.attempts[0].progress(ConnectionPhase.LINK, 0, note = "正在查找相机")
        runCurrent()
        assertEquals(0, CamerasMapping.wizard(model, wizard).currentStep)

        fake.attempts[0].progress(ConnectionPhase.AWAITING_APPROVAL, 0, prompt = ConnectionPrompt.ApproveOnCamera("Approve"))
        runCurrent()
        val approve = CamerasMapping.wizard(model, wizard)
        assertTrue(approve.currentStep == 1 && approve.title == "在相机上确认" && approve.body == "Approve body")
        assertTrue(approve.hint == "无需输入，请在相机上确认" && approve.checks.isEmpty())
        assertEquals(
            listOf(
                MonitorPairingInstruction("在相机上操作", MonitorPairingInstructionIcon.CAMERA, listOf("Tap Allow")),
                MonitorPairingInstruction(
                    "在本机操作", MonitorPairingInstructionIcon.PHONE,
                    listOf("请停留在此页面，以保持蓝牙连接", "请勿强行关闭应用"),
                ),
            ),
            approve.instructions,
        )

        fake.attempts[0].progress(ConnectionPhase.JOINING_NETWORK, 1)
        runCurrent()
        val join = CamerasMapping.wizard(model, wizard.copy(joinedNetwork = "LUNA-1"))
        assertTrue(join.currentStep == 2 && join.hint == "Android 将提示加入相机网络")
        assertEquals(
            listOf(
                MonitorPairingInstruction("在相机上操作", MonitorPairingInstructionIcon.CAMERA, listOf("Leave it on")),
                MonitorPairingInstruction(
                    "在本机操作", MonitorPairingInstructionIcon.PHONE,
                    listOf("Android 提示加入相机网络时，请轻点“连接”", LocalVPNFilter.JOIN_WIFI_PHONE_STEP),
                ),
            ),
            join.instructions,
        )
        assertEquals(
            listOf(
                MonitorPairingCheck("蓝牙连接", "CAM-2", MonitorPairingCheckState.COMPLETE, "确定"),
                MonitorPairingCheck("相机 Wi-Fi", "LUNA-1", MonitorPairingCheckState.ACTIVE, "正在加入"),
                MonitorPairingCheck("取景画面", "加入 Wi-Fi 后启动", MonitorPairingCheckState.WAITING, "等待中"),
            ),
            join.checks,
        )

        fake.attempts[0].progress(ConnectionPhase.LINK, 2)
        runCurrent()
        val video = CamerasMapping.wizard(model, wizard)
        assertTrue(video.currentStep == 3 && video.instructions.isEmpty() && video.checks.last().stateLabel == "等待中")
        assertEquals("正在建立图传连接…", video.checks.last().subtitle)
        assertEquals(
            listOf(MonitorPairingInstruction("检查连接", MonitorPairingInstructionIcon.PHONE, listOf(LocalVPNFilter.WIZARD_BANNER))),
            CamerasMapping.wizard(model, wizard.copy(vpnActive = true)).instructions,
        )
    }

    @Test
    fun aFailureReturnsToTheFirstPageWithItsErrorAndAction() = runTest {
        val fake = FakeIntegration(id = IntegrationID("test.cameras"))
        val model = model(fake, backgroundScope)
        model.beginPairing()
        fake.publish(listOf(Values.sighting))
        runCurrent()
        model.select("n2")
        model.connect(model.state.value.nearby.first())
        runCurrent()
        fake.attempts[0].fail(Values.failure)
        runCurrent()
        val failed = CamerasMapping.wizard(model, wizard)
        assertTrue(failed.currentStep == 0 && failed.title == "查找相机" && failed.body == "Find body")
        assertTrue(failed.error == "Turn on Bluetooth for this app." && failed.progress == null && failed.instructions.isEmpty())
        // Android main: Try again even when Settings could fix it.
        assertTrue(failed.primaryAction == "重试" && failed.primaryActionEnabled)

        model.dismissFailure("n2")
        assertEquals(null, CamerasMapping.wizard(model, wizard).error)
        assertTrue(RecoveryAction.OpenSettings in Values.failure.actions)
    }

    @Test
    fun aFailedPairKeepsItsReasonAfterTheRescanClearsSightings() = runTest {
        val fake = FakeIntegration(id = IntegrationID("test.cameras"))
        val model = model(fake, backgroundScope)
        model.beginPairing()
        fake.publish(listOf(Values.sighting))
        runCurrent()
        model.select("n2")
        model.connect(model.state.value.nearby.first())
        runCurrent()
        fake.attempts[0].fail(Values.failure)
        runCurrent()
        fake.publish(emptyList())
        runCurrent()
        assertTrue(model.state.value.nearby.isEmpty())
        val failed = CamerasMapping.wizard(model, wizard)
        assertEquals(Values.failure.message, failed.error)
        assertEquals("重试", failed.primaryAction)
        assertEquals("CAM-2", failed.target)
    }

    @Test
    fun theAppWordsNearbyCards() = runTest {
        val fake = FakeIntegration(id = IntegrationID("test.cameras"))
        val model = model(fake, backgroundScope, copy = { NearbyCardCopy(it.name, "Brand · 附近", "Needs approval on the camera") })
        fake.publish(
            listOf(
                Values.sighting,
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
                    id = "n2", name = "CAM-2", subtitle = "Brand · 附近", badge = "新设备",
                    status = "Needs approval on the camera", actionTitle = "配对",
                ),
                CameraItem(
                    id = "n3", name = "CAM-3", subtitle = "Brand · 附近", badge = "使用中",
                    status = "其他设备正在使用", actionTitle = "配对", isAvailable = false,
                ),
            ),
            CamerasMapping.nearby(model),
        )
    }
}
