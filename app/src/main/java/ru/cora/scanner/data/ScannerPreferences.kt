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

    companion object {
        private const val KEY_FORCE_OFFLINE = "force_offline"
        private const val KEY_AUTO_SCAN = "auto_scan"
        private const val KEY_SOUND = "sound"
        private const val KEY_VIBRATION = "vibration"
        private const val KEY_DIRECTION = "scan_direction"
    }
}