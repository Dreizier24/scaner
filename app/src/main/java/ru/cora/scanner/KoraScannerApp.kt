package ru.cora.scanner

import android.app.Application
import java.security.Security
import org.conscrypt.Conscrypt
import androidx.room.Room
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.cora.scanner.data.ConcertScanRepository
import ru.cora.scanner.data.ScannerPreferences
import ru.cora.scanner.data.ExternalBarcodeImporter
import ru.cora.scanner.data.ExternalEventImporter
import ru.cora.scanner.data.NetworkMonitor
import ru.cora.scanner.data.TicketRepository
import ru.cora.scanner.data.gosuslugi.GosuslugiDigitalIdService
import ru.cora.scanner.data.local.KoraDatabase
import ru.cora.scanner.data.pushkin.PushkinRedemptionService
import ru.cora.scanner.data.remote.TicketApiService
import ru.cora.scanner.data.sync.LocalTicketRetention
import ru.cora.scanner.data.sync.ManifestSyncManager
import ru.cora.scanner.data.sync.OfflineSyncManager
import ru.cora.scanner.data.sync.RedemptionSyncManager
import ru.cora.scanner.data.sync.RedemptionWebSocketManager

class KoraScannerApp : Application() {

    lateinit var scanRepository: ConcertScanRepository
        private set

    lateinit var networkMonitor: NetworkMonitor
        private set

    lateinit var scannerPreferences: ScannerPreferences
        private set

    lateinit var redemptionWebSocket: RedemptionWebSocketManager
        private set

    lateinit var httpClient: OkHttpClient
        private set

    override fun onCreate() {
        super.onCreate()

        runCatching {
            Security.insertProviderAt(Conscrypt.newProvider(), 1)
        }

        val database = Room.databaseBuilder(
            this,
            KoraDatabase::class.java,
            "kora_scanner.db",
        )
            .fallbackToDestructiveMigration()
            .build()

        networkMonitor = NetworkMonitor(this)
        scannerPreferences = ScannerPreferences(database.appSettingsDao())

        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        httpClient = OkHttpClient.Builder()
            .connectTimeout(45, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Accept", "application/json")
                    .header("User-Agent", "KoraScanner/${BuildConfig.VERSION_NAME}")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .build()

        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(httpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        val api = retrofit.create(TicketApiService::class.java)

        val offlineSync = OfflineSyncManager(
            api = api,
            cachedTicketDao = database.cachedTicketDao(),
            pendingValidationDao = database.pendingValidationDao(),
        )

        val ticketRepository = TicketRepository(
            api = api,
            cachedTicketDao = database.cachedTicketDao(),
            scanHistoryDao = database.scanHistoryDao(),
            networkMonitor = networkMonitor,
            offlineSyncManager = offlineSync,
        )

        val barcodeImporter = ExternalBarcodeImporter(
            api = api,
            placeTicketDao = database.placeTicketDao(),
            barcodeIndexDao = database.barcodeIndexDao(),
        )
        val eventImporter = ExternalEventImporter(database, barcodeImporter)
        val manifestSync = ManifestSyncManager(database, api)
        val ticketRetention = LocalTicketRetention(database)
        val redemptionSync = RedemptionSyncManager(
            api = api,
            pendingRedemptionDao = database.pendingRedemptionDao(),
            concertDao = database.concertDao(),
        )

        scanRepository = ConcertScanRepository(
            ticketRepository = ticketRepository,
            concertDao = database.concertDao(),
            placeTicketDao = database.placeTicketDao(),
            barcodeIndexDao = database.barcodeIndexDao(),
            scanHistoryDao = database.scanHistoryDao(),
            pendingRedemptionDao = database.pendingRedemptionDao(),
            manifestSyncManager = manifestSync,
            redemptionSyncManager = redemptionSync,
            localTicketRetention = ticketRetention,
            externalBarcodeImporter = barcodeImporter,
            externalEventImporter = eventImporter,
            pushkinRedemptionService = PushkinRedemptionService(api),
            gosuslugiDigitalIdService = GosuslugiDigitalIdService(api),
        )

        redemptionWebSocket = RedemptionWebSocketManager(
            httpClient = httpClient,
            repository = scanRepository,
            json = json,
        )

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { scanRepository.purgeTicketsAfterMidnight() }
        }
    }
}
