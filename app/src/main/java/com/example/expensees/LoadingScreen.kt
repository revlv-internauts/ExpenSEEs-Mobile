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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
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
        delay(3500L)
        onLoadingComplete()
    }

    val logoScale by animateFloatAsState(
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "logoScale"
    )

    val pulse by animateFloatAsState(
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    val haloRotation by animateFloatAsState(
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000),
            repeatMode = RepeatMode.Restart
        ), label = "haloRotation"
    )

    val shimmerColors = listOf(
        Color(0xFFB3E5FC),
        Color(0xFF81D4FA),
        Color(0xFF29B6F6),
        Color(0xFF0288D1)
    )

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0D47A1),
            Color(0xFF1565C0),
            Color(0xFF1E88E5)
        ),
        startY = 0f,
        endY = Float.POSITIVE_INFINITY
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = backgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ✨ Gradient Logo Text with glow
            Text(
                text = "ExpenSEEs",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 46.sp,
                    fontWeight = FontWeight.ExtraBold,
                    shadow = Shadow(
                        color = Color.Cyan.copy(alpha = 0.6f),
                        offset = Offset(2f, 8f),
                        blurRadius = 25f
                    )
                ),
                modifier = Modifier
                    .scale(logoScale)
                    .padding(bottom = 10.dp),
                color = Color.Unspecified,
                softWrap = false
            )

            Box(
                modifier = Modifier
                    .height(2.dp)
                    .width(200.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color.White, Color.Transparent)
                        )
                    )
            )

            Spacer(modifier = Modifier.height(40.dp))

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                // 🔄 Rotating outer gradient ring
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .rotate(haloRotation)
                        .background(
                            brush = Brush.sweepGradient(shimmerColors),
                            shape = CircleShape
                        )
                        .alpha(0.3f)
                )

                // 🌟 Inner glowing aura
                // 🚀 Enhanced dual-ring loading spinner (no background circle)
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {

                    // Outer thin ring rotating clockwise
                    CircularProgressIndicator(
                        strokeWidth = 3.dp,
                        color = Color.Cyan.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(100.dp)
                            .rotate(haloRotation)
                    )

                    // Inner thicker ring rotating counter-clockwise
                    CircularProgressIndicator(
                        strokeWidth = 5.dp,
                        color = Color.White,
                        modifier = Modifier
                            .size(70.dp)
                            .rotate(-haloRotation)
                            .scale(pulse)
                    )
                }



                Spacer(modifier = Modifier.height(32.dp))

                // 📝 Dynamic Tagline
                Text(
                    text = "Loading your financial power...",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.5f),
                            offset = Offset(1f, 2f),
                            blurRadius = 8f
                        )
                    ),
                    color = Color.White.copy(alpha = 0.95f),
                    modifier = Modifier.alpha(0.97f)
                )
            }
        }
    }
}
