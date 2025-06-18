package com.example.expensees

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.expensees.ui.theme.ExpenSEEsTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpenSEEsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

private fun createImageUri(context: Context): Uri? {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return try {
        val imageFile = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    } catch (e: Exception) {
        null
    }
}

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val expenses = remember { mutableStateListOf<Expense>() }
    val submittedBudgets = remember { mutableStateListOf<SubmittedBudget>() }

    NavHost(navController = navController, startDestination = "loading") {
        composable("loading") {
            LoadingScreen(
                modifier = modifier,
                onLoadingComplete = {
                    navController.navigate("login") {
                        popUpTo("loading") { inclusive = true }
                    }
                }
            )
        }
        composable("login") {
            LoginScreen(
                modifier = modifier,
                navController = navController, // Pass navController here
                onLoginClick = { _, _ ->
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            HomeScreen(
                expenses = expenses,
                navController = navController,
                onRecordExpensesClick = { navController.navigate("record_expenses") },
                onListExpensesClick = { navController.navigate("list_expenses") },
                onLogoutClick = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                modifier = modifier
            )
        }
        composable("record_expenses") {
            RecordExpensesScreen(
                navController = navController,
                expenses = expenses,
                onLogoutClick = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                modifier = modifier
            )
        }
        composable("list_expenses") {
            ExpenseListScreen(
                navController = navController,
                expenses = expenses,
                onDeleteExpenses = { expensesToDelete ->
                    expenses.removeAll(expensesToDelete)
                },
                onLogoutClick = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                modifier = modifier
            )
        }
        composable("fund_request") {
            FundRequest(
                navController = navController,
                submittedBudgets = submittedBudgets,
                modifier = modifier
            )
        }
        composable("liquidation_report") {
            LiquidationReport(
                submittedBudgets = submittedBudgets,
                expenses = expenses, // Pass the expenses list
                navController = navController,
                modifier = modifier
            )
        }
        composable("reset_password") {
            ResetPassword(
                navController = navController,
                modifier = modifier
            )
        }
        composable("forgot_password") {
            ForgotPassword(
                navController = navController,
                modifier = modifier
            )
        }
    }
}

data class ExpenseItem(
    val category: String,
    val quantity: Int,
    val amountPerUnit: Double,
    val remarks: String = ""
)

enum class BudgetStatus {
    PENDING, APPROVED, DENIED
}

data class SubmittedBudget(
    val name: String,
    val expenses: List<ExpenseItem>,
    val total: Double,
    val status: BudgetStatus = BudgetStatus.PENDING
)

data class Expense(
    val description: String,
    val amount: Double,
    val category: String,
    val photoUri: Uri? = null,
    val dateOfTransaction: String,
    val dateAdded: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
) {
    companion object {
        const val DEFAULT_CATEGORY = "Other"
    }
}

@Composable
fun LoadingScreen(modifier: Modifier = Modifier, onLoadingComplete: () -> Unit = {}) {
    // Trigger loading completion after 3 seconds
    LaunchedEffect(Unit) {
        delay(3000L)
        onLoadingComplete()
    }

    // Animation for text scaling
    val textScale by animateFloatAsState(
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "textScale"
    )

    // Animation for progress indicator rotation
    val rotation by animateFloatAsState(
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "rotation"
    )

    // Animation for pulse effect
    val pulse by animateFloatAsState(
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    // Gradient background
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradientBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated title with shadow and scaling
            Text(
                text = "ExpenSEEs",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        offset = Offset(4f, 4f),
                        blurRadius = 8f
                    )
                ),
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .scale(textScale)
                    .padding(bottom = 48.dp)
            )

            // Rotating and pulsing progress indicator
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 6.dp,
                modifier = Modifier
                    .size(64.dp)
                    .rotate(rotation)
                    .scale(pulse)
            )

            // Additional flair: animated subtitle
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Loading Your Financial Adventure...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                modifier = Modifier
                    .alpha(animateFloatAsState(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 1500, easing = LinearOutSlowInEasing),
                        label = "subtitleAlpha"
                    ).value)
            )
        }

        // Corner decorations: subtle gradient orbs
        Box(
            modifier = Modifier
                .size(100.dp)
                .align(Alignment.TopStart)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(100.dp)
                .align(Alignment.BottomEnd)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    navController: NavController, // Add navController parameter
    onLoginClick: (String, String) -> Unit = { _, _ -> }
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoginComplete by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isLoading && !isLoginComplete) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "button_scale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ExpenSEEs",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email or Username") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .scale(scale)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(12.dp),
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .alpha(if (isLoginComplete) 0.5f else 1f)
                .clickable(
                    interactionSource = interactionSource,
                    indication = rememberRipple(),
                    enabled = !isLoading && !isLoginComplete
                ) {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Please enter email and password"
                    } else {
                        coroutineScope.launch {
                            if (email == "andrew@gmail.com" && password == "123") {
                                isLoading = true
                                delay(1500L)
                                isLoading = false
                                isLoginComplete = true
                                onLoginClick(email, password)
                                Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()
                            } else {
                                errorMessage = "Invalid email or password"
                                Toast.makeText(context, "Login failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else if (!isLoginComplete) {
                Text(
                    text = "Login",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }

        TextButton(
            onClick = { navController.navigate("forgot_password") }, // Use navController for navigation
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(
                text = "Forgot password?",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 16.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPassword(
    modifier: Modifier = Modifier,
    navController: NavController
) {
    var email by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isResetSent by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isLoading && !isResetSent) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "button_scale"
    )

    // Handle system back button
    BackHandler(enabled = true) {
        navController.navigate("login") {
            popUpTo("login") { inclusive = false }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Forgot Password",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigate("login") {
                            popUpTo("login") { inclusive = false }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Forgot Your Password?",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Text(
                text = "Enter your email to receive a password reset link.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .scale(scale)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(12.dp),
                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .alpha(if (isResetSent) 0.5f else 1f)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = rememberRipple(),
                        enabled = !isLoading && !isResetSent
                    ) {
                        if (email.isBlank()) {
                            errorMessage = "Please enter your email"
                        } else if (email != "andrew@gmail.com") {
                            errorMessage = "Email not found"
                        } else {
                            coroutineScope.launch {
                                isLoading = true
                                delay(1500L)
                                isLoading = false
                                isResetSent = true
                                errorMessage = null
                                Toast.makeText(context, "Password reset link sent to $email", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else if (!isResetSent) {
                    Text(
                        text = "Send Reset Link",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                } else {
                    Text(
                        text = "Link Sent",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPassword(
    modifier: Modifier = Modifier,
    navController: NavController
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isResetComplete by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isLoading && !isResetComplete) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "button_scale"
    )

    // Handle system back button
    BackHandler(enabled = true) {
        navController.navigate("home") {
            popUpTo("home") { inclusive = false }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Reset Password",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = false }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Reset Your Password",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("New Password") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .scale(scale)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(12.dp),
                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .alpha(if (isResetComplete) 0.5f else 1f)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = rememberRipple(),
                        enabled = !isLoading && !isResetComplete
                    ) {
                        if (newPassword.isBlank() || confirmPassword.isBlank()) {
                            errorMessage = "Please fill all fields"
                        } else if (newPassword != confirmPassword) {
                            errorMessage = "Passwords do not match"
                        } else {
                            coroutineScope.launch {
                                isLoading = true
                                delay(1500L)
                                isLoading = false
                                isResetComplete = true
                                errorMessage = null
                                Toast.makeText(context, "Password reset successful", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else if (!isResetComplete) {
                    Text(
                        text = "Reset Password",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                } else {
                    Text(
                        text = "Reset Complete",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
fun RecordExpensesScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    expenses: MutableList<Expense>,
    onLogoutClick: () -> Unit = {}
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var dateOfTransaction by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var permissionType by remember { mutableStateOf("") }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var selectedExpense by remember { mutableStateOf<Expense?>(null) }
    var showFullScreenImage by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var expenseImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val categories = listOf(
        "Utilities",
        "Food",
        "Transportation",
        "Gas",
        "Office Supplies",
        "Rent",
        "Parking",
        "Electronic Supplies",
        "Other Expenses"
    )
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val calendar = Calendar.getInstance()
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                dateOfTransaction = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && selectedImageUri != null) {
            selectedImageBitmap = try {
                BitmapFactory.decodeStream(context.contentResolver.openInputStream(selectedImageUri!!))
            } catch (e: Exception) {
                null
            }
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            selectedImageBitmap = try {
                BitmapFactory.decodeStream(context.contentResolver.openInputStream(it))
            } catch (e: Exception) {
                null
            }
        }
    }

    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when (permissionType) {
            "camera" -> {
                if (permissions[Manifest.permission.CAMERA] == true) {
                    createImageUri(context)?.let { uri ->
                        selectedImageUri = uri
                        takePictureLauncher.launch(uri)
                    } ?: Toast.makeText(context, "Error creating image file", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Camera permission denied", Toast.LENGTH_LONG).show()
                }
            }
            "storage" -> {
                val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
                if (permissions[storagePermission] == true) {
                    pickImageLauncher.launch("image/*")
                } else {
                    Toast.makeText(context, "Storage permission denied", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = { Text("Permission Required") },
            text = {
                Text(
                    when (permissionType) {
                        "camera" -> "Camera access is needed to take photos of your expense receipts."
                        "storage" -> "Storage access is needed to select photos from your gallery."
                        else -> ""
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionRationale = false
                        val permission = when (permissionType) {
                            "camera" -> Manifest.permission.CAMERA
                            "storage" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Manifest.permission.READ_MEDIA_IMAGES
                            } else {
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            }
                            else -> ""
                        }
                        multiplePermissionsLauncher.launch(arrayOf(permission))
                    }
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationale = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showExpenseDialog && selectedExpense != null) {
        AlertDialog(
            onDismissRequest = {
                showExpenseDialog = false
                selectedExpense = null
            },
            title = { Text("${selectedExpense!!.category} Receipt") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    selectedExpense!!.photoUri?.let { uri ->
                        val bitmap = try {
                            BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
                        } catch (e: Exception) {
                            null
                        }
                        bitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "${selectedExpense!!.category} receipt",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .padding(bottom = 8.dp)
                                    .clickable {
                                        expenseImageBitmap = it
                                        showFullScreenImage = true
                                    }
                            )
                        } ?: Text(
                            text = "No receipt photo available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    } ?: Text(
                        text = "No receipt photo available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExpenseDialog = false
                        selectedExpense = null
                    }
                ) {
                    Text("Close")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showInfoDialog = true
                    }
                ) {
                    Text("Info")
                }
            }
        )
    }

    if (showInfoDialog && selectedExpense != null) {
        AlertDialog(
            onDismissRequest = {
                showInfoDialog = false
            },
            title = { Text("Expense Details") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Category: ${selectedExpense!!.category}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Amount: ₱${String.format("%.2f", selectedExpense!!.amount)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Date of Transaction: ${selectedExpense!!.dateOfTransaction}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Date Added: ${selectedExpense!!.dateAdded}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Remarks: ${selectedExpense!!.description}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showInfoDialog = false
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }

    if (showFullScreenImage) {
        AlertDialog(
            onDismissRequest = { showFullScreenImage = false },
            modifier = Modifier.fillMaxSize(),
            confirmButton = {
                TextButton(onClick = { showFullScreenImage = false }) {
                    Text("Close")
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(
                            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    expenseImageBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Full screen expense photo",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentScale = ContentScale.Fit
                        )
                    } ?: selectedExpense?.photoUri?.let { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = "Full screen expense photo",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentScale = ContentScale.Fit,
                            onError = {
                                Toast.makeText(context, "Failed to load full screen image", Toast.LENGTH_SHORT).show()
                            }
                        )
                    } ?: Text(
                        text = "No image available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "A",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "User Profile",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "andrew@gmail.com",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    TextButton(
                        onClick = {
                            navController.navigate("reset_password")
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Reset Password",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(
                        onClick = {
                            Toast.makeText(context, "Theme clicked", Toast.LENGTH_SHORT).show()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Theme",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(
                        onClick = {
                            Toast.makeText(context, "About clicked", Toast.LENGTH_SHORT).show()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "About",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = onLogoutClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(
                            text = "Logout",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.navigate("home") }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to home",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open navigation drawer",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "Record Expenses",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { },
                    label = { Text("Category") },
                    modifier = Modifier
                        .fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        TextButton(onClick = { expanded = !expanded }) {
                            Text(if (expanded) "▲" else "▼")
                        }
                    }
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categories.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                category = option
                                expanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            val interactionSource = remember { MutableInteractionSource() }
            OutlinedTextField(
                value = dateOfTransaction,
                onValueChange = { },
                label = { Text("Date of Transaction (YYYY-MM-DD)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                readOnly = true,
                interactionSource = interactionSource,
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Select date",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect { interaction ->
                    if (interaction is PressInteraction.Release) {
                        datePickerDialog.show()
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Remarks") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        permissionType = "camera"
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                            context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                        ) {
                            if (ActivityCompat.shouldShowRequestPermissionRationale(
                                    context as Activity,
                                    Manifest.permission.CAMERA
                                )
                            ) {
                                showPermissionRationale = true
                            } else {
                                multiplePermissionsLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                            }
                        } else {
                            createImageUri(context)?.let { uri ->
                                selectedImageUri = uri
                                takePictureLauncher.launch(uri)
                            } ?: Toast.makeText(context, "Error creating image file", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(
                        text = "Take Photo",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                }
                Button(
                    onClick = {
                        permissionType = "storage"
                        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Manifest.permission.READ_MEDIA_IMAGES
                        } else {
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                            context.checkSelfPermission(storagePermission) != PackageManager.PERMISSION_GRANTED
                        ) {
                            if (ActivityCompat.shouldShowRequestPermissionRationale(
                                    context as Activity,
                                    storagePermission
                                )
                            ) {
                                showPermissionRationale = true
                            } else {
                                multiplePermissionsLauncher.launch(arrayOf(storagePermission))
                            }
                        } else {
                            pickImageLauncher.launch("image/*")
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(
                        text = "Pick from Gallery",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                }
            }

            selectedImageBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Selected expense photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(bottom = 16.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable {
                            expenseImageBitmap = bitmap
                            showFullScreenImage = true
                        },
                    contentScale = ContentScale.Crop
                )
            } ?: Text(
                text = "No image selected",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (showFullScreenImage) {
                AlertDialog(
                    onDismissRequest = { showFullScreenImage = false },
                    modifier = Modifier.fillMaxSize(),
                    confirmButton = {
                        TextButton(onClick = { showFullScreenImage = false }) {
                            Text("Close")
                        }
                    },
                    text = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(
                                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            expenseImageBitmap?.let { bitmap ->
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Full screen expense photo",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } ?: Text(
                                text = "No image available",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }

            Button(
                onClick = {
                    if (description.isNotBlank() && amount.isNotBlank() && category.isNotBlank() && dateOfTransaction.isNotBlank()) {
                        val amountValue = amount.toDoubleOrNull()
                        if (amountValue != null) {
                            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                            expenses.add(
                                Expense(
                                    description = description,
                                    amount = amountValue,
                                    category = category,
                                    photoUri = selectedImageUri,
                                    dateOfTransaction = dateOfTransaction,
                                    dateAdded = timestamp
                                )
                            )
                            description = ""
                            amount = ""
                            category = ""
                            dateOfTransaction = ""
                            selectedImageBitmap = null
                            selectedImageUri = null
                            Toast.makeText(context, "Expense added", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Invalid amount", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Add Expense",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Recent Transactions",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                items(expenses.sortedByDescending {
                    try {
                        dateFormat.parse(it.dateAdded) ?: Date(0)
                    } catch (e: Exception) {
                        Date(0)
                    }
                }) { expense ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                selectedExpense = expense
                                showExpenseDialog = true
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = expense.description,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = expense.category,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "₱${String.format("%.2f", expense.amount)}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    expenses: List<Expense>,
    navController: NavController,
    onRecordExpensesClick: () -> Unit = {},
    onListExpensesClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val categories = listOf(
        "Utilities",
        "Food",
        "Transportation",
        "Gas",
        "Office Supplies",
        "Rent",
        "Parking",
        "Electronic Supplies",
        "Other Expenses"
    )

    val totalExpenses = expenses.sumOf { it.amount }
    val categoryTotals = expenses.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }
        .take(5)
    val chartData = categories.map { category ->
        expenses.filter { it.category == category }.sumOf { it.amount }
    }

    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedTransaction by remember { mutableStateOf<Expense?>(null) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var expenseImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val transactionsForCategory = expenses.filter { it.category == selectedCategory }

    var showFullScreenImage by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    var selectedChartCategory by remember { mutableStateOf<String?>(null) }
    var selectedCategoryAmount by remember { mutableStateOf(0.0) }

    val categoryColors = categories.zip(
        listOf(
            Color(0xFFFF6B6B),
            Color(0xFF4ECDC4),
            Color(0xFF45B7D1),
            Color(0xFF96CEB4),
            Color(0xFFFFEEAD),
            Color(0xFFD4A5A5),
            Color(0xFFA8DADC),
            Color(0xFFF4A261),
            Color(0xFFE76F51)
        )
    ).toMap()

    val animatedScale = remember { List(categoryTotals.size) { Animatable(0f) } }
    LaunchedEffect(categoryTotals) {
        animatedScale.forEachIndexed { index, animatable ->
            launch {
                animatable.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 300,
                        delayMillis = index * 100,
                        easing = FastOutSlowInEasing
                    )
                )
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .heightIn(max = 500.dp)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "A",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "User Profile",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "andrew@gmail.com",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    TextButton(
                        onClick = {
                            navController.navigate("reset_password")
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Reset Password",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(
                        onClick = {
                            Toast.makeText(context, "Theme clicked", Toast.LENGTH_SHORT).show()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Theme",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(
                        onClick = {
                            Toast.makeText(context, "About clicked", Toast.LENGTH_SHORT).show()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "About",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = onLogoutClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(
                            text = "Logout",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
            }
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp, top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open navigation drawer",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "ExpenSEEs",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "Welcome to ExpenSEEs!",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (expenses.isEmpty()) {
                Text(
                    text = "No expenses recorded yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            try {
                                loadDataWithBaseURL(
                                    null,
                                    """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
                        <style>
                            html, body {
                                margin: 0;
                                padding: 0;
                                background: transparent;
                                height: 100%;
                                width: 100%;
                            }
                            #expenseChart {
                                width: 100%;
                                height: 100%;
                            }
                        </style>
                    </head>
                    <body>
                        <canvas id="expenseChart"></canvas>
                        <script>
                            const chartData = [${chartData.joinToString()}];
                            const ctx = document.getElementById('expenseChart').getContext('2d');
                            const chart = new Chart(ctx, {
                                type: 'pie',
                                data: {
                                    labels: ['Utilities', 'Food', 'Transportation', 'Gas', 'Office Supplies', 'Rent', 'Parking', 'Electronic Supplies', 'Other Expenses'],
                                    datasets: [{
                                        data: chartData,
                                        backgroundColor: [
                                            '#FF6B6B',
                                            '#4ECDC4',
                                            '#45B7D1',
                                            '#96CEB4',
                                            '#FFEEAD',
                                            '#D4A5A5',
                                            '#A8DADC',
                                            '#F4A261',
                                            '#E76F51'
                                        ],
                                        borderColor: ['#FFFFFF'],
                                        borderWidth: 1
                                    }]
                                },
                                options: {
                                    responsive: true,
                                    maintainAspectRatio: false,
                                    plugins: {
                                        legend: {
                                            display: false
                                        },
                                        title: {
                                            display: true,
                                            text: 'Expense Distribution by Category',
                                            color: '#333333',
                                            font: { size: 18 },
                                            align: 'center'
                                        },
                                        tooltip: {
                                            enabled: true
                                        }
                                    },
                                    onClick: (event, elements, chart) => {
                                        if (elements.length > 0) {
                                            const index = elements[0].index;
                                            const label = chart.data.labels[index];
                                            chart.options.plugins.title.text = label;
                                            window.android.onCategorySelected(label, chart.data.datasets[0].data[index]);
                                        } else {
                                            chart.options.plugins.title.text = 'Expense Distribution by Category';
                                            window.android.onCategorySelected('', 0);
                                        }
                                        chart.update();
                                    }
                                }
                            });
                        </script>
                    </body>
                    </html>
                    """.trimIndent(),
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            } catch (e: Exception) {
                                Toast.makeText(ctx, "Failed to load chart", Toast.LENGTH_SHORT).show()
                            }
                            addJavascriptInterface(object : Any() {
                                @JavascriptInterface
                                fun onCategorySelected(category: String, amount: Double) {
                                    scope.launch {
                                        selectedChartCategory = if (category.isEmpty()) null else category
                                        selectedCategoryAmount = amount
                                    }
                                }
                            }, "android")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(vertical = 4.dp)
                )
                Text(
                    text = if (selectedChartCategory != null) {
                        "${selectedChartCategory} Expenses: ₱${String.format("%.2f", selectedCategoryAmount)}"
                    } else {
                        "Total Expenses: ₱${String.format("%.2f", totalExpenses)}"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = selectedChartCategory?.let { categoryColors[it] } ?: MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = "Your Top 5 Expenses",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            categoryTotals.forEachIndexed { index, (category, amount) ->
                                val scale by animatedScale.getOrNull(index)?.asState() ?: remember { mutableStateOf(1f) }
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .scale(scale)
                                        .clickable { selectedCategory = category },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Transparent
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(
                                                        categoryColors[category]!!.copy(alpha = 0.2f),
                                                        categoryColors[category]!!.copy(alpha = 0.05f)
                                                    ),
                                                    start = Offset(0f, 0f),
                                                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                                )
                                            )
                                            .border(
                                                1.dp,
                                                categoryColors[category]!!,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = when (index) {
                                                    0 -> Color(0xFFFFD700)
                                                    1 -> Color(0xFFC0C0C0)
                                                    2 -> Color(0xFFCD7F32)
                                                    else -> MaterialTheme.colorScheme.primary
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "${index + 1}",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onPrimary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = category,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = "₱${String.format("%.2f", amount)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.weight(1f))
                                            Text(
                                                text = if (totalExpenses > 0) {
                                                    "${String.format("%.2f", (amount / totalExpenses) * 100)}%"
                                                } else {
                                                    "0.00%"
                                                },
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = categoryColors[category]!!,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = onListExpensesClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .padding(end = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "List of Expenses",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                    Button(
                        onClick = onRecordExpensesClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .padding(start = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "Record Expenses",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { navController.navigate("fund_request") },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .padding(end = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "Request Fund",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                    Button(
                        onClick = { navController.navigate("liquidation_report") },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .padding(start = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "Liquidation Report",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        if (selectedCategory != null) {
            AlertDialog(
                onDismissRequest = { selectedCategory = null },
                title = {
                    Text(
                        text = "Transactions for ${selectedCategory ?: ""}",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    if (transactionsForCategory.isEmpty()) {
                        Text(
                            text = "No transactions found for this category.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        settings.javaScriptEnabled = true
                                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                        try {
                                            loadDataWithBaseURL(
                                                null,
                                                """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
                                <style>
                                    html, body {
                                        margin: 0;
                                        padding: 0;
                                        background: transparent;
                                        height: 100%;
                                        width: 100%;
                                    }
                                    #transactionChart {
                                        width: 100%;
                                        height: 100%;
                                    }
                                </style>
                            </head>
                            <body>
                                <canvas id="transactionChart"></canvas>
                                <script>
                                    const transactionData = [${transactionsForCategory.map { it.amount }.joinToString()}];
                                    const totalTransactions = transactionData.reduce((sum, value) => sum + value, 0);
                                    const ctx = document.getElementById('transactionChart').getContext('2d');
                                    const chart = new Chart(ctx, {
                                        type: 'bar',
                                        data: {
                                            labels: [${transactionsForCategory.mapIndexed { index, expense ->
                                                    "'${expense.description.replace("'", "\\'")}'"
                                                }.joinToString()}],
                                            datasets: [{
                                                data: transactionData,
                                                backgroundColor: [
                                                    '#FF6B6B',
                                                    '#4ECDC4',
                                                    '#45B7D1',
                                                    '#96CEB4',
                                                    '#FFEEAD',
                                                    '#D4A5A5',
                                                    '#A8DADC',
                                                    '#F4A261',
                                                    '#E76F51'
                                                ],
                                                borderColor: ['#FFFFFF'],
                                                borderWidth: 1
                                            }]
                                        },
                                        options: {
                                            responsive: true,
                                            maintainAspectRatio: false,
                                            plugins: {
                                                legend: {
                                                    display: false
                                                },
                                                title: {
                                                    display: true,
                                                    text: 'Transaction Distribution for ${selectedCategory ?: ""}',
                                                    color: '#333333',
                                                    font: { size: 18 },
                                                    align: 'center'
                                                },
                                                tooltip: {
                                                    enabled: true,
                                                    callbacks: {
                                                        label: function(context) {
                                                            const label = context.label || '';
                                                            const value = context.raw || 0;
                                                            const percentage = totalTransactions > 0 ? ((value / totalTransactions) * 100).toFixed(2) : 0;
                                                            return ': ' + value;
                                                        }
                                                    }
                                                }
                                            },
                                            onClick: (event, elements, chart) => {
                                                if (elements.length > 0) {
                                                    const index = elements[0].index;
                                                    const label = chart.data.labels[index];
                                                    const value = chart.data.datasets[0].data[index];
                                                    const percentage = totalTransactions > 0 ? ((value / totalTransactions) * 100).toFixed(2) : 0;
                                                    chart.options.plugins.title.text = label + ': ' + percentage + '%';
                                                    chart.update();
                                                } else {
                                                    chart.options.plugins.title.text = 'Transaction Distribution for ${selectedCategory ?: ""}';
                                                    chart.update();
                                                }
                                            }
                                        }
                                    });
                                </script>
                            </body>
                            </html>
                            """.trimIndent(),
                                                "text/html",
                                                "UTF-8",
                                                null
                                            )
                                        } catch (e: Exception) {
                                            Toast.makeText(ctx, "Failed to load transaction chart", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                                    .padding(vertical = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                            ) {
                                items(transactionsForCategory) { expense ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                selectedTransaction = expense
                                                showExpenseDialog = true
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp)
                                        ) {
                                            Text(
                                                text = expense.dateOfTransaction,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.align(Alignment.Start)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = expense.description,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "₱${String.format("%.2f", expense.amount)}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedCategory = null }) {
                        Text("Close")
                    }
                }
            )
        }
        if (showExpenseDialog && selectedTransaction != null) {
            AlertDialog(
                onDismissRequest = {
                    showExpenseDialog = false
                    selectedTransaction = null
                },
                title = { Text("${selectedTransaction!!.category} Receipt") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        selectedTransaction!!.photoUri?.let { uri ->
                            val bitmap = try {
                                BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
                            } catch (e: Exception) {
                                null
                            }
                            bitmap?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = "${selectedTransaction!!.category} receipt",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                        .padding(bottom = 8.dp)
                                        .clickable {
                                            expenseImageBitmap = it
                                            showFullScreenImage = true
                                        }
                                )
                            } ?: Text(
                                text = "No receipt photo available",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        } ?: Text(
                            text = "No receipt photo available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showExpenseDialog = false
                            selectedTransaction = null
                        }
                    ) {
                        Text("Close")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showInfoDialog = true
                        }
                    ) {
                        Text("Info")
                    }
                }
            )
        }
        if (showInfoDialog && selectedTransaction != null) {
            AlertDialog(
                onDismissRequest = {
                    showInfoDialog = false
                },
                title = { Text("Expense Details") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Category: ${selectedTransaction!!.category}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Amount: ₱${String.format("%.2f", selectedTransaction!!.amount)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Date of Transaction: ${selectedTransaction!!.dateOfTransaction}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Date Added: ${selectedTransaction!!.dateAdded}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Remarks: ${selectedTransaction!!.description}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showInfoDialog = false
                        }
                    ) {
                        Text("Close")
                    }
                }
            )
        }
        if (showFullScreenImage) {
            AlertDialog(
                onDismissRequest = { showFullScreenImage = false },
                modifier = Modifier.fillMaxSize(),
                confirmButton = {
                    TextButton(onClick = { showFullScreenImage = false }) {
                        Text("Close")
                    }
                },
                text = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(
                                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        expenseImageBitmap?.let { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Full screen expense photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentScale = ContentScale.Fit
                            )
                        } ?: selectedTransaction?.photoUri?.let { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = "Full screen expense photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentScale = ContentScale.Fit,
                                onError = {
                                    Toast.makeText(context, "Failed to load full screen image", Toast.LENGTH_SHORT).show()
                                }
                            )
                        } ?: Text(
                            text = "No image available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            )
        }
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiquidationReport(
    modifier: Modifier = Modifier,
    submittedBudgets: List<SubmittedBudget>,
    expenses: List<Expense>,
    navController: NavController
) {
    val context = LocalContext.current
    var selectedBudget by remember { mutableStateOf<SubmittedBudget?>(null) }
    var showExpenseSelectionDialog by remember { mutableStateOf(false) }
    var currentExpenseItem by remember { mutableStateOf<Pair<ExpenseItem, Int>?>(null) }
    val selectedExpensesMap = remember { mutableStateMapOf<Int, MutableList<Expense>>() }
    val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Define status colors
    val statusColors = mapOf(
        BudgetStatus.PENDING to Color(0xFFFFCA28), // Yellow
        BudgetStatus.APPROVED to Color(0xFF4CAF50), // Green
        BudgetStatus.DENIED to Color(0xFFF44336) // Red
    )

    // Define text colors for budget list
    val textColors = mapOf(
        BudgetStatus.PENDING to Color.Gray,
        BudgetStatus.APPROVED to Color.Black,
        BudgetStatus.DENIED to Color.Black
    )

    // Calculate total remaining balance for the selected budget
    val totalRemainingBalance = selectedBudget?.let { budget ->
        budget.expenses.withIndex().sumOf { (index, expense) ->
            val budgetedAmount = expense.quantity * expense.amountPerUnit
            val actualExpenseTotal = selectedExpensesMap[index]?.sumOf { it.amount } ?: 0.0
            budgetedAmount - actualExpenseTotal
        }
    } ?: 0.0

    // Calculate total actual expenses
    val totalActualExpenses = selectedBudget?.let { budget ->
        budget.expenses.withIndex().sumOf { (index, _) ->
            selectedExpensesMap[index]?.sumOf { it.amount } ?: 0.0
        }
    } ?: 0.0

    // Handle system back button
    BackHandler(enabled = true) {
        if (selectedBudget != null) {
            selectedBudget = null
        } else {
            if (navController.currentBackStackEntry?.destination?.route != "home") {
                navController.navigate("home") {
                    popUpTo("home") { inclusive = false }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Liquidation Report",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedBudget != null) {
                            selectedBudget = null // Return to budget selection
                        } else {
                            // Navigate to home if not already there
                            if (navController.currentBackStackEntry?.destination?.route != "home") {
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = false }
                                }
                            }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            if (submittedBudgets.isEmpty()) {
                Text(
                    text = "No budget requests submitted yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center
                )
            } else if (selectedBudget == null) {
                Text(
                    text = "Select a Budget Request",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(submittedBudgets) { budget ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedBudget = budget },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = budget.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = textColors[budget.status] ?: MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(statusColors[budget.status] ?: Color.Gray)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Total: ₱${numberFormat.format(budget.total)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColors[budget.status] ?: MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Status: ${budget.status.name.lowercase(Locale.US).replaceFirstChar { it.uppercase() }}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = statusColors[budget.status] ?: MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                selectedBudget?.let { budget ->
                    Text(
                        text = "Liquidation Report: ${budget.name}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Status: ${budget.status.name.lowercase(Locale.US).replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = statusColors[budget.status] ?: MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(statusColors[budget.status] ?: Color.Gray)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Total Budgeted Expenses: ₱${numberFormat.format(budget.total)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "Expense Details",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(budget.expenses.withIndex().toList()) { (index, expense) ->
                            // Calculate actual expenses for this expense item
                            val actualExpenseTotal = selectedExpensesMap[index]?.sumOf { it.amount } ?: 0.0
                            // Calculate budgeted amount
                            val budgetedAmount = expense.quantity * expense.amountPerUnit
                            // Calculate remaining balance
                            val remainingBalance = budgetedAmount - actualExpenseTotal

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = expense.category,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "₱${numberFormat.format(budgetedAmount)}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    currentExpenseItem = expense to index
                                                    showExpenseSelectionDialog = true
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Upload receipt",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    selectedExpensesMap.remove(index)
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            message = "All receipts removed for ${expense.category}",
                                                            duration = SnackbarDuration.Short
                                                        )
                                                    }
                                                },
                                                enabled = selectedExpensesMap.containsKey(index)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete receipts",
                                                    tint = if (selectedExpensesMap.containsKey(index))
                                                        MaterialTheme.colorScheme.error
                                                    else
                                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Quantity: ${expense.quantity}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Unit Price: ₱${numberFormat.format(expense.amountPerUnit)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Actual Expenses: ₱${numberFormat.format(actualExpenseTotal)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Remaining Balance: ₱${numberFormat.format(remainingBalance)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (remainingBalance >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                                    )
                                    if (expense.remarks.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Remarks: ${expense.remarks}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    // Display selected transactions
                                    selectedExpensesMap[index]?.let { selectedExpenses ->
                                        if (selectedExpenses.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Selected Receipts:",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            selectedExpenses.forEach { selected ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(start = 8.dp, top = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = selected.description,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "₱${numberFormat.format(selected.amount)}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Display total actual expenses
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text(
                            text = "Total Actual Expenses: ₱${numberFormat.format(totalActualExpenses)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Display total remaining balance for the budget
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = if (totalRemainingBalance >= 0) {
                                "Remaining Credit: ₱${numberFormat.format(totalRemainingBalance)}"
                            } else {
                                "Over Budget: ₱${numberFormat.format(-totalRemainingBalance)}"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (totalRemainingBalance >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { selectedBudget = null },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .padding(end = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text(
                                text = "Back to Budgets",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                        Button(
                            onClick = {
                                // Generate LaTeX report
                                val reportContent = buildString {
                                    append("\\documentclass[a4paper,11pt]{article}\n")
                                    append("\\usepackage[margin=1in]{geometry}\n")
                                    append("\\usepackage{booktabs}\n")
                                    append("\\usepackage{tabularx}\n")
                                    append("\\usepackage{siunitx}\n")
                                    append("\\usepackage{fancyhdr}\n")
                                    append("\\usepackage{lastpage}\n")
                                    append("\\sisetup{group-separator={,},round-mode=places,round-precision=2}\n")
                                    append("\\pagestyle{fancy}\n")
                                    append("\\fancyhf{}\n")
                                    append("\\fancyhead[C]{Liquidation Report: ${budget.name.replace("&", "\\&").replace("%", "\\%").replace("_", "\\_")}}\n")
                                    append("\\fancyfoot[C]{Page \\thepage\\ of \\pageref{LastPage}}\n")
                                    append("\\renewcommand{\\headrulewidth}{0.4pt}\n")
                                    append("\\renewcommand{\\footrulewidth}{0.4pt}\n")
                                    append("\\begin{document}\n")
                                    append("\\begin{center}\n")
                                    append("{\\Large \\textbf{Liquidation Report}}\\\\[0.5cm]\n")
                                    append("{\\large Budget: ${budget.name.replace("&", "\\&").replace("%", "\\%").replace("_", "\\_")}}\\\\\n")
                                    append("Status: ${budget.status.name.lowercase(Locale.US).replaceFirstChar { it.uppercase() }}\\\\[-0.3cm]\n")
                                    append("Generated on: ${SimpleDateFormat("MMMM dd, yyyy").format(Date())}\n")
                                    append("\\end{center}\n")
                                    append("\\vspace{0.5cm}\n")

                                    // Budget Summary
                                    append("\\section*{Budget Summary}\n")
                                    append("Total Budgeted Expenses: \\SI{${numberFormat.format(budget.total)}}{PHP}\\\\[-0.2cm]\n")
                                    append("Total Actual Expenses: \\SI{${numberFormat.format(totalActualExpenses)}}{PHP}\\\\[-0.2cm]\n")
                                    append("Remaining Balance: \\SI{${numberFormat.format(totalRemainingBalance)}}{PHP}\n")
                                    append("\\vspace{0.3cm}\n")

                                    // Expense Details Table
                                    append("\\section*{Expense Details}\n")
                                    append("\\begin{tabularx}{\\textwidth}{Xrrrc}\n")
                                    append("\\toprule\n")
                                    append("Category & Quantity & Unit Price (PHP) & Budgeted (PHP) & Actual (PHP) & Balance (PHP) \\\\\n")
                                    append("\\midrule\n")
                                    budget.expenses.withIndex().forEach { (index, expense) ->
                                        val actualTotal = selectedExpensesMap[index]?.sumOf { it.amount } ?: 0.0
                                        val budgeted = expense.quantity * expense.amountPerUnit
                                        val balance = budgeted - actualTotal
                                        append("${expense.category.replace("&", "\\&").replace("%", "\\%").replace("_", "\\_")} & " +
                                                "${expense.quantity} & " +
                                                "\\SI{${numberFormat.format(expense.amountPerUnit)}}{PHP} & " +
                                                "\\SI{${numberFormat.format(budgeted)}}{PHP} & " +
                                                "\\SI{${numberFormat.format(actualTotal)}}{PHP} & " +
                                                "\\SI{${numberFormat.format(balance)}}{PHP} \\\\\n")
                                        if (expense.remarks.isNotBlank()) {
                                            append("\\multicolumn{6}{l}{\\textit{Remarks: ${expense.remarks.replace("&", "\\&").replace("%", "\\%").replace("_", "\\_")}}} \\\\\n")
                                        }
                                    }
                                    append("\\bottomrule\n")
                                    append("\\end{tabularx}\n")
                                    append("\\vspace{0.3cm}\n")

                                    // Receipts Details
                                    append("\\section*{Receipts Details}\n")
                                    budget.expenses.withIndex().forEach { (index, expense) ->
                                        selectedExpensesMap[index]?.let { receipts ->
                                            if (receipts.isNotEmpty()) {
                                                append("\\subsection*{${expense.category.replace("&", "\\&").replace("%", "\\%").replace("_", "\\_")}}\n")
                                                append("\\begin{itemize}\n")
                                                receipts.forEach { receipt ->
                                                    append("\\item Description: ${receipt.description.replace("&", "\\&").replace("%", "\\%").replace("_", "\\_")} \n")
                                                    append("(\\SI{${numberFormat.format(receipt.amount)}}{PHP}, " +
                                                            "Date: ${receipt.dateOfTransaction}, " +
                                                            "Category: ${receipt.category.replace("&", "\\&").replace("%", "\\%").replace("_", "\\_")})\n")
                                                }
                                                append("\\end{itemize}\n")
                                            }
                                        }
                                    }

                                    append("\\vspace{0.5cm}\n")
                                    append("\\noindent \\textbf{Note:} This report reflects the financial status as of the generation date. All amounts are in Philippine Peso (PHP).\n")
                                    append("\\end{document}\n")
                                }

                                // Note: In a real app, you'd save the LaTeX content to a .tex file and compile it using a LaTeX engine or service.
                                // For this response, the LaTeX content is provided as the artifact output.
                                // Show confirmation to the user
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Liquidation Report Generated for ${budget.name}",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .padding(start = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = "Generate Report",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Expense selection dialog
    if (showExpenseSelectionDialog && currentExpenseItem != null) {
        AlertDialog(
            onDismissRequest = {
                showExpenseSelectionDialog = false
                currentExpenseItem = null
            },
            title = { Text("Select Receipt for ${currentExpenseItem!!.first.category}") },
            text = {
                if (expenses.isEmpty()) {
                    Text(
                        text = "No transactions recorded yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        items(expenses) { expense ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        // Initialize the list if not present
                                        if (!selectedExpensesMap.containsKey(currentExpenseItem!!.second)) {
                                            selectedExpensesMap[currentExpenseItem!!.second] = mutableListOf()
                                        }
                                        // Add expense to the specific expense item's selected expenses
                                        selectedExpensesMap[currentExpenseItem!!.second]!!.add(expense)
                                        showExpenseSelectionDialog = false
                                        currentExpenseItem = null
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "Selected receipt: ${expense.description}",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = expense.description,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "₱${numberFormat.format(expense.amount)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Category: ${expense.category}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showExpenseSelectionDialog = false
                    currentExpenseItem = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FundRequest(
    modifier: Modifier = Modifier,
    navController: NavController,
    submittedBudgets: MutableList<SubmittedBudget>
) {
    // State management
    var budgetName by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    val expenses = remember { mutableStateListOf<ExpenseItem>() }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog state
    var category by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var amountPerUnit by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }

    // Number formatter for comma-separated amounts
    val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    // Calculate total expenses
    val totalExpenses = expenses.sumOf { it.quantity * it.amountPerUnit }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Request Fund",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Budget Name Input
            OutlinedTextField(
                value = budgetName,
                onValueChange = { budgetName = it },
                label = { Text("Budget Name (Purpose)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp)),
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Expenses List Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Expenses",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = { showDialog = true },
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Expense",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Expenses List with Card Layout
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(expenses) { expense ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = expense.category,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "₱${numberFormat.format(expense.quantity * expense.amountPerUnit)}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Quantity: ${expense.quantity}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Unit Price: ₱${numberFormat.format(expense.amountPerUnit)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (expense.remarks.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Remarks: ${expense.remarks}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Total Expenses
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "Total Expenses: ₱${numberFormat.format(totalExpenses)}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Request Budget Button
            Button(
                onClick = {
                    if (budgetName.isNotBlank() && expenses.isNotEmpty()) {
                        submittedBudgets.add(
                            SubmittedBudget(
                                name = budgetName,
                                expenses = expenses.toList(),
                                total = totalExpenses,
                                status = BudgetStatus.PENDING
                            )
                        )
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Budget '$budgetName' submitted successfully!",
                                duration = SnackbarDuration.Short
                            )
                        }
                        // Clear form after submission
                        budgetName = ""
                        expenses.clear()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(8.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                enabled = budgetName.isNotBlank() && expenses.isNotEmpty()
            ) {
                Text(
                    text = "Request Budget",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        // Add Expense Dialog
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = {
                    Text(
                        text = "Add Expense",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                text = {
                    Column {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Expense Category") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp)),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it.filter { c -> c.isDigit() } },
                            label = { Text("Quantity") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp)),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = amountPerUnit,
                            onValueChange = { amountPerUnit = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Amount per Unit (₱)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp)),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = remarks,
                            onValueChange = { remarks = it },
                            label = { Text("Remarks") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp)),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Total: ₱${numberFormat.format(
                                (quantity.toIntOrNull() ?: 0) * (amountPerUnit.toDoubleOrNull() ?: 0.0))}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (category.isNotBlank() && quantity.isNotBlank() && amountPerUnit.isNotBlank()) {
                                expenses.add(
                                    ExpenseItem(
                                        category = category,
                                        quantity = quantity.toIntOrNull() ?: 0,
                                        amountPerUnit = amountPerUnit.toDoubleOrNull() ?: 0.0,
                                        remarks = remarks
                                    )
                                )
                                category = ""
                                quantity = ""
                                amountPerUnit = ""
                                remarks = ""
                                showDialog = false
                            }
                        },
                        enabled = category.isNotBlank() && quantity.isNotBlank() && amountPerUnit.isNotBlank(),
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDialog = false },
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = "Cancel",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                modifier = Modifier.clip(RoundedCornerShape(16.dp))
            )
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    expenses: List<Expense>,
    onDeleteExpenses: (List<Expense>) -> Unit,
    onLogoutClick: () -> Unit
) {
    var selectedExpenses by remember { mutableStateOf(setOf<Expense>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var selectedExpense by remember { mutableStateOf<Expense?>(null) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var expenseImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showFullScreenImage by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "A",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "User Profile",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "andrew@gmail.com",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    TextButton(
                        onClick = {
                            navController.navigate("reset_password")
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Reset Password",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(
                        onClick = {
                            Toast.makeText(context, "Theme clicked", Toast.LENGTH_SHORT).show()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Theme",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(
                        onClick = {
                            Toast.makeText(context, "About clicked", Toast.LENGTH_SHORT).show()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "About",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = onLogoutClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(
                            text = "Logout",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "List of Expenses",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        navigationIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = false }
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Back to home",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Open navigation drawer",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.primary
                        )
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                modifier = modifier.fillMaxSize()
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    if (expenses.isEmpty()) {
                        Text(
                            text = "No expenses recorded yet.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(expenses) { expense ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedExpense = expense
                                            showExpenseDialog = true
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (expense in selectedExpenses)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        else
                                            MaterialTheme.colorScheme.surface
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = expense in selectedExpenses,
                                            onCheckedChange = { checked ->
                                                selectedExpenses = if (checked) {
                                                    selectedExpenses + expense
                                                } else {
                                                    selectedExpenses - expense
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = expense.description,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "₱${String.format("%.2f", expense.amount)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "Date of Transaction: ${expense.dateOfTransaction}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        expense.photoUri?.let {
                                            AsyncImage(
                                                model = it,
                                                contentDescription = "Expense receipt",
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("Delete Expenses") },
                        text = { Text("Are you sure you want to delete ${selectedExpenses.size} expense(s)?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    onDeleteExpenses(selectedExpenses.toList())
                                    selectedExpenses = emptySet()
                                    showDeleteDialog = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Expenses deleted",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            ) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
                if (showExpenseDialog && selectedExpense != null) {
                    AlertDialog(
                        onDismissRequest = {
                            showExpenseDialog = false
                            selectedExpense = null
                        },
                        title = { Text("${selectedExpense!!.category} Receipt") },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                selectedExpense!!.photoUri?.let { uri ->
                                    val bitmap = try {
                                        BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
                                    } catch (e: Exception) {
                                        null
                                    }
                                    bitmap?.let {
                                        Image(
                                            bitmap = it.asImageBitmap(),
                                            contentDescription = "${selectedExpense!!.category} receipt",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp)
                                                .clip(MaterialTheme.shapes.medium)
                                                .padding(bottom = 8.dp)
                                                .clickable {
                                                    expenseImageBitmap = it
                                                    showFullScreenImage = true
                                                }
                                        )
                                    } ?: Text(
                                        text = "No receipt photo available",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                } ?: Text(
                                    text = "No receipt photo available",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showExpenseDialog = false
                                    selectedExpense = null
                                }
                            ) {
                                Text("Close")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showInfoDialog = true
                                }
                            ) {
                                Text("Info")
                            }
                        }
                    )
                }
                if (showInfoDialog && selectedExpense != null) {
                    AlertDialog(
                        onDismissRequest = {
                            showInfoDialog = false
                        },
                        title = { Text("Expense Details") },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = "Description: ${selectedExpense!!.description}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Amount: ₱${String.format("%.2f", selectedExpense!!.amount)}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Category: ${selectedExpense!!.category}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Date of Transaction: ${selectedExpense!!.dateOfTransaction}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Date Added: ${selectedExpense!!.dateAdded}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                selectedExpense!!.photoUri?.let { uri ->
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Receipt photo",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showInfoDialog = false
                                }
                            ) {
                                Text("Close")
                            }
                        }
                    )
                }
                if (showFullScreenImage) {
                    AlertDialog(
                        onDismissRequest = { showFullScreenImage = false },
                        modifier = Modifier.fillMaxSize(),
                        confirmButton = {
                            TextButton(onClick = { showFullScreenImage = false }) {
                                Text("Close")
                            }
                        },
                        text = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background),
                                contentAlignment = Alignment.Center
                            ) {
                                expenseImageBitmap?.let { bitmap ->
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Full screen receipt photo",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                } ?: selectedExpense?.photoUri?.let { uri ->
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Full screen receipt photo",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                } ?: Text(
                                    text = "No image available",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }
            }
            // Bottom FABs for Deselect and Delete
            if (selectedExpenses.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ExtendedFloatingActionButton(
                        onClick = { selectedExpenses = emptySet() },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Deselect all")
                        Spacer(Modifier.width(8.dp))
                        Text("Deselect All")
                    }
                    ExtendedFloatingActionButton(
                        onClick = { showDeleteDialog = true },
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                        Spacer(Modifier.width(8.dp))
                        Text("Delete (${selectedExpenses.size})")
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun LiquidationReportPreview() {
    ExpenSEEsTheme {
        LiquidationReport(
            submittedBudgets = listOf(
                SubmittedBudget(
                    name = "Project A",
                    expenses = listOf(
                        ExpenseItem("Materials", 10, 50.0, "Wood"),
                        ExpenseItem("Labor", 5, 100.0, "Construction")
                    ),
                    total = 750.0,
                    status = BudgetStatus.APPROVED
                ),
                SubmittedBudget(
                    name = "Project B",
                    expenses = listOf(
                        ExpenseItem("Tools", 3, 200.0, "Rental")
                    ),
                    total = 600.0,
                    status = BudgetStatus.PENDING
                ),
                SubmittedBudget(
                    name = "Project C",
                    expenses = listOf(
                        ExpenseItem("Supplies", 2, 150.0, "Office")
                    ),
                    total = 300.0,
                    status = BudgetStatus.DENIED
                )
            ),
            expenses = listOf( // Add sample expenses
                Expense(
                    description = "Office Supplies",
                    amount = 200.0,
                    category = "Supplies",
                    dateOfTransaction = "2025-06-01",
                    dateAdded = "2025-06-01 10:00:00"
                ),
                Expense(
                    description = "Fuel",
                    amount = 150.0,
                    category = "Transportation",
                    dateOfTransaction = "2025-06-02",
                    dateAdded = "2025-06-02 12:00:00"
                )
            ),
            navController = rememberNavController(),
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FundRequestPreview() {
    ExpenSEEsTheme {
        FundRequest(
            navController = rememberNavController(),
            submittedBudgets = remember { mutableStateListOf<SubmittedBudget>() },
            modifier = Modifier.fillMaxSize()
        )
    }
}


@Preview(showBackground = true)
@Composable
fun LoadingScreenPreview() {
    ExpenSEEsTheme {
        LoadingScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    ExpenSEEsTheme {
        LoginScreen(
            navController = rememberNavController(),
            onLoginClick = { _, _ -> }// No-op lambda for preview
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreviewWithData() {
    ExpenSEEsTheme {
        HomeScreen(
            expenses = listOf(
                Expense("Electricity", 120.50, "Utilities", null, "2025-06-01 10:00:00"),
                Expense("Groceries", 85.25, "Food", null, "2025-06-02 12:30:00")
            ),
            onRecordExpensesClick = {},
            onListExpensesClick = {},
            onLogoutClick = {},
            modifier = TODO(),
            navController = TODO()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RecordExpensesScreenPreview() {
    ExpenSEEsTheme {
        RecordExpensesScreen(
            navController = rememberNavController(),
            expenses = mutableListOf()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ExpenseListScreenPreview() {
    ExpenSEEsTheme {
        ExpenseListScreen(
            navController = NavController(LocalContext.current),
            expenses = listOf(), // Empty list
            onDeleteExpenses = { /* No-op for preview */ },
            onLogoutClick = { /* No-op for preview */ },
            modifier = Modifier
        )
    }
}

