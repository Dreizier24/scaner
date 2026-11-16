package ru.cora.scanner.data

import kotlinx.coroutines.flow.Flow
import ru.cora.scanner.data.gosuslugi.GosuslugiDigitalIdService
import ru.cora.scanner.data.local.dao.ConcertDao
import ru.cora.scanner.data.local.dao.PendingRedemptionDao
import ru.cora.scanner.data.local.dao.PlaceTicketDao
import ru.cora.scanner.data.local.dao.ScanHistoryDao
import ru.cora.scanner.data.local.dao.BarcodeIndexDao
import ru.cora.scanner.data.local.entity.ConcertEntity
import ru.cora.scanner.data.local.entity.PendingRedemptionEntity
import ru.cora.scanner.data.local.entity.ScanHistoryEntity
import ru.cora.scanner.data.pushkin.PushkinRedemptionService
import ru.cora.scanner.data.sync.LocalTicketRetention
import ru.cora.scanner.data.sync.ManifestSyncManager
import ru.cora.scanner.data.sync.ManifestSyncResult
import ru.cora.scanner.data.sync.RedemptionSyncManager
import ru.cora.scanner.domain.BarcodeNormalizer
import ru.cora.scanner.domain.PushkinTicketDetector
import ru.cora.scanner.domain.QrPayloadParser
import ru.cora.scanner.domain.model.ScanHistoryEntry
import ru.cora.scanner.domain.model.ScanSource
import ru.cora.scanner.domain.model.TicketInfo
import ru.cora.scanner.domain.model.TicketKind
import ru.cora.scanner.domain.model.ValidationStatus

