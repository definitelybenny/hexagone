package dev.definitelybenny.hexflipper.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0F0C29),
        Color(0xFF302B63),
        Color(0xFF24243E)
    )
)

private val UnlockedGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF4FACFE),
        Color(0xFF00F2FE)
    )
)

private val CompletedGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF43E97B),
        Color(0xFF38F9D7)
    )
)

private val StarGold = Color(0xFFFFD700)
private val StarEmpty = Color.White.copy(alpha = 0.2f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelSelectScreen(
    totalLevels: Int,
    completedLevels: Map<Int, Int>,
    unlockedUpTo: Int,
    onLevelClick: (Int) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Select Level",
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGradient)
                .padding(padding)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items((1..totalLevels).toList()) { levelNumber ->
                    val isLocked = levelNumber > unlockedUpTo
                    val starsEarned = completedLevels[levelNumber]
                    val isCompleted = starsEarned != null && starsEarned > 0

                    LevelButton(
                        levelNumber = levelNumber,
                        isLocked = isLocked,
                        isCompleted = isCompleted,
                        stars = starsEarned ?: 0,
                        onClick = { if (!isLocked) onLevelClick(levelNumber) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelButton(
    levelNumber: Int,
    isLocked: Boolean,
    isCompleted: Boolean,
    stars: Int,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(shape)
            .then(
                when {
                    isLocked -> Modifier.background(Color.White.copy(alpha = 0.06f), shape)
                    isCompleted -> Modifier.background(CompletedGradient, shape)
                    else -> Modifier
                        .background(Color.Transparent, shape)
                        .border(2.dp, Brush.linearGradient(
                            listOf(Color(0xFF4FACFE), Color(0xFF00F2FE))
                        ), shape)
                        .background(Color.White.copy(alpha = 0.08f), shape)
                }
            )
            .clickable(enabled = !isLocked, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLocked) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$levelNumber",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$levelNumber",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isCompleted && stars > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                        for (i in 1..3) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (i <= stars) StarGold else StarEmpty,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
