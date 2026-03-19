package dev.definitelybenny.hexflipper.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.definitelybenny.hexflipper.model.DifficultyTier
import dev.definitelybenny.hexflipper.model.TierStats

private val BackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0F0C29),
        Color(0xFF302B63),
        Color(0xFF24243E)
    )
)

private val StarGold = Color(0xFFFFD700)

private val tierGradients = mapOf(
    DifficultyTier.EASY to Brush.horizontalGradient(listOf(Color(0xFF43E97B), Color(0xFF38F9D7))),
    DifficultyTier.MEDIUM to Brush.horizontalGradient(listOf(Color(0xFFFDD663), Color(0xFFFBD786))),
    DifficultyTier.HARD to Brush.horizontalGradient(listOf(Color(0xFFFA8231), Color(0xFFF7B731))),
    DifficultyTier.EXPERT to Brush.horizontalGradient(listOf(Color(0xFFFF6B6B), Color(0xFFEE5A24))),
    DifficultyTier.MASTER to Brush.horizontalGradient(listOf(Color(0xFFA855F7), Color(0xFF7C3AED))),
    DifficultyTier.INSANE to Brush.horizontalGradient(listOf(Color(0xFFF093FB), Color(0xFFF5576C)))
)

private val tierTextColors = mapOf(
    DifficultyTier.EASY to Color(0xFF065F46),
    DifficultyTier.MEDIUM to Color(0xFF713F12),
    DifficultyTier.HARD to Color(0xFF7C2D12),
    DifficultyTier.EXPERT to Color.White,
    DifficultyTier.MASTER to Color.White,
    DifficultyTier.INSANE to Color.White
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DifficultySelectScreen(
    tierStats: Map<DifficultyTier, TierStats>,
    isTierUnlocked: (DifficultyTier) -> Boolean,
    onTierClick: (DifficultyTier) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Random Mode",
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(DifficultyTier.entries) { tier ->
                    val unlocked = isTierUnlocked(tier)
                    val stats = tierStats[tier] ?: TierStats()

                    DifficultyCard(
                        tier = tier,
                        stats = stats,
                        unlocked = unlocked,
                        onClick = { if (unlocked) onTierClick(tier) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DifficultyCard(
    tier: DifficultyTier,
    stats: TierStats,
    unlocked: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val gradient = tierGradients[tier] ?: tierGradients[DifficultyTier.EASY]!!
    val textColor = tierTextColors[tier] ?: Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .alpha(if (unlocked) 1f else 0.4f)
            .background(gradient, shape)
            .clickable(enabled = unlocked, onClick = onClick)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = tier.label,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${tier.stackRange.first}-${tier.stackRange.last} stacks",
                    fontSize = 14.sp,
                    color = textColor.copy(alpha = 0.7f)
                )
                if (!unlocked) {
                    val prereq = tier.prerequisiteTier
                    if (prereq != null) {
                        Text(
                            text = "Solve ${tier.unlockRequirement} ${prereq.label} puzzles",
                            fontSize = 12.sp,
                            color = textColor.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            if (unlocked && stats.solved > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = StarGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${stats.totalStars}",
                            fontSize = 14.sp,
                            color = textColor
                        )
                    }
                    Text(
                        text = "${stats.solved} solved",
                        fontSize = 12.sp,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    if (stats.bestMoves != null) {
                        Text(
                            text = "Best: ${stats.bestMoves} moves",
                            fontSize = 12.sp,
                            color = textColor.copy(alpha = 0.7f)
                        )
                    }
                }
            } else if (!unlocked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = textColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
