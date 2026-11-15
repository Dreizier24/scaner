package ru.cora.scanner.domain

import kotlinx.serialization.Serializable
import ru.cora.scanner.domain.model.TicketInfo
import ru.cora.scanner.domain.model.ValidationStatus

@Serializable
data class QrTicketJson(
    val ticketId: String? = null,
    val eventId: String? = null,
    val eventTitle: String? = null,
    val venue: String? = null,
    val dateTime: String? = null,
    val row: String? = null,
    val seat: String? = null,
    val sector: String? = null,
    val holderName: String? = null,
    val orderId: String? = null,
    val status: String? = null,
) {
    fun toTicketInfo(rawPayload: String): TicketInfo = TicketInfo(
        ticketId = ticketId,
        eventId = eventId,
        eventTitle = eventTitle,
        venue = venue,
        dateTime = dateTime,
        row = row,
        seat = seat,
        sector = sector,
        holderName = holderName,
        orderId = orderId,
        validationStatus = status?.toValidationStatus() ?: ValidationStatus.Unknown,
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
