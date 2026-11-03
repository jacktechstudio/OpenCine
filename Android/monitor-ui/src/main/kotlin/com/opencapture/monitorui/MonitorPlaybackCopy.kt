package com.opencapture.monitorui

import com.opencapture.opencine.media.MonitorConformPreview

/**
 * What the shared playback screen says to the operator and to assistive technologies: control
 * labels, their states and hints, the clip information sheet and the deletion confirmation.
 *
 * The Compose twin of `MonitorPlaybackCopy` (`Sources/MonitorPresentation/MonitorPlaybackCopy.swift`).
 * Nothing links the two, so `Fixtures/playback-copy.tsv` lists every string and both platforms'
 * tests read it (`MonitorPlaybackCopyVectorTests`, `MonitorPlaybackCopyVectorTest`): a label
 * edited on one side only fails a test.
 *
 * One accepted platform difference: [HIDE_CONTROLS_HINT] is read only on iOS, as the hide
 * control's accessibility hint. Compose has no hint semantics; TalkBack builds its own usage hint
 * from the click action, so Android carries the string but does not show it.
 */
object MonitorPlaybackCopy {
    // Header chrome.
    const val BACK_TO_MEDIA = "返回素材库"
    const val FAVORITE = "收藏素材"
    const val CLIP_INFORMATION = "素材信息"
    const val SHARE = "分享素材"
    const val SHARE_TITLE = "分享"
    const val DELETE = "从相机删除素材"

    // Clip information sheet.
    const val CLIP_INFORMATION_TITLE = "素材信息"
    const val CLOSE_CLIP_INFORMATION = "关闭素材信息"

    // Deletion confirmation, when the host supplies no captured copy of its own.
    const val DELETE_CONFIRMATION_TITLE = "要从相机存储卡删除这段素材吗？"
    const val DELETE_CONFIRM = "删除"
    const val DELETE_CANCEL = "取消"
    const val DELETE_CANCEL_DESCRIPTION = "取消删除"
    const val DELETE_CONFIRM_DESCRIPTION = "从相机存储卡删除"

    // Transport.
    const val BACK_FIFTEEN = "快退 15 秒"
    const val FORWARD_FIFTEEN = "快进 15 秒"
    const val PLAY = "播放"
    const val PAUSE = "暂停"
    const val RESTART = "从头播放"
    const val PREVIOUS_CLIP = "上一段"
    const val NEXT_CLIP = "下一段"

    // Options.
    const val CONFORM_PREVIEW = "变帧率回放预览"
    const val CLOSE_CONFORM_PREVIEW = "关闭变帧率回放预览"
    const val MUTE = "静音"
    const val UNMUTE = "取消静音"
    const val LOOP = "循环播放"
    const val ON = "开"
    const val OFF = "关"
    const val HIDE_CONTROLS = "隐藏回放控件"
    /** iOS reads it as the hide control's hint; Compose has no hint semantics (TalkBack derives its own). */
    const val HIDE_CONTROLS_HINT = "可轻点角落的按钮恢复显示"
    const val SHOW_CONTROLS = "显示回放控件"

    // Conform preview panel.
    const val CONFORM_TITLE = "变帧率回放预览"
    /** The drum's first choice: play as recorded. */
    const val REAL_TIME = "原速"

    // Scrubber. The gesture well over the picture has no label on either platform: a screen
    // reader cannot act on it (tests find it by [MonitorPlaybackTags.PICTURE]).
    const val POSITION = "回放位置"

    // Rating shade.
    const val SHOW_RATING = "显示星级评分"
    const val HIDE_RATING = "隐藏星级评分"

    /** The one transport button: restart after the end, else pause while playing, else play. */
    fun transport(reachedEnd: Boolean, playing: Boolean): String =
        if (reachedEnd) RESTART else if (playing) PAUSE else PLAY

    fun muteToggle(muted: Boolean): String = if (muted) UNMUTE else MUTE

    /** The state read with a toggle's label (the loop chip). */
    fun toggleValue(on: Boolean): String = if (on) ON else OFF

    /** The scrubber's state: whole seconds, never negative; an unknown position reads 0. */
    fun positionValue(seconds: Double): String {
        val whole = if (seconds.isFinite()) seconds.coerceAtLeast(0.0).toLong() else 0L
        return "$whole 秒"
    }

    /** The conform panel's line under its title: the clip's capture rate, leading into the choices. */
    fun conformSubtitle(captureRate: Double): String = MonitorConformPreview.menuHeader(captureRate)

    fun rating(expanded: Boolean): String = if (expanded) HIDE_RATING else SHOW_RATING

    /** One star choice of the rating control. */
    fun rate(level: Int): String = "评为 $level 星"
}

/** Test tags of shared playback nodes that carry no accessibility label. */
object MonitorPlaybackTags {
    /** The gesture well over the picture (pinch, pan, tap, frame scrub). */
    const val PICTURE = "monitor.playback.picture"
}
