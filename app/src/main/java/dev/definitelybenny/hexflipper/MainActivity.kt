package dev.definitelybenny.hexflipper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.rememberNavController
import dev.definitelybenny.hexflipper.game.ProgressManager
import dev.definitelybenny.hexflipper.ui.screens.HexFlipperNavHost
import dev.definitelybenny.hexflipper.ui.theme.HexFlipperTheme

class MainActivity : ComponentActivity() {

    private lateinit var progressManager: ProgressManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        progressManager = ProgressManager(applicationContext)

        // Hide system navigation bar for immersive gameplay
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.navigationBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            HexFlipperTheme(dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F0C29)
                ) {
                    val navController = rememberNavController()
                    HexFlipperNavHost(
                        navController = navController,
                        progressManager = progressManager
                    )
                }
            }
        }
    }
}
