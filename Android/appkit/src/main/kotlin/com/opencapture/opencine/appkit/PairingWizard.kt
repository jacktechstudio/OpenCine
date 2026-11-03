package com.opencapture.opencine.appkit

import androidx.compose.runtime.Composable
import com.opencapture.monitorui.MonitorPairingCheck
import com.opencapture.monitorui.MonitorPairingCheckState
import com.opencapture.monitorui.MonitorPairingDevice
import com.opencapture.monitorui.MonitorPairingInstruction
import com.opencapture.monitorui.MonitorPairingInstructionIcon
import com.opencapture.monitorui.MonitorPairingPresentation
import com.opencapture.monitorui.MonitorPairingStep
import com.opencapture.opencine.core.ConnectTarget
import com.opencapture.opencine.core.ConnectionPhase
import com.opencapture.opencine.core.LocalVPNFilter

/**
 * The four-page pair wizard the camera apps showed before the shared pair page (Swift
 * `PairingWizard`): find, approve, join camera Wi-Fi, open the video link, with a body and cards per
 * page, a check list from the Wi-Fi page on, the connection's live label and a failure as the page's
 * error. The app gives its copy and what only it knows. On Android, [blocker] is the primary action
 * while discovery cannot run (Nearby devices, Settings or Bluetooth), and the app's callouts are the
 * flow's content.
 */
public data class PairingWizard(
    /** One body per page. */
    val bodies: List<String>,
    /** "On the camera" lines on the approve page and on the Wi-Fi page. */
    val approveLines: List<String>,
    val joinLines: List<String>,
    /** The card while choosing; null shows none. */
    val choosing: MonitorPairingInstruction? = null,
    /** The connection's live label, shown while it scans or connects. */
    val label: String? = null,
    val joinedNetwork: String? = null,
    val vpnActive: Boolean = false,
    val blocker: String? = null,
) {
    public companion object {
        internal val titles = listOf("查找相机", "在相机上确认", "加入相机 Wi-Fi", "建立图传连接")
        internal val rail = listOf("搜索蓝牙设备", "相机提示", "相机网络", "图传连接")

        /**
         * The apps' live label for a connection phase (Swift `ConnectionPhase.label`). Reads each
         * integration's phase enum by its constant names, which match by design.
         */
        public fun phaseLabel(phase: Enum<*>): String = when (phase.name) {
            "SCANNING" -> "正在搜索相机…"
            "CONNECTING_GATT" -> "正在连接蓝牙…"
            "PAIRING" -> "正在配对…"
            "AWAITING_APPROVAL" -> "请在相机屏幕上确认"
            "READING_WIFI_CREDS" -> "正在读取 Wi-Fi 连接信息…"
            "JOINING_WIFI" -> "正在加入相机 Wi-Fi…"
            "OPENING_DATALINK" -> "正在建立数据连接…"
            "LIVE" -> "已连接"
            else -> "空闲"
        }

        /** A connect's scan (a first step with a live note, as both integrations report it) is still page 0. */
        internal fun page(attempt: CamerasModel.Attempt?): Int {
            attempt ?: return 0
            val progress = attempt.progress ?: return 1
            return when (progress.current) {
                0 -> if (progress.phase == ConnectionPhase.LINK && progress.note != null) 0 else 1
                1 -> 2
                else -> 3
            }
        }
    }

    /** The wizard with its actions: Continue connects the selection; [blocker] calls [onBlocker]; a failure's Try again goes back to choosing and calls [rescan]; Cancel stops the connect and calls [onDone]. */
    public fun flow(
        model: CamerasModel,
        onDone: () -> Unit,
        rescan: () -> Unit,
        onBlocker: () -> Unit = {},
        content: @Composable () -> Unit = {},
    ): IntegrationPairingFlow = IntegrationPairingFlow(
        presentation = CamerasMapping.wizard(model, this),
        onSelect = { id -> if (model.state.value.attempts.isEmpty()) model.select(id) },
        onPrimary = primary@{
            val state = model.state.value
            if (state.attempts.isNotEmpty()) return@primary
            val sighting = state.nearby.firstOrNull { it.id == state.selection }
            val failure = model.failureFor(state.selection)
            when {
                failure != null -> {
                    model.dismissFailure(failure.target.cameraID)
                    rescan()
                }
                blocker != null -> onBlocker()
                sighting != null -> model.connect(sighting)
            }
        },
        onBack = {
            model.cancelAll()
            onDone()
        },
        content = content,
    )
}

