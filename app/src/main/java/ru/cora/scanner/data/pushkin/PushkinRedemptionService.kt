package ru.cora.scanner.data.pushkin

import ru.cora.scanner.data.remote.PushkinRedeemRequest
import ru.cora.scanner.data.remote.TicketApiService
import ru.cora.scanner.domain.model.ValidationStatus

class PushkinRedemptionService(
    private val api: TicketApiService,
) {

    suspend fun redeemIfOnline(
        concertId: String,
        placeId: String,
        barcode: String,
        isOnline: Boolean,
    ): ValidationStatus {
        if (!isOnline) return ValidationStatus.PendingSync
        return runCatching {
            api.redeemPushkin(
                PushkinRedeemRequest(
                    concertId = concertId,
                    placeId = placeId,
                    barcode = barcode,
                    scannedAtEpochMs = System.currentTimeMillis(),
                ),
            )
        }.map { response ->
            when (response.status.lowercase()) {
                "ok", "valid", "redeemed" -> ValidationStatus.Valid
                "used", "already_used" -> ValidationStatus.AlreadyUsed
                "expired" -> ValidationStatus.Expired
                else -> ValidationStatus.Unknown
            }
        }.getOrDefault(ValidationStatus.PendingSync)
    }
}
