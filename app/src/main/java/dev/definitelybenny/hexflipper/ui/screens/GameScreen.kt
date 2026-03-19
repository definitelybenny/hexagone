package dev.definitelybenny.hexflipper.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Undo
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.definitelybenny.hexflipper.model.BoardState
import dev.definitelybenny.hexflipper.model.HexCell
import dev.definitelybenny.hexflipper.ui.components.AnimatingStack
import dev.definitelybenny.hexflipper.ui.components.HexBoard

private val DarkBackground = Color(0xFF0F0C29)
private val LightText = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    boardState: BoardState,
    animatingStack: AnimatingStack?,
    moveCount: Int,
    canUndo: Boolean,
    onCellTapped: (HexCell) -> Unit,
    onUndo: () -> Unit,
    onHint: () -> Unit,
    onPause: () -> Unit,
    onAnimationFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Moves: $moveCount",
                        color = LightText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onPause) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause",
                            tint = LightText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Undo button
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = if (canUndo) LightText.copy(alpha = 0.12f)
                            else LightText.copy(alpha = 0.05f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) LightText else LightText.copy(alpha = 0.3f),
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Hint button
                IconButton(
                    onClick = onHint,
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = Color(0xFFFDD663).copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "Hint",
                        tint = Color(0xFFFDD663),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        containerColor = Color.Transparent,
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            HexBoard(
                boardState = boardState,
                animatingStack = animatingStack,
                onCellTapped = onCellTapped,
                onAnimationFinished = onAnimationFinished,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
