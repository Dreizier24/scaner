package ru.cora.scanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.cora.scanner.ui.KoraNavHost
import ru.cora.scanner.ui.theme.KoraScannerTheme
import ru.cora.scanner.ui.update.AppUpdateDialog
import ru.cora.scanner.update.ApkUpdateInstaller
import ru.cora.scanner.update.AppUpdateChecker
import ru.cora.scanner.update.AppUpdateViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as KoraScannerApp
        val checker = AppUpdateChecker(app.httpClient)
        val installer = ApkUpdateInstaller(this, app.httpClient)

        setContent {
            val updateViewModel: AppUpdateViewModel = viewModel(
                factory = AppUpdateViewModel.Factory(checker, installer),
            )
            val updateState by updateViewModel.state.collectAsState()

            LaunchedEffect(Unit) {
                updateViewModel.checkOnLaunch()
            }

            KoraScannerTheme {
                KoraNavHost(app = app)
                AppUpdateDialog(
                    state = updateState,
                    onUpdate = { updateViewModel.startUpdate() },
                    onDismiss = { updateViewModel.dismiss() },
                    onOpenSettings = { updateViewModel.openInstallSettings() },
                )
            }
        }
    }
}