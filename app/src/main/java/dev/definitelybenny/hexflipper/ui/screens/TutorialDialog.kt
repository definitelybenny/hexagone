package dev.definitelybenny.hexflipper.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TutorialDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1B3A),
        titleContentColor = Color.White,
        title = {
            Text(
                text = "How to Play",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TutorialStep(
                    number = "1",
                    color = Color(0xFF4FACFE),
                    text = "Each colored stack has an arrow showing which direction it will slide."
                )
                TutorialStep(
                    number = "2",
                    color = Color(0xFF43E97B),
                    text = "Tap a stack to slide it off the board. The entire stack moves as one piece."
                )
                TutorialStep(
                    number = "3",
                    color = Color(0xFFFA8231),
                    text = "A stack is blocked if another stack or wall is anywhere along its path."
                )
                TutorialStep(
                    number = "4",
                    color = Color(0xFFF093FB),
                    text = "Clear all stacks to complete the level. Fewer moves = more stars!"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4FACFE)
                )
            ) {
                Text(
                    text = "Got it!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    )
}

@Composable
private fun TutorialStep(
    number: String,
    color: Color,
    text: String
) {
    Row(
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = number,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.3f))
                .padding(top = 4.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 15.sp,
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
