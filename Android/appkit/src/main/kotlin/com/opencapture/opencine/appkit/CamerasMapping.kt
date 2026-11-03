package com.opencapture.opencine.appkit

import com.opencapture.monitorui.MonitorConnectFailure
import com.opencapture.monitorui.MonitorConnectStep
import com.opencapture.monitorui.MonitorPairingDevice
import com.opencapture.monitorui.MonitorPairingInstruction
import com.opencapture.monitorui.MonitorPairingInstructionIcon
import com.opencapture.monitorui.MonitorPairingPresentation
import com.opencapture.monitorui.MonitorPairingStep
import com.opencapture.monitorui.MonitorSetupChip
import com.opencapture.opencine.core.Availability
import com.opencapture.opencine.core.ConnectTarget
import com.opencapture.opencine.core.ConnectionFailure
import com.opencapture.opencine.core.ConnectionPrompt
import com.opencapture.opencine.core.ConnectionRouteID
import com.opencapture.opencine.core.RecoveryAction
import com.opencapture.opencine.core.RouteKind
import com.opencapture.opencine.core.SavedCamera
import com.opencapture.opencine.core.SavedRoute

/** One card of the cameras page (Swift `CameraListItem`). */
public data class CameraItem(
    public val id: String,
    public val name: String,
    public val subtitle: String,
    public val badge: String = "",
    public val status: String,
    public val actionTitle: String,
    public val isPrimary: Boolean = false,
    public val isBusy: Boolean = false,
    public val isAvailable: Boolean = true,
    public val signalBars: Int? = null,
    public val setups: List<MonitorSetupChip> = emptyList(),
    public val canAddSetup: Boolean = false,
    public val steps: List<MonitorConnectStep> = emptyList(),
    public val failure: MonitorConnectFailure? = null,
)

/** The route id as both platforms print it (Swift `UUID.uuidString`). */
public val ConnectionRouteID.text: String get() = uuid.toString().uppercase()

/**
 * The model as the shared camera pages show it (Swift `CamerasMapping`, same values). Integration
 * copy (steps, prompts, failures) is rendered as given; the shell's own copy is the pages' frame.
 */
public object CamerasMapping {
    /** Reopens a failed saved route only when it still exists and the chooser advertises it. */
    public fun addSetupSelection(model: CamerasModel, camera: SavedCamera, chooser: AddSetupChooser): String? {
        val target = model.state.value.failures[camera.id]?.target as? ConnectTarget.Saved ?: return null
        val kind = camera.routes.firstOrNull { it.id == target.route }?.kind ?: return null
        return chooser.options.firstOrNull { it.kind == kind && it.enabled }?.id
    }

    /** Saved cameras, most recently connected first. */
    public fun paired(model: CamerasModel): List<CameraItem> {
        val state = model.state.value
        val ordered = state.saved.withIndex().sortedWith { a, b ->
            val x = a.value.lastConnected
            val y = b.value.lastConnected
            when {
                x != null && y != null && x != y -> y.compareTo(x)
                x != null && y == null -> -1
                x == null && y != null -> 1
                else -> a.index.compareTo(b.index)
            }
        }.map { it.value }
        val latest = ordered.firstOrNull { it.lastConnected != null }?.id
        return ordered.map { camera ->
            val attempt = state.attempts[camera.id]
            val failure = state.failures[camera.id]
            val nearby = sighting(camera, state) != null
            val addable = addableRoutes(camera, model)
            val active = attempt?.route ?: failure?.let { route(it.target) } ?: model.defaultRoute(camera)
            val network = camera.routes.firstOrNull { it.id == active }?.network
            CameraItem(
                id = camera.id,
                name = camera.displayName,
                subtitle = listOfNotNull(camera.model, network).filter { it.isNotEmpty() }.joinToString(" · "),
                badge = when {
                    attempt != null -> "正在连接"
                    failure != null -> "未连接"
                    camera.id == latest -> "最近使用"
                    nearby -> "已配对"
                    else -> "离线"
                },
                status = attempt?.let(::status) ?: if (nearby) "附近 · 可以连接" else "未找到相机。请开机后重新连接",
                actionTitle = if (nearby) "连接" else "重新连接",
                isPrimary = camera.id == latest,
                isBusy = attempt != null,
                isAvailable = nearby,
                setups = camera.routes.map {
                    MonitorSetupChip(
                        it.id.text, title(it), it.id == active, it.kind in addable && camera.routes.size > 1,
                        setupKind(it.kind),
                    )
                },
                // As on main: Add only while an addable kind is still missing.
                canAddSetup = (addable - camera.routes.map { it.kind }.toSet()).isNotEmpty(),
                steps = attempt?.let(::steps).orEmpty(),
                failure = failure?.let { failure(it.failure, camera) },
            )
        }
    }

