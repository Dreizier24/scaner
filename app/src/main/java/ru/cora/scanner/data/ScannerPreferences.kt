package ru.cora.scanner.data

import ru.cora.scanner.data.local.dao.AppSettingsDao
import ru.cora.scanner.data.local.entity.AppSettingEntity

enum class ScanDirection {
    Entry,
    Exit,
}

class ScannerPreferences(
    private val dao: AppSettingsDao,
) {
    suspend fun isForceOffline(): Boolean = dao.get(KEY_FORCE_OFFLINE) == "1"

    suspend fun setForceOffline(value: Boolean) {
        dao.set(AppSettingEntity(KEY_FORCE_OFFLINE, if (value) "1" else "0"))
    }

    suspend fun isAutoScan(): Boolean = dao.get(KEY_AUTO_SCAN) != "0"

    suspend fun setAutoScan(value: Boolean) {
        dao.set(AppSettingEntity(KEY_AUTO_SCAN, if (value) "1" else "0"))
    }

    suspend fun isSoundEnabled(): Boolean = dao.get(KEY_SOUND) != "0"

    suspend fun setSoundEnabled(value: Boolean) {
        dao.set(AppSettingEntity(KEY_SOUND, if (value) "1" else "0"))
    }

    suspend fun isVibrationEnabled(): Boolean = dao.get(KEY_VIBRATION) != "0"

    suspend fun setVibrationEnabled(value: Boolean) {
        dao.set(AppSettingEntity(KEY_VIBRATION, if (value) "1" else "0"))
    }

    suspend fun scanDirection(): ScanDirection =
        if (dao.get(KEY_DIRECTION) == "exit") ScanDirection.Exit else ScanDirection.Entry

    suspend fun setScanDirection(direction: ScanDirection) {
        dao.set(
            AppSettingEntity(
                KEY_DIRECTION,
                if (direction == ScanDirection.Exit) "exit" else "entry",
            ),
        )
    }

    suspend fun isPassStatsEnabled(): Boolean = dao.get(KEY_PASS_STATS) != "0"

    suspend fun setPassStatsEnabled(value: Boolean) {
        dao.set(AppSettingEntity(KEY_PASS_STATS, if (value) "1" else "0"))
    }

    suspend fun isAutoSyncEnabled(): Boolean = dao.get(KEY_AUTO_SYNC) != "0"

    suspend fun setAutoSyncEnabled(value: Boolean) {
        dao.set(AppSettingEntity(KEY_AUTO_SYNC, if (value) "1" else "0"))
    }

    suspend fun isCameraEnabled(): Boolean = dao.get(KEY_CAMERA) != "0"

    suspend fun setCameraEnabled(value: Boolean) {
        dao.set(AppSettingEntity(KEY_CAMERA, if (value) "1" else "0"))
    }

    companion object {
        private const val KEY_FORCE_OFFLINE = "force_offline"
        private const val KEY_AUTO_SCAN = "auto_scan"
        private const val KEY_SOUND = "sound"
        private const val KEY_VIBRATION = "vibration"
        private const val KEY_DIRECTION = "scan_direction"
        private const val KEY_PASS_STATS = "pass_stats"
        private const val KEY_AUTO_SYNC = "auto_sync"
        private const val KEY_CAMERA = "use_camera"
    }
}