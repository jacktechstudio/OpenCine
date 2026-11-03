package com.opencapture.monitorui.settings

import androidx.compose.runtime.Composable

/** Common touch safety choices with storage and optional domain copy supplied by the host. */
@Composable
public fun MonitorOperatorSafetySettings(
    recordConfirmation: Boolean, haptics: Boolean, keepScreenAwake: Boolean,
    onRecordConfirmation: (Boolean) -> Unit, onHaptics: (Boolean) -> Unit,
    onKeepScreenAwake: (Boolean) -> Unit,
    hapticsHelp: String = "操作开关、设置或控件时提供短振反馈。",
    keepScreenAwakeHelp: String = "使用应用时保持屏幕常亮，防止监看过程中自动锁屏。",
    feedback: () -> Unit = {},
) {
    MonitorSettingsRowCard(title = "触控与防误触") {
        MonitorSettingsSwitchInlineRow(
            title = "录制前确认", help = "开始或停止录制前要求确认，防止误触。",
            showTopDivider = false, isOn = recordConfirmation,
        ) { feedback(); onRecordConfirmation(!recordConfirmation) }
        MonitorSettingsSwitchInlineRow(title = "触感反馈", help = hapticsHelp, isOn = haptics) {
            feedback(); onHaptics(!haptics)
        }
        MonitorSettingsSwitchInlineRow(title = "保持屏幕常亮", help = keepScreenAwakeHelp, isOn = keepScreenAwake) {
            feedback(); onKeepScreenAwake(!keepScreenAwake)
        }
    }
}

/** A display preference, independently of camera capture settings. */
@Composable
public fun MonitorPanelSettings(hdrDisplay: Boolean, help: String,
    onChange: (Boolean) -> Unit, feedback: () -> Unit = {}) {
    MonitorSettingsRowCard(title = "显示面板") {
        MonitorSettingsSwitchInlineRow(title = "HDR 显示", help = help, showTopDivider = false, isOn = hdrDisplay) {
            feedback(); onChange(!hdrDisplay)
        }
    }
}
