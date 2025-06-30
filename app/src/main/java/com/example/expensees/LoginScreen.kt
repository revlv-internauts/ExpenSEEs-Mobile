package com.example.expensees.screens

import android.content.Context
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.expensees.network.AuthRepository
import androidx.compose.ui.res.painterResource
import com.example.expensees.R
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.math.cos

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    authRepository: AuthRepository
) {
    var usernameOrEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val context = LocalContext.current

    // Animation states
    val alpha by animateFloatAsState(
        targetValue = if (isPressed && !isLoading) 0.85f else 1f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "button_alpha"
    )
    val formAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing),
        label = "form_fade"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isLoading) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "button_scale"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "background_animation")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_offset"
    )

    // Enhanced background with dynamic layered blue gradients
    val baseGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0D47A1).copy(alpha = 0.85f), // Deep blue
            Color(0xFF1976D2).copy(alpha = 0.65f), // Vibrant blue
            Color(0xFF64B5F6).copy(alpha = 0.45f)  // Soft blue
        ),
        start = Offset(
            x = 0f + 250f * cos(animatedOffset * 0.35f),
            y = 0f + 250f * sin(animatedOffset * 0.45f)
        ),
        end = Offset(
            x = 900f + 350f * sin(animatedOffset * 0.55f),
            y = 1300f + 350f * cos(animatedOffset * 0.35f)
        )
    )

    val accentGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF0288D1).copy(alpha = 0.45f), // Bright blue
            Color(0xFF4FC3F7).copy(alpha = 0.25f), // Light blue
            Color.Transparent
        ),
        center = Offset(
            x = 500f + 700f * sin(animatedOffset * 0.45f),
            y = 700f + 500f * cos(animatedOffset * 0.55f)
        ),
        radius = 1100f + 350f * sin(animatedOffset * 0.25f)
    )

    // Subtle blue shimmer effect
    val shimmerGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFB3E5FC).copy(alpha = 0.08f), // Pale blue
            Color(0xFF81D4FA).copy(alpha = 0.04f), // Lighter blue
            Color(0xFFB3E5FC).copy(alpha = 0.08f)  // Pale blue
        ),
        start = Offset(
            x = 0f + 350f * sin(animatedOffset * 0.65f),
            y = 0f
        ),
        end = Offset(
            x = 700f + 350f * sin(animatedOffset * 0.65f),
            y = 700f
        )
    )

    // Shake animation for form card on error
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            shakeOffset.animateTo(
                targetValue = 8f,
                animationSpec = tween(durationMillis = 50, easing = FastOutSlowInEasing)
            )
            shakeOffset.animateTo(
                targetValue = -8f,
                animationSpec = tween(durationMillis = 50, easing = FastOutSlowInEasing)
            )
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 50, easing = FastOutSlowInEasing)
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseGradient)
            .background(accentGradient)
            .background(shimmerGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header with "Login" and Notification Bell
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RectangleShape),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E3A8A)
                ),
                shape = RectangleShape
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "      Login",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 32.sp,
                            color = Color.White
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { navController.navigate("notifications") },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "View notifications",
                            tint = Color(0xFFB0C4DE)
                        )
                    }
                }
            }

            // Welcome Message, App Name, and Logo
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.2f) // Increased weight to push content lower
                    .padding(top = 32.dp) // Increased top padding to move content down
            ) {
                Text(
                    text = "Welcome to ExpenSEEs!",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp // Increased font size for larger welcome message
                    ),
                    color = Color(0xFF4FC3F7).copy(alpha = 0.95f), // Light vibrant blue
                    textAlign = TextAlign.Center
                )
                Image(
                    painter = painterResource(id = R.drawable.expensees),
                    contentDescription = "ExpenSEEs logo",
                    modifier = Modifier
                        .size(400.dp) // Increased size for larger logo
                        .clip(RoundedCornerShape(8.dp))
                )
            }

            // Main Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 6.dp, bottom = 32.dp)
                    .alpha(formAlpha)
                    .offset(x = shakeOffset.value.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Username or Email Field with Icon
                CustomTextField(
                    value = usernameOrEmail,
                    onValueChange = { usernameOrEmail = it.trim() },
                    label = "Username or Email",
                    placeholder = "Enter username or email",
                    keyboardType = KeyboardType.Text,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email or username icon",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                )

                // Password Field with Icon and Visibility Toggle
                CustomTextField(
                    value = password,
                    onValueChange = { password = it.trim() },
                    label = "Password",
                    placeholder = "Enter password",
                    keyboardType = KeyboardType.Password,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Password icon",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    },
                    isPassword = true
                )

                // Server-side Error Message
                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(formAlpha),
                        textAlign = TextAlign.Left
                    )
                }

                // Login Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                        .alpha(alpha)
                        .scale(scale)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            enabled = !isLoading
                        ) {
                            when {
                                usernameOrEmail.isBlank() -> errorMessage = "Please enter a username or email"
                                password.isBlank() -> errorMessage = "Please enter a password"
                                password.length < 6 -> errorMessage = "Password must be at least 6 characters"
                                !usernameOrEmail.contains("@") && usernameOrEmail.length < 3 -> errorMessage = "Username must be at least 3 characters"
                                usernameOrEmail.contains("@") && !usernameOrEmail.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) -> errorMessage = "Please enter a valid email"
                                else -> {
                                    isLoading = true
                                    errorMessage = null
                                    coroutineScope.launch {
                                        Log.d("LoginScreen", "Attempting login with usernameOrEmail: '$usernameOrEmail', coroutineContext=${currentCoroutineContext()}")
                                        val result = authRepository.login(usernameOrEmail, password)
                                        isLoading = false
                                        result.onSuccess {
                                            val refreshToken = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                                                .getString("refresh_token", null)
                                            Log.d(
                                                "LoginScreen",
                                                "Login successful, refreshToken=${refreshToken?.take(10) ?: "null"}... (length=${refreshToken?.length ?: 0})"
                                            )
                                            navController.navigate("home") {
                                                popUpTo("login") { inclusive = true }
                                            }
                                        }.onFailure { e ->
                                            Log.e("LoginScreen", "Login failed: ${e.message}", e)
                                            errorMessage = when (e.message) {
                                                "Invalid credentials" -> "Incorrect username/email or password"
                                                "No internet connection" -> "No internet connection. Please check your network"
                                                "Invalid login response: token or userId is missing" -> "Server error: Invalid response. Please try again."
                                                else -> e.message ?: "Login failed. Please try again"
                                            }
                                        }
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp
                        )
                    } else {
                        Text(
                            text = "Sign In",
                            fontSize = 18.sp,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    }
                }

                // Recover Password Button
                TextButton(
                    onClick = { navController.navigate("forgotA_password") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Recover Password",
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Version Text
            Text(
                text = "Version 1.0",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }
    }
}

// Reusable TextField Composable with Icon and Animations
@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: (@Composable () -> Unit)? = null,
    isPassword: Boolean = false
) {
    // Scale animation for focus state
    val focusInteractionSource = remember { MutableInteractionSource() }
    val isFocused by focusInteractionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "field_scale"
    )

    // Password visibility state
    var isPasswordVisible by remember { mutableStateOf(false) }

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            ),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.87f),
            textAlign = TextAlign.Left,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, bottom = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .scale(scale)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else visualTransformation,
                placeholder = {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    )
                },
                trailingIcon = if (isPassword) {
                    {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else null,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    focusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f),
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    cursorColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f),
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha =0.6f)
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                ),
                interactionSource = focusInteractionSource
            )
        }
    }
}