package ru.cora.scanner.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.cora.scanner.data.local.entity.CachedTicketEntity
import ru.cora.scanner.data.local.entity.PendingValidationEntity
import ru.cora.scanner.data.local.entity.ScanHistoryEntity

@Dao
interface CachedTicketDao {
    @Query("SELECT * FROM cached_tickets WHERE ticketId = :ticketId LIMIT 1")
    suspend fun findById(ticketId: String): CachedTicketEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(ticket: CachedTicketEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tickets: List<CachedTicketEntity>)
}

@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY scannedAtEpochMs DESC LIMIT 50")
    fun observeRecent(): Flow<List<ScanHistoryEntity>>

    @Insert
    suspend fun insert(entry: ScanHistoryEntity)

    @Query(
        """
        SELECT * FROM scan_history
        WHERE concertId = :concertId AND ticketId = :placeId
        ORDER BY scannedAtEpochMs DESC
        """,
    )
    suspend fun getForPlace(concertId: String, placeId: String): List<ScanHistoryEntity>
}

@Dao
interface PendingValidationDao {
    @Query("SELECT * FROM pending_validations ORDER BY scannedAtEpochMs ASC")
    suspend fun getAll(): List<PendingValidationEntity>

    @Insert
    suspend fun insert(entry: PendingValidationEntity)

    @Query("DELETE FROM pending_validations WHERE id = :id")
    suspend fun delete(id: Long)
}
