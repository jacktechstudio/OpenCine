package com.opencapture.opencine.appkit

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StationNetworkCredentialsTest {
    @Test fun openWifiIsAllowedButHotspotRequiresAPassphrase() {
        assertTrue(validStationCredentials("Test network", "", false))
        assertFalse(validStationCredentials("Test hotspot", "", true))
        assertFalse(validStationCredentials("Test network", "short", false))
        assertTrue(validStationCredentials("Test hotspot", "test-passphrase", true))
    }

    @Test fun namesAndPasswordsRespectWireByteBounds() {
        assertFalse(validStationCredentials("", "test-passphrase", false))
        assertTrue(validStationCredentials("é".repeat(16), "a".repeat(63), false))
        assertFalse(validStationCredentials("é".repeat(17), "test-passphrase", false))
        assertFalse(validStationCredentials("Test network", "é".repeat(32), false))
    }

    // A Wi-Fi password: required for a network this phone knows is secured, optional when the
    // security is unknown or open (a refused join then asks to check it). Swift twin:
    // StationNetworkSetupRulesTests.

    @Test fun aSecuredNetworkNeedsItsPasswordBeforeConnect() {
        assertFalse(validStationCredentials("Studio", "", false, secured = true))
        assertTrue(validStationCredentials("Studio", "synthetic1", false, secured = true))
        kotlin.test.assertEquals("此网络已加密，请输入密码。", stationPasswordNote("", secured = true))
    }

    @Test fun anEmptyPasswordIsAllowedWhenTheSecurityIsUnknownOrOpen() {
        assertTrue(validStationCredentials("Studio", "", false, secured = null))
        assertTrue(validStationCredentials("Studio", "", false, secured = false))
        kotlin.test.assertEquals("仅开放网络可不填密码。", stationPasswordNote("", secured = null))
        kotlin.test.assertNull(stationPasswordNote("", secured = false))
    }

    @Test fun aSetupSavedWithAPasswordMarksItsNetworkSecured() {
        val saved = listOf(StationNetwork("Studio", "synthetic1", false))
        kotlin.test.assertEquals(true, stationNetworkSecurity("Studio", saved, current = null))
        kotlin.test.assertEquals(false, stationNetworkSecurity("Venue", saved, current = false))
        kotlin.test.assertNull(stationNetworkSecurity("Venue", saved, current = null))
    }
}

