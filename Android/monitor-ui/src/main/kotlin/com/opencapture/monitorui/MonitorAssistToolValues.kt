package com.opencapture.monitorui

/** Current monitor-assist identities. Sources provide availability and choose their tool ordering. */
enum class MonitorAssistTool {
    LUT,
    PEAK,
    FALSE,
    ZEBRA,
    WAVE,
    PARADE,
    HISTO,
    VECTOR,
    LIGHTS,
    ND,
    EV,
    AUDIO,
    GUIDES,
    GRID,
    CROSS,
    LEVEL,
    DESQ,
    MIRROR,
    ;

    val chipLabel: String
        get() = when (this) {
            PEAK -> "峰值"
            FALSE -> "假色"
            ZEBRA -> "斑马纹"
            WAVE -> "波形"
            PARADE -> "分量"
            HISTO -> "直方图"
            VECTOR -> "矢量"
            LIGHTS -> "剪切"
            AUDIO -> "电平"
            GUIDES -> "框线"
            GRID -> "网格"
            CROSS -> "十字线"
            LEVEL -> "水平仪"
            DESQ -> "还原"
            MIRROR -> "镜像"
            LUT, ND, EV -> name
        }

    val label: String
        get() = name

    /** Settings names, matching the shared Swift catalog. */
    val displaySettingsTitle: String
        get() = when (this) {
            PARADE -> "分量波形"
            ND -> "ND"
            else -> title
        }

    val title: String
        get() =
            when (this) {
                LUT -> "LUT"
                PEAK -> "峰值对焦"
                FALSE -> "假色"
                ZEBRA -> "斑马纹"
                WAVE -> "波形图"
                PARADE -> "RGB 分量波形"
                HISTO -> "直方图"
                VECTOR -> "矢量示波器"
                LIGHTS -> "RGB 剪切指示灯"
                ND -> "ND 滤镜建议"
                EV -> "曝光指示器"
                AUDIO -> "音频电平"
                GUIDES -> "画幅框线"
                GRID -> "构图网格"
                CROSS -> "中心十字线"
                LEVEL -> "电子水平仪"
                DESQ -> "变形宽银幕还原"
                MIRROR -> "镜像"
            }

    /** The fixed camera EV meter and LEVEL are tap-only toggles. */
    val hasConfiguration: Boolean
        get() =
            when (this) {
                EV, LEVEL -> false
                else -> true
            }

    companion object {
        val toolbarGroups: List<List<MonitorAssistTool>> get() =
            listOf(
                listOf(LUT, PEAK, FALSE),
                listOf(ZEBRA, WAVE, PARADE),
                listOf(HISTO, VECTOR, LIGHTS, ND, EV),
                listOf(GUIDES, GRID, CROSS, LEVEL, DESQ),
                listOf(MIRROR),
            )

        /** AUDIO is appended as its own trailing section. */
        val toolbarCases: List<MonitorAssistTool> get() = toolbarGroups.flatten()

        /** Camera EV and LEVEL have no clip telemetry; AUDIO rides last like live. */
        val playbackToolbarCases: List<MonitorAssistTool> get() = toolbarCases.filter { it != EV && it != LEVEL } + AUDIO

        /**
         * What a still photo offers, in playback order (Swift `MonitorAssistSelection.photoToolbarCases`).
         * No LUT: the assist cubes convert the camera's log curve and a still is display-referred
         * Rec.709. No ND, AUDIO, LEVEL or EV: a still has no exposure to change, no sound and no attitude.
         */
        val photoToolbarCases: List<MonitorAssistTool> get() = playbackToolbarCases.filter {
            it in setOf(PEAK, FALSE, ZEBRA, WAVE, PARADE, HISTO, VECTOR, LIGHTS, GUIDES, GRID, CROSS, DESQ, MIRROR)
        }

        /** Photo tools that remake the still's pixels; a page offers them only with a still look renderer. */
        val photoPictureTools: Set<MonitorAssistTool> get() = setOf(PEAK, FALSE, ZEBRA)

        val settingsCases: List<MonitorAssistTool> get() = toolbarCases + AUDIO

        val cleanPinCases: List<MonitorAssistTool> get() = settingsCases.filter { it != EV }

        fun fromPersisted(raw: String): MonitorAssistTool? =
            if (raw == "DE-SQ") DESQ else entries.firstOrNull { it.name == raw || it.chipLabel == raw }
    }
}
