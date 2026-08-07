package com.narcic.ng.ui.main

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.narcic.ng.R

private val NarcicTeal = Color(0xFF00E89D)

/**
 * Large, glowing circular connect button matching the Narcic NG brand,
 * shown at the top of the main screen above the server list.
 * Reuses existing MainUiState fields only (isRunning, statusText, selectedGuid) —
 * no new service/state wiring, safe to render alongside the existing bottom bar.
 */
@Composable
fun ConnectHero(
    isRunning: Boolean,
    isTesting: Boolean,
    statusText: String,
    selectedServerName: String?,
    onToggle: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Small status card (like the old app's "Ready to connect" bar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_lock_24dp),
                contentDescription = null,
                tint = if (isRunning) NarcicTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Box(modifier = Modifier.padding(start = 14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isRunning) "Connected" else "Ready to connect",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = statusText.ifBlank { "Tap Connect to get started" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }

        Box(modifier = Modifier.padding(top = 16.dp))

        if (!selectedServerName.isNullOrBlank()) {
            Text(
                text = selectedServerName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Box(modifier = Modifier.padding(bottom = 14.dp))
        }

        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            // Pulsing glow ring — only visible while connected
            if (isRunning) {
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .scale(pulseScale)
                        .background(
                            color = NarcicTeal.copy(alpha = pulseAlpha),
                            shape = CircleShape
                        )
                )
            }

            // Main circular button
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = if (isRunning) {
                                listOf(Color(0xFF0D4A38), Color(0xFF082A20))
                            } else {
                                listOf(Color(0xFF1A322C), Color(0xFF0D1A17))
                            }
                        ),
                        shape = CircleShape
                    )
                    .border(
                        width = if (isRunning) 2.dp else 1.dp,
                        color = if (isRunning) NarcicTeal else NarcicTeal.copy(alpha = 0.25f),
                        shape = CircleShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggle
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = if (isRunning) painterResource(R.drawable.ic_stop_24dp)
                        else painterResource(R.drawable.ic_play_24dp),
                        contentDescription = null,
                        tint = if (isRunning) NarcicTeal else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(40.dp)
                    )
                    Box(modifier = Modifier.padding(top = 6.dp))
                    Text(
                        text = if (isTesting) "…" else if (isRunning) "Disconnect" else "Connect",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isRunning) NarcicTeal else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        Box(modifier = Modifier.padding(top = 12.dp))
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodySmall,
            color = if (isRunning) NarcicTeal else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