    /** Sightings that are not saved cameras. */
    public fun nearby(model: CamerasModel): List<CameraItem> {
        val state = model.state.value
        return state.nearby.filter { sighting -> state.saved.none { matches(sighting, it) } }.map { device(it, state, model) }
    }

    /** The chip title: the operator's setup name, else the shell's name for the kind. */
    public fun title(route: SavedRoute): String = route.title ?: when (route.kind) {
        RouteKind.CAMERA_ACCESS_POINT -> "相机 Wi-Fi"
        RouteKind.INFRASTRUCTURE_NETWORK -> route.network?.let { "Wi-Fi · $it" } ?: "Wi-Fi"
        RouteKind.PHONE_HOTSPOT -> "热点"
        RouteKind.USB -> "USB-C"
        RouteKind.CAPTURE_DEVICE -> "HDMI"
    }

    /** The chip's accessibility name: the setup names the apps' tests have always used. */
    internal fun setupKind(kind: RouteKind): String = when (kind) {
        RouteKind.CAMERA_ACCESS_POINT -> "cameraWiFi"
        RouteKind.INFRASTRUCTURE_NETWORK -> "wifi"
        RouteKind.PHONE_HOTSPOT, RouteKind.USB, RouteKind.CAPTURE_DEVICE -> kind.rawValue
    }

    /** The attempt's plan as card steps; the running step carries the one live caption. */
    public fun steps(attempt: CamerasModel.Attempt): List<MonitorConnectStep> {
        val progress = attempt.progress ?: return emptyList()
        return progress.steps.mapIndexed { index, step ->
            val state = when {
                index < progress.current -> MonitorConnectStep.State.DONE
                index == progress.current -> MonitorConnectStep.State.ACTIVE
                else -> MonitorConnectStep.State.WAITING
            }
            val detail = if (index == progress.current) attempt.prompt?.text ?: progress.note ?: step.detail else ""
            MonitorConnectStep(step.title, detail, state)
        }
    }

    public fun failure(failure: ConnectionFailure, camera: SavedCamera? = null): MonitorConnectFailure {
        val actions = mutableListOf<MonitorConnectFailure.Action>()
        var link: MonitorConnectFailure.Action? = null
        for (action in failure.actions) {
            when (action) {
                is RecoveryAction.UseRoute -> {
                    val route = camera?.routes?.firstOrNull { it.id == action.route }
                    link = MonitorConnectFailure.Action(id(action), "改用${route?.let(::title) ?: "其他连接方案"}连接")
                }
                RecoveryAction.Retry -> actions += MonitorConnectFailure.Action(id(action), "重试", primary = true)
                is RecoveryAction.EditRoute -> actions += MonitorConnectFailure.Action(id(action), "编辑连接方案")
                RecoveryAction.PairAgain -> actions += MonitorConnectFailure.Action(id(action), "重新配对")
                RecoveryAction.OpenSettings -> actions += MonitorConnectFailure.Action(id(action), "打开设置")
                is RecoveryAction.Integration -> actions += MonitorConnectFailure.Action(id(action), action.title)
            }
        }
        return MonitorConnectFailure(failure.title, failure.message, actions, link)
    }

    /** The stable id of a failure action on the page. */
    public fun id(action: RecoveryAction): String = when (action) {
        RecoveryAction.Retry -> "retry"
        is RecoveryAction.UseRoute -> "route:${action.route.text}"
        is RecoveryAction.EditRoute -> "edit:${action.route.text}"
        RecoveryAction.PairAgain -> "pair"
        RecoveryAction.OpenSettings -> "settings"
        is RecoveryAction.Integration -> "integration:${action.id}"
    }

    /** The failure action of [cameraID] a page action id names. */
    public fun action(id: String, cameraID: String, model: CamerasModel): RecoveryAction? =
        model.state.value.failures[cameraID]?.failure?.actions?.firstOrNull { id(it) == id }

