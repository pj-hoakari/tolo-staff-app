package dev.usbharu.tolo_staff.feature.appshell

import dev.usbharu.tolo.communication.v1.ReportCategory
import dev.usbharu.tolo.communication.v1.ReportStatus
import dev.usbharu.tolo.communication.v1.Severity
import kotlin.test.Test
import kotlin.test.assertEquals

class ReportMappingsTest {
    @Test
    fun `report categories map every supported category`() {
        assertEquals(ReportCategory.REPORT_CATEGORY_CONGESTION, ReportCategoryValue.CONGESTION.toProto())
        assertEquals(ReportCategory.REPORT_CATEGORY_SAFETY_HAZARD, ReportCategoryValue.SAFETY_HAZARD.toProto())
        assertEquals(ReportCategory.REPORT_CATEGORY_EQUIPMENT, ReportCategoryValue.EQUIPMENT.toProto())
        assertEquals(ReportCategory.REPORT_CATEGORY_GATE, ReportCategoryValue.GATE.toProto())
        assertEquals(ReportCategory.REPORT_CATEGORY_OTHER, ReportCategoryValue.OTHER.toProto())
        assertEquals(ReportCategoryValue.OTHER, ReportCategory.UNRECOGNIZED(99).toValue())
    }

    @Test
    fun `severity and status unknown values degrade safely`() {
        assertEquals(Severity.SEVERITY_INFO, ReportSeverityValue.INFO.toProto())
        assertEquals(Severity.SEVERITY_ATTENTION, ReportSeverityValue.ATTENTION.toProto())
        assertEquals(Severity.SEVERITY_URGENT, ReportSeverityValue.URGENT.toProto())
        assertEquals(ReportSeverityValue.INFO, Severity.UNRECOGNIZED(99).toValue())
        assertEquals("", ReportStatus.UNRECOGNIZED(99).toLabel())
    }
}
