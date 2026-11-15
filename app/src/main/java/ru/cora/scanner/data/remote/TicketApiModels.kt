package ru.cora.scanner.data.remote

import kotlinx.serialization.Serializable
import ru.cora.scanner.domain.model.TicketInfo
import ru.cora.scanner.domain.model.ValidationStatus

@Serializable
data class ValidateTicketRequest(
    val ticketId: String?,
    val rawPayload: String,
    val scannedAtEpochMs: Long,
)

@Serializable
data class ValidateTicketResponse(
    val ticketId: String? = null,
    val eventId: String? = null,
    val eventTitle: String? = null,
    val venue: String? = null,
    val dateTime: String? = null,
    val seat: String? = null,
    val sector: String? = null,
    val holderName: String? = null,
    val orderId: String? = null,
    val status: String = "unknown",
) {
    fun toTicketInfo(rawPayload: String): TicketInfo = TicketInfo(
        ticketId = ticketId,
        eventId = eventId,
        eventTitle = eventTitle,
        venue = venue,
        dateTime = dateTime,
        seat = seat,
        sector = sector,
        holderName = holderName,
        orderId = orderId,
        validationStatus = status.toValidationStatus(),
        rawPayload = rawPayload,
    )

    private fun String.toValidationStatus(): ValidationStatus = when (lowercase()) {
        "valid", "ok" -> ValidationStatus.Valid
        "used", "already_used" -> ValidationStatus.AlreadyUsed
        "not_found" -> ValidationStatus.NotFound
        "expired" -> ValidationStatus.Expired
        else -> ValidationStatus.Unknown
    }
}

@Serializable
data class SyncTicketsResponse(
    val tickets: List<ValidateTicketResponse> = emptyList(),
    val syncedAtEpochMs: Long = System.currentTimeMillis(),
)
