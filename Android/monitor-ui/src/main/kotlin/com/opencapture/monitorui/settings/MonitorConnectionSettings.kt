package com.opencapture.monitorui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key

/** Source-interpreted connection facts; no transport or credential storage ownership. */
@Composable
fun MonitorConnectionSettings(
    transport: String, transportHelp: String, phase: String, phaseHelp: String,
    network: String? = null, networkHelp: String = "",
) {
    MonitorSettingsRowCard(title = "连接") {
        MonitorSettingsInlineRow(title = "当前连接方式", help = transportHelp, showTopDivider = false) {
            MonitorSettingsValueText(transport)
        }
        MonitorSettingsInlineRow(title = "连接阶段", help = phaseHelp) { MonitorSettingsValueText(phase) }
        if (network != null) {
            MonitorSettingsInlineRow(title = "相机 Wi-Fi", help = networkHelp) { MonitorSettingsValueText(network) }
        }
    }
}

data class MonitorSavedCameraSummary(val id: String, val name: String, val detail: String, val network: String? = null)

/** Read-only summaries; pairing remains the host's cameras flow. */
@Composable
fun MonitorSavedCameraSettings(cameras: List<MonitorSavedCameraSummary>, emptyHelp: String) {
    MonitorSettingsRowCard(title = "我的相机") {
        if (cameras.isEmpty()) {
            MonitorSettingsInlineRow(title = "已保存", help = emptyHelp, showTopDivider = false) {
                MonitorSettingsValueText("无")
            }
        } else {
            cameras.forEachIndexed { index, camera ->
                key(camera.id) {
                    MonitorSettingsInlineRow(title = camera.name, help = camera.detail, showTopDivider = index > 0) {
                        MonitorSettingsValueText(camera.network ?: "已保存")
                    }
                }
            }
        }
    }
}
