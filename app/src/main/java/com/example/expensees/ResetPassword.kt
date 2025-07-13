package com.example.expensees.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.expensees.network.AuthRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPassword(
    modifier: Modifier = Modifier,
    navController: NavController,
    authRepository: AuthRepository
) {
    var email by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isResetComplete by remember { mutableStateOf(false) }
    var showCurrentPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isLoading && !isResetComplete) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "button_scale"
    )

    // Focus management
    val focusManager = LocalFocusManager.current
    val emailFocusRequester = remember { FocusRequester() }
    val currentPasswordFocusRequester = remember { FocusRequester() }
    val newPasswordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }

    // Get logged-in user's email from SharedPreferences
    val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    val loggedInEmail = prefs.getString("email", null)

    // Check if the button should be enabled
    val isButtonEnabled = email.isNotBlank() &&
            currentPassword.isNotBlank() &&
            newPassword.isNotBlank() &&
            confirmPassword.isNotBlank() &&
            email == loggedInEmail &&
            newPassword == confirmPassword &&
            !isLoading &&
            !isResetComplete

    // Handle password reset logic
    val performResetPassword: () -> Unit = {
        when {
            email.isBlank() || currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank() -> {
                errorMessage = "Please fill all fields"
            }
            email != loggedInEmail -> {
                errorMessage = "Email does not match the logged-in user's email"
            }
            newPassword != confirmPassword -> {
                errorMessage = "New password and confirm password do not match"
            }
            else -> {
                coroutineScope.launch {
                    isLoading = true
                    errorMessage = null
                    val result = authRepository.resetPassword(email, currentPassword, newPassword, confirmPassword)
                    isLoading = false
                    result.onSuccess {
                        isResetComplete = true
                        Toast.makeText(context, "Password reset successful", Toast.LENGTH_SHORT).show()
                        navController.navigate("login") {
                            popUpTo("reset_password") { inclusive = true }
                        }
                    }.onFailure { e ->
                        errorMessage = when (e.message) {
                            "Email does not match the logged-in user's email" -> "Email does not match the logged-in user's email"
                            "Not authenticated. Please log in." -> "Please log in to reset your password"
                            "No internet connection or server issue" -> "No internet connection or server issue. Please check your network"
                            "User not found" -> "User not found. Please check the email"
                            "Unauthorized: Invalid or expired token" -> "Invalid or expired session. Please log in again"
                            "Server response was incomplete. Please try again later." -> "Server error: Incomplete response. Please try again"
                            "Server error: Unable to process request" -> "Server error: Unable to process request. Please try again"
                            else -> "Failed to reset password: ${e.message}"
                        }
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // Handle clicking outside to dismiss keyboard
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .windowInsetsPadding(WindowInsets(0, 0, 0, 0))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                focusManager.clearFocus()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 50.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Reset Password",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp
                ),
                color = Color(0xFF1F2937),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim() },
                label = { Text("Email Address") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 100.dp)
                    .focusRequester(emailFocusRequester)
                    .onKeyEvent { event ->
                        if (event.key == Key.Enter && event.type == KeyEventType.KeyUp) {
                            focusManager.moveFocus(FocusDirection.Down)
                            true
                        } else {
                            false
                        }
                    },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        focusManager.moveFocus(FocusDirection.Down)
                    }
                )
            )

            OutlinedTextField(
                value = currentPassword,
                onValueChange = { currentPassword = it.trim() },
                label = { Text("Current Password") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .focusRequester(currentPasswordFocusRequester),
                visualTransformation = if (showCurrentPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showCurrentPassword = !showCurrentPassword }) {
                        Icon(
                            imageVector = if (showCurrentPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showCurrentPassword) "Hide password" else "Show password",
                            tint = Color(0xFF1F2937)
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        focusManager.moveFocus(FocusDirection.Down)
                    }
                )
            )

            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it.trim() },
                label = { Text("New Password") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .focusRequester(newPasswordFocusRequester),
                visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showNewPassword = !showNewPassword }) {
                        Icon(
                            imageVector = if (showNewPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showNewPassword) "Hide password" else "Show password",
                            tint = Color(0xFF1F2937)
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        focusManager.moveFocus(FocusDirection.Down)
                    }
                )
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it.trim() },
                label = { Text("Confirm Password") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .focusRequester(confirmPasswordFocusRequester),
                visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                        Icon(
                            imageVector = if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showConfirmPassword) "Hide password" else "Show password",
                            tint = Color(0xFF1F2937)
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        performResetPassword()
                    }
                )
            )

            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .scale(scale),
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
                shadowElevation = 4.dp,
                onClick = { performResetPassword() },
                enabled = isButtonEnabled
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    if (isButtonEnabled) Color(0xFF734656) else Color.Gray,
                                    if (isButtonEnabled) Color(0xFF8A5B6E) else Color.Gray
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(Float.POSITIVE_INFINITY, 0f)
                            )
                        )
                        .alpha(if (isResetComplete) 0.5f else 1f)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else if (!isResetComplete) {
                        Text(
                            text = "Reset Password",
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = "Reset Complete",
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        IconButton(
            onClick = {
                navController.navigate("home") {
                    popUpTo("home") { inclusive = false }
                }
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 50.dp)
                .background(Color(0xFFE5E7EB), CircleShape)
                .size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFF1F2937)
            )
        }
    }

    BackHandler(enabled = true) {
        navController.navigate("home") {
            popUpTo("home") { inclusive = false }
        }
    }
}