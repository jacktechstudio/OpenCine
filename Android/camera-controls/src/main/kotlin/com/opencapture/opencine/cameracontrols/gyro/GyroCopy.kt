package com.opencapture.opencine.cameracontrols.gyro

import com.opencapture.opencine.gyro.GyroCalibrationBlock
import com.opencapture.opencine.gyro.GyroCalibrationInvalidation
import com.opencapture.opencine.gyro.GyroNotice
import com.opencapture.opencine.gyro.GyroPillKind
import com.opencapture.opencine.gyro.GyroTakeStatus
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Every user-facing string of the shared gyro screens, the Android twin of the Swift `GyroCopy`
 * table (OpenCineCameraControls): the same member names in camelCase and the same literal texts.
 * `Tests/OpenCineCameraControlsTests/GyroCopyParityTests.swift` reads this file and checks every
 * fixed literal, so the Swift table stays the source of truth.
 *
 * No angle, sample rate or millisecond figure on a primary screen; the sample rate appears only in
 * the take page's Details section.
 */
public object GyroCopy {
    // Pill

    /** The pill's label while gyro is armed between takes. */
    public const val pillArmed: String = "陀螺仪待机"

    /** The pill's label while a take is recording. */
    public const val pillRecording: String = "陀螺仪记录中"

    /** The pill's label while a finished take is still being written. */
    public const val pillSaving: String = "正在保存陀螺仪数据"

    /** The pill's label while the open take has sensor gaps. */
    public const val pillGaps: String = "陀螺仪数据有缺失"

    /** The pill's label when logging failed; tap opens the logs. */
    public const val pillError: String = "陀螺仪错误"

    /** The pill's label while gyro waits for the camera. */
    public const val pillOffline: String = "陀螺仪离线"

    /** The pill's label when the mount moved during the open take. */
    public const val pillMountMoved: String = "安装位置已移动"

    /** The pill's visible label; empty for [GyroPillKind.HIDDEN], which renders nothing. */
    public fun pill(kind: GyroPillKind): String = when (kind) {
        GyroPillKind.HIDDEN -> ""
        GyroPillKind.OFFLINE -> pillOffline
        GyroPillKind.ARMED -> pillArmed
        GyroPillKind.RECORDING -> pillRecording
        GyroPillKind.SAVING -> pillSaving
        GyroPillKind.GAPS -> pillGaps
        GyroPillKind.MOUNT_MOVED -> pillMountMoved
        GyroPillKind.ERROR -> pillError
    }

    /** The pill's TalkBack label; the warning mark appends ". IBIS is still on". */
    public fun pillA11y(kind: GyroPillKind, warning: Boolean): String {
        val base = when (kind) {
            GyroPillKind.HIDDEN -> return ""
            GyroPillKind.ARMED -> "陀螺仪已就绪"
            GyroPillKind.RECORDING -> "正在记录陀螺仪数据"
            GyroPillKind.SAVING -> "正在保存本条陀螺仪数据"
            GyroPillKind.GAPS -> "正在记录陀螺仪数据，存在少量采样缺失"
            GyroPillKind.ERROR -> "陀螺仪错误。打开陀螺仪日志。"
            GyroPillKind.OFFLINE -> "正在等待相机"
            GyroPillKind.MOUNT_MOVED -> "手机安装位置已移动"
        }
        return if (warning) base + "。机身防抖仍已开启" else base
    }

    // Arming sheet

    /** The arming sheet's title. */
    public const val ibisTitle: String = "请关闭机身防抖以使用 Gyroflow"

    /** Why the camera's own stabilization has to be off. */
    public const val ibisReason: String =
        "Gyroflow 无法校正已经过相机防抖处理的画面。"

    /** The lens-side stabilization hint. */
    public const val ibisLens: String = "同时请关闭镜头上的 VR 防抖。"

    /** The arming sheet's confirm button. */
    public const val ibisTurnOff: String = "关闭机身防抖"

    /** The arming sheet's keep-stabilization button. */
    public const val ibisKeep: String = "保持开启"

    // Calibration

    /** The overline above the intro, complete and invalidated mount screens. */
    public const val phoneMount: String = "手机安装方向"

    /** The calibration screen's title. */
    public const val calibrateTitle: String = "校准手机安装方向"

