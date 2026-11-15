package ru.cora.scanner.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.cora.scanner.KoraScannerApp
import ru.cora.scanner.ui.concert.ConcertListScreen
import ru.cora.scanner.ui.concert.ConcertListViewModel

@Composable
fun KoraNavHost(app: KoraScannerApp) {
    val navController = rememberNavController()

    Surface(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = "concerts") {
            composable("concerts") {
                val viewModel: ConcertListViewModel = viewModel(
                    factory = ConcertListViewModel.Factory(
                        repository = app.scanRepository,
                        networkMonitor = app.networkMonitor,
                    ),
                )
                ConcertListScreen(
                    viewModel = viewModel,
                    onConcertSelected = { id ->
                        navController.navigate("scanner/concert/$id")
                    },
                    onDigitalIdScan = {
                        navController.navigate("scanner/digital-id")
                    },
                )
            }
            composable(
                route = "scanner/concert/{concertId}",
                arguments = listOf(navArgument("concertId") { type = NavType.StringType }),
            ) { entry ->
                val concertId = entry.arguments?.getString("concertId")
                requireNotNull(concertId)
                val viewModel: ScannerViewModel = viewModel(
                    factory = ScannerViewModel.Factory(
                        concertId = concertId,
                        purpose = ScannerPurpose.Concert,
                        repository = app.scanRepository,
                        networkMonitor = app.networkMonitor,
                    ),
                )
                ScannerScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("scanner/digital-id") {
                val viewModel: ScannerViewModel = viewModel(
                    factory = ScannerViewModel.Factory(
                        concertId = null,
                        purpose = ScannerPurpose.DigitalId,
                        repository = app.scanRepository,
                        networkMonitor = app.networkMonitor,
                    ),
                )
                ScannerScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}