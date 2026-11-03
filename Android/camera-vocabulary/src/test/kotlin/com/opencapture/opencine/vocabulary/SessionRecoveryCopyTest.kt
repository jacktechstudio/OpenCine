package com.opencapture.opencine.vocabulary

import kotlin.test.Test
import kotlin.test.assertEquals

class SessionRecoveryCopyTest {
    private val paused = SessionRecoveryUi.PausedAfterDrops(drops = 3)

    @Test fun pausedCopyWithoutASuspectIsUnchanged() {
        assertEquals(
            "Z 6III重连后短时间内又断开 3 次。为保护相机，已暂停自动重连。下方画面已定格。",
            SessionRecoveryCopy.detail(paused, "Z 6III"),
        )
    }

    @Test fun pausedCopyNamesAnotherScanningDevice() {
        assertEquals(
            "相机重连后短时间内又断开 3 次，可能有其他设备正在搜索相机。为保护相机，已暂停自动重连。下方画面已定格。",
            SessionRecoveryCopy.detail(paused, " ", SessionDropSuspect.OTHER_DEVICE_SCANNING),
        )
    }

    /** Hardware pass 5.1: "attempt 1 of 8", then the 3 minute budget stopped at 3 tries. */
    @Test fun retryingCopyNamesTheTimeBudgetNotAnAttemptCount() {
        assertEquals(
            "OsmoPocket4P-EA4A已断开，画面停留在最后一帧。正在第 1 次重连。最多尝试 3 分钟。",
            SessionRecoveryCopy.detail(SessionRecoveryUi.Retrying(attempt = 1, maxAttempts = 8), "OsmoPocket4P-EA4A"),
        )
    }

    @Test fun givingUpSuggestsRestartingTheCamera() {
        assertEquals(
            "尝试 8 次后仍未能连接Luna Ultra。请重启相机，再点按“重新连接”。下方画面已定格。",
            SessionRecoveryCopy.detail(SessionRecoveryUi.WaitingForOperator(attemptsMade = 8), "Luna Ultra"),
        )
    }

    @Test fun onlyThePausedCaseReadsTheSuspect() {
        val retrying = SessionRecoveryUi.Retrying(attempt = 2, maxAttempts = 8)
        assertEquals(
            SessionRecoveryCopy.detail(retrying, "Z 6III"),
            SessionRecoveryCopy.detail(retrying, "Z 6III", SessionDropSuspect.OTHER_DEVICE_SCANNING),
        )
    }

    @Test fun takenOverNamesTheHolderAndHowToTakeItBack() {
        val taken = SessionRecoveryUi.TakenOver(holder = "Phone B")
        assertEquals("相机已连接到其他设备", SessionRecoveryCopy.title(taken))
        assertEquals(
            "Phone B已连接到Z 6III，本机已停止重连。点按“重新连接”可重新接管相机。下方画面已定格。",
            SessionRecoveryCopy.detail(taken, "Z 6III"),
        )
        assertEquals(
            "其他设备已连接到相机，本机已停止重连。点按“重新连接”可重新接管相机。下方画面已定格。",
            SessionRecoveryCopy.detail(SessionRecoveryUi.TakenOver(holder = " "), ""),
        )
    }
}
