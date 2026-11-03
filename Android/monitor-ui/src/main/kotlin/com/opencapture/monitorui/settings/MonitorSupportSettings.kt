package com.opencapture.monitorui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/** Optional queued report values from an app-owned reporting workflow. */
public data class MonitorReportStatus(val label: String, val detail: String, val id: String? = null)

/** A focused action from the host's real support workflow. */
public data class MonitorSupportAction(
    val id: String, val title: String, val help: String? = null, val actionTitle: String,
    val accessibilityLabel: String, val enabled: Boolean = true, val action: () -> Unit,
)

@Composable
public fun MonitorSupportSettings(
    automaticReports: Boolean? = null, available: Boolean = false, automaticHelp: String = "", unavailableHelp: String = "",
    onAutomaticReports: ((Boolean) -> Unit)? = null, report: () -> Unit, privacy: (() -> Unit)? = null,
    status: MonitorReportStatus? = null, discard: (() -> Unit)? = null, feedback: () -> Unit = {},
    reportTitle: String = "反馈问题",
    reportHelp: String = "请描述遇到的问题。可选择附上技术信息，或留下电子邮箱以便回复。",
    reportActionTitle: String = "打开", actions: List<MonitorSupportAction> = emptyList(),
) {
    MonitorSettingsRowCard(title = "帮助与反馈") {
        MonitorSettingsInlineRow(reportTitle, reportHelp, showTopDivider = false) {
            MonitorSettingsActionPill(reportActionTitle, onClick = report,
                modifier = Modifier.testTag("support.report.open").semantics { contentDescription = reportTitle })
        }
        actions.forEach { action ->
            MonitorSettingsInlineRow(action.title, action.help) {
                MonitorSettingsActionPill(action.actionTitle, enabled = action.enabled, onClick = action.action,
                    modifier = Modifier.testTag(action.id).semantics { contentDescription = action.accessibilityLabel })
            }
        }
        if (status != null) {
            MonitorSettingsInlineRow("报告状态", status.detail) { MonitorSettingsValueText(status.label) }
            status.id?.let { id ->
                MonitorSettingsInlineRow("报告编号", "后续查询时请提供此编号。") { MonitorSettingsValueText(id) }
            }
            if (discard != null) {
                MonitorSettingsInlineRow("待发送的报告", "从本机移除尚未发送的报告。") {
                    MonitorSettingsActionPill("丢弃", onClick = discard)
                }
            }
        }
        if (automaticReports != null) {
            if (available && onAutomaticReports != null) {
                MonitorSettingsSwitchInlineRow("自动错误报告", automaticReports, automaticHelp) {
                    feedback(); onAutomaticReports(!automaticReports)
                }
            } else {
                MonitorSettingsInlineRow("自动错误报告", unavailableHelp) { MonitorSettingsValueText("关") }
            }
        }
        privacy?.let { action ->
            MonitorSettingsInlineRow("诊断报告隐私说明", "了解报告内容、保留期限和申请删除的方式。") {
                MonitorSettingsActionPill("信号源", onClick = action)
            }
        }
    }
}

@Composable
public fun MonitorDiagnosticSettings(
    expanded: Boolean, onExpanded: (Boolean) -> Unit, deleteHelp: String,
    save: () -> Unit, delete: () -> Unit,
) {
    MonitorSettingsGroupCard(title = "诊断选项", caption = "保存或移除本机的诊断报告",
        expanded = expanded, onExpandToggle = { onExpanded(!expanded) }) {
        MonitorSettingsInlineRow("保存诊断报告", "保存副本或分享给技术支持。",
            showTopDivider = false) { MonitorSettingsActionPill("保存", onClick = save) }
        MonitorSettingsInlineRow("删除已保存的图传报告", deleteHelp) {
            MonitorSettingsActionPill("删除", onClick = delete)
        }
    }
}
