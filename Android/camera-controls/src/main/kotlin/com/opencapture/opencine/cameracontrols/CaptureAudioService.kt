package com.opencapture.opencine.cameracontrols

import com.opencapture.opencine.core.CameraSessionID
import com.opencapture.opencine.core.ControlEpoch

/** A coherent optional audio domain, outside the common capture command entry. */
enum class CaptureAudioControl { CHANNEL, WIND, DIRECTION, VOCAL }
enum class CaptureAudioChannel(val raw: Int, val label: String) { STEREO(2, "立体声"), MONO(1, "单声道"), SPATIAL(3, "空间音频") }
enum class CaptureAudioDirection(val raw: Int, val label: String) { ALL(0, "全向"), FRONT(1, "前方"), FRONT_AND_BACK(2, "前后双向") }
sealed interface CaptureAudioValue {
    val label: String
    data class Channel(val value: CaptureAudioChannel) : CaptureAudioValue { override val label get() = value.label }
    data class Wind(val enabled: Boolean) : CaptureAudioValue { override val label get() = if (enabled) "开" else "关" }
    data class Direction(val value: CaptureAudioDirection) : CaptureAudioValue { override val label get() = value.label }
    data class Vocal(val enabled: Boolean) : CaptureAudioValue { override val label get() = if (enabled) "开" else "关" }
}
data class CaptureAudioField(val id: CaptureAudioControl, val title: String,
    val choices: List<CaptureAudioValue>, val observed: CaptureAudioValue? = null, val isEditable: Boolean = true)
enum class CaptureAudioCommitResult { SUBMITTED, UNAVAILABLE, REFUSED }
data class CaptureAudioSourceID(val session: CameraSessionID, val epoch: ControlEpoch)
class CaptureAudioService(val sourceID: CaptureAudioSourceID, val fields: List<CaptureAudioField>,
    val commit: (CaptureAudioValue) -> CaptureAudioCommitResult, val refresh: suspend () -> Unit)
