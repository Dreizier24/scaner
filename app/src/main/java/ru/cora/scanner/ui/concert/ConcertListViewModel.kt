package ru.cora.scanner.ui.concert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConcertListUiState())
    val uiState: StateFlow<ConcertListUiState> = _uiState.asStateFlow()

    val concerts: StateFlow<List<ConcertEntity>> = repository.concerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
        }
    }

    fun refreshConcerts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            repository.refreshConcerts()
                .onSuccess { count ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = if (count > 0) "Концертов: $count" else "Список пуст (подключите API)",
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = "Не удалось загрузить концерты",
                        )
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