    /** How long calibration takes and what to do with the phone. */
    public const val calibrateBody: String = "约需 20 秒。请将手机牢固安装在相机上。"

    /** The calibration screen's start button. */
    public const val getStarted: String = "开始"

    /** The calibration screen's saved-calibration button. */
    public const val useLastCalibration: String = "使用上次校准"

    /** The shared cancel button. */
    public const val cancel: String = "取消"

    /** The instruction for ring step [step]: 0 level, 1 tilt up, 2 tilt and lean. */
    public fun step(step: Int): String = when (step) {
        0 -> "保持相机水平"
        1 -> "缓慢向上抬起镜头"
        else -> "抬起镜头并向一侧倾斜"
    }

    /** The step counter, for example "Step 2 of 3". */
    public fun stepCounter(n: Int): String = "第 " + n + "/3 步"

    /** The hint under the filling ring. */
    public const val holdHint: String = "保持静止，直至圆环填满"

    /** Why the ring cannot accept a reading on the current step. */
    public fun block(reason: GyroCalibrationBlock): String = when (reason) {
        GyroCalibrationBlock.CAMERA_DISCONNECTED -> "连接相机以进行校准"
        GyroCalibrationBlock.RECORDING -> "停止录制后再进行校准"
        GyroCalibrationBlock.MOVING -> "请保持稳定"
        GyroCalibrationBlock.WAITING_FOR_CAMERA -> "正在等待相机"
    }

    /** Why the ring did not line up, on the retry screen. */
    public const val retry: String = "方向不一致，请再试一次。"

    /** Why an in-progress ring calibration was discarded. */
    public fun invalidation(reason: GyroCalibrationInvalidation): String = when (reason) {
        GyroCalibrationInvalidation.CAMERA_CHANGED -> "相机已变更，请重新开始。"
        GyroCalibrationInvalidation.RECORDING_STARTED ->
            "已开始录制。请在停止录制后重新校准。"
        GyroCalibrationInvalidation.CONNECTION_ENDED ->
            "相机已断开。请在重新连接后校准。"
    }

    /** The completion screen's title. */
    public const val completeTitle: String = "校准完成"

    /** The completion screen's body. */
    public const val completeBody: String = "陀螺仪数据已与此相机对齐。"

    /** The completion screen's button. */
    public const val done: String = "完成"

    /** The ring's hold progress, for example "Holding, 40 percent". */
    public fun holdProgressA11y(percent: Int): String = "请保持，" + percent + "%"

    /** One finished step, for example "Step 1 done". */
    public fun stepDoneA11y(n: Int): String = "第 " + n + "已完成"

    // Drift

    /** The drift popup's title. */
    public const val driftTitle: String = "拍摄期间手机的安装位置发生了移动。"

    /** The drift popup's question. */
    public const val driftQuestion: String = "现在重新校准？"

    /** The drift popup's confirm button. */
    public const val recalibrate: String = "重新校准"

    /** The drift popup's dismiss button. */
    public const val notNow: String = "暂不开启"

    // Logs

    /** The gyro logs screen's title. */
    public const val logsTitle: String = "陀螺仪日志"

    /** What to do with a shared log. */
    public const val tip: String =
        "将日志发送至电脑，在 Gyroflow 中与对应视频一起打开，然后选择镜头。"

    /** The VoiceOver and TalkBack label of the tip's close button. */
    public const val dismissTip: String = "关闭提示"

    /** The Today section title. */
    public const val today: String = "今天"

    /** The Yesterday section title. */
    public const val yesterday: String = "昨天"

    /** The selection mode's button. */
    public const val select: String = "选择"

    /** The selection button, for example "Share (2)". */
    public fun shareCount(n: Int): String = "分享（" + n + ")"

    /** One row's share button. */
    public const val shareLog: String = "分享日志"

    /** The separator between the parts of a row or take subtitle (U+00B7 with spaces). */
    public const val separator: String = " · "

    /** The one status badge of a take row. */
    public fun status(s: GyroTakeStatus): String = when (s) {
        GyroTakeStatus.Recording -> "REC"
        is GyroTakeStatus.MountMoved -> "安装位置移动于 " + length(s.offsetSeconds)
        GyroTakeStatus.CutShort -> "提前结束"
        GyroTakeStatus.SmallGaps -> "少量数据缺失"
        GyroTakeStatus.NotCalibrated -> "未校准"
        GyroTakeStatus.Ready -> "就绪"
    }

