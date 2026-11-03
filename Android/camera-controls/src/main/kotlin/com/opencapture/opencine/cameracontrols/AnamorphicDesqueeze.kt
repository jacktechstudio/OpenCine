package com.opencapture.opencine.cameracontrols

import com.opencapture.opencine.media.MonitorAnamorphicDesqueeze
import com.opencapture.opencine.media.MonitorDesqueezeDirection

/** Presentation only: the source pixels, scope samples and delivered files stay unchanged. */
object AnamorphicDesqueeze {
    const val HELP = "在取景和回放时还原变形宽银幕镜头的画面比例。" +
        "水平反挤压拉宽画面，垂直反挤压拉高画面；两种方式均完整显示画幅。" +
        "不影响录像、照片、导出文件和示波器读数。"

    fun snap(value: Double): Double = MonitorAnamorphicDesqueeze.snap(value)
    fun label(value: Double): String = MonitorAnamorphicDesqueeze.label(value)
    fun aspect(sourceAspect: Float, enabled: Boolean, factor: Double, direction: MonitorDesqueezeDirection): Float {
        val source = sourceAspect.takeIf { it.isFinite() && it > 0f } ?: (16f / 9f)
        return requireNotNull(MonitorAnamorphicDesqueeze.aspect(source, enabled, factor, direction))
    }
}
