package com.opencapture.opencine.appkit

import androidx.compose.runtime.Composable
import com.opencapture.monitorui.MonitorAssistTool
import com.opencapture.monitorui.settings.MonitorDisplaySectionStyle
import com.opencapture.monitorui.settings.MonitorDisplaySections
import com.opencapture.monitorui.settings.MonitorDisplayToggle
import com.opencapture.monitorui.settings.MonitorDisplaySection as DisplaySection

/** Common display composition over existing, app-namespaced preference values, one section per
 * DISP mode the source offers. */
@Composable
public fun MonitorOperatorDisplaySections(
    modes: List<MonitorDisplayMode>,
    chrome: (MonitorDisplayMode) -> MonitorDisplayChrome,
    assist: MonitorAssistState,
    onChange: (MonitorDisplayMode, MonitorDisplayChrome) -> Unit,
    available: Set<MonitorDisplaySection> = MonitorDisplaySection.entries.toSet(),
    pinnable: Set<MonitorAssistTool> = MonitorAssistTool.cleanPinCases.toSet(),
    onPinsChanged: ((Set<MonitorAssistTool>) -> Unit)? = null,
    edit: ((MonitorDisplayMode) -> Unit)? = null,
    feedback: () -> Unit = {},
): Unit {
    val order = listOf(MonitorDisplaySection.STATUS_BAR, MonitorDisplaySection.TOOL_BAR,
        MonitorDisplaySection.CAMERA_VALUES, MonitorDisplaySection.LOCK_BUTTON, MonitorDisplaySection.BATTERIES,
        MonitorDisplaySection.REC_READOUT, MonitorDisplaySection.TIMECODE, MonitorDisplaySection.FORMAT,
        MonitorDisplaySection.CODEC, MonitorDisplaySection.COLOR, MonitorDisplaySection.STORAGE, MonitorDisplaySection.FPS,
        MonitorDisplaySection.RAIL_RECORD, MonitorDisplaySection.RAIL_MEDIA, MonitorDisplaySection.RAIL_SETTINGS,
        MonitorDisplaySection.ZOOM_CHIP, MonitorDisplaySection.GIMBAL_STICK, MonitorDisplaySection.FOCUS_BOX)
    MonitorDisplaySections(modes.map { mode ->
        val value = chrome(mode)
        DisplaySection(id = mode.name, title = mode.settingsTitle, caption = mode.settingsCaption,
            reset = {
                feedback()
                onChange(mode, MonitorDisplayChrome.defaults(mode))
            }, editHint = "直接在监看界面布置工具栏、参数和工具。",
            edit = edit?.let { action -> { action(mode) } },
            unavailableEditHelp = "连接相机后可在监看界面调整布局。",
            visibility = order.filter { it in available && MonitorDisplayChrome.isConfigurable(it, mode) }.map { section ->
                MonitorDisplayToggle(id = mode.name + "." + section.name,
                    title = if (section == MonitorDisplaySection.FOCUS_BOX) "自动对焦框" else section.title,
                    isOn = value.isVisible(section), help = displayHelp(section)) {
                    feedback(); onChange(mode, value.toggling(section))
                }
            }, pinsTitle = "净画面保留的监看辅助",
            pins = if (mode != MonitorDisplayMode.CLEAN) emptyList() else MonitorAssistTool.cleanPinCases.filter { it in pinnable }.map { tool ->
                MonitorDisplayToggle(id = "clean.pin." + tool.name, title = tool.title,
                    isOn = tool in assist.pinned, accessibilityLabel = "在净画面保留${tool.title}") {
                    feedback()
                    if (onPinsChanged == null) assist.togglePin(tool)
                    else {
                        val next = if (tool in assist.pinned) assist.pinned - tool else assist.pinned + tool
                        assist.pinned = next; onPinsChanged(next)
                    }
                }
            }, style = MonitorDisplaySectionStyle.CONTROLS)
    })
}

private fun displayHelp(section: MonitorDisplaySection): String = when (section) {
    MonitorDisplaySection.STATUS_BAR -> "画面顶部的录制状态、时间码、格式和帧率。"
    MonitorDisplaySection.TOOL_BAR -> "取景画面下方的监看辅助工具栏。"
    MonitorDisplaySection.CAMERA_VALUES -> "感光度、快门、白平衡等拍摄参数。"
    MonitorDisplaySection.LOCK_BUTTON -> "侧栏锁定键。锁定界面时始终显示。"
    MonitorDisplaySection.BATTERIES -> "手机与相机电量。"
    MonitorDisplaySection.REC_READOUT -> "状态栏的待机或录制指示。"
    MonitorDisplaySection.TIMECODE -> "状态栏的运行时间码。"
    MonitorDisplaySection.FORMAT -> "录制分辨率与帧率。"
    MonitorDisplaySection.CODEC -> "状态栏的录制编码。"
    MonitorDisplaySection.COLOR -> "状态栏的色彩模式。"
    MonitorDisplaySection.STORAGE -> "状态栏的剩余可录时长。"
    MonitorDisplaySection.FPS -> "取景帧率与图传质量。"
    MonitorDisplaySection.RAIL_RECORD -> "侧栏录制指示灯。录制时保持可用。"
    MonitorDisplaySection.RAIL_MEDIA -> "侧栏素材按钮。"
    MonitorDisplaySection.RAIL_SETTINGS -> "侧栏设置按钮，始终可用。"
    MonitorDisplaySection.ZOOM_CHIP -> "取景画面上的实时变焦倍率。"
    MonitorDisplaySection.GIMBAL_STICK -> "屏幕云台摇杆。"
    MonitorDisplaySection.FOCUS_BOX -> "取景画面上的对焦框和人脸跟踪框。"
}
