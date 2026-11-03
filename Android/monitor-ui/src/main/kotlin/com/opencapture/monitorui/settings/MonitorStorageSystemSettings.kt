package com.opencapture.monitorui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.opencapture.monitorui.MonitorPalette

/** The host supplies cache scope, current IO state and actions. */
@Composable
public fun MonitorCacheSettings(
    cacheOriginals: Boolean? = null, sizeLabel: String, clearing: Boolean = false,
    cacheHelp: String = "", scopeHelp: String, clearHelp: String,
    onCacheOriginals: ((Boolean) -> Unit)? = null, clear: () -> Unit, feedback: () -> Unit = {},
) {
    MonitorSettingsRowCard {
        if (cacheOriginals != null && onCacheOriginals != null) {
            MonitorSettingsSwitchInlineRow(
                title = "缓存原始分辨率素材", help = cacheHelp,
                showTopDivider = false, isOn = cacheOriginals,
            ) { feedback(); onCacheOriginals(!cacheOriginals) }
        }
        MonitorSettingsInlineRow("本地素材缓存", scopeHelp,
            showTopDivider = cacheOriginals != null && onCacheOriginals != null) {
            MonitorSettingsValueText(sizeLabel)
        }
        MonitorSettingsInlineRow("清除缓存", clearHelp) {
            if (clearing) MonitorSettingsValueText("正在清除…")
            else MonitorSettingsTextAction("清除", color = MonitorPalette.recording, onClick = clear)
        }
    }
}

/** App identity and protocol descriptions are supplied as content. */
@Composable
public fun MonitorAppInformationSettings(
    theme: String, protocolName: String, version: String,
    themeHelp: String, protocolHelp: String, versionHelp: String,
) {
    MonitorSettingsRowCard(title = "应用信息") {
        MonitorSettingsInlineRow("主题", themeHelp, showTopDivider = false) { MonitorSettingsValueText(theme) }
        MonitorSettingsInlineRow("通信协议实现", protocolHelp) { MonitorSettingsValueText(protocolName) }
        MonitorSettingsInlineRow("应用版本", versionHelp) { MonitorSettingsValueText(version) }
    }
}

/** App-owned document and external navigation actions. */
@Composable
public fun MonitorProjectLegalSettings(
    sourceHelp: String, termsHelp: String,
    source: () -> Unit, privacy: () -> Unit, terms: () -> Unit,
    licenses: (() -> Unit)? = null, notice: (() -> Unit)? = null,
) {
    MonitorSettingsRowCard(title = "项目与法律信息") {
        MonitorSettingsInlineRow("源代码", sourceHelp, showTopDivider = false) {
            MonitorSettingsActionPill("打开", onClick = source,
                modifier = Modifier.semantics { contentDescription = "查看源代码" })
        }
        MonitorSettingsInlineRow("隐私", "查看应用在本机保存的数据。") {
            MonitorSettingsActionPill("打开", onClick = privacy,
                modifier = Modifier.semantics { contentDescription = "查看隐私政策" })
        }
        MonitorSettingsInlineRow("使用条款", termsHelp) {
            MonitorSettingsActionPill("打开", onClick = terms,
                modifier = Modifier.semantics { contentDescription = "查看使用条款" })
        }
        licenses?.let { action ->
            MonitorSettingsInlineRow("许可证", "Apache 2.0 许可证及第三方声明。") {
                MonitorSettingsActionPill("打开", onClick = action,
                    modifier = Modifier.semantics { contentDescription = "查看许可证" })
            }
        }
        notice?.let { action ->
            MonitorSettingsInlineRow("NOTICE", "随应用分发的署名声明。") {
                MonitorSettingsActionPill("打开", onClick = action,
                    modifier = Modifier.semantics { contentDescription = "查看署名声明" })
            }
        }
    }
}
