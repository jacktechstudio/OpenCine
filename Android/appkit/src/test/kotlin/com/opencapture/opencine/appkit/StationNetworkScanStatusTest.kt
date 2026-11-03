package com.opencapture.opencine.appkit

import kotlin.test.Test
import kotlin.test.assertEquals

/** The wizard's Nearby status row (`stationSetup.scanStatus`), iOS `StationNetworkSetupView` copy. */
class StationNetworkScanStatusTest {
    @Test fun aCameraThatCannotListNetworksSaysSoInsteadOfAskingToTurnItOn() {
        assertEquals(
            "此相机无法列出网络" to "请选择已知网络或“其他网络”。",
            nearbyScanStatus(scanning = false, scanAvailable = false, cameraScansNetworks = false, scanFailed = false),
        )
    }

    @Test fun aCameraThatScansKeepsItsStatusCopy() {
        assertEquals(
            "打开相机以查找网络" to null,
            nearbyScanStatus(scanning = false, scanAvailable = false, cameraScansNetworks = true, scanFailed = false),
        )
        assertEquals(
            "相机正在搜索网络…" to "发现的网络将显示在此处",
            nearbyScanStatus(scanning = true, scanAvailable = true, cameraScansNetworks = true, scanFailed = false),
        )
        assertEquals(
            "尚未发现网络" to "扫描未完成，请轻点“重新扫描”。",
            nearbyScanStatus(scanning = false, scanAvailable = true, cameraScansNetworks = true, scanFailed = true),
        )
    }
}
