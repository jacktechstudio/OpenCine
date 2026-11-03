package com.opencapture.opencine.cameracontrols

import kotlin.test.Test
import kotlin.test.assertEquals

class MediaSelectionTrayCopyTest {
    @Test fun selectionActionsNameTheCountAndTheCameraCard() {
        assertEquals("从相机存储卡删除所选的 3 项素材", selectionDeleteDescription(3, true))
        assertEquals("从相机存储卡删除所选的 1 项素材", selectionDeleteDescription(1, true))
        assertEquals("删除所选素材", selectionDeleteDescription(2, false))
        assertEquals("分享所选的 3 项素材", selectionShareDescription(3))
        assertEquals("分享所选的 1 项素材", selectionShareDescription(1))
    }
}
