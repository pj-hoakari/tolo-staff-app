package dev.usbharu.tolo_staff.streaming

import com.google.protobuf.kotlin.Timestamp
import com.google.protobuf.kotlin.invoke
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.io.readByteString
import kotlinx.rpc.internal.utils.ExperimentalRpcApi
import kotlinx.rpc.internal.utils.InternalRpcApi
import tolo.operation.v1.DeliveryCoordinationService
import tolo.operation.v1.DeliveryPayload
import tolo.operation.v1.DeliveryPayloadInternal
import tolo.operation.v1.LineGuidance
import tolo.operation.v1.RecordFeedbackValuesRequest
import tolo.operation.v1.RecordFeedbackValuesResponse
import tolo.operation.v1.RequestProposalDeliveryRequest
import tolo.operation.v1.RequestProposalDeliveryResponse
import tolo.operation.v1.StaffMessage
import tolo.operation.v1.UpdateConnectionStateRequest
import tolo.operation.v1.UpdateConnectionStateResponse
import tolo.operation.v1.invoke
import tolo.realtime.v1.Envelope
import tolo.realtime.v1.FetchDeliveriesRequest
import tolo.realtime.v1.FetchDeliveriesResponse
import tolo.realtime.v1.IssueFirestoreTokenRequest
import tolo.realtime.v1.IssueFirestoreTokenResponse
import tolo.realtime.v1.RealtimeFetchService
import tolo.realtime.v1.invoke

class RealtimeDeliveryFeedTest {
    private val scope = OperationsScope(
        eventId = "fedcba9876543210",
        tenantId = "0123456789abcdef",
        accessToken = jwtWithSubject("staff-user-1"),
    )

    @Test
    fun `fetches deliveries after each notified id and decodes payloads`() = runTest {
        val realtime = FakeRealtime(
            pages = mapOf(
                null to page("d2", envelope("d1", "first"), envelope("d2", "second")),
                "d2" to page("d3", envelope("d3", "third")),
            )
        )
        val coordination = FakeCoordination()

        val events = feed(realtime, coordination, flowOf("d2", "d2", "d3")).events().toList()
        val deliveries = events.deliveries()

        assertEquals(RealtimeFeedEvent.Connected, events.first())

        assertEquals(listOf("d1", "d2", "d3"), deliveries.map { it.deliveryId })
        assertEquals(
            listOf("first", "second", "third"),
            deliveries.map { (it.payload.kind as DeliveryPayload.Kind.StaffMessage).value.body },
        )
        assertEquals(listOf(null, "d2"), realtime.cursors)
        assertEquals(listOf(true, false), coordination.requests.map { it.connected })
        assertTrue(coordination.requests.all { it.userId == "staff-user-1" && it.tenantId == scope.tenantId })
    }

    @Test
    fun `stops paging when notified id is not addressed to the staff`() = runTest {
        val realtime = FakeRealtime(pages = mapOf(null to page("d1", envelope("d1", "first"))))

        val deliveries = feed(realtime, FakeCoordination(), flowOf("d1", "d9")).events().toList().deliveries()

        assertEquals(listOf("d1"), deliveries.map { it.deliveryId })
        assertEquals(listOf(null, "d1"), realtime.cursors)
    }

    @Test
    fun `feed is empty without access token`() = runTest {
        val coordination = FakeCoordination()

        val deliveries = RealtimeDeliveryFeed(
            realtime = FakeRealtime(emptyMap()),
            deliveryCoordination = coordination,
            scope = scope.copy(accessToken = ""),
            latestDeliveryIds = { flowOf("d1") },
        ).events().toList().deliveries()

        assertEquals(emptyList(), deliveries)
        assertEquals(emptyList(), coordination.requests)
    }

    @Test
    fun `connection and staff messages refresh lists while proposals do not`() {
        val staffMessage = RealtimeFeedEvent.Delivered(
            RealtimeDelivery("d1", "", DeliveryPayload { kind = DeliveryPayload.Kind.StaffMessage(StaffMessage {}) })
        )
        val guidance = RealtimeFeedEvent.Delivered(
            RealtimeDelivery("d2", "", DeliveryPayload { kind = DeliveryPayload.Kind.LineGuidance(LineGuidance {}) })
        )

        assertEquals(OperationEntityType.UNKNOWN, RealtimeFeedEvent.Connected.toChangeHead()?.entityType)
        assertEquals(OperationEntityType.INSTRUCTION, staffMessage.toChangeHead()?.entityType)
        assertEquals(null, guidance.toChangeHead())
    }

    private fun List<RealtimeFeedEvent>.deliveries() =
        filterIsInstance<RealtimeFeedEvent.Delivered>().map { it.delivery }

    private fun feed(
        realtime: RealtimeFetchService,
        coordination: DeliveryCoordinationService,
        latest: Flow<String>,
    ) = RealtimeDeliveryFeed(
        realtime = realtime,
        deliveryCoordination = coordination,
        scope = scope,
        latestDeliveryIds = { eventId ->
            assertEquals(scope.eventId, eventId)
            latest
        },
        retryDelay = Duration.ZERO,
    )

    private class FakeRealtime(
        private val pages: Map<String?, FetchDeliveriesResponse>,
    ) : RealtimeFetchService {
        val cursors = mutableListOf<String?>()

        override suspend fun FetchDeliveries(message: FetchDeliveriesRequest): FetchDeliveriesResponse {
            cursors += message.resumeAfterDeliveryId
            return pages[message.resumeAfterDeliveryId] ?: page(message.resumeAfterDeliveryId.orEmpty())
        }

        override suspend fun IssueFirestoreToken(message: IssueFirestoreTokenRequest): IssueFirestoreTokenResponse =
            error("not used")
    }

    private class FakeCoordination : DeliveryCoordinationService {
        val requests = mutableListOf<UpdateConnectionStateRequest>()

        override suspend fun UpdateConnectionState(message: UpdateConnectionStateRequest): UpdateConnectionStateResponse {
            requests += message
            return UpdateConnectionStateResponse {}
        }

        override suspend fun RequestProposalDelivery(message: RequestProposalDeliveryRequest): RequestProposalDeliveryResponse =
            error("not used")

        override suspend fun RecordFeedbackValues(message: RecordFeedbackValuesRequest): RecordFeedbackValuesResponse =
            error("not used")
    }
}

private fun page(latest: String, vararg envelopes: Envelope) = FetchDeliveriesResponse {
    this.envelopes = envelopes.toList()
    latestDeliveryId = latest
}

@OptIn(InternalRpcApi::class, ExperimentalRpcApi::class)
private fun envelope(id: String, body: String) = Envelope {
    deliveryId = id
    sentAt = Timestamp { seconds = 1_750_324_800L }
    payload = DeliveryPayloadInternal.MARSHALLER.encode(
        DeliveryPayload { kind = DeliveryPayload.Kind.StaffMessage(StaffMessage { this.body = body }) },
        null,
    ).readByteString()
}

@OptIn(ExperimentalEncodingApi::class)
private fun jwtWithSubject(subject: String): String {
    val encode = { text: String -> Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(text.encodeToByteArray()) }
    return "${encode("""{"alg":"none"}""")}.${encode("""{"sub":"$subject"}""")}.signature"
}
