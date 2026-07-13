package dev.usbharu.tolo_staff.streaming

import com.google.protobuf.kotlin.Timestamp
import com.google.protobuf.kotlin.invoke
import dev.usbharu.tolo.communication.v1.AssignmentId
import dev.usbharu.tolo.communication.v1.EventId
import dev.usbharu.tolo.communication.v1.EventScope
import dev.usbharu.tolo.communication.v1.MessageId
import dev.usbharu.tolo.communication.v1.PageRequest
import dev.usbharu.tolo.communication.v1.PointId
import dev.usbharu.tolo.communication.v1.ReportId
import dev.usbharu.tolo.communication.v1.StaffId
import dev.usbharu.tolo.communication.v1.TenantId
import dev.usbharu.tolo.communication.v1.ThreadId
import dev.usbharu.tolo.communication.v1.invoke
import dev.usbharu.tolo_staff.runtime.AppSessionContext
import kotlin.time.Instant

internal fun AppSessionContext.toProtoScope(): EventScope = EventScope {
    tenantId = TenantId { value = this@toProtoScope.tenantId }
    eventId = EventId { value = this@toProtoScope.eventId }
}

internal fun String.toProtoStaffId(): StaffId = StaffId { value = this@toProtoStaffId }

internal fun String.toProtoPointId(): PointId = PointId { value = this@toProtoPointId }

internal fun String.toProtoAssignmentId(): AssignmentId = AssignmentId { value = this@toProtoAssignmentId }

internal fun String.toProtoThreadId(): ThreadId = ThreadId { value = this@toProtoThreadId }

internal fun String.toProtoMessageId(): MessageId = MessageId { value = this@toProtoMessageId }

internal fun String.toProtoReportId(): ReportId = ReportId { value = this@toProtoReportId }

internal fun pageRequest(pageSize: Int, pageToken: String): PageRequest = PageRequest {
    this.pageSize = pageSize
    this.pageToken = pageToken
}

internal fun Timestamp.toIsoStringOrEmpty(): String =
    if (seconds == 0L && nanos == 0) "" else Instant.fromEpochSeconds(seconds, nanos).toString()

internal fun Instant.toProtoTimestamp(): Timestamp = Timestamp {
    seconds = epochSeconds
    nanos = nanosecondsOfSecond
}

internal suspend fun <T> paginate(
    fetchPage: suspend (String) -> PagedResult<T>,
): List<T> {
    val items = mutableListOf<T>()
    var token = ""
    do {
        val page = fetchPage(token)
        items += page.items
        token = page.nextPageToken.orEmpty()
    } while (token.isNotBlank())
    return items
}
