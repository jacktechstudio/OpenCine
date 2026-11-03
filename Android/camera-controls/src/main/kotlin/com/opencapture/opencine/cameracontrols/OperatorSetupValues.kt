package com.opencapture.opencine.cameracontrols

import android.content.Context
import com.opencapture.monitorui.MonitorLinkHealth
import com.opencapture.opencine.vocabulary.ConnectionPhase
import com.opencapture.opencine.vocabulary.LutLookResolver
import com.opencapture.opencine.vocabulary.MediaClipFormatting
import java.io.File
import java.util.Locale

/** Live link health for the settings link tile and the FPS chip. */
object OperatorLinkHealth {
    const val TARGET_FPS = 25.0

    fun bars(
        isLive: Boolean,
        videoPackets: Int,
        hasVideoFormat: Boolean,
        measuredFps: Double = -1.0,
    ): Int {
        if (!isLive) return 0
        if (measuredFps > 0.0) {
            val score = ((measuredFps / TARGET_FPS) * 100.0).coerceIn(0.0, 100.0)
            val rounded = kotlin.math.round(score).toInt()
            if (rounded <= 0) return 1
            return ((rounded + 24) / 25).coerceIn(1, 4)
        }
        return when {
            hasVideoFormat && videoPackets > 0 -> 4
            videoPackets >= 400 -> 4
            videoPackets >= 120 -> 3
            videoPackets >= 1 -> 2
            else -> 1
        }
    }

    fun score(bars: Int): Int = MonitorLinkHealth.score(bars)

    fun caption(isLive: Boolean, bars: Int): String {
        if (!isLive) return "未建立图传连接。"
        if (bars <= 0) return "正在等待图传连接。"
        return when (MonitorLinkHealth.band(score(bars))) {
            MonitorLinkHealth.Band.STABLE -> "图传正常 · 稳定"
            MonitorLinkHealth.Band.WATCH -> "图传有丢包 · 注意"
            MonitorLinkHealth.Band.POOR -> "图传不稳定 · 较差"
        }
    }

    @Deprecated("Use MonitorLinkMeter.compactFPS")
    fun compactFps(incoming: String): String = MonitorLinkMeter.compactFPS(incoming)

    @Deprecated("Use MonitorLinkMeter.formatMeasuredFPS")
    fun formatMeasuredFps(fps: Double): String = MonitorLinkMeter.formatMeasuredFPS(fps)

    @Deprecated("Use MonitorLinkMeter.chipLabel")
    fun fpsChipLabel(isLive: Boolean, recovering: Boolean, measuredFps: Double,
        phase: ConnectionPhase): String {
        val label = MonitorLinkMeter.chipLabel(phase, recovering, false,
            MonitorLinkMeter.compactFPS(MonitorLinkMeter.formatMeasuredFPS(measuredFps)), measuredFps)
        return if (isLive && phase == ConnectionPhase.IDLE && !recovering && measuredFps <= 0.0) "图传" else label
    }

    fun liveTileDetail(
        isLive: Boolean,
        cameraName: String,
        fpsLabel: String,
        phaseLabel: String,
    ): String = if (isLive) "$cameraName · BLE + Wi-Fi · $fpsLabel FPS" else phaseLabel
}

fun connectionPhaseLabel(phase: ConnectionPhase, failure: String?): String =
    when (phase) {
        ConnectionPhase.IDLE -> "空闲"
        ConnectionPhase.SCANNING -> "正在搜索相机…"
        ConnectionPhase.CONNECTING_GATT -> "正在连接蓝牙…"
        ConnectionPhase.PAIRING -> "正在配对…"
        ConnectionPhase.AWAITING_APPROVAL -> "请在相机屏幕上确认"
        ConnectionPhase.READING_WIFI_CREDS -> "正在读取 Wi-Fi 连接信息…"
        ConnectionPhase.JOINING_WIFI -> "正在加入相机 Wi-Fi…"
        ConnectionPhase.OPENING_DATALINK -> "正在建立数据连接…"
        ConnectionPhase.LIVE -> "已连接"
        ConnectionPhase.FAILED ->
            if (failure.isNullOrBlank()) "失败" else "失败：${failure}"
    }

/** The assists a clean view can keep pinned, by stored key. */
enum class CleanPinTool(val key: String, val title: String) {
    LUT("LUT", "LUT"),
    PEAKING("PEAK", "峰值对焦"),
    FALSE_COLOR("FALSE", "假色"),
    ZEBRA("ZEBRA", "斑马纹"),
    WAVEFORM("WAVE", "波形图"),
    PARADE("PARADE", "RGB 分量波形"),
    HISTOGRAM("HISTO", "直方图"),
    VECTORSCOPE("VECTOR", "矢量示波器"),
    TRAFFIC_LIGHTS("LIGHTS", "通道警示灯"),
    ND("ND", "ND 滤镜建议"),
    GUIDES("GUIDES", "构图辅助线"),
    GRID("GRID", "网格线"),
    CROSSHAIR("CROSS", "中心十字线"),
    DESQUEEZE("DESQ", "变形宽银幕反挤压"),
    MIRROR("MIRROR", "镜像"),
    AUDIO("AUDIO", "音频电平"),
}

