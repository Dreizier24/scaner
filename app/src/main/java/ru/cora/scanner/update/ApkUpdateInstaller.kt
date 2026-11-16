package ru.cora.scanner.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class ApkUpdateInstaller(
    private val context: Context,
    private val httpClient: OkHttpClient,
) {
    private val updateDir: File
        get() {
            val dir = File(context.getExternalFilesDir(null), "apk-updates")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    fun canInstallPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    suspend fun downloadApk(url: String, onProgress: (Float) -> Unit): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder().url(url).get().build()
                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    error("HTTP ${response.code}")
                }
                val body = response.body ?: error("Пустой ответ")
                val total = body.contentLength().coerceAtLeast(1L)
                val outFile = File(updateDir, "kora-scanner-update.apk")
                body.byteStream().use { input ->
                    outFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        var downloaded = 0L
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            onProgress((downloaded.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                }
                outFile
            }
        }

    fun startInstall(apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }
}