/** The selected camera's connect as the wizard shows it; any running attempt makes it busy (Swift `CamerasMapping.wizard`). */
public fun CamerasMapping.wizard(model: CamerasModel, wizard: PairingWizard): MonitorPairingPresentation {
    val state = model.state.value
    val selected = state.nearby.firstOrNull { it.id == state.selection }
    val cameraID = selected?.let { ConnectTarget.Nearby(it.camera).cameraID }
    val busy = state.attempts.isNotEmpty()
    val failed = if (busy) null else model.failureFor(state.selection)
    val failure = failed?.failure
    val page = if (failure != null) 0 else PairingWizard.page(cameraID?.let { state.attempts[it] })
    val choosing = page == 0
    val instructions = when (page) {
        0 -> listOfNotNull(wizard.choosing.takeIf { failure == null })
        1 -> listOf(
            MonitorPairingInstruction("在相机上操作", MonitorPairingInstructionIcon.CAMERA, wizard.approveLines),
            MonitorPairingInstruction(
                "在本机操作", MonitorPairingInstructionIcon.PHONE,
                listOf("请停留在此页面，以保持蓝牙连接", "请勿强行关闭应用"),
            ),
        )
        2 -> listOf(
            MonitorPairingInstruction("在相机上操作", MonitorPairingInstructionIcon.CAMERA, wizard.joinLines),
            MonitorPairingInstruction(
                "在本机操作", MonitorPairingInstructionIcon.PHONE,
                listOf("Android 提示加入相机网络时，请轻点“连接”", LocalVPNFilter.JOIN_WIFI_PHONE_STEP),
            ),
        )
        else -> if (wizard.vpnActive) {
            listOf(MonitorPairingInstruction("检查连接", MonitorPairingInstructionIcon.PHONE, listOf(LocalVPNFilter.WIZARD_BANNER)))
        } else {
            emptyList()
        }
    }
    val failedName = when (val target = failed?.target) {
        is ConnectTarget.Nearby -> target.camera.name
        is ConnectTarget.Saved -> target.camera.displayName
        is ConnectTarget.AddRoute -> target.camera.displayName
        null -> null
    }
    val name = selected?.let { device(it, state, model).name } ?: failedName
    val checks = if (page >= 2) {
        listOf(
            MonitorPairingCheck("蓝牙连接", name ?: "相机已连接", MonitorPairingCheckState.COMPLETE, "确定"),
            MonitorPairingCheck(
                "相机 Wi-Fi", wizard.joinedNetwork ?: "正在等待相机网络",
                if (page > 2) MonitorPairingCheckState.COMPLETE else MonitorPairingCheckState.ACTIVE,
                if (page > 2) "确定" else "正在加入",
            ),
            MonitorPairingCheck(
                "取景画面", if (page > 2) "正在建立图传连接…" else "加入 Wi-Fi 后启动",
                if (page > 2) MonitorPairingCheckState.ACTIVE else MonitorPairingCheckState.WAITING, "等待中",
            ),
        )
    } else {
        emptyList()
    }
    val hint = when (page) {
        0 -> name?.let { "已选择 ${it}" } ?: "选择与相机屏幕显示一致的设备"
        1 -> "无需输入，请在相机上确认"
        2 -> "Android 将提示加入相机网络"
        else -> "收到画面后将进入监看"
    }
    val (primary, enabled) = when {
        failure != null -> "重试" to true
        wizard.blocker != null -> wizard.blocker to true
        choosing -> "继续" to (selected != null && !busy)
        else -> null to false
    }
    return MonitorPairingPresentation(
        steps = PairingWizard.titles.zip(PairingWizard.rail) { title, rail -> MonitorPairingStep(title, rail) },
        currentStep = page,
        title = PairingWizard.titles[page],
        body = wizard.bodies[page],
        target = name ?: "尚未选择",
        hint = hint,
        // A blocker means discovery is not running (main's scan never started).
        progress = wizard.label.takeIf { failure == null && (busy || state.scanning && wizard.blocker == null) },
        error = failure?.message,
        devices = if (choosing) {
            state.nearby.map {
                val item = device(it, state, model)
                MonitorPairingDevice(it.id, item.name, item.subtitle, selected = item.isPrimary, busy = busy, signalBars = item.signalBars)
            }
        } else {
            emptyList()
        },
        instructions = instructions,
        checks = checks,
        emptyTitle = if (choosing && state.nearby.isEmpty() && wizard.blocker == null) {
            if (state.scanning) "正在搜索相机" else "尚未发现相机"
        } else {
            null
        },
        primaryAction = primary,
        primaryActionEnabled = enabled,
        backAction = if (busy) "取消" else null,
    )
}
