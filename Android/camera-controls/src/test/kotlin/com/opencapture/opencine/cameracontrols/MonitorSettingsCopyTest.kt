package com.opencapture.opencine.cameracontrols

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Swift twin: `MonitorSettingsCopyParityTests.reliabilityReportsPrivacyIsOneTextOnEveryPlatform`. */
class MonitorSettingsCopyTest {
    @Test fun reliabilityReportsPrivacyStatesTheInstallIdentifierAndTheTogglePlace() {
        val text = MonitorSettingsCopy.reliabilityReportsPrivacy
        listOf(
            "可选的可靠性报告\n", "通过 Sentry",
            "随机安装标识在开启报告时创建，关闭时删除",
            "监看设置 > 系统", "删除安装标识", "不含图像",
        ).forEach { assertTrue(text.contains(it), it) }
        assertFalse(text.contains('—'), "Operator copy never uses the em dash")
    }
}
