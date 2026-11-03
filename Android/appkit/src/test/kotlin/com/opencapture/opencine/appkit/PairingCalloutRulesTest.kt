package com.opencapture.opencine.appkit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PairingCalloutRulesTest {
    private val copy = PairingCalloutCopy(cameraNoun = "相机", precheck = "close the app", stillLooking = "move closer")

    @Test
    fun stillLookingClockRunsOnlyWhileEverythingIsReadyAndNothingIsNearby() {
        assertTrue(PairingCalloutRules.waitingForCamera(true, true, true, true, false, true))
        assertFalse(PairingCalloutRules.waitingForCamera(false, true, true, true, false, true))
        assertFalse(PairingCalloutRules.waitingForCamera(true, false, true, true, false, true))
        assertFalse(PairingCalloutRules.waitingForCamera(true, true, false, true, false, true))
        assertFalse(PairingCalloutRules.waitingForCamera(true, true, true, false, false, true))
        assertFalse(PairingCalloutRules.waitingForCamera(true, true, true, true, true, true))
        assertFalse(PairingCalloutRules.waitingForCamera(true, true, true, true, false, false))
    }

    @Test
    fun bodiesUseTheAppsCameraNoun() {
        assertEquals("请打开蓝牙，以便查找相机。", PairingCalloutRules.bluetoothBody(copy))
        assertEquals(
            "请打开蓝牙，以便查找机身。",
            PairingCalloutRules.bluetoothBody(copy.copy(cameraNoun = "机身")),
        )
        assertEquals(
            "请允许访问附近设备，以便通过蓝牙查找相机。无需位置权限。",
            PairingCalloutRules.permissionBody(copy, "附近的设备", needsSettings = false, needsLocation = false),
        )
        assertEquals(
            "在 Android 10 和 11 上，通过蓝牙扫描相机需要位置权限，仅用于查找相机。",
            PairingCalloutRules.permissionBody(copy, "位置信息", needsSettings = false, needsLocation = true),
        )
        assertEquals(
            "Android 不会再次询问。请前往“设置 › 权限 › 位置信息”并选择“允许”。",
            PairingCalloutRules.permissionBody(copy, "位置信息", needsSettings = true, needsLocation = true),
        )
    }
}
