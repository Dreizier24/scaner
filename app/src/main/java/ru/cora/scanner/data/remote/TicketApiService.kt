package ru.cora.scanner.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TicketApiService {

    @POST("tickets/validate")
    suspend fun validateTicket(@Body request: ValidateTicketRequest): ValidateTicketResponse

    @GET("tickets/offline-pack")
    suspend fun downloadOfflinePack(): SyncTicketsResponse

    @GET("concerts")
    suspend fun listConcerts(): ConcertsResponse

    @GET("concerts/{concertId}/manifest")
    suspend fun getManifest(
        @Path("concertId") concertId: String,
        @Query("since") sinceRevision: Long? = null,
    ): ConcertManifestResponse

    @GET("concerts/{concertId}/sync-state")
    suspend fun getSyncState(@Path("concertId") concertId: String): SyncStateResponse

    @POST("concerts/{concertId}/redemptions/sync")
    suspend fun syncRedemptions(
        @Path("concertId") concertId: String,
        @Body request: RedemptionSyncRequest,
    ): RedemptionSyncResponse

    @POST("concerts/{concertId}/barcodes/import")
    suspend fun importExternalBarcodes(
        @Path("concertId") concertId: String,
        @Body request: ExternalBarcodeImportRequest,
    ): ConcertManifestResponse

    @POST("pushkin/redeem")
    suspend fun redeemPushkin(@Body request: PushkinRedeemRequest): PushkinRedeemResponse

  /** Заготовка: проверка цифрового ID через шлюз Госуслуг (прокси на бэкенде КОРА). */
    @POST("gosuslugi/digital-id/validate")
    suspend fun validateDigitalId(@Body request: GosuslugiIdValidateRequest): GosuslugiIdValidateResponse
}
