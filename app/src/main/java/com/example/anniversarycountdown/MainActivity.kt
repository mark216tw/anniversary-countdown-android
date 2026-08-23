package com.example.anniversarycountdown

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.anniversarycountdown.ui.AnniversaryApp
import com.example.anniversarycountdown.ui.AnniversaryViewModel
import com.example.anniversarycountdown.ui.theme.AnniversaryCountdownTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: AnniversaryViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            AnniversaryCountdownTheme(
                themeColor = settings.themeColor,
                darkTheme = settings.darkMode,
            ) {
                SystemBarStyle(settings.darkMode)
                AnniversaryApp(viewModel)
            }
        }
    }

    @Suppress("DEPRECATION")
    @androidx.compose.runtime.Composable
    private fun SystemBarStyle(darkMode: Boolean) {
        val background = MaterialTheme.colorScheme.background
        SideEffect {
            window.navigationBarColor = background.toArgb()
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightNavigationBars = !darkMode
                isAppearanceLightStatusBars = !darkMode
            }
        }
    }
}
