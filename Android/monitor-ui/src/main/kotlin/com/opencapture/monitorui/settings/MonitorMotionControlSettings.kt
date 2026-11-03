package com.opencapture.monitorui.settings

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/** Host-owned values and actions, with no transport, preference store or camera dependency. */
data class MonitorMotionIntegerSetting(
    val value: Int, val range: IntRange, val help: String, val suffix: String = "",
    val onChange: (Int) -> Unit,
)

data class MonitorMotionChoiceSetting(
    val options: List<String>, val selected: String, val help: String,
    val onSelect: (String) -> Unit,
)

data class MonitorMotionToggleSetting(
    val value: Boolean, val help: String, val onChange: (Boolean) -> Unit,
)

@Composable
fun MonitorGimbalSettings(
    sensitivity: MonitorMotionIntegerSetting,
    headTracking: MonitorMotionToggleSetting? = null,
) {
    MonitorSettingsRowCard(title = "云台") {
        MonitorSettingsInlineRow(title = "摇杆灵敏度", help = sensitivity.help,
            showTopDivider = false, stacked = true) {
            MonitorSettingsValueSlider(value = sensitivity.value, range = sensitivity.range,
                label = "${sensitivity.value}${sensitivity.suffix}", labelWidth = 24,
                onChange = sensitivity.onChange)
        }
        if (headTracking != null) {
            MonitorSettingsSwitchInlineRow(title = "头部追踪（实验功能）",
                help = headTracking.help, isOn = headTracking.value) {
                headTracking.onChange(!headTracking.value)
            }
        }
    }
}

@Composable
fun MonitorJoystickSettings(
    size: MonitorMotionChoiceSetting,
    invertPan: MonitorMotionToggleSetting,
    invertTilt: MonitorMotionToggleSetting,
    deadZone: MonitorMotionIntegerSetting,
    response: MonitorMotionChoiceSetting,
    identifierPrefix: String = "monitor.joystick",
) {
    MonitorSettingsRowCard(title = "屏幕摇杆") {
        MonitorSettingsInlineRow(title = "大小", help = size.help,
            showTopDivider = false, stacked = true) {
            MonitorSettingsSegmented(options = size.options, selected = size.selected,
                testTag = "$identifierPrefix.size", onSelect = size.onSelect)
        }
        MonitorSettingsSwitchInlineRow(title = "反转水平转向", help = invertPan.help,
            isOn = invertPan.value, testTag = "$identifierPrefix.invertPan") {
            invertPan.onChange(!invertPan.value)
        }
        MonitorSettingsSwitchInlineRow(title = "反转俯仰方向", help = invertTilt.help,
            isOn = invertTilt.value, testTag = "$identifierPrefix.invertTilt") {
            invertTilt.onChange(!invertTilt.value)
        }
        MonitorSettingsInlineRow(title = "摇杆死区", help = deadZone.help, stacked = true) {
            Box(Modifier.testTag("$identifierPrefix.deadzone")) {
                MonitorSettingsValueSlider(value = deadZone.value, range = deadZone.range,
                    label = "${deadZone.value}${deadZone.suffix}", onChange = deadZone.onChange)
            }
        }
        MonitorSettingsInlineRow(title = "响应曲线", help = response.help, stacked = true) {
            MonitorSettingsSegmented(options = response.options, selected = response.selected,
                testTag = "$identifierPrefix.response", onSelect = response.onSelect)
        }
    }
}

@Composable
fun MonitorControllerSettings(stick: MonitorMotionChoiceSetting, status: String, statusHelp: String) {
    MonitorSettingsRowCard(title = "控制器") {
        MonitorSettingsInlineRow(title = "云台摇杆", help = stick.help,
            showTopDivider = false, stacked = true) {
            MonitorSettingsSegmented(options = stick.options, selected = stick.selected,
                onSelect = stick.onSelect)
        }
        MonitorSettingsInlineRow(title = "游戏手柄", help = statusHelp) {
            MonitorSettingsValueText(status)
        }
    }
}
