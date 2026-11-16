package ru.cora.scanner.data.sync

import androidx.room.withTransaction
import ru.cora.scanner.data.local.KoraDatabase
import ru.cora.scanner.data.local.entity.BarcodeIndexEntity
import ru.cora.scanner.data.local.entity.ConcertEntity
import ru.cora.scanner.data.local.entity.PlaceTicketEntity
import ru.cora.scanner.data.remote.ConcertDto
import ru.cora.scanner.data.remote.ConcertManifestResponse
import ru.cora.scanner.data.remote.PlaceManifestDto
import ru.cora.scanner.data.remote.TicketApiService
import ru.cora.scanner.domain.BarcodeNormalizer

class ManifestSyncManager(
    private val database: KoraDatabase,
    private val api: TicketApiService,
) {

    suspend fun refreshConcerts(): Result<Int> = runCatching {
        val response = api.listConcerts()
        val now = System.currentTimeMillis()
        val entities = response.concerts.map { dto ->
            val prev = database.concertDao().findById(dto.id)
            dto.toEntity(now, manifestRevision = prev?.manifestRevision ?: 0).copy(
                ticketsLoadedAtEpochMs = prev?.ticketsLoadedAtEpochMs,
                source = prev?.source ?: ConcertEntity.SOURCE_API,
            )
        }
        database.concertDao().upsertAll(entities)
        entities.size
    }

    suspend fun syncManifest(concertId: String): Result<ManifestSyncResult> = runCatching {
        val local = database.concertDao().findById(concertId)
        if (local?.source == ru.cora.scanner.data.local.entity.ConcertEntity.SOURCE_FILE) {
            val count = database.placeTicketDao().countForConcert(concertId)
            return@runCatching ManifestSyncResult(
                placeCount = count,
                revision = local.manifestRevision,
                skippedDownload = true,
            )
        }
        val since = local?.manifestRevision?.takeIf { it > 0 }
        val response = api.getManifest(concertId, since)

        if (response.notModified || response.places.isEmpty() && since != null) {
            return@runCatching ManifestSyncResult(
                placeCount = database.placeTicketDao().countForConcert(concertId),
                revision = response.revision,
                skippedDownload = true,
            )
        }

        val now = System.currentTimeMillis()
        val concertMeta = local ?: ConcertEntity(
            concertId = concertId,
            title = concertId,
            venue = null,
            dateTime = null,
            manifestRevision = 0,
            lastSyncedAtEpochMs = now,
        )

        val places = response.places.map { it.toEntity(concertId) }
        val barcodes = response.places.flatMap { place ->
            place.barcodes.map { code ->
                BarcodeIndexEntity(
                    barcode = BarcodeNormalizer.normalize(code),
                    concertId = concertId,
                    placeId = place.placeId,
                    source = "api",
                )
            }
        }

        database.withTransaction {
            database.barcodeIndexDao().deleteForConcert(concertId)
            database.placeTicketDao().deleteForConcert(concertId)
            database.concertDao().upsert(
                concertMeta.copy(
                    manifestRevision = response.revision,
                    lastSyncedAtEpochMs = now,
                    ticketsLoadedAtEpochMs = now,
                ),
            )
            database.placeTicketDao().upsertAll(places)
            database.barcodeIndexDao().upsertAll(barcodes)
        }

        ManifestSyncResult(
            placeCount = places.size,
            revision = response.revision,
            skippedDownload = false,
        )
    }

    suspend fun checkServerRevision(concertId: String): Result<Long> = runCatching {
        api.getSyncState(concertId).revision
    }

    private fun ConcertDto.toEntity(now: Long, manifestRevision: Long) = ConcertEntity(
        concertId = id,
        title = title,
        venue = venue,
        dateTime = dateTime,
        manifestRevision = manifestRevision,
        lastSyncedAtEpochMs = now,
        source = ConcertEntity.SOURCE_API,
    )

    private fun PlaceManifestDto.toEntity(concertId: String) = PlaceTicketEntity(
        concertId = concertId,
        placeId = placeId,
        rowLabel = row,
        seatLabel = seat,
        sector = sector,
        isPushkin = isPushkin,
        isRedeemed = isRedeemed,
        redeemedAtEpochMs = if (isRedeemed) System.currentTimeMillis() else null,
        pendingSync = false,
    )
}

data class ManifestSyncResult(
    val placeCount: Int,
    val revision: Long,
    val skippedDownload: Boolean,
)
