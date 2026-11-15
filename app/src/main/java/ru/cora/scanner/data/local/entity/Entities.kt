package ru.cora.scanner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_tickets")
data class CachedTicketEntity(
    @PrimaryKey val ticketId: String,
    val eventId: String?,
    val eventTitle: String?,
    val venue: String?,
    val dateTime: String?,
    val seat: String?,
    val sector: String?,
    val holderName: String?,
    val orderId: String?,
    val status: String,
    val rawPayload: String,
    val syncedAtEpochMs: Long,
)

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val concertId: String? = null,
    val ticketId: String?,
    val eventTitle: String?,
    val rowLabel: String? = null,
    val seatLabel: String? = null,
    val status: String,
    val rawPayload: String,
    val scannedAtEpochMs: Long,
    val wasOffline: Boolean,
)

@Entity(tableName = "pending_validations")
data class PendingValidationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ticketId: String?,
    val rawPayload: String,
    val scannedAtEpochMs: Long,
    val retryCount: Int = 0,
)