/** The cache size line: the shared decimal byte label, "Empty" for nothing cached. */
fun formatCacheSize(bytes: Long): String = if (bytes <= 0L) "空" else MediaClipFormatting.byteLabel(bytes)

fun formatAppVersion(versionName: String, versionCode: Long): String = "$versionName ($versionCode)"

/** The operator setup help copy both apps show; [appName] fills the lines that name the app. */
@Suppress("PropertyName")
open class OperatorHelpCopy(appName: String) {
    val CURRENT_TRANSPORT =
        "Pocket 通过蓝牙配对，再经相机 Wi-Fi 传输 HEVC 图像。本版本不支持 USB-C、热点和 HDMI 采集。"
    val PHASE = "当前蓝牙、Wi-Fi 和数据链路的连接阶段。"
    val CAMERA_WIFI =
        "当前连接的网络名称（SSID）。密码仅存于本机的 Android 密钥库。"
    val SAVED_CAMERAS =
        "请在主页的“我的相机”列表中发起配对。"
    val EDIT_VIEW = "打开监看界面，轻点眼睛图标选择显示或隐藏各项目。"
    val FRAME_IO =
        "登录后可从分享面板上传片段。Frame.io 需要互联网连接，上传时手机将暂时断开相机 Wi-Fi。"
    val RECORD_CONFIRMATION = "开始或停止录制前先确认，以防误触。"
    val SHOOTING_MODE =
        "切换录像、拍照或延时等模式。相机会回报当前模式，" +
            "在机身上更改后也会同步更新。"
    val HAPTICS =
        "操作开关、修改设置或云台到达限位时短振提醒。连接的手柄在到达限位时也会振动。"
    val JOYSTICK_SENSITIVITY =
        "调整屏幕或手柄摇杆的云台响应。轻推时慢速移动，推到底速度最快。4 为默认手感；5 更快达到全速，1 最慢。"
    val VIRTUAL_JOYSTICK_SIZE =
        "调整屏幕摇杆尺寸。默认为中号，大号更易操控，小号遮挡画面更少。"
    val VIRTUAL_JOYSTICK_INVERT_PAN =
        "反转屏幕摇杆左右方向。默认关闭，不影响外接手柄。"
    val VIRTUAL_JOYSTICK_INVERT_TILT =
        "反转屏幕摇杆上下方向。默认关闭，不影响外接手柄。"
    val VIRTUAL_JOYSTICK_DEADZONE =
        "忽略摇杆中心附近的微小动作。默认为 8%，调大可降低中心灵敏度。"
    val VIRTUAL_JOYSTICK_RESPONSE =
        "标准保留默认手感；线性响应均匀；精细可让小幅度操作更柔和。"
    val GIMBAL_JOYSTICK =
        "选择控制云台平移和俯仰的摇杆，默认为左摇杆。另一侧摇杆不控制云台。"
    val GAMEPAD =
        "外接手柄：所选摇杆控制平移和俯仰。×/A 录制，○/B 回中，□/X 旋转 180°，△/Y 跟踪人脸。L1/R1 分级拉远/推近，按住 L2/R2 连续变焦（按得越深越快）。十字键上下调整 ISO，左右调整快门。断开手柄时摇杆归中。操作屏幕摇杆时，以屏幕摇杆为准。"
    val KEEP_SCREEN_AWAKE =
        MonitorSettingsCopy.keepScreenAwake
    val HDR_DISPLAY =
        "提升户外屏幕亮度。波形图、直方图、RGB 分量波形、矢量示波器、斑马纹及假色仍测量解码后的相机信号，请以其判断曝光。录屏时恢复标准 SDR 画面，文件不会因 HDR 增亮。此选项与相机 HDR/HLG 色彩模式独立。默认关闭。"
    val THEME = "深灰色监看界面配以天蓝色标识，减少片场反光干扰。"
    val SUPPORT = "连接、取景、相机控制与故障排查。"
    val REPORT = "在 GitHub 打开项目的公开问题反馈表。"
    val REPORT_PROBLEM =
        "请描述遇到的问题。你可以选择是否附上技术信息，并添加最多三张照片。发送报告不会开启自动报告。"
    val RELIABILITY_REPORTS = MonitorSettingsCopy.automaticReports
    val RELIABILITY_UNAVAILABLE =
        "本版本无法发送自动报告。仍可分享或删除本机保存的报告。"
    val FEATURE = "在项目功能建议讨论区发表想法。"
    val SOURCE =
        "在 GitHub 查看 ${appName} 项目。如果当前只连接了相机 Wi-Fi，打开页面时可能会断开该连接。"
    val LINK_HEALTH = "当前相机图传质量，根据数据传输情况判断。"
    val CLEAR_CACHE =
        "移除所有相机的缓存片段、缩略图和素材索引。相机连接后将重新载入素材库。"
    val LUT_LOOK = "仅调整本机监看画面，不影响相机原片。"
    val APP_VERSION = "由原生项目信息取得的 ${appName} 构建版本。"
    val LOCAL_CACHE = "本机保存的各相机片段缓存、缩略图和素材索引。"
    val CACHE_FULL_RESOLUTION =
        "打开片段时下载相机原片。关闭则仅保留 720p 代理文件以节省空间。分享需要原片；如未缓存，请连接相机。"
    val FEED_UPSCALER = MonitorSettingsCopy.feedUpscaler
    val FALSE_COLOR_SCALE =
        "根据相机色彩模式自动选择 D-Log、D-Log2、Rec.709 或 HLG。" +
            "CineStop 以 18% 灰为基准，经相机的" +
            "Log 曲线换算曝光档位：深绿 −2、黄绿 −1、18% 中灰、淡粉 +1" +
            "（肤色）、淡黄 +2（肤色上限）。红色表示高光溢出，紫色表示暗部死黑；" +
            "其他阴影显示为深灰，其他高光显示为浅灰。" +
            "视频模式在亮度灰阶画面叠加 IRE 色带（绿 41–48，粉 61–70，红色表示高光溢出）。" +
            "IRE 模式将亮度分为六个电平区域：" +
            "紫色表示暗部死黑、蓝色表示近黑、绿色表示 18% 灰、粉色表示高一档、" +
            "黄色表示接近溢出、红色表示溢出。对于 Log 画面，视频和 IRE 模式可选择基于" +
            "Rec.709 转换（709）或原始信号（LOG）测量。限幅模式仅标示阴影和高光警告，其余颜色保持不变。"
    val FALSE_COLOR_REFERENCE =
        "使用假色时，在取景画面叠加小型颜色图例。"
    val PEAKING_SENSITIVITY =
        "灵敏度越高，越容易显示细小边缘，但复杂场景中也可能出现杂点。"
    val PEAKING_COLOR = "选择在常拍场景中清晰可见的轮廓颜色。"
    val ZEBRA_UNITS =
        "在原始 0–255 编码值与 0–100 IRE 监看刻度之间切换。"
    val ZEBRA_HIGHLIGHT =
        "根据当前 Log 曲线补偿后，高光接近溢出时显示高光斑马纹。"
    val ZEBRA_MIDTONE =
        "中间调斑马纹根据曲线补偿，为肤色或主体曝光提供参考区间。"
    val WAVEFORM_BRIGHTNESS =
        "强光下波形不易辨认时，可提高波形亮度。"
    val PARADE_BRIGHTNESS = "各通道分量不易辨认时，可提高波形亮度。"
    val VECTORSCOPE_ZOOM =
        "仅放大色度轨迹，刻度不变。矢量示波器以相机信号经内置显示变换后的结果为准，不受监看 LUT 影响。"
    val VECTORSCOPE_BRIGHTNESS =
        "色度轨迹不易辨认时，可提高轨迹亮度。"
    val TRAFFIC_LIGHTS_COMPENSATION =
        "设置通道欠曝或溢出指示灯的容差（档），与直方图通道警示灯共用。"}

