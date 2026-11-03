package com.opencapture.opencine.core

/**
 * Local VPN / ad-blocker filters (AdGuard, Blokada, RethinkDNS) capture the
 * camera UDP datalink. `bindProcessToNetwork` cannot bypass a VpnService
 * that did not call `allowBypass()`. Swift `LocalVPNFilter` twin: copy and
 * live-wait policy (#239). The platform module's `LocalVPNProbe` detects the tunnel.
 */
public object LocalVPNFilter {
    public const val LIVE_HINT_DELAY_SECONDS: Double = 8.0
    public const val LIVE_HINT_DELAY_MS: Long = 8_000L

    public const val WIZARD_BANNER: String =
        "请暂停 VPN 和广告拦截工具，或将本应用加入排除列表。此类工具可能会拦截图传。"

    public const val LIVE_HINT: String =
        "VPN 或广告拦截工具可能会拦截图传。请暂停过滤或将本应用加入排除列表后重试。"

    public const val JOIN_WIFI_PHONE_STEP: String = "暂停 VPN 和广告拦截工具，或将本应用加入排除列表"

    public fun shouldHintOnLiveWait(
        vpnActive: Boolean,
        hadVideo: Boolean,
        secondsWithoutVideo: Double,
    ): Boolean = vpnActive && !hadVideo && secondsWithoutVideo >= LIVE_HINT_DELAY_SECONDS
}
