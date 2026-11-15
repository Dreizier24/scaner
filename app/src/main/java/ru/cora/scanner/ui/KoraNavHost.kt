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
                        navController.navigate("scanner/$id")
                    },
                    onQuickScan = { navController.navigate("scanner/_none") },
                )
            }
            composable(
                route = "scanner/{concertId}",
                arguments = listOf(navArgument("concertId") { type = NavType.StringType }),
            ) { entry ->
                val rawId = entry.arguments?.getString("concertId")
                val concertId = rawId?.takeUnless { it == "_none" }
                val viewModel: ScannerViewModel = viewModel(
                    factory = ScannerViewModel.Factory(
                        concertId = concertId,
                        repository = app.scanRepository,
                        networkMonitor = app.networkMonitor,
                    ),
                )
                ScannerScreen(
                    viewModel = viewModel,
                    concertId = concertId,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
