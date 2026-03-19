package dev.definitelybenny.hexflipper.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

private val BackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0F0C29),
        Color(0xFF302B63),
        Color(0xFF24243E)
    )
)

private val AccentGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF4FACFE),
        Color(0xFF00F2FE)
    )
)

private val SecondaryGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFFF093FB),
        Color(0xFFF5576C)
    )
)

private val TitleGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF4FACFE),
        Color(0xFF43E97B),
        Color(0xFFFDD663)
    )
)

@Composable
fun MainMenuScreen(
    onCampaignClick: () -> Unit,
    onRandomClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGradient)
    ) {
        // Decorative hex pattern behind the title
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.06f)
        ) {
            drawHexPattern(this)
        }

        // Settings gear icon in top-right corner
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(28.dp)
            )
        }

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Game title with gradient effect
            Text(
                text = "HEX",
                style = TextStyle(
                    fontSize = 72.sp,
                    fontWeight = FontWeight.ExtraBold,
                    brush = TitleGradient,
                    letterSpacing = 8.sp
                )
            )
            Text(
                text = "FLIPPER",
                style = TextStyle(
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    brush = TitleGradient,
                    letterSpacing = 12.sp
                )
            )

            Spacer(modifier = Modifier.height(80.dp))

            // Campaign button
            Button(
                onClick = onCampaignClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AccentGradient, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Campaign",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Random Mode button
            Button(
                onClick = onRandomClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SecondaryGradient, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Random Mode",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

private fun drawHexPattern(drawScope: DrawScope) {
    val hexSize = 40f
    val hexWidth = hexSize * 2f
    val hexHeight = hexSize * kotlin.math.sqrt(3f)
    val cols = (drawScope.size.width / (hexWidth * 0.75f)).toInt() + 2
    val rows = (drawScope.size.height / hexHeight).toInt() + 2

    for (row in 0..rows) {
        for (col in 0..cols) {
            val offsetX = if (row % 2 == 1) hexWidth * 0.375f else 0f
            val cx = col * hexWidth * 0.75f + offsetX
            val cy = row * hexHeight * 0.5f

            val path = Path()
            for (i in 0..5) {
                val angle = Math.PI / 3.0 * i
                val px = cx + hexSize * cos(angle).toFloat()
                val py = cy + hexSize * sin(angle).toFloat()
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            drawScope.drawPath(
                path = path,
                color = Color.White,
                style = Stroke(width = 1f)
            )
        }
    }
}