class ConcertScanRepository(
    private val ticketRepository: TicketRepository,
    private val concertDao: ConcertDao,
    private val placeTicketDao: PlaceTicketDao,
    private val barcodeIndexDao: BarcodeIndexDao,
    private val scanHistoryDao: ScanHistoryDao,
    private val pendingRedemptionDao: PendingRedemptionDao,
    private val manifestSyncManager: ManifestSyncManager,
    private val redemptionSyncManager: RedemptionSyncManager,
    private val localTicketRetention: LocalTicketRetention,
    private val externalBarcodeImporter: ExternalBarcodeImporter,
    private val externalEventImporter: ExternalEventImporter,
    private val pushkinRedemptionService: PushkinRedemptionService,
    private val gosuslugiDigitalIdService: GosuslugiDigitalIdService,
) {

    val recentScans: Flow<List<ScanHistoryEntity>> = scanHistoryDao.observeRecent()
    val concerts: Flow<List<ConcertEntity>> = concertDao.observeAll()

    suspend fun getConcertTitle(concertId: String): String? =
        concertDao.findById(concertId)?.title

    fun observeRedeemedCount(concertId: String) = placeTicketDao.observeRedeemedCount(concertId)

    fun observeTicketCount(concertId: String) = placeTicketDao.observeTicketCount(concertId)

    fun observePendingRedemptions(concertId: String) = pendingRedemptionDao.observePendingCount(concertId)

    suspend fun purgeTicketsAfterMidnight() = localTicketRetention.purgeTicketsAfterMidnight()

    suspend fun refreshConcerts() = manifestSyncManager.refreshConcerts()

    suspend fun loadManifest(concertId: String): Result<ManifestSyncResult> =
        manifestSyncManager.syncManifest(concertId)

    suspend fun importEventFromFile(json: String) = externalEventImporter.importFromFile(json)

    suspend fun isFileConcert(concertId: String): Boolean =
        concertDao.findById(concertId)?.source == ConcertEntity.SOURCE_FILE

    suspend fun flushRedemptions() = redemptionSyncManager.flushAll()

    suspend fun applyRemoteRedemption(concertId: String, placeId: String, scannedAtEpochMs: Long) {
        placeTicketDao.applyRemoteRedemption(concertId, placeId, scannedAtEpochMs)
        pendingRedemptionDao.deleteForPlace(concertId, placeId)
    }

    suspend fun flushPendingWhenOnline() {
        redemptionSyncManager.flushAll()
        ticketRepository.flushPendingWhenOnline()
    }

    suspend fun resolveScan(
        concertId: String?,
        raw: String,
        isOnline: Boolean,
        modeDigitalId: Boolean = false,
        exitOnly: Boolean = false,
    ): TicketInfo {
        val normalized = BarcodeNormalizer.normalize(raw)

        if (modeDigitalId || gosuslugiDigitalIdService.looksLikeGosuslugiSession(normalized)) {
            val status = gosuslugiDigitalIdService.validate(normalized, isOnline)
            return TicketInfo(
                validationStatus = status,
                source = ScanSource.RemoteApi,
                rawPayload = raw,
                ticketKind = TicketKind.Unknown,
            )
        }

        if (concertId != null) {
            scanViaLocalManifest(concertId, normalized, raw, isOnline, exitOnly)?.let { return it }
        }

        val legacy = ticketRepository.resolveScannedPayload(raw, isOnline)
        val kind = when {
            legacy.ticketKind != TicketKind.Unknown -> legacy.ticketKind
            PushkinTicketDetector.isPushkinPayload(raw) -> TicketKind.Pushkin
            else -> TicketKind.Standard
        }
        return legacy.copy(ticketKind = kind)
    }

    private suspend fun scanViaLocalManifest(
        concertId: String,
        normalized: String,
        raw: String,
        isOnline: Boolean,
        exitOnly: Boolean,
    ): TicketInfo? {
        val index = barcodeIndexDao.find(normalized)
            ?: barcodeIndexDao.find(raw.trim())
            ?: return null

        if (index.concertId != concertId) {
            return TicketInfo(
                concertId = concertId,
                validationStatus = ValidationStatus.NotFound,
                source = ScanSource.LocalCache,
                rawPayload = raw,
                ticketKind = TicketKind.ExternalBarcode,
                eventTitle = "Билет другого мероприятия",
            )
        }

        val place = placeTicketDao.find(concertId, index.placeId) ?: return null
        val concert = concertDao.findById(concertId)

        val kind = when {
            place.isPushkin -> TicketKind.Pushkin
            index.source == "external" -> TicketKind.ExternalBarcode
            else -> TicketKind.Standard
        }

        if (place.isRedeemed) {
            val info = buildTicketInfo(concert, place, kind, ValidationStatus.AlreadyUsed, raw, ScanSource.LocalCache)
            return finishScan(info, wasOffline = !isOnline)
        }

        val now = System.currentTimeMillis()
        if (exitOnly) {
            val info = buildTicketInfo(
                concert,
                place,
                kind,
                ValidationStatus.Valid,
                raw,
                ScanSource.LocalCache,
            )
            return finishScan(info, wasOffline = !isOnline)
        }

        val updated = placeTicketDao.markRedeemed(concertId, place.placeId, now)
        if (updated == 0) {
            val info = buildTicketInfo(concert, place, kind, ValidationStatus.AlreadyUsed, raw, ScanSource.LocalCache)
            return finishScan(info, wasOffline = !isOnline)
        }

        pendingRedemptionDao.insert(
            PendingRedemptionEntity(
                concertId = concertId,
                placeId = place.placeId,
                barcode = normalized,
                isPushkin = place.isPushkin,
                scannedAtEpochMs = now,
            ),
        )

        var status = ValidationStatus.Valid
        if (place.isPushkin) {
            val pushkinStatus = pushkinRedemptionService.redeemIfOnline(
                concertId,
                place.placeId,
                normalized,
                isOnline,
            )
            status = when (pushkinStatus) {
                ValidationStatus.Valid -> ValidationStatus.Valid
                ValidationStatus.AlreadyUsed -> ValidationStatus.AlreadyUsed
                ValidationStatus.PendingSync -> ValidationStatus.Valid
                else -> pushkinStatus
            }
        }

        val info = buildTicketInfo(concert, place, kind, status, raw, ScanSource.LocalCache)
        val result = finishScan(info, wasOffline = !isOnline)

        if (isOnline) {
            runCatching { flushRedemptions() }
        }

        return result
    }

    private fun buildTicketInfo(
        concert: ConcertEntity?,
        place: ru.cora.scanner.data.local.entity.PlaceTicketEntity,
        kind: TicketKind,
        status: ValidationStatus,
        raw: String,
        source: ScanSource,
    ) = TicketInfo(
        ticketId = place.placeId,
        placeId = place.placeId,
        concertId = place.concertId,
        eventId = place.concertId,
        eventTitle = concert?.title,
        venue = concert?.venue,
        dateTime = concert?.dateTime,
        row = place.rowLabel,
        seat = place.seatLabel,
        sector = place.sector,
        ticketKind = kind,
        validationStatus = status,
        source = source,
        rawPayload = raw,
    )

    private suspend fun finishScan(info: TicketInfo, wasOffline: Boolean): TicketInfo {
        persistScan(info, wasOffline)
        return if (info.validationStatus == ValidationStatus.AlreadyUsed) {
            enrichScanHistory(info)
        } else {
            info
        }
    }

    private suspend fun enrichScanHistory(info: TicketInfo): TicketInfo {
        val concertId = info.concertId ?: return info
        val placeId = info.placeId ?: info.ticketId ?: return info
        val rows = scanHistoryDao.getForPlace(concertId, placeId)
        return info.copy(
            scanHistoryCount = rows.size,
            scanHistory = rows.map { row ->
                ScanHistoryEntry(
                    scannedAtEpochMs = row.scannedAtEpochMs,
                    status = row.status.toValidationStatus(),
                    wasOffline = row.wasOffline,
                )
            },
        )
    }

    private fun String.toValidationStatus(): ValidationStatus = runCatching {
        ValidationStatus.valueOf(this)
    }.getOrDefault(ValidationStatus.Unknown)

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
}