/**
 * The clip cache directories: the app's [mediaRoots] (per-camera folders, each keeping its `index.json`)
 * first, then the older flat caches.
 */
open class OperatorMediaCacheDirs(private val mediaRoots: (Context) -> List<File>) {
    fun candidates(context: Context): List<File> =
        mediaRoots(context) + listOf(
            File(context.filesDir, "media-cache"),
            File(context.cacheDir, "media-cache"),
        )

    fun existingDir(context: Context): File? = candidates(context).firstOrNull { it.isDirectory }

    fun byteCount(context: Context): Long {
        val dir = existingDir(context) ?: return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    fun clear(context: Context) {
        val mediaRoot = mediaRoots(context).firstOrNull { it.isDirectory }
        if (mediaRoot != null) {
            mediaRoot.listFiles()?.forEach { cameraDir ->
                if (!cameraDir.isDirectory) return@forEach
                cameraDir.listFiles()?.forEach { child ->
                    if (child.name != "index.json") child.deleteRecursively()
                }
            }
            return
        }
        existingDir(context)?.listFiles()?.forEach { it.deleteRecursively() }
    }
}

fun lutLookLabel(
    selection: String,
    enabled: Boolean = true,
    colorMode: Int = -1,
    profile: com.opencapture.opencine.vocabulary.CameraLUTProfile? = null,
): String {
    val source =
        LutLookResolver.resolve(
            selection = selection,
            lutOn = true,
            colorMode = colorMode,
            profile = profile,
        )
    return LutLookResolver.statusLabel(enabled, selection, source)
}
