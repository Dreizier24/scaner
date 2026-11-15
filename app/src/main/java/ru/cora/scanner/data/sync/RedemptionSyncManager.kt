package ru.cora.scanner.data.sync

import ru.cora.scanner.data.local.dao.ConcertDao
import ru.cora.scanner.data.local.dao.PendingRedemptionDao
import ru.cora.scanner.data.remote.RedemptionItemDto
import ru.cora.scanner.data.remote.RedemptionSyncRequest
import ru.cora.scanner.data.remote.TicketApiService

class RedemptionSyncManager(
    private val api: TicketApiService,
    private val pendingRedemptionDao: PendingRedemptionDao,
    private val concertDao: ConcertDao,
) {

    suspend fun flushAll(): Int {
        val pending = pendingRedemptionDao.getAll()
        if (pending.isEmpty()) return 0

        val byConcert = pending.groupBy { it.concertId }
        var synced = 0

        for ((concertId, items) in byConcert) {
            val localRev = concertDao.findById(concertId)?.manifestRevision
            val result = runCatching {
                api.syncRedemptions(
                    concertId,
                    RedemptionSyncRequest(
                        concertId = concertId,
                        clientRevision = localRev,
                        items = items.map {
                            RedemptionItemDto(
                                placeId = it.placeId,
                                barcode = it.barcode,
                                isPushkin = it.isPushkin,
                                scannedAtEpochMs = it.scannedAtEpochMs,
                            )
                        },
                    ),
                )
            }
            if (result.isSuccess) {
                items.forEach { pendingRedemptionDao.delete(it.id) }
                synced += result.getOrThrow().accepted
            }
        }
        return synced
    }
}
