package ru.cora.scanner.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.cora.scanner.update.AppUpdateUiState.Available
import ru.cora.scanner.update.AppUpdateUiState.Idle

class AppUpdateViewModel(
    private val checker: AppUpdateChecker,
    private val installer: ApkUpdateInstaller,
) : ViewModel() {

    private val _state = MutableStateFlow<AppUpdateUiState>(Idle)
    val state: StateFlow<AppUpdateUiState> = _state.asStateFlow()

    fun checkOnLaunch() {
        if (_state.value !is Idle) return
        viewModelScope.launch {
            val manifest = checker.fetchIfNewer() ?: return@launch
            _state.value = Available(manifest = manifest)
        }
    }

    fun dismiss() {
        _state.value = Idle
    }

    fun startUpdate() {
        val current = _state.value as? Available ?: return
        if (!installer.canInstallPackages()) {
            _state.value = current.copy(error = "Разрешите установку приложений в настройках")
            return
        }
        viewModelScope.launch {
            _state.value = current.copy(downloading = true, error = null, progress = 0f)
            val result = installer.downloadApk(current.manifest.apkUrl) { p ->
                _state.value = ( _state.value as? Available)?.copy(progress = p) ?: return@downloadApk
            }
            result.fold(
                onSuccess = { file ->
                    installer.startInstall(file)
                    _state.value = Available(
                        manifest = current.manifest,
                        downloading = false,
                        progress = 1f,
                    )
                },
                onFailure = { e ->
                    _state.value = current.copy(
                        downloading = false,
                        error = e.message ?: "Ошибка загрузки",
                    )
                },
            )
        }
    }

    fun openInstallSettings() {
        installer.openInstallPermissionSettings()
    }

    class Factory(
        private val checker: AppUpdateChecker,
        private val installer: ApkUpdateInstaller,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppUpdateViewModel(checker, installer) as T
        }
    }
}