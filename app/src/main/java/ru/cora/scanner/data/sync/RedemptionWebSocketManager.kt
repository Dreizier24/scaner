package ru.cora.scanner.data.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import ru.cora.scanner.BuildConfig
import ru.cora.scanner.data.ConcertScanRepository
import ru.cora.scanner.data.remote.WsRedemptionMessage

class RedemptionWebSocketManager(
    private val httpClient: OkHttpClient,
    private val repository: ConcertScanRepository,
    private val json: Json,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var webSocket: WebSocket? = null
    private var activeConcertId: String? = null
    private var reconnectJob: Job? = null

    fun connect(concertId: String) {
        if (activeConcertId == concertId && webSocket != null) return
        disconnect()
        activeConcertId = concertId
        openSocket(concertId)
    }

    fun disconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
        webSocket?.close(1000, null)
        webSocket = null
        activeConcertId = null
    }

    private fun openSocket(concertId: String) {
        val request = Request.Builder()
            .url(wsUrl(concertId))
            .header("User-Agent", "KoraScanner/${BuildConfig.VERSION_NAME}")
            .build()
        webSocket = httpClient.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    scheduleReconnect(concertId)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (activeConcertId == concertId) scheduleReconnect(concertId)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    scope.launch {
                        runCatching {
                            val msg = json.decodeFromString<WsRedemptionMessage>(text)
                            if (msg.type == "redemption") {
                                repository.applyRemoteRedemption(
                                    msg.concertId,
                                    msg.placeId,
                                    msg.scannedAtEpochMs,
                                )
                            }
                        }
                    }
                }
            },
        )
    }

    private fun scheduleReconnect(concertId: String) {
        if (reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            delay(3_000)
            if (isActive && activeConcertId == concertId) {
                openSocket(concertId)
            }
        }
    }

    private fun wsUrl(concertId: String): String {
        return BuildConfig.API_BASE_URL
            .trim()
            .replace("https://", "wss://")
            .replace("http://", "ws://")
            .removeSuffix("/") + "/ws/concerts/$concertId"
    }

    fun shutdown() {
        disconnect()
        scope.cancel()
    }
}