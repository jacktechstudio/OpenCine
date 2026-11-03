package com.opencapture.opencine.appkit.diagnostics

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.opencapture.monitorui.MonitorPalette
import com.opencapture.monitorui.MonitorTypography
import com.opencapture.monitorui.monitorScrollFade
import com.opencapture.monitorui.settings.MonitorReportStatus
import com.opencapture.monitorui.settings.MonitorSettingsActionPill
import com.opencapture.monitorui.settings.MonitorSettingsSwitchGraphic

private val ChromeShape = RoundedCornerShape(12.dp)

/** Ripple-free click carrying a semantics [role], like the settings rows. */
/** The Settings support card's report status, or null while this phone holds no report. */
public val ManualProblemReportSnapshot.settingsStatus: MonitorReportStatus?
    get() = if (delivery == ManualProblemReportDelivery.IDLE) null else
        MonitorReportStatus(statusLabel, detail ?: "本机会保留一份报告，直到 Sentry 接收或你将其丢弃。", eventId)

@Composable
private fun Modifier.settingsClickable(role: Role, onClick: () -> Unit): Modifier =
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = role, onClick = onClick)

private data class ManualImageDraft(val jpeg: ByteArray, val preview: android.graphics.Bitmap)

/**
 * The operator's problem report form over [manualReport]: description, optional reply email,
 * technical details from [diagnosticText] (off by default, previewed) and up to three photos.
 * [actionMinimumTouchTarget] is the host's settings action pill touch target.
 */
