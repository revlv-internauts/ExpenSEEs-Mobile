package com.example.expensees.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun LoadingScreen(
    modifier: Modifier = Modifier,
    onLoadingComplete: () -> Unit = {}
) {
    LaunchedEffect(Unit) {
        delay(3000L)
        onLoadingComplete()
    }

    val scale by animateFloatAsState(
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    val pulseAnim = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        pulseAnim.animateTo(
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    val rotation by animateFloatAsState(
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000),
            repeatMode = RepeatMode.Restart
        ), label = "rotation"
    )

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0D47A1),
            Color(0xFF1976D2)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = backgroundGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // Logo
            Text(
                text = "ExpenSEEs",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                modifier = Modifier.scale(scale)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 🔵 Glowing Pulsing Orb + Spinner
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {

                // Outer spinner
                CircularProgressIndicator(
                    strokeWidth = 4.dp,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(120.dp)
                        .rotate(rotation)
                )

                // Glowing pulsing orb in center
                Box(
                    modifier = Modifier
                        .size((36 * pulseAnim.value).dp)
                        .background(Color.Cyan.copy(alpha = 0.8f), shape = CircleShape)
                        .shadow(
                            elevation = 16.dp,
                            shape = CircleShape,
                            ambientColor = Color.Cyan,
                            spotColor = Color.Cyan
                        )
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = "Powering your budget brilliance...",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 16.sp
                )
            )
        }
    }
}
