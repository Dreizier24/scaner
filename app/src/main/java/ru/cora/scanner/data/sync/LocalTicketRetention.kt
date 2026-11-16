package ru.cora.scanner.data.sync

import androidx.room.withTransaction
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import ru.cora.scanner.data.local.KoraDatabase

/**
 * Удаляет с устройства только загруженные билеты (места и штрихкоды),
 * если с момента загрузки наступила следующая календарная полуночь.
 * Список мероприятий и история сканов не трогаются.
 */
class LocalTicketRetention(
    private val database: KoraDatabase,
) {
    suspend fun purgeTicketsAfterMidnight(): Int {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        var purgedConcerts = 0

        database.withTransaction {
            val concerts = database.concertDao().listAll()
            for (concert in concerts) {
                val loadedAt = concert.ticketsLoadedAtEpochMs ?: continue
                val loadDay = Instant.ofEpochMilli(loadedAt).atZone(zone).toLocalDate()
                if (!today.isAfter(loadDay)) continue

                database.placeTicketDao().deleteForConcert(concert.concertId)
                database.barcodeIndexDao().deleteForConcert(concert.concertId)
                database.pendingRedemptionDao().deleteForConcert(concert.concertId)
                database.concertDao().upsert(
                    concert.copy(
                        ticketsLoadedAtEpochMs = null,
                        manifestRevision = 0,
                    ),
                )
                purgedConcerts++
            }
        }
        return purgedConcerts
    }
}