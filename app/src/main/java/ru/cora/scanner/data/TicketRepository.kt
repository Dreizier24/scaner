package ru.cora.scanner.data

import ru.cora.scanner.data.local.dao.CachedTicketDao
import ru.cora.scanner.data.local.dao.ScanHistoryDao
import ru.cora.scanner.data.local.entity.ScanHistoryEntity
import ru.cora.scanner.data.remote.TicketApiService
import ru.cora.scanner.data.remote.ValidateTicketRequest
import ru.cora.scanner.data.sync.OfflineSyncManager
import ru.cora.scanner.domain.QrPayloadParser
import ru.cora.scanner.domain.model.ScanSource
import ru.cora.scanner.domain.model.TicketInfo
import ru.cora.scanner.domain.model.ValidationStatus

class TicketRepository(
    private val api: TicketApiService,
    private val cachedTicketDao: CachedTicketDao,
    private val scanHistoryDao: ScanHistoryDao,
    private val networkMonitor: NetworkMonitor,
    private val offlineSyncManager: OfflineSyncManager,
) {

    val recentScans = scanHistoryDao.observeRecent()

    suspend fun resolveScannedPayload(raw: String, isOnline: Boolean): TicketInfo {
        val parsed = QrPayloadParser.parse(raw)
        val ticketId = parsed.ticketId

        if (isOnline) {
            val remoteResult = runCatching {
                api.validateTicket(
                    ValidateTicketRequest(
                        ticketId = ticketId,
                        rawPayload = raw,
                        scannedAtEpochMs = System.currentTimeMillis(),
                    ),
                ).toTicketInfo(raw)
            }
            if (remoteResult.isSuccess) {
                val info = remoteResult.getOrThrow().copy(source = ScanSource.RemoteApi)
                persistScan(info, wasOffline = false)
                return info
            }
        }

        if (ticketId != null) {
            cachedTicketDao.findById(ticketId)?.let { cached ->
                val info = TicketInfo(
                    ticketId = cached.ticketId,
                    eventId = cached.eventId,
                    eventTitle = cached.eventTitle,
                    venue = cached.venue,
                    dateTime = cached.dateTime,
                    seat = cached.seat,
                    sector = cached.sector,
                    holderName = cached.holderName,
                    orderId = cached.orderId,
                    validationStatus = cached.status.toValidationStatus(),
                    source = ScanSource.LocalCache,
                    rawPayload = raw,
                )
                if (!isOnline) {
                    offlineSyncManager.enqueuePending(raw, ticketId)
                }
                persistScan(info.copy(validationStatus = offlineStatus(isOnline, info.validationStatus)), wasOffline = !isOnline)
                return info.copy(validationStatus = offlineStatus(isOnline, info.validationStatus))
            }
        }

        val fallback = parsed.copy(
            validationStatus = if (isOnline) ValidationStatus.Unknown else ValidationStatus.PendingSync,
            source = ScanSource.QrPayload,
        )
        if (!isOnline) {
            offlineSyncManager.enqueuePending(raw, ticketId)
        }
        persistScan(fallback, wasOffline = !isOnline)
        return fallback
    }

    suspend fun refreshOfflinePack() = offlineSyncManager.refreshOfflinePack()

    suspend fun flushPendingWhenOnline() = offlineSyncManager.flushPendingValidations()

    private suspend fun persistScan(info: TicketInfo, wasOffline: Boolean) {
        scanHistoryDao.insert(
            ScanHistoryEntity(
                concertId = info.concertId ?: info.eventId,
                ticketId = info.ticketId,
                eventTitle = info.eventTitle,
                rowLabel = info.row,
                seatLabel = info.seat,
                status = info.validationStatus.name,
                rawPayload = info.rawPayload,
                scannedAtEpochMs = System.currentTimeMillis(),
                wasOffline = wasOffline,
            ),
        )
    }

    private fun offlineStatus(isOnline: Boolean, status: ValidationStatus): ValidationStatus {
        if (isOnline) return status
        return when (status) {
            ValidationStatus.Valid,
            ValidationStatus.AlreadyUsed,
            ValidationStatus.Expired,
            -> status
            else -> ValidationStatus.PendingSync
        }
    }

    private fun String.toValidationStatus(): ValidationStatus = when (lowercase()) {
        "valid", "ok" -> ValidationStatus.Valid
        "used", "already_used" -> ValidationStatus.AlreadyUsed
        "not_found" -> ValidationStatus.NotFound
        "expired" -> ValidationStatus.Expired
        else -> ValidationStatus.Unknown
    }
}