    public fun pairing(model: CamerasModel): MonitorPairingPresentation {
        val state = model.state.value
        val selected = state.nearby.firstOrNull { it.id == state.selection }
        val cameraID = selected?.let { ConnectTarget.Nearby(it.camera).cameraID }
        val attempt = cameraID?.let { state.attempts[it] }
        val failure = cameraID?.let { state.failures[it] }?.failure
        val plan = attempt?.progress?.steps.orEmpty()
        val current = attempt?.progress?.let { 1 + it.current } ?: 0
        val choosing = attempt == null
        val step = attempt?.progress?.let { it.steps.getOrNull(it.current) }
        val instructions = when (val prompt = attempt?.prompt) {
            is ConnectionPrompt.ApproveOnCamera ->
                listOf(MonitorPairingInstruction("在相机上操作", MonitorPairingInstructionIcon.CAMERA, listOf(prompt.text)))
            is ConnectionPrompt.AcceptNetworkJoin ->
                listOf(MonitorPairingInstruction("在本机操作", MonitorPairingInstructionIcon.PHONE, listOf(prompt.text)))
            is ConnectionPrompt.Integration ->
                listOf(MonitorPairingInstruction("下一步", MonitorPairingInstructionIcon.CAMERA, listOf(prompt.text)))
            null -> emptyList()
        }
        val settings = failure?.actions?.contains(RecoveryAction.OpenSettings) == true
        return MonitorPairingPresentation(
            steps = listOf(MonitorPairingStep("查找相机", "附近的相机")) + plan.map { MonitorPairingStep(it.title, it.detail) },
            currentStep = current,
            title = failure?.title ?: step?.title ?: "查找相机",
            body = failure?.message ?: step?.detail ?: "请打开相机，并将手机靠近相机。",
            target = selected?.camera?.name ?: "尚未选择",
            hint = if (choosing) {
                selected?.let { "已选择 ${it.camera.name}" } ?: "选择与相机屏幕显示一致的设备"
            } else {
                "收到画面后将进入监看"
            },
            // The prompt has its own card; the live line is the integration's note or step.
            progress = attempt?.let { it.progress?.note ?: step?.title ?: "正在连接…" },
            devices = if (choosing) {
                state.nearby.map {
                    MonitorPairingDevice(
                        it.id, it.camera.name, "${it.camera.model} · 附近", selected = state.selection == it.id,
                        busy = state.attempts.isNotEmpty(), signalBars = it.camera.signalBars,
                    )
                }
            } else {
                emptyList()
            },
            instructions = instructions,
            emptyTitle = if (choosing && state.nearby.isEmpty()) {
                if (state.scanning) "正在搜索相机" else "尚未发现相机"
            } else {
                null
            },
            primaryAction = when {
                failure != null -> if (settings) "打开设置" else "重试"
                choosing -> "继续"
                else -> null
            },
            primaryActionEnabled = failure != null || (choosing && selected != null),
            backAction = when {
                !choosing -> "取消"
                state.saved.isEmpty() -> null
                else -> "返回"
            },
        )
    }

    internal fun device(sighting: CamerasModel.Sighting, state: CamerasModel.State, model: CamerasModel): CameraItem {
        val camera = sighting.camera
        val held = camera.availability != Availability.Available
        val copy = model.entries.firstOrNull { it.integration.id == sighting.integration }?.slots?.nearbyCard?.invoke(camera)
        return CameraItem(
            id = camera.id,
            name = copy?.name ?: camera.name,
            subtitle = copy?.subtitle ?: if (camera.model.isEmpty()) "附近" else "${camera.model} · 附近",
            badge = if (held) "使用中" else "新设备",
            status = if (held) "其他设备正在使用" else copy?.status ?: "可以配对",
            actionTitle = "配对",
            isPrimary = state.selection == camera.id,
            isBusy = ConnectTarget.Nearby(camera).cameraID in state.attempts,
            isAvailable = !held,
            signalBars = camera.signalBars,
        )
    }

    internal fun status(attempt: CamerasModel.Attempt): String {
        val progress = attempt.progress ?: return "正在连接…"
        return attempt.prompt?.text ?: progress.note ?: progress.steps.getOrNull(progress.current)?.title ?: "正在连接…"
    }

    internal fun matches(sighting: CamerasModel.Sighting, camera: SavedCamera): Boolean =
        sighting.integration == camera.integration && (sighting.camera.savedID == camera.id || sighting.camera.id == camera.id)

    private fun sighting(camera: SavedCamera, state: CamerasModel.State) = state.nearby.firstOrNull { matches(it, camera) }

    /** The routes the camera's profile (matched by model title) can add; without one, any the integration can add. */
    private fun addableRoutes(camera: SavedCamera, model: CamerasModel): Set<RouteKind> {
        val profiles = model.entries.firstOrNull { it.integration.id == camera.integration }?.integration?.profiles ?: return emptySet()
        return profiles.firstOrNull { it.title == camera.model }?.addableRoutes ?: profiles.flatMapTo(mutableSetOf()) { it.addableRoutes }
    }

    private fun route(target: ConnectTarget): ConnectionRouteID? = (target as? ConnectTarget.Saved)?.route
}
