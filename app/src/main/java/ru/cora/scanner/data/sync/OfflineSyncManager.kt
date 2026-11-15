package ru.cora.scanner.data.sync

import ru.cora.scanner.data.local.dao.CachedTicketDao
import ru.cora.scanner.data.local.dao.PendingValidationDao
import ru.cora.scanner.data.local.entity.CachedTicketEntity
import ru.cora.scanner.data.local.entity.PendingValidationEntity
import ru.cora.scanner.data.remote.TicketApiService
import ru.cora.scanner.data.remote.ValidateTicketRequest

/**
 * Синхронизация офлайн-данных:
 * 1. Загрузка «пакета билетов» перед сменой / мероприятием.
 * 2. Отправка накопленных сканов при появлении сети.
 */
class OfflineSyncManager(
    private val api: TicketApiService,
    private val cachedTicketDao: CachedTicketDao,
    private val pendingValidationDao: PendingValidationDao,
) {

    suspend fun refreshOfflinePack(): Result<Int> = runCatching {
        val response = api.downloadOfflinePack()
        val entities = response.tickets.mapNotNull { ticket ->
            val id = ticket.ticketId ?: return@mapNotNull null
            CachedTicketEntity(
                ticketId = id,
                eventId = ticket.eventId,
                eventTitle = ticket.eventTitle,
                venue = ticket.venue,
                dateTime = ticket.dateTime,
                seat = ticket.seat,
                sector = ticket.sector,
                holderName = ticket.holderName,
                orderId = ticket.orderId,
                status = ticket.status,
                rawPayload = "",
                syncedAtEpochMs = response.syncedAtEpochMs,
            )
        }
        cachedTicketDao.upsertAll(entities)
        entities.size
    }

    suspend fun flushPendingValidations(): Int {
        val pending = pendingValidationDao.getAll()
        var synced = 0
        for (item in pending) {
            val result = runCatching {
                api.validateTicket(
                    ValidateTicketRequest(
                        ticketId = item.ticketId,
                        rawPayload = item.rawPayload,
                        scannedAtEpochMs = item.scannedAtEpochMs,
                    ),
                )
            }
            if (result.isSuccess) {
                pendingValidationDao.delete(item.id)
                synced++
            }
        }
        return synced
    }

    suspend fun enqueuePending(rawPayload: String, ticketId: String?) {
        pendingValidationDao.insert(
            PendingValidationEntity(
                ticketId = ticketId,
                rawPayload = rawPayload,
                scannedAtEpochMs = System.currentTimeMillis(),
            ),
        )
    }
}
