package com.opencapture.opencine.cameracontrols


enum class LiveSheet {
    ISO,
    SHUTTER,
    WB,
    FOCUS,
    /** Action 6 aperture strategy, in the FOCUS slot. */
    APERTURE,
    /** Capture-bar MODE: expo Auto/Manual. Not shooting Video/Photo. */
    EXPO,
    AUDIO,
    COLOR,
    FORMAT,
    /** Top-deck shooting Video/Photo/… Not capture-bar EXPO Auto/Manual. */
    MODE,
}

val LiveSheet.isRecordingSetup: Boolean
    get() = this == LiveSheet.FORMAT || this == LiveSheet.COLOR

val LiveSheet.isTopAnchored: Boolean
    get() = isRecordingSetup || this == LiveSheet.MODE

/** Lower ISO/WB/… stay visible for top FORMAT/COLOR/MODE tap or hold. */
fun hidesLowerCaptureValues(
    sheet: LiveSheet?,
    stripQuick: Boolean,
    topQuick: Boolean,
): Boolean {
    if (topQuick) return false
    if (sheet?.isTopAnchored == true) return false
    return sheet != null || stripQuick
}

val LiveSheet.headerLabel: String
    get() =
        when (this) {
            LiveSheet.ISO -> "ISO"
            LiveSheet.SHUTTER -> "快门"
            LiveSheet.WB -> "白平衡"
            LiveSheet.FOCUS -> "对焦"
            LiveSheet.APERTURE -> "光圈"
            LiveSheet.EXPO -> "模式"
            LiveSheet.AUDIO -> "音频"
            LiveSheet.COLOR -> "色彩"
            LiveSheet.FORMAT -> "分辨率"
            LiveSheet.MODE -> "拍摄模式"
        }

val LiveSheet.subtitle: String
    get() =
        when (this) {
            LiveSheet.ISO -> "感光度"
            LiveSheet.SHUTTER -> "快门角度 · 快门速度"
            LiveSheet.WB -> "色温 · 自动 · 色调"
            LiveSheet.FOCUS -> "AF-S / AF-C"
            LiveSheet.APERTURE -> "光圈控制方式"
            LiveSheet.EXPO -> "曝光"
            LiveSheet.AUDIO -> "声道 · 风噪抑制 · 收音指向 · 人声"
            LiveSheet.COLOR -> "色彩模式"
            LiveSheet.FORMAT -> "帧率"
            LiveSheet.MODE -> "拍摄模式"
        }
