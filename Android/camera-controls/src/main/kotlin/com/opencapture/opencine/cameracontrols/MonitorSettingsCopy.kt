package com.opencapture.opencine.cameracontrols

/** Operator help shared by the settings compositions (Swift `MonitorSettingsCopy`). */
public object MonitorSettingsCopy {
    /** The Feed Upscaler help on the Link tab's Processing card, in every app (A2-78). */
    public const val feedUpscaler: String =
        "选择取景画面的放大方式。相机图传分辨率低于屏幕分辨率。“关”使用普通采样，“快速”使用固定锐化滤镜，“高质量”使用系统的空间放大算法。\n\nAI 会推算相机未实际记录的细节。画面看起来更锐利，但新增纹理未必真实。精准判断焦点请使用“高质量”或“快速”。\n\n仅显示本机支持的选项。"

    public const val keepScreenAwake: String =
        "使用应用时保持屏幕常亮。关闭后使用系统自动锁屏设置。设备过热时系统仍可能降低亮度。"

    /**
     * Automatic reports, one disclosure on every platform (A2-37). It states what the shared
     * reporter sends: crashes and hangs, live-feed reports and session health counts, with no
     * images, footage or location.
     */
    public const val automaticReports: String =
        "可选择通过 Sentry 向 OpenCapture 发送崩溃、卡顿、图传报告及会话健康统计。默认关闭，随时可关闭且不影响功能。断开相机 Wi-Fi 后才会上传。自动报告不含图片、拍摄素材或 GPS 位置。Sentry 会接收连接 IP 地址。详见下方隐私说明。"

    /**
     * Shown in place of the false colour and zebra settings while the camera has not named its colour
     * curve (product ruling 2026-10-16). Swift twin: `MonitorSettingsCopy.sourceCurveUnknown`.
     */
    public const val sourceCurveUnknown: String =
        "正在等待相机报告色彩曲线。取得参数后会自动恢复假色、斑马纹和 LUT 曝光补偿。"

    /**
     * The reliability-report paragraph of every app's bundled privacy text, the same facts as the
     * websites: the random install identifier made on opt-in and deleted on opt-out, and the toggle's place.
     */
    public const val reliabilityReportsPrivacy: String =
        "可选的可靠性报告\n在支持此功能的版本中，经你同意，应用将通过 Sentry 发送崩溃、卡顿及图传诊断、应用/系统/设备版本和会话健康统计。随机安装标识在开启报告时创建，关闭时删除。自动报告不含图像、拍摄素材、截图、原始相机数据包或 GPS 位置。Sentry 会接收连接 IP 地址。事件存储于德国，部分处理可能在其他国家或地区进行。\n可随时在“监看设置 > 系统”中关闭报告，不影响应用功能。关闭后会清除待上传项目并删除安装标识，已接收的报告不会被删除。可私下联系我们申请删除。有关保留期限、跨境传输和你的权利，请参阅网站隐私政策。"
}
