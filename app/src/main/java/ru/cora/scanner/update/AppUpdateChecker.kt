package ru.cora.scanner.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.cora.scanner.BuildConfig

class AppUpdateChecker(
    private val httpClient: OkHttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun fetchIfNewer(): AppUpdateManifest? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(BuildConfig.APP_UPDATE_URL)
            .get()
            .build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return@withContext null
        val body = response.body?.string() ?: return@withContext null
        val manifest = json.decodeFromString<AppUpdateManifest>(body)
        if (manifest.versionCode <= BuildConfig.VERSION_CODE || manifest.apkUrl.isBlank()) {
            null
        } else {
            manifest
        }
    }
}