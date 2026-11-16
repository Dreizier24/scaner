package ru.cora.scanner.ui.concert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.cora.scanner.data.ApiErrorMessages
import ru.cora.scanner.data.ConcertScanRepository
import ru.cora.scanner.data.NetworkMonitor
import ru.cora.scanner.data.local.entity.ConcertEntity

data class ConcertListUiState(
    val isOnline: Boolean = true,
    val isLoading: Boolean = false,
    val message: String? = null,
)

class ConcertListViewModel(
    private val repository: ConcertScanRepository,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConcertListUiState())
    val uiState: StateFlow<ConcertListUiState> = _uiState.asStateFlow()

    val concerts: StateFlow<List<ConcertEntity>> = repository.concerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            runCatching { repository.purgeTicketsAfterMidnight() }
        }
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
        }
        viewModelScope.launch {
            if (networkMonitor.isOnline.first()) {
                refreshConcerts(silent = true)
            }
        }
    }

    fun refreshConcerts(silent: Boolean = false) {
        viewModelScope.launch {
            if (!_uiState.value.isOnline) {
                if (!silent) {
                    _uiState.update { it.copy(message = "Нет сети") }
                }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, message = if (silent) it.message else null) }
            repository.refreshConcerts()
                .onSuccess { count ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = if (silent) null else "Событий: $count",
                        )
                    }
                }
                .onFailure { error ->
                    val cached = concerts.first()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = if (cached.isNotEmpty() && silent) {
                                null
                            } else {
                                ApiErrorMessages.forUser(error)
                            },
                        )
                    }
                }
        }
    }

    fun importEventFromFile(fileContent: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            repository.importEventFromFile(fileContent)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = "${result.title}: ${result.ticketCount} шт.",
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(isLoading = false, message = "Ошибка файла")
                    }
                }
        }
    }

    class Factory(
        private val repository: ConcertScanRepository,
        private val networkMonitor: NetworkMonitor,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ConcertListViewModel(repository, networkMonitor) as T
        }
    }
}