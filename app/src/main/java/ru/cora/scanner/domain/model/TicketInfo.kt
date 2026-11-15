package ru.cora.scanner.domain.model

data class TicketInfo(
    val ticketId: String? = null,
    val placeId: String? = null,
    val concertId: String? = null,
    val eventId: String? = null,
    val eventTitle: String? = null,
    val venue: String? = null,
    val dateTime: String? = null,
    val row: String? = null,
    val seat: String? = null,
    val sector: String? = null,
    val holderName: String? = null,
    val orderId: String? = null,
    val ticketKind: TicketKind = TicketKind.Unknown,
    val validationStatus: ValidationStatus = ValidationStatus.Unknown,
    val source: ScanSource = ScanSource.QrPayload,
    val rawPayload: String,
)

enum class ValidationStatus {
    Valid,
    AlreadyUsed,
    NotFound,
    Expired,
    Unknown,
    PendingSync,
}

enum class ScanSource {
    QrPayload,
    LocalCache,
    RemoteApi,
}