@Composable
public fun ManualProblemReportDialog(
    manualReport: ManualProblemReport,
    diagnosticText: () -> String,
    actionMinimumTouchTarget: Boolean,
    onClose: () -> Unit,
    onPrivacy: () -> Unit,
) {
    @Composable
    fun SettingsActionPill(
        title: String,
        tint: Color = MonitorPalette.accent,
        background: Color = MonitorPalette.accent.copy(alpha = 0.16f),
        enabled: Boolean = true,
        onClick: () -> Unit,
    ) = MonitorSettingsActionPill(title, tint = tint, background = background, enabled = enabled,
        minimumTouchTarget = actionMinimumTouchTarget, onClick = onClick)
    if (!manualReport.isConfigured()) {
        AlertDialog(
            onDismissRequest = onClose,
            title = { Text("无法报告", color = MonitorPalette.text) },
            text = {
                Text(
                    "此版本无法发送报告。可通过配对菜单的“分享诊断信息”或系统设置的“诊断选项”保存报告。",
                    color = MonitorPalette.muted,
                )
            },
            confirmButton = {
                TextButton(onClick = onClose) { Text("确定", color = MonitorPalette.accent) }
            },
            containerColor = MonitorPalette.surface,
        )
        return
    }
    val report by manualReport.snapshot.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var includeTech by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf("") }
    var previewReady by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var images by remember { mutableStateOf(listOf<ManualImageDraft>()) }
    var preparingImages by remember { mutableStateOf(false) }
    val liveImages = remember { mutableStateOf(images) }
    liveImages.value = images
    DisposableEffect(Unit) {
        onDispose {
            liveImages.value.forEach { if (!it.preview.isRecycled) it.preview.recycle() }
        }
    }
    LaunchedEffect(includeTech) {
        if (!includeTech) {
            preview = ""
            previewReady = true
            return@LaunchedEffect
        }
        previewReady = false
        try {
            preview = withContext(Dispatchers.Default) {
                diagnosticText()
            }
            previewReady = true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            error = "无法准备技术信息，仍可发送问题描述。"
            includeTech = false
        }
    }
    val remainingSlots = ManualReportImages.MAX_COUNT - images.size
    val prepareSelection: (List<android.net.Uri>) -> Unit = selection@ { uris ->
            if (uris.isEmpty()) return@selection
            preparingImages = true
            error = null
            scope.launch {
                val prepared = mutableListOf<ManualImageDraft>()
                var adopted = false
                try {
                    val room = ManualReportImages.MAX_COUNT - images.size
                    var rejected: String? = null
                    withContext(Dispatchers.IO) {
                        for (uri in uris.take(room)) {
                            ensureActive()
                            when (val result = ManualReportImageNormalizer.prepare(context, uri)) {
                                is ManualReportImageNormalizer.Result.Ok -> {
                                    val bitmap =
                                        BitmapFactory.decodeByteArray(result.jpeg, 0, result.jpeg.size)
                                    if (bitmap != null) {
                                        prepared += ManualImageDraft(result.jpeg, bitmap)
                                    } else {
                                        rejected =
                                            "无法打开图片，请选择其他照片或截图。"
                                    }
                                }
                                ManualReportImageNormalizer.Result.Unreadable ->
                                    rejected =
                                        "无法打开图片，请选择其他照片或截图。"
                                ManualReportImageNormalizer.Result.TooLarge ->
                                    rejected = "图片过大，请选择较小的图片。"
                            }
                        }
                    }
                    val accepted = ManualReportImages.accept(images.map { it.jpeg } + prepared.map { it.jpeg })
                    if (accepted == null) {
                        prepared.forEach { if (!it.preview.isRecycled) it.preview.recycle() }
                        error = "最多可附加三张图片，每张不超过 1 MB，总计不超过 3 MB。"
                    } else {
                        images = images + prepared
                        adopted = true
                    }
                    if (rejected != null) error = rejected
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    error = "无法处理此图片，请选择其他照片或截图。"
                } finally {
                    if (!adopted) prepared.forEach { if (!it.preview.isRecycled) it.preview.recycle() }
                    preparingImages = false
                }
            }
        }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxOf(2, remainingSlots)),
        onResult = prepareSelection,
    )
    val singlePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
        prepareSelection(listOfNotNull(it))
    }
    val sendBlocked = preparingImages || !previewReady || message.isBlank()
    Dialog(onDismissRequest = onClose) {
        val formScroll = rememberScrollState()
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .background(MonitorPalette.surface, ChromeShape)
                .border(1.dp, MonitorPalette.border, ChromeShape)
                .padding(16.dp)
                .monitorScrollFade(formScroll)
                .verticalScroll(formScroll),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "报告问题",
                style = MonitorTypography.text(16f, FontWeight.SemiBold),
                color = MonitorPalette.text,
            )
            Text(
                "请描述遇到的问题。你可以选择是否附上技术信息，并添加最多三张照片。发送报告不会开启自动报告。",
                style = MonitorTypography.text(12f, FontWeight.Normal),
                color = MonitorPalette.muted,
            )
            if (!report.allowsNewSubmit) {
                Text(
                    "${report.statusLabel}${report.eventId?.let { " · $it" } ?: ""}",
                    style = MonitorTypography.readout(11f, FontWeight.Medium),
                    color = MonitorPalette.accent,
                )
                Text(
                    "请先丢弃排队中的报告，再发送新报告。",
                    style = MonitorTypography.text(12f, FontWeight.Normal),
                    color = MonitorPalette.muted,
                )
            } else {
                val fieldColors =
                    OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MonitorPalette.text,
                        unfocusedTextColor = MonitorPalette.text,
                        focusedBorderColor = MonitorPalette.accent,
                        unfocusedBorderColor = MonitorPalette.border,
                        focusedLabelColor = MonitorPalette.accent,
                        unfocusedLabelColor = MonitorPalette.muted,
                        cursorColor = MonitorPalette.accent,
                    )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it.take(ManualProblemReport.MESSAGE_MAX) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("发生了什么？") },
                    supportingText = {
                        Text("${message.length}/${ManualProblemReport.MESSAGE_MAX}", color = MonitorPalette.faint)
                    },
                    minLines = 4,
                    maxLines = 8,
                    colors = fieldColors,
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.take(ManualProblemReport.EMAIL_MAX) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("回复邮箱（选填）") },
                    supportingText = {
                        Text("${email.length}/${ManualProblemReport.EMAIL_MAX}", color = MonitorPalette.faint)
                    },
                    singleLine = true,
                    colors = fieldColors,
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .settingsClickable(role = Role.Switch) { includeTech = !includeTech },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "包含技术信息",
                            style = MonitorTypography.text(13f, FontWeight.SemiBold),
                            color = MonitorPalette.text,
                        )
                        Text(
                            "默认关闭。发送前可查看预览，其中包含应用和设备版本、连接事件及错误，不含拍摄素材。",
                            style = MonitorTypography.text(11f, FontWeight.Normal),
                            color = MonitorPalette.muted,
                        )
                    }
                    MonitorSettingsSwitchGraphic(isOn = includeTech)
                }
                if (includeTech && preview.isNotEmpty()) {
                    Text(
                        "技术信息预览",
                        style = MonitorTypography.text(11f, FontWeight.SemiBold),
                        color = MonitorPalette.muted,
                    )
                    Text(
                        preview,
                        style = MonitorTypography.readout(10f),
                        color = MonitorPalette.text,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(MonitorPalette.background.copy(alpha = 0.5f), ChromeShape)
                                .padding(8.dp),
                    )
                }
                Text(
                    "照片或截图（选填）",
                    style = MonitorTypography.text(13f, FontWeight.SemiBold),
                    color = MonitorPalette.text,
                )
                Text(
                    "最多可选择三张有权分享的图片。位置信息会被移除。应用不会自动截图，图片仅随本次报告发送。",
                    style = MonitorTypography.text(11f, FontWeight.Normal),
                    color = MonitorPalette.muted,
                )
                if (remainingSlots > 0) {
                    SettingsActionPill(
                        "添加照片或截图",
                        enabled = !preparingImages,
                    ) {
                        val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        if (remainingSlots == 1) singlePicker.launch(request) else picker.launch(request)
                    }
                }
                if (preparingImages) {
                    Text(
                        "正在准备图片…",
                        style = MonitorTypography.text(11f, FontWeight.Normal),
                        color = MonitorPalette.muted,
                    )
                }
                images.forEachIndexed { index, draft ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Image(
                            bitmap = draft.preview.asImageBitmap(),
                            contentDescription = "已选照片 ${index + 1}",
                            modifier =
                                Modifier
                                    .width(72.dp)
                                    .height(72.dp)
                                    .clip(ChromeShape),
                            contentScale = ContentScale.Fit,
                        )
                        SettingsActionPill(
                            "移除",
                            tint = MonitorPalette.recording,
                            background = MonitorPalette.recording.copy(alpha = 0.15f),
                            enabled = !preparingImages,
                        ) {
                            val next = images.toMutableList()
                            val removed = next.removeAt(index)
                            images = next
                            if (!removed.preview.isRecycled) removed.preview.recycle()
                        }
                    }
                }
            }
            Text(
                "报告将通过 Sentry 发送至 OpenCapture。发送不会启用自动报告。请仅分享有权发送的图片，勿包含密码等敏感信息。",
                style = MonitorTypography.text(11f, FontWeight.Normal),
                color = MonitorPalette.muted,
            )
            error?.let {
                Text(it, style = MonitorTypography.text(12f, FontWeight.SemiBold), color = MonitorPalette.recording)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onPrivacy) {
                    Text("报告隐私说明", color = MonitorPalette.muted)
                }
                SettingsActionPill("取消", tint = MonitorPalette.muted, background = MonitorPalette.faint.copy(alpha = 0.2f), onClick = onClose)
                if (report.allowsNewSubmit) {
                    SettingsActionPill("发送报告", enabled = !sendBlocked) {
                        if (sendBlocked) return@SettingsActionPill
                        if (includeTech && !previewReady) {
                            error = "技术信息尚未准备就绪。请稍候，或关闭此选项，仅发送问题描述。"
                            return@SettingsActionPill
                        }
                        when (
                            manualReport.submit(
                                message = message,
                                replyEmail = email,
                                diagnostics =
                                    if (includeTech) {
                                        preview
                                    } else {
                                        null
                                    },
                                images = images.map { it.jpeg },
                            )
                        ) {
                            ManualSubmitResult.QUEUED -> onClose()
                            ManualSubmitResult.INVALID ->
                                error = "请填写问题描述并检查回复邮箱（选填）。"
                            ManualSubmitResult.UNAVAILABLE ->
                                error = "此版本无法发送报告。"
                            ManualSubmitResult.STORAGE_ERROR ->
                                error = "无法保存报告。已保留输入的内容，请重试。"
                            ManualSubmitResult.PENDING_EXISTS ->
                                error = "已有一份报告正在等待发送。"
                        }
                    }
                } else if (report.allowsDiscard) {
                    SettingsActionPill("丢弃") {
                        manualReport.discard()
                        onClose()
                    }
                }
            }
        }
    }
}

