package ru.cora.scanner.data.gosuslugi

import ru.cora.scanner.data.remote.GosuslugiIdValidateRequest
import ru.cora.scanner.data.remote.TicketApiService
import ru.cora.scanner.domain.model.ValidationStatus

/**
 * Проверка цифрового ID через API КОРА (прокси к Госуслугам / MAX).
 * Включится отдельным режимом сканера, когда бэкенд будет готов.
 */
class GosuslugiDigitalIdService(
    private val api: TicketApiService,
) {

    fun looksLikeGosuslugiSession(raw: String): Boolean {
        val trimmed = raw.trim()
        return trimmed.contains("gosuslugi.ru/m", ignoreCase = true)
    }

    suspend fun validate(sessionId: String, isOnline: Boolean): ValidationStatus {
        if (!isOnline) return ValidationStatus.PendingSync
        return runCatching {
            api.validateDigitalId(
                GosuslugiIdValidateRequest(
                    sessionId = sessionId,
                    scannedAtEpochMs = System.currentTimeMillis(),
                ),
            )
        }.map { response ->
            when (response.status.lowercase()) {
                "ok", "valid" -> ValidationStatus.Valid
                "expired" -> ValidationStatus.Expired
                else -> ValidationStatus.Unknown
            }
        }.getOrDefault(ValidationStatus.Unknown)
    }
}
