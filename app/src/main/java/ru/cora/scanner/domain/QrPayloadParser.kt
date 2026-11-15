package ru.cora.scanner.domain

import android.net.Uri
import kotlinx.serialization.json.Json
import ru.cora.scanner.domain.model.TicketInfo
import ru.cora.scanner.domain.model.ValidationStatus

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

object QrPayloadParser {

    fun parse(raw: String): TicketInfo {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            return TicketInfo(rawPayload = raw, validationStatus = ValidationStatus.Unknown)
        }

        parseJson(trimmed)?.let { return it.copy(rawPayload = raw) }

        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return parseUrl(trimmed).copy(rawPayload = raw)
        }

        return TicketInfo(
            eventTitle = trimmed.takeIf { it.length <= 120 },
            rawPayload = raw,
            validationStatus = ValidationStatus.Unknown,
        )
    }

    private fun parseJson(raw: String): TicketInfo? {
        if (!raw.startsWith("{")) return null
        return runCatching {
            json.decodeFromString(QrTicketJson.serializer(), raw).toTicketInfo(raw)
        }.getOrNull()
    }

    private fun parseUrl(url: String): TicketInfo {
        val uri = Uri.parse(url)
        return TicketInfo(
            ticketId = uri.getQueryParameter("ticket") ?: uri.getQueryParameter("id"),
            eventId = uri.getQueryParameter("event"),
            eventTitle = uri.getQueryParameter("title"),
            venue = uri.getQueryParameter("venue"),
            dateTime = uri.getQueryParameter("datetime") ?: uri.getQueryParameter("date"),
            row = uri.getQueryParameter("row") ?: uri.getQueryParameter("ryad"),
            seat = uri.getQueryParameter("seat") ?: uri.getQueryParameter("mesto"),
            sector = uri.getQueryParameter("sector"),
            holderName = uri.getQueryParameter("holder"),
            orderId = uri.getQueryParameter("order"),
            rawPayload = url,
        )
    }
}
