package ru.cora.scanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ru.cora.scanner.ui.KoraNavHost
import ru.cora.scanner.ui.theme.KoraScannerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as KoraScannerApp

        setContent {
            KoraScannerTheme {
                KoraNavHost(app = app)
            }
        }
    }
}
