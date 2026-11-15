package ru.cora.scanner.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import ru.cora.scanner.data.local.dao.BarcodeIndexDao
import ru.cora.scanner.data.local.dao.PlaceTicketDao
import ru.cora.scanner.data.local.entity.BarcodeIndexEntity
import ru.cora.scanner.data.local.entity.PlaceTicketEntity
import ru.cora.scanner.data.remote.ExternalBarcodeImportDto
import ru.cora.scanner.data.remote.ExternalBarcodeImportRequest
import ru.cora.scanner.data.remote.TicketApiService
import ru.cora.scanner.domain.BarcodeNormalizer

class ExternalBarcodeImporter(
    private val api: TicketApiService,
    private val placeTicketDao: PlaceTicketDao,
    private val barcodeIndexDao: BarcodeIndexDao,
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun importFromJson(concertId: String, jsonBody: String): Result<Int> = runCatching {
        val rows = parseBarcodeFile(jsonBody)
        if (rows.isEmpty()) error("В файле нет билетов")
        applyLocal(concertId, rows.map { it.toDto() })
    }

    private fun parseBarcodeFile(jsonBody: String): List<ExternalBarcodeRow> {
        val trimmed = jsonBody.trim()
        if (trimmed.isEmpty()) return emptyList()

        val element = json.parseToJsonElement(trimmed)
        val arr = element.jsonArray
        if (arr.isNotEmpty() && runCatching { arr.first().jsonPrimitive.isString }.getOrDefault(false)) {
            return arr.map { item ->
                ExternalBarcodeRow(barcode = item.jsonPrimitive.content)
            }
        }

        return json.decodeFromJsonElement<List<ExternalBarcodeRow>>(element)
    }

    suspend fun importViaApi(concertId: String, rows: List<ExternalBarcodeImportDto>): Result<Int> =
        runCatching {
            val response = api.importExternalBarcodes(
                concertId,
                ExternalBarcodeImportRequest(rows),
            )
            applyLocal(
                concertId,
                rows,
            )
            response.places.size.coerceAtLeast(rows.size)
        }

    private suspend fun applyLocal(
        concertId: String,
        rows: List<ExternalBarcodeImportDto>,
    ): Int {
        val barcodes = mutableListOf<BarcodeIndexEntity>()
        val places = mutableListOf<PlaceTicketEntity>()

        rows.forEachIndexed { index, row ->
            val placeId = row.placeId ?: "ext-$index-${row.barcode.takeLast(8)}"
            val code = BarcodeNormalizer.normalize(row.barcode)
            barcodes += BarcodeIndexEntity(
                barcode = code,
                concertId = concertId,
                placeId = placeId,
                source = "external",
            )
            places += PlaceTicketEntity(
                concertId = concertId,
                placeId = placeId,
                rowLabel = row.row,
                seatLabel = row.seat,
                sector = row.sector,
                isPushkin = row.isPushkin,
                isRedeemed = false,
                redeemedAtEpochMs = null,
                pendingSync = false,
            )
        }

        placeTicketDao.upsertAll(places)
        barcodeIndexDao.upsertAll(barcodes)
        return rows.size
    }
}

@Serializable
data class ExternalBarcodeRow(
    val barcode: String,
    val placeId: String? = null,
    val row: String? = null,
    val seat: String? = null,
    val sector: String? = null,
    val isPushkin: Boolean = false,
) {
    fun toDto() = ExternalBarcodeImportDto(
        barcode = barcode,
        placeId = placeId,
        row = row,
        seat = seat,
        sector = sector,
        isPushkin = isPushkin,
    )
}
