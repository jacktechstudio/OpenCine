package com.opencapture.monitorui

/** How urgently a battery reads, on the live telemetry pills. Swift twin: `MonitorBatteryUrgency`. */
public enum class MonitorBatteryUrgency {
    NOMINAL, LOW, DEPLETED;

    public companion object {
        /**
         * The tint tier of one reading on one scale: a fifth or less (or a reported exhaustion) is
         * depleted, two fifths low, more nominal. A coarse gauge keeps the three tiers its body
         * shows (five bars: three fine, two find a spare, one swap now); a source [warning] reads
         * at least low. Unknown charge has no urgency.
         */
        public fun of(percent: Int?, bars: Int?, barCount: Int, warning: Boolean, exhausted: Boolean): MonitorBatteryUrgency? {
            if (exhausted) return DEPLETED
            val fraction = when {
                bars != null && barCount > 0 && bars in 0..barCount -> bars.toDouble() / barCount
                bars == null && percent != null && percent in 0..100 -> percent / 100.0
                else -> return if (warning) LOW else null
            }
            if (fraction <= 0.2) return DEPLETED
            return if (fraction <= 0.4 || warning) LOW else NOMINAL
        }
    }
}

/** The text of the live telemetry pills, apart from their drawing. Swift twin: `MonitorTelemetryValues`. */
public object MonitorTelemetryValues {
    /**
     * The printed battery value: null while bars draw the reading, a percentage, "EXT" for a body
     * on external power that reports no level, else a dash.
     */
    public fun batteryValue(percent: Int?, bars: Int?, charging: Boolean?, externalPower: Boolean?): String? {
        if (bars != null) return null
        if (percent != null) return "$percent%"
        return if (charging == true || externalPower == true) "EXT" else "\u2014"
    }

    /**
     * The spoken battery value after its title, or null when nothing is known: the level, then
     * "exhausted" and the power source when reported.
     */
    public fun batterySpokenValue(percent: Int?, bars: Int?, barCount: Int, charging: Boolean?,
        externalPower: Boolean?, exhausted: Boolean): String? {
        val power = if (externalPower == true) "外接电源" else if (charging == true) "正在充电" else null
        val level = bars?.let { "$it/$barCount 格" } ?: percent?.let { "$it%" }
        val parts = listOfNotNull(level, "电量耗尽".takeIf { exhausted }, power)
        return if (parts.isEmpty()) null else parts.joinToString("，")
    }

    /**
     * The link pill's printed value: null while bars draw the link, else the frame rate or a dash.
     * A state word in place of a rate (NO LINK, RECOV, FAIL, LINK) always shows: bars alone
     * cannot say the picture is held or restarting.
     */
    public fun linkValue(fps: String?, signalBars: Int?, showsRate: Boolean): String? =
        if (fps != null && isStateWord(fps)) stateLabel(fps)
        // Only a finite rate in frame-rate range is rounded; anything else prints as reported.
        else if (showsRate) fps?.toDoubleOrNull()?.takeIf { it.isFinite() && kotlin.math.abs(it) <= MAXIMUM_RATE }
            ?.let { "${Math.round(it)} fps" } ?: fps ?: "\u2014"
        else if (signalBars == null) "\u2014" else null

    /**
     * The link pill's spoken label. A held or failed feed (NO LINK, FAIL) delivers nothing, so it
     * names no delivery bars; any other state word is the feed state, not a frame rate.
     */
    public fun linkSpokenLabel(fps: String?, signalBars: Int?): String {
        val word = fps?.takeIf(::isStateWord)
        val signal = when {
            word in LINK_DOWN_WORDS -> "已断开"
            signalBars != null -> "链路质量 $signalBars/4 格"
            else -> "不可用"
        }
        val feed = when {
            word != null -> stateLabel(word)
            fps == null || fps == "\u2014" -> "不可用"
            else -> "每秒 $fps 帧"
        }
        return "监看链路：$signal，图传：$feed"
    }

    /** The held-frame badge (`SessionRecoveryCopy.HELD_FRAME_BADGE`) and the failed-link word. */
    private val LINK_DOWN_WORDS = setOf("NO LINK", "FAIL", "图传中断", "失败")

    /** Keep wire and diagnostic markers stable while the operator sees a readable state. */
    private fun stateLabel(word: String): String = when (word) {
        "NO LINK" -> "图传中断"
        "FAIL" -> "失败"
        "RECOV" -> "恢复中"
        "LINK" -> "图传"
        "BUSY" -> "忙碌"
        "READY" -> "就绪"
        else -> word
    }

    /** The largest rate printed as a rounded frame rate. */
    internal const val MAXIMUM_RATE: Double = 100_000.0

    /** A link reading that is a word rather than a measured rate or the idle dash. */
    internal fun isStateWord(fps: String): Boolean = fps.isNotEmpty() && fps != "\u2014" && fps.toDoubleOrNull() == null
}
