package ru.cora.scanner.update

import kotlinx.serialization.Serializable

@Serializable
data class AppUpdateManifest(
    val versionCode: Int = 0,
    val versionName: String = "",
    val apkUrl: String = "",
    val releaseNotes: String = "",
    val mandatory: Boolean = false,
)

sealed interface AppUpdateUiState {
    data object Idle : AppUpdateUiState
    data class Available(
        val manifest: AppUpdateManifest,
        val downloading: Boolean = false,
        val progress: Float? = null,
        val error: String? = null,
    ) : AppUpdateUiState
}