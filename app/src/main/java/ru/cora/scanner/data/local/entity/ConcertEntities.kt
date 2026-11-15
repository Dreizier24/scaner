package ru.cora.scanner.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "concerts")
data class ConcertEntity(
    @PrimaryKey val concertId: String,
    val title: String,
    val venue: String?,
    val dateTime: String?,
    val manifestRevision: Long,
    val lastSyncedAtEpochMs: Long,
)

@Entity(
    tableName = "place_tickets",
    primaryKeys = ["concertId", "placeId"],
    indices = [Index("concertId")],
)
data class PlaceTicketEntity(
    val concertId: String,
    val placeId: String,
    val rowLabel: String?,
    val seatLabel: String?,
    val sector: String?,
    val isPushkin: Boolean,
    val isRedeemed: Boolean,
    val redeemedAtEpochMs: Long?,
    val pendingSync: Boolean,
)

@Entity(
    tableName = "barcode_index",
    primaryKeys = ["barcode"],
    indices = [Index("concertId"), Index("placeId")],
)
data class BarcodeIndexEntity(
    val barcode: String,
    val concertId: String,
    val placeId: String,
    val source: String,
)

@Entity(tableName = "pending_redemptions")
data class PendingRedemptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val concertId: String,
    val placeId: String,
    val barcode: String,
    val isPushkin: Boolean,
    val scannedAtEpochMs: Long,
    val retryCount: Int = 0,
)

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey val key: String,
    val value: String,
)
