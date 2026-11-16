package ru.cora.scanner.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WsRedemptionMessage(
    val type: String,
    val concertId: String,
    val placeId: String,
    val barcode: String? = null,
    val scannedAtEpochMs: Long,
)