/** First-launch question for automatic reports; [product] names the app in the title. */
@Composable
public fun AutomaticReportsPrompt(
    product: String,
    onPrivacy: () -> Unit,
    onEnable: () -> Unit,
    onNotNow: () -> Unit,
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .widthIn(max = 560.dp)
                .fillMaxWidth(),
            shape = ChromeShape,
            color = MonitorPalette.surface,
        ) {
            val promptScroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .monitorScrollFade(promptScroll)
                    .verticalScroll(promptScroll)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "帮助改进 ${product}",
                    style = MonitorTypography.text(21f, FontWeight.SemiBold),
                    color = MonitorPalette.text,
                )
                Text(
                    "可选择通过 Sentry 向 OpenCapture 发送崩溃、错误和图传中断报告，以改进应用稳定性与可靠性。自动报告不含图片、拍摄素材、截图或 GPS 位置。可随时在“系统”中更改。",
                    style = MonitorTypography.text(14f, FontWeight.Normal),
                    color = MonitorPalette.muted,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .settingsClickable(role = Role.Button, onClick = onPrivacy),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        "报告隐私说明",
                        style = MonitorTypography.text(14f, FontWeight.Normal),
                        color = MonitorPalette.accent,
                    )
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .background(MonitorPalette.accent.copy(alpha = 0.08f), ChromeShape)
                            .border(1.dp, MonitorPalette.accent.copy(alpha = 0.35f), ChromeShape)
                            .settingsClickable(role = Role.Button, onClick = onEnable)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "启用自动报告",
                            style = MonitorTypography.text(14f, FontWeight.SemiBold),
                            color = MonitorPalette.accent,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .background(MonitorPalette.muted.copy(alpha = 0.08f), ChromeShape)
                            .border(1.dp, MonitorPalette.muted.copy(alpha = 0.35f), ChromeShape)
                            .settingsClickable(role = Role.Button, onClick = onNotNow)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "暂不开启",
                            style = MonitorTypography.text(14f, FontWeight.SemiBold),
                            color = MonitorPalette.muted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
