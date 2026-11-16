package ru.cora.scanner.data

import androidx.room.withTransaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ru.cora.scanner.data.local.KoraDatabase
import ru.cora.scanner.data.local.entity.ConcertEntity

class ExternalEventImporter(
    private val database: KoraDatabase,
    private val barcodeImporter: ExternalBarcodeImporter,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun importFromFile(jsonBody: String): Result<ImportEventResult> = runCatching {
        val parsed = parseEventFile(jsonBody)
        val concertId = parsed.id ?: "local-${UUID.randomUUID().toString().take(12)}"
        val now = System.currentTimeMillis()
        val title = parsed.title ?: defaultTitle()

        database.withTransaction {
            database.concertDao().upsert(
                ConcertEntity(
                    concertId = concertId,
                    title = title,
                    venue = parsed.venue,
                    dateTime = parsed.dateTime,
                    manifestRevision = 1,
                    lastSyncedAtEpochMs = now,
                    ticketsLoadedAtEpochMs = now,
                    source = ConcertEntity.SOURCE_FILE,
                ),
            )
            database.placeTicketDao().deleteForConcert(concertId)
            database.barcodeIndexDao().deleteForConcert(concertId)
        }

        val count = barcodeImporter.importFromJson(concertId, parsed.ticketsJson).getOrThrow()

        ImportEventResult(
            concertId = concertId,
            title = title,
            ticketCount = count,
        )
    }

    private fun defaultTitle(): String {
        val fmt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru", "RU"))
        return "Стороннее мероприятие ${fmt.format(Date())}"
    }

    private fun parseEventFile(jsonBody: String): ParsedEventFile {
        val trimmed = jsonBody.trim()
        val element: JsonElement = json.parseToJsonElement(trimmed)

        if (element is JsonObject) {
            val dto = json.decodeFromJsonElement<ExternalEventFile>(element)
            val ticketsJson = when {
                !dto.tickets.isNullOrEmpty() -> json.encodeToString(
                    ListSerializer(ExternalBarcodeRow.serializer()),
                    dto.tickets,
                )
                !dto.barcodes.isNullOrEmpty() -> json.encodeToString(
                    ListSerializer(String.serializer()),
                    dto.barcodes,
                )
                else -> error("В файле нет списка tickets или barcodes")
            }
            return ParsedEventFile(
                id = dto.id,
                title = dto.title,
                venue = dto.venue,
                dateTime = dto.dateTime,
                ticketsJson = ticketsJson,
            )
        }

        if (element.jsonArray.isNotEmpty()) {
            return ParsedEventFile(
                title = defaultTitle(),
                ticketsJson = trimmed,
            )
        }

        error("Неверный формат файла")
    }

    private data class ParsedEventFile(
        val id: String? = null,
        val title: String? = null,
        val venue: String? = null,
        val dateTime: String? = null,
        val ticketsJson: String,
    )
}

@Serializable
data class ExternalEventFile(
    val id: String? = null,
    val title: String? = null,
    val venue: String? = null,
    val dateTime: String? = null,
    val tickets: List<ExternalBarcodeRow>? = null,
    val barcodes: List<String>? = null,
)

data class ImportEventResult(
    val concertId: String,
    val title: String,
    val ticketCount: Int,
)