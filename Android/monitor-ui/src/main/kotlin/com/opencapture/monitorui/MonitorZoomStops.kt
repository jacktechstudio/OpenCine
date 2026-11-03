package com.opencapture.monitorui

import kotlin.math.abs

/** Shortcut groups come from adapter-supported stops; this policy never invents zoom values. */
data class MonitorZoomTapStops(val singleTap: List<Double>, val doubleTap: List<Double>) {
    val primary: List<Double> get() = singleTap
    val secondary: List<Double> get() = doubleTap

    fun next(from: Double, extended: Boolean = false): Double? {
        val stops = if (extended) doubleTap else singleTap
        val first = stops.firstOrNull() ?: return null
        if (!from.isFinite()) return first
        return stops.firstOrNull { it > from + 0.05 } ?: first
    }

    companion object {
        fun from(supported: List<Double>, extended: List<Double> = emptyList()): MonitorZoomTapStops {
            val stops = supported.filter { it.isFinite() && it > 0.0 }.distinct().sorted()
            val extendedStops = stops.filter { it in extended }
            return MonitorZoomTapStops(
                singleTap = stops.filter { it !in extendedStops },
                doubleTap = extendedStops,
            )
        }

        fun from(supported: List<Double>, separateDigital: Boolean): MonitorZoomTapStops =
            from(supported, if (separateDigital) listOf(6.0, 12.0) else emptyList())
    }
}

object MonitorZoomCaption {
    fun label(factor: Double, opticalStops: List<Double>): String {
        if (!factor.isFinite()) return "广角"
        if (abs(factor - 1.0) < 0.05) return "广角"
        if (opticalStops.any { abs(it - 3.0) < 0.01 }) {
            if (abs(factor - 3.0) < 0.05) return "长焦"
            return if (factor > 3.0) "数码变焦 · 画质下降" else "广角裁切"
        }
        return "数码裁切"
    }

    /** Past the last optical stop the picture is a digital crop (detail loss). */
    fun isDigital(factor: Double, opticalStops: List<Double>): Boolean {
        if (!factor.isFinite()) return false
        val opticalMax = opticalStops.filter { it.isFinite() && it > 0.0 }.maxOrNull() ?: 1.0
        return factor > opticalMax + 0.02
    }
}
