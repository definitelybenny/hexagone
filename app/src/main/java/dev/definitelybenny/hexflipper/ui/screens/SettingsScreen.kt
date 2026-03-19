package dev.definitelybenny.hexflipper.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BackgroundGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF0F0C29), Color(0xFF302B63), Color(0xFF24243E))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    soundEnabled: Boolean,
    musicEnabled: Boolean,
    hapticEnabled: Boolean,
    onSoundToggle: (Boolean) -> Unit,
    onMusicToggle: (Boolean) -> Unit,
    onHapticToggle: (Boolean) -> Unit,
    onResetProgress: () -> Unit,
    onBackClick: () -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGradient)
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Sound toggle
            SettingsToggleRow(
                label = "Sound Effects",
                checked = soundEnabled,
                onCheckedChange = onSoundToggle
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Music toggle
            SettingsToggleRow(
                label = "Music",
                checked = musicEnabled,
                onCheckedChange = onMusicToggle
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Haptic toggle
            SettingsToggleRow(
                label = "Vibration",
                checked = hapticEnabled,
                onCheckedChange = onHapticToggle
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Reset Progress button
            Button(
                onClick = { showResetDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEE5A24).copy(alpha = 0.8f)
                )
            ) {
                Text(
                    text = "Reset Progress",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }

    // Confirmation dialog for reset
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "Reset Progress",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Are you sure you want to reset all progress? This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        onResetProgress()
                    }
                ) {
                    Text(
                        text = "Reset",
                        color = Color(0xFFEE5A24),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF1E1B3A),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color.White.copy(alpha = 0.06f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF4FACFE),
                uncheckedThumbColor = Color.White.copy(alpha = 0.4f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
            )
        )
    }
}
