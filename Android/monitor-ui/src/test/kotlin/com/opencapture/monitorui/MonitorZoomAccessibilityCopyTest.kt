package com.opencapture.monitorui

import kotlin.test.Test
import kotlin.test.assertEquals

class MonitorZoomAccessibilityCopyTest {
    @Test fun zoomStateDescribesTheCommittedScale() {
        assertEquals("放大 1.0 倍", monitorZoomStateDescription(1f))
        assertEquals("放大 2.5 倍", monitorZoomStateDescription(2.5f))
        assertEquals("放大 4.0 倍", monitorZoomStateDescription(4f))
    }
}
