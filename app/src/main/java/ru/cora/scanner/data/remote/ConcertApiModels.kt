package ru.cora.scanner.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class ConcertDto(
    val id: String,
    val title: String,
    val venue: String? = null,
    val dateTime: String? = null,
)

@Serializable
data class ConcertsResponse(
    val concerts: List<ConcertDto> = emptyList(),
)

@Serializable
data class PlaceManifestDto(
    val placeId: String,
    val row: String? = null,
    val seat: String? = null,
    val sector: String? = null,
    val barcodes: List<String> = emptyList(),
    val isPushkin: Boolean = false,
    val isRedeemed: Boolean = false,
)

@Serializable
data class ConcertManifestResponse(
    val concertId: String,
    val revision: Long,
    val notModified: Boolean = false,
    val places: List<PlaceManifestDto> = emptyList(),
)

@Serializable
data class SyncStateResponse(
    val concertId: String,
    val revision: Long,
)

@Serializable
data class RedemptionItemDto(
    val placeId: String,
    val barcode: String,
    val isPushkin: Boolean,
    val scannedAtEpochMs: Long,
)

@Serializable
data class RedemptionSyncRequest(
    val concertId: String,
    val clientRevision: Long?,
    val items: List<RedemptionItemDto>,
)

@Serializable
data class RedemptionSyncResponse(
    val accepted: Int,
    val serverRevision: Long,
)

@Serializable
data class PushkinRedeemRequest(
    val concertId: String,
    val placeId: String,
    val barcode: String,
    val scannedAtEpochMs: Long,
)

@Serializable
data class PushkinRedeemResponse(
    val status: String,
    val message: String? = null,
)

@Serializable
data class ExternalBarcodeImportDto(
    val barcode: String,
    val placeId: String? = null,
    val row: String? = null,
    val seat: String? = null,
    val sector: String? = null,
    val isPushkin: Boolean = false,
)

@Serializable
data class ExternalBarcodeImportRequest(
    val barcodes: List<ExternalBarcodeImportDto>,
)

@Serializable
data class GosuslugiIdValidateRequest(
    val sessionId: String,
    val scannedAtEpochMs: Long,
)

@Serializable
data class GosuslugiIdValidateResponse(
    val status: String,
    val details: String? = null,
)
