package com.opencapture.opencine.vocabulary

enum class GimbalMode(val label: String) {
    FOLLOW("跟随"),
    TILT_LOCKED("俯仰锁定"),
    FPV("FPV"),
    DIRECTION_LOCK("方向锁定"),
    ;

    /** The session's own name for this mode (for example Roll Lock or FPV-V). */
    fun label(profile: CameraLabelProfile): String = profile.gimbalModeLabels[this] ?: label

    companion object {
        val pickerOrder = listOf(FOLLOW, TILT_LOCKED, FPV, DIRECTION_LOCK)
    }
}

enum class GimbalSpeed(val wire: Int, val label: String) {
    FAST(0, "快速"),
    DEFAULT(1, "默认"),
    SLOW(2, "慢速"),
    ;

    companion object {
        val pickerOrder = listOf(SLOW, DEFAULT, FAST)

        fun fromWire(value: Int): GimbalSpeed? = entries.firstOrNull { it.wire == value }
    }
}

enum class GimbalRamp(val raw: Int, val label: String, val tau: Double) {
    OFF(0, "关", 0.0),
    SOFT(1, "柔和", 0.35),
    MEDIUM(2, "中等", 0.18),
    ;

    companion object {
        val pickerOrder = listOf(OFF, SOFT, MEDIUM)

        fun fromRaw(value: Int): GimbalRamp = entries.firstOrNull { it.raw == value } ?: OFF
    }
}

/** Stick double-tap and gamepad Circle/B: body recenter or a world-level snap. */
enum class GimbalDoubleTap(val raw: Int, val label: String) {
    RECENTER(0, "云台回中"),
    LEVEL(1, "水平校正"),
    ;

    companion object {
        val pickerOrder = listOf(RECENTER, LEVEL)

        fun fromRaw(value: Int): GimbalDoubleTap = entries.firstOrNull { it.raw == value } ?: RECENTER
    }
}

object GimbalHudCopy {
    const val POSE_NOT_READY = "云台姿态尚未就绪"
    const val NEED_AB = "请先设置 A、B 两个点位"
    const val HOLD_STILL = "请保持云台静止"
    const val TITLE = "云台"
    const val MODE = "模式"
    const val SPEED = "速度"
    const val RAMP = "缓启缓停"
    const val PROGRAMMED = "运动控制"
    const val RUN = "开始"
    const val STOP = "停止"
}
