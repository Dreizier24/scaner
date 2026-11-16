package ru.cora.scanner.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import ru.cora.scanner.data.local.dao.AppSettingsDao
import ru.cora.scanner.data.local.dao.BarcodeIndexDao
import ru.cora.scanner.data.local.dao.CachedTicketDao
import ru.cora.scanner.data.local.dao.ConcertDao
import ru.cora.scanner.data.local.dao.PendingRedemptionDao
import ru.cora.scanner.data.local.dao.PendingValidationDao
import ru.cora.scanner.data.local.dao.PlaceTicketDao
import ru.cora.scanner.data.local.dao.ScanHistoryDao
import ru.cora.scanner.data.local.entity.AppSettingEntity
import ru.cora.scanner.data.local.entity.BarcodeIndexEntity
import ru.cora.scanner.data.local.entity.CachedTicketEntity
import ru.cora.scanner.data.local.entity.ConcertEntity
import ru.cora.scanner.data.local.entity.PendingRedemptionEntity
import ru.cora.scanner.data.local.entity.PendingValidationEntity
import ru.cora.scanner.data.local.entity.PlaceTicketEntity
import ru.cora.scanner.data.local.entity.ScanHistoryEntity

@Database(
    entities = [
        CachedTicketEntity::class,
        ScanHistoryEntity::class,
        PendingValidationEntity::class,
        ConcertEntity::class,
        PlaceTicketEntity::class,
        BarcodeIndexEntity::class,
        PendingRedemptionEntity::class,
        AppSettingEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class KoraDatabase : RoomDatabase() {
    abstract fun cachedTicketDao(): CachedTicketDao
    abstract fun scanHistoryDao(): ScanHistoryDao
    abstract fun pendingValidationDao(): PendingValidationDao
    abstract fun concertDao(): ConcertDao
    abstract fun placeTicketDao(): PlaceTicketDao
    abstract fun barcodeIndexDao(): BarcodeIndexDao
    abstract fun pendingRedemptionDao(): PendingRedemptionDao
    abstract fun appSettingsDao(): AppSettingsDao
}