    /** The take page's verdict line. */
    public fun verdict(s: GyroTakeStatus): String = when (s) {
        GyroTakeStatus.Recording -> "仍在录制"
        GyroTakeStatus.Ready -> "可用于 Gyroflow"
        else -> status(s)
    }

    /** The delete confirmation's title. */
    public const val deleteTitle: String = "删除此日志？"

    /** The delete confirmation's button. */
    public const val delete: String = "删除"

    /** The empty logs screen's title. */
    public const val emptyTitle: String = "暂无日志"

    /** The empty logs screen's body. */
    public const val emptyBody: String =
        "开启陀螺仪后开始录制。每条镜头均会保存一份可用 Gyroflow 打开的日志。"

    /** The empty logs screen's button. */
    public const val turnOnGyro: String = "开启陀螺仪"

    /** A take row's calibration badge when a verified mount is stored. */
    public const val calibratedMount: String = "已校准安装方向"

    /** A take row's calibration badge when no verified mount is stored. */
    public const val notCalibrated: String = "未校准"

    // Take details

    /** The Details section's title. */
    public const val details: String = "详情"

    /** The start offset row's label. */
    public const val startsLabel: String = "开始"

    /** The smoothness row's label. */
    public const val smoothnessLabel: String = "平滑度"

    /** The sample rate row's label. */
    public const val sampleRateLabel: String = "采样率"

    /** How far the log starts before (positive) or after (negative) REC, to two decimals. */
    public fun startOffset(seconds: Double): String {
        val value = String.format(Locale.ROOT, "%.2f", abs(seconds))
        return if (seconds >= 0) value + "秒（起录前）" else value + "秒（起录后）"
    }

    /** The Details smoothness row. A recording take has no verdict yet. */
    public fun smoothness(s: GyroTakeStatus): String = when (s) {
        GyroTakeStatus.Ready, GyroTakeStatus.NotCalibrated, is GyroTakeStatus.MountMoved -> "无采样缺失"
        GyroTakeStatus.SmallGaps -> "少量数据缺失"
        GyroTakeStatus.CutShort -> "提前结束"
        GyroTakeStatus.Recording -> "仍在录制"
    }

    /** The Details sample rate, rounded to whole hertz, for example "200 Hz". */
    public fun sampleRate(hz: Double): String = (if (hz.isFinite()) hz.roundToInt() else 0).toString() + " Hz"

    /**
     * `m:ss`: whole seconds (truncated), minutes unbounded, seconds zero-padded; negative and
     * non-finite values read as zero.
     */
    public fun length(seconds: Double): String {
        val total = if (seconds.isFinite()) maxOf(0, seconds.toInt()) else 0
        val secs = total % 60
        return (total / 60).toString() + ":" + (if (secs < 10) "0" else "") + secs
    }

    // Notices

    /** The notice line for a capture problem. */
    public fun notice(n: GyroNotice): String = when (n) {
        GyroNotice.TURN_OFF_FAILED -> "相机未接受更改，机身防抖仍已开启。"
        GyroNotice.RESTORE_FAILED -> "无法重新开启机身防抖，请在相机上检查。"
        GyroNotice.SENSOR_UNAVAILABLE -> "此手机的陀螺仪不可用。"
        GyroNotice.SENSOR_STALLED -> "传感器不再回报数据，已停止记录。"
        GyroNotice.SAMPLES_LOST -> "采样丢失，已停止记录陀螺仪数据。"
        GyroNotice.RECORDING_EVENTS_LOST ->
            "录制事件未能及时保存，已停止记录陀螺仪数据。"
        GyroNotice.SAVE_FAILED -> "无法保存陀螺仪日志。"
        GyroNotice.OPEN_FAILED -> "无法打开陀螺仪日志。"
        GyroNotice.SHARE_FAILED -> "此日志暂时无法分享。"
        GyroNotice.DELETE_FAILED -> "无法删除陀螺仪日志。"
        GyroNotice.RECOVERY_INCOMPLETE ->
            "部分中断的日志无法恢复。已保存的日志仍然保留。"
    }

    // Settings

    /** The gyro settings row that opens calibration. */
    public const val settingsCalibrate: String = "校准手机安装方向"
}
