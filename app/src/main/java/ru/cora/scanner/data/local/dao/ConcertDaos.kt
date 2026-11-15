package ru.cora.scanner.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.cora.scanner.data.local.entity.AppSettingEntity
import ru.cora.scanner.data.local.entity.BarcodeIndexEntity
import ru.cora.scanner.data.local.entity.ConcertEntity
import ru.cora.scanner.data.local.entity.PendingRedemptionEntity
import ru.cora.scanner.data.local.entity.PlaceTicketEntity

@Dao
interface ConcertDao {
    @Query("SELECT * FROM concerts ORDER BY dateTime ASC")
    fun observeAll(): Flow<List<ConcertEntity>>

    @Query("SELECT * FROM concerts WHERE concertId = :id LIMIT 1")
    suspend fun findById(id: String): ConcertEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(concert: ConcertEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(concerts: List<ConcertEntity>)
}

@Dao
interface PlaceTicketDao {
    @Query("SELECT * FROM place_tickets WHERE concertId = :concertId AND placeId = :placeId LIMIT 1")
    suspend fun find(concertId: String, placeId: String): PlaceTicketEntity?

    @Query("SELECT COUNT(*) FROM place_tickets WHERE concertId = :concertId")
    suspend fun countForConcert(concertId: String): Int

    @Query("SELECT COUNT(*) FROM place_tickets WHERE concertId = :concertId AND isRedeemed = 1")
    fun observeRedeemedCount(concertId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(places: List<PlaceTicketEntity>)

    @Query(
        """
        UPDATE place_tickets
        SET isRedeemed = 1, redeemedAtEpochMs = :at, pendingSync = 1
        WHERE concertId = :concertId AND placeId = :placeId AND isRedeemed = 0
        """,
    )
    suspend fun markRedeemed(concertId: String, placeId: String, at: Long): Int

    @Query("DELETE FROM place_tickets WHERE concertId = :concertId")
    suspend fun deleteForConcert(concertId: String)
}

@Dao
interface BarcodeIndexDao {
    @Query("SELECT * FROM barcode_index WHERE barcode = :barcode LIMIT 1")
    suspend fun find(barcode: String): BarcodeIndexEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<BarcodeIndexEntity>)

    @Query("DELETE FROM barcode_index WHERE concertId = :concertId")
    suspend fun deleteForConcert(concertId: String)
}

@Dao
interface PendingRedemptionDao {
    @Query("SELECT * FROM pending_redemptions ORDER BY scannedAtEpochMs ASC")
    suspend fun getAll(): List<PendingRedemptionEntity>

    @Query("SELECT * FROM pending_redemptions WHERE concertId = :concertId")
    suspend fun getForConcert(concertId: String): List<PendingRedemptionEntity>

    @Insert
    suspend fun insert(entry: PendingRedemptionEntity)

    @Query("DELETE FROM pending_redemptions WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface AppSettingsDao {
    @Query("SELECT value FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(setting: AppSettingEntity)
}
