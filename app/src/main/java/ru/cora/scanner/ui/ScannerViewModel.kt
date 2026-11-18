package ru.cora.scanner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.cora.scanner.data.ApiErrorMessages
import ru.cora.scanner.data.ConcertScanRepository
import ru.cora.scanner.data.NetworkMonitor
import ru.cora.scanner.data.ScanDirection
import ru.cora.scanner.data.ScannerPreferences
import ru.cora.scanner.data.sync.RedemptionWebSocketManager
import ru.cora.scanner.domain.model.TicketInfo

data class ScannerUiState(
    val isOnline: Boolean = true,
    val forceOffline: Boolean = false,
    val autoScan: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val passStats: Boolean = true,
    val useCamera: Boolean = true,
    val scanDirection: ScanDirection = ScanDirection.Entry,
    val isProcessing: Boolean = false,
    val lastTicket: TicketInfo? = null,
    val errorMessage: String? = null,
    val scanPaused: Boolean = false,
    val statusMessage: String? = null,
    val ticketsReady: Boolean = false,
    val redeemedCount: Int = 0,
    val ticketCount: Int = 0,
    val pendingSyncCount: Int = 0,
    val concertTitle: String? = null,
    val purpose: ScannerPurpose = ScannerPurpose.Concert,
    val showTicketDetails: Boolean = false,
    val isLoadingTickets: Boolean = false,
)

class ScannerViewModel(
    private val concertId: String?,
    private val purpose: ScannerPurpose,
    private val repository: ConcertScanRepository,
    private val preferences: ScannerPreferences,
    private val redemptionWebSocket: RedemptionWebSocketManager?,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState(purpose = purpose))
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
                if (online) {
                    runCatching { repository.flushPendingWhenOnline() }
                }
                manageWebSocket()
            }
        }
        viewModelScope.launch { refreshPreferences() }
        viewModelScope.launch {
            runCatching { repository.purgeTicketsAfterMidnight() }
        }
        if (purpose == ScannerPurpose.Concert && concertId != null) {
            viewModelScope.launch {
                repository.observeRedeemedCount(concertId).collect { count ->
                    _uiState.update { it.copy(redeemedCount = count) }
                }
            }
            viewModelScope.launch {
                repository.observeTicketCount(concertId).collect { count ->
                    _uiState.update {
                        it.copy(ticketCount = count, ticketsReady = count > 0)
                    }
                }
            }
            viewModelScope.launch {
                repository.observePendingRedemptions(concertId).collect { count ->
                    _uiState.update { it.copy(pendingSyncCount = count) }
                }
            }
            viewModelScope.launch {
                val title = repository.getConcertTitle(concertId)
                _uiState.update { it.copy(concertTitle = title) }
            }
        }
        if (purpose == ScannerPurpose.DigitalId) {
            _uiState.update { it.copy(concertTitle = "Цифровой ID") }
        }
    }

    override fun onCleared() {
        redemptionWebSocket?.disconnect()
        super.onCleared()
    }

    private fun manageWebSocket() {
        val id = concertId ?: return
        val ws = redemptionWebSocket ?: return
        if (purpose != ScannerPurpose.Concert) {
            ws.disconnect()
            return
        }
        viewModelScope.launch {
            if (repository.isFileConcert(id)) {
                ws.disconnect()
                return@launch
            }
            val state = _uiState.value
            if (state.isOnline && !state.forceOffline) {
                ws.connect(id)
            } else {
                ws.disconnect()
            }
        }
    }

    fun refreshPreferences() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    forceOffline = preferences.isForceOffline(),
                    autoScan = preferences.isAutoScan(),
                    soundEnabled = preferences.isSoundEnabled(),
                    vibrationEnabled = preferences.isVibrationEnabled(),
                    passStats = preferences.isPassStatsEnabled(),
                    useCamera = preferences.isCameraEnabled(),
                    scanDirection = preferences.scanDirection(),
                )
            }
            manageWebSocket()
        }
    }

    fun setScanDirection(direction: ScanDirection) {
        viewModelScope.launch {
            preferences.setScanDirection(direction)
            _uiState.update { it.copy(scanDirection = direction) }
        }
    }

    fun toggleTicketDetails() {
        _uiState.update { it.copy(showTicketDetails = !it.showTicketDetails) }
    }

    fun onBarcodeDetected(raw: String, fromManual: Boolean = false) {
        val state = _uiState.value
        if (state.isProcessing || state.scanPaused) return
        if (!fromManual && !state.autoScan) return
        if (state.lastTicket?.rawPayload == raw) return
        if (purpose == ScannerPurpose.Concert && state.ticketCount == 0) {
            _uiState.update { it.copy(errorMessage = "Загрузите билеты") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
            val effectiveOnline = state.isOnline && !state.forceOffline
            val exitOnly = state.scanDirection == ScanDirection.Exit &&
                purpose == ScannerPurpose.Concert
            runCatching {
                repository.resolveScan(
                    concertId = concertId,
                    raw = raw,
                    isOnline = effectiveOnline,
                    modeDigitalId = purpose == ScannerPurpose.DigitalId,
                    exitOnly = exitOnly,
                )
            }.onSuccess { ticket ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        lastTicket = ticket,
                        scanPaused = true,
                        showTicketDetails = false,
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "Не удалось прочитать код. Попробуйте ещё раз.",
                    )
                }
            }
        }
    }

    fun resumeScanning() {
        _uiState.update {
            it.copy(
                scanPaused = false,
                lastTicket = null,
                errorMessage = null,
                showTicketDetails = false,
            )
        }
    }

    fun loadTickets() {
        val id = concertId ?: return
        if (_uiState.value.isLoadingTickets) return
        viewModelScope.launch {
            val fromFile = repository.isFileConcert(id)
            _uiState.update {
                it.copy(isLoadingTickets = true, statusMessage = null, errorMessage = null)
            }
            repository.loadManifest(id)
                .onSuccess { result ->
                    runCatching { repository.flushRedemptions() }
                    _uiState.update {
                        it.copy(
                            isLoadingTickets = false,
                            ticketsReady = result.placeCount > 0,
                            statusMessage = if (result.placeCount > 0) {
                                "В базе: ${result.placeCount}"
                            } else if (fromFile) {
                                "Нет билетов"
                            } else {
                                "Пусто"
                            },
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingTickets = false,
                            ticketsReady = fromFile && it.ticketCount > 0,
                            statusMessage = ApiErrorMessages.forUser(error),
                        )
                    }
                }
        }
    }

    class Factory(
        private val concertId: String?,
        private val purpose: ScannerPurpose,
        private val repository: ConcertScanRepository,
        private val preferences: ScannerPreferences,
        private val redemptionWebSocket: RedemptionWebSocketManager?,
        private val networkMonitor: NetworkMonitor,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ScannerViewModel(
                concertId,
                purpose,
                repository,
                preferences,
                redemptionWebSocket,
                networkMonitor,
            ) as T
        }
    }
}