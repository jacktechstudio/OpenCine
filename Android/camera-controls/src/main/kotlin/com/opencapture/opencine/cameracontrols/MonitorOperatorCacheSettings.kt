package com.opencapture.opencine.cameracontrols

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import com.opencapture.monitorui.MonitorPalette
import com.opencapture.monitorui.settings.MonitorCacheSettings
import com.opencapture.opencine.appkit.MonitorMediaLibraryModel
import com.opencapture.opencine.appkit.settingsCacheSource
import com.opencapture.opencine.appkit.MonitorSettingsCacheModel
import com.opencapture.opencine.appkit.MonitorSettingsCacheSource

/** One presentation owner serves production setup and a public-session scene. */
@Composable
public fun <P> MonitorOperatorCacheSettings(library: MonitorMediaLibraryModel<P>, cacheOriginals: Boolean,
    onCacheOriginals: (Boolean) -> Unit) {
    MonitorOperatorCacheSettings(monitorSettingsCacheSource(library), cacheOriginals, onCacheOriginals)
}

/** The settings cache source over the gallery's [library], renewed with its lifetime and cache revision. */
@Composable
public fun <P> monitorSettingsCacheSource(library: MonitorMediaLibraryModel<P>): MonitorSettingsCacheSource {
    val value by library.state.collectAsState()
    return library.settingsCacheSource(value)
}

/** All-camera settings retain the current library's lifetime and cache-revision admission. */
@Composable
public fun <P> monitorSettingsCacheSource(library: MonitorMediaLibraryModel<P>,
    cacheRoot: com.opencapture.opencine.appkit.MonitorMediaCacheRoot): MonitorSettingsCacheSource {
    val value by library.state.collectAsState()
    return library.settingsCacheSource(cacheRoot = cacheRoot, value = value)
}

/** All-camera settings over one declared parent in the existing shared storage registry. */
@Composable
public fun <P> monitorSettingsCacheSource(library: MonitorMediaLibraryModel<P>,
    storage: com.opencapture.opencine.appkit.MonitorMediaCacheStorage, parent: java.io.File): MonitorSettingsCacheSource {
    val value by library.state.collectAsState()
    return library.settingsCacheSource(storage = storage, parent = parent, value = value)
}

/**
 * SDK adapters provide captured IO; this owner presents count, confirmation, progress and errors.
 * Clearing always asks first (the Pocket baseline): one policy for every app.
 */
@Composable
public fun MonitorOperatorCacheSettings(
    source: MonitorSettingsCacheSource,
    cacheOriginals: Boolean? = null,
    onCacheOriginals: ((Boolean) -> Unit)? = null,
    cacheHelp: String = "保留为回放下载的原片。",
    scopeHelp: String = "从相机下载的原片与回放代理文件。",
    clearHelp: String = "删除已下载的影像文件，保留素材列表。",
    formatBytes: ((Long) -> String)? = null,
    feedback: () -> Unit = {},
    checkingLabel: String = "正在检查…",
    readFailureLabel: String = "无法读取缓存。",
    clearFailureLabel: String = "未能清除缓存，请重试。",
    summaryColor: Color = MonitorPalette.accent,
) {
    val scope = rememberCoroutineScope()
    val model = remember { MonitorSettingsCacheModel(scope) }
    val value by model.state.collectAsState()
    DisposableEffect(model) { onDispose { model.retire() } }
    LaunchedEffect(model, source.identity, source.revision) { model.refresh(source) }
    MonitorCacheSettings(cacheOriginals = cacheOriginals,
        sizeLabel = value.bytes?.let { bytes ->
            formatBytes?.invoke(bytes) ?: formatCacheSize(bytes)
        } ?: checkingLabel,
        clearing = value.clearing, cacheHelp = cacheHelp, scopeHelp = scopeHelp, clearHelp = clearHelp,
        onCacheOriginals = onCacheOriginals, clear = model::requestClear, feedback = feedback)
    value.clearSummary?.let { Text(it, color = summaryColor) }
    if (value.readFailed || value.clearFailed) Text(value.failure ?: if (value.clearFailed) clearFailureLabel else readFailureLabel,
        color = MonitorPalette.accent)
    if (value.confirmationPresented) AlertDialog(
        onDismissRequest = model::cancelConfirmation,
        title = { Text("清除缓存？", color = LiveDesign.text) },
        text = { Text(clearHelp, color = LiveDesign.muted) },
        confirmButton = { TextButton(onClick = model::clear) { Text("清除", color = MonitorPalette.recording) } },
        dismissButton = { TextButton(onClick = model::cancelConfirmation) { Text("取消", color = LiveDesign.muted) } },
        containerColor = LiveDesign.surface,
    )
    if (value.clearFailed) AlertDialog(onDismissRequest = model::dismissError,
        title = { Text(value.failure ?: clearFailureLabel) },
        confirmButton = { TextButton(onClick = model::dismissError) { Text("确定") } })
}
