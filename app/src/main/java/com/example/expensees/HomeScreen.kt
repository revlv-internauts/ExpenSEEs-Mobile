package com.example.expensees.screens

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.expensees.ApiConfig
import com.example.expensees.models.Expense
import com.example.expensees.network.AuthRepository
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.SimpleDateFormat
import android.app.DownloadManager
import android.os.Build
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.max

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    expenses: SnapshotStateList<Expense>,
    navController: NavController,
    authRepository: AuthRepository,
    onRecordExpensesClick: () -> Unit = {},
    onListExpensesClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
        isGroupingUsed = true
    }

    val themeColor = Color(0xFF734656)
    var isLoggingOut by remember { mutableStateOf(false) } // New state to track logout

    BackHandler(enabled = true) {
        (context as? Activity)?.moveTaskToBack(true)
    }

    val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    val username by remember { mutableStateOf(prefs.getString("username", "User") ?: "User") }
    val email by remember { mutableStateOf(prefs.getString("email", "user@example.com") ?: "user@example.com") }
    var profileImageUri by remember { mutableStateOf<Uri?>(prefs.getString("profile_image", null)?.let { Uri.parse(it) }) }
    var isLoadingProfilePicture by remember { mutableStateOf(false) }
    var showProfileOptionsDialog by remember { mutableStateOf(false) }
    var showProfilePictureDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (authRepository.isAuthenticated()) {
            isLoadingProfilePicture = true
            val result = authRepository.getProfilePicture()
            isLoadingProfilePicture = false
            if (result.isSuccess) {
                profileImageUri = result.getOrNull()
                prefs.edit().putString("profile_image", profileImageUri?.toString()).apply()
                Log.d("HomeScreen", "Profile picture fetched successfully: $profileImageUri")
            } else {
                val errorMessage = result.exceptionOrNull()?.message
                Log.e("HomeScreen", "Failed to fetch profile picture: $errorMessage")
                if (errorMessage?.contains("Unauthorized") == true || errorMessage?.contains("Invalid token") == true) {
                    Toast.makeText(context, "Authentication error: Please log in again", Toast.LENGTH_SHORT).show()
                    isLoggingOut = true
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                } else if (errorMessage?.contains("Profile picture not found") == true) {
                    profileImageUri = null
                    prefs.edit().remove("profile_image").apply()
                }
            }
        }
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                prefs.edit().putString("profile_image", it.toString()).apply()
                profileImageUri = it
                val result = authRepository.uploadProfilePicture(it)
                if (result.isSuccess) {
                    Toast.makeText(context, "Profile picture uploaded successfully", Toast.LENGTH_SHORT).show()
                    Log.d("HomeScreen", "Profile picture uploaded: $it")
                    isLoadingProfilePicture = true
                    val fetchResult = authRepository.getProfilePicture()
                    isLoadingProfilePicture = false
                    if (fetchResult.isSuccess) {
                        profileImageUri = fetchResult.getOrNull()
                        prefs.edit().putString("profile_image", profileImageUri?.toString()).apply()
                        Log.d("HomeScreen", "Profile picture fetched after upload: $profileImageUri")
                    } else {
                        Log.e("HomeScreen", "Failed to fetch profile picture after upload: ${fetchResult.exceptionOrNull()?.message}")
                    }
                } else {
                    Toast.makeText(context, "Failed to upload profile picture: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    Log.e("HomeScreen", "Profile picture upload failed: ${result.exceptionOrNull()?.message}")
                    if (result.exceptionOrNull()?.message?.contains("Unauthorized") == true) {
                        isLoggingOut = true
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                }
            }
        }
    }

    // Profile picture options dialog
    if (showProfileOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showProfileOptionsDialog = false },
            title = { Text("Profile Picture Options") },
            text = { Text("Would you like to view or change your profile picture?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showProfileOptionsDialog = false
                        showProfilePictureDialog = true
                    }
                ) {
                    Text("View")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showProfileOptionsDialog = false
                        imagePickerLauncher.launch("image/*")
                    }
                ) {
                    Text("Change")
                }
            }
        )
    }

    // Profile picture viewer dialog
    if (showProfilePictureDialog && profileImageUri != null) {
        Dialog(
            onDismissRequest = { showProfilePictureDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(profileImageUri)
                        .build(),
                    contentDescription = "Full-size profile picture",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(0.dp)),
                    contentScale = ContentScale.Fit,
                    onError = {
                        Log.e("HomeScreen", "Failed to load full-size profile image: ${it.result.throwable.message}")
                        Toast.makeText(context, "Failed to load profile image", Toast.LENGTH_SHORT).show()
                        showProfilePictureDialog = false
                    }
                )
                IconButton(
                    onClick = { showProfilePictureDialog = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color(0xFFE5E7EB), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close image",
                        tint = Color(0xFF1F2937)
                    )
                }
            }
        }
    }

    val categories = listOf(
        "Utilities", "Food", "Transportation", "Gas", "Office Supplies",
        "Rent", "Parking", "Electronic Supplies", "Grocery", "Other Expenses"
    )

    val totalExpenses = expenses.sumOf { it.amount.coerceAtLeast(0.0) }
    val categoryTotals = expenses.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount.coerceAtLeast(0.0) } }
        .toList()
        .sortedByDescending { it.second }
        .take(5)
    val chartData = categories.map { category ->
        expenses.filter { it.category == category }.sumOf { it.amount.coerceAtLeast(0.0) }
    }

    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedTransaction by remember { mutableStateOf<Expense?>(null) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var expenseImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showFullScreenImage by remember { mutableStateOf(false) }
    var selectedImagePath by remember { mutableStateOf<String?>(null) }

    var selectedChartCategory by remember { mutableStateOf<String?>(null) }
    var selectedCategoryAmount by remember { mutableStateOf(0.0) }

    var chartType by remember { mutableStateOf("pie") }

    val colorList = listOf(
        Color(0xFF6B4E38), Color(0xFFE7685D), Color(0xFFFBBD92), Color(0xFF4CAF50),
        Color(0xFF2196F3), Color(0xFFFF9800), Color(0xFFE28743), Color(0xFF009688),
        Color(0xFFFF5722), Color(0xFF607D8B)
    )
    val categoryColors = categories.zip(colorList).toMap()

    val animatedScale = remember {
        SnapshotStateList<Animatable<Float, *>>().apply {
            repeat(categoryTotals.size) { add(Animatable(0f)) }
        }
    }
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    LaunchedEffect(categoryTotals) {
        animatedScale.forEachIndexed { index, animatable ->
            launch {
                animatable.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 400,
                        delayMillis = index * 100,
                        easing = FastOutSlowInEasing
                    )
                )
            }
        }
    }

    var token by remember { mutableStateOf<String?>(null) }
    var tokenFetchFailed by remember { mutableStateOf(false) }
    var retryCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        if (retryCount < 2) {
            try {
                val tokenResult = authRepository.getValidToken()
                if (tokenResult.isSuccess) {
                    token = tokenResult.getOrNull()
                    Log.d("HomeScreen", "Initial token fetched: ${token?.take(20)}...")
                } else {
                    retryCount++
                    Log.e("HomeScreen", "Initial token retrieval failed: ${tokenResult.exceptionOrNull()?.message}")
                    tokenFetchFailed = true
                }
            } catch (e: Exception) {
                retryCount++
                Log.e("HomeScreen", "Initial token retrieval exception: ${e.message}")
                tokenFetchFailed = true
            }
        } else {
            tokenFetchFailed = true
            Toast.makeText(context, "Authentication error: Please log in again", Toast.LENGTH_SHORT).show()
            isLoggingOut = true
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        modifier = Modifier.pointerInput(Unit) {
            detectHorizontalDragGestures(
                onHorizontalDrag = { change, dragAmount ->
                    if (drawerState.isOpen && dragAmount < 0) {
                        scope.launch {
                            drawerState.close()
                        }
                        change.consume()
                    } else if (!drawerState.isOpen && dragAmount > 0) {
                        scope.launch {
                            drawerState.open()
                        }
                        change.consume()
                    }
                },
                onDragStart = { offset ->
                    if (!drawerState.isOpen && offset.x > 0) {
                        println("Detected swipe-right start, but handled in onHorizontalDrag")
                    }
                }
            )
        },
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.75f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 50.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable {
                                    if (isLoadingProfilePicture) {
                                        Toast.makeText(context, "Profile picture is still loading", Toast.LENGTH_SHORT).show()
                                    } else {
                                        showProfileOptionsDialog = true
                                    }
                                },
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isLoadingProfilePicture) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color(0xFF734656),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    profileImageUri?.let { uri ->
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(uri)
                                                .build(),
                                            contentDescription = "Profile image",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop,
                                            onError = {
                                                Log.e("HomeScreen", "Failed to load profile image: ${it.result.throwable.message}")
                                                Toast.makeText(context, "Failed to load profile image", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    } ?: Text(
                                        text = (prefs.getString("username", "User")?.firstOrNull()?.uppercase() ?: "U"),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = username,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Lock, contentDescription = "Reset Password Icon") },
                        label = { Text("Reset Password") },
                        selected = false,
                        onClick = {
                            navController.navigate("reset_password")
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Info, contentDescription = "About Icon") },
                        label = { Text("About") },
                        selected = false,
                        onClick = {
                            navController.navigate("about")
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                isLoggingOut = true // Set logout state
                                authRepository.logout() // Assuming this clears auth data
                                // Clear SharedPreferences
                                prefs.edit().clear().apply()
                                // Navigate to login screen
                                navController.navigate("login") {
                                    popUpTo("home") { inclusive = true }
                                }
                                onLogoutClick()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .padding(vertical = 12.dp)
                            .align(Alignment.CenterHorizontally),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(0.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color(0xFF4A2E3F), Color(0xFF6B4E56)),
                                        start = Offset(0f, 0f),
                                        end = Offset(Float.POSITIVE_INFINITY, 0f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFF8A5B6E).copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(vertical = 10.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sign Out",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    ) {
        // Show loading screen during logout to prevent welcome screen flash
        if (isLoggingOut) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = themeColor,
                    modifier = Modifier.size(48.dp)
                )
            }
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5))
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 50.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { scope.launch { drawerState.open() } },
                        modifier = Modifier
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open navigation drawer",
                            tint = Color(0xFF1F2937)
                        )
                    }
                    Text(
                        text = "ExpenSEEs",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp
                        ),
                        color = Color(0xFF1F2937),
                        textAlign = TextAlign.Center
                    )

                    var notificationsViewed by remember {
                        mutableStateOf(prefs.getBoolean("notifications_viewed", false))
                    }
                    val lastNotificationViewTime by remember {
                        mutableStateOf(prefs.getLong("last_notification_view_time", 0L))
                    }

                    LaunchedEffect(expenses) {
                        if (expenses.isNotEmpty()) {
                            val latestExpenseTime = expenses.maxOfOrNull {
                                it.createdAt?.let { createdAt ->
                                    java.time.LocalDateTime.parse(createdAt).toEpochSecond(java.time.ZoneOffset.UTC)
                                } ?: 0L
                            } ?: 0L
                            if (latestExpenseTime > lastNotificationViewTime) {
                                notificationsViewed = false
                                prefs.edit().putBoolean("notifications_viewed", false).apply()
                            }
                        }
                    }

                    LaunchedEffect(notificationsViewed) {
                        prefs.edit().putBoolean("notifications_viewed", notificationsViewed).apply()
                        if (notificationsViewed) {
                            prefs.edit().putLong("last_notification_view_time", System.currentTimeMillis() / 1000).apply()
                        }
                    }
                    IconButton(
                        onClick = {
                            navController.navigate("notifications")
                            notificationsViewed = true
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = Color(0xFF1F2937)
                            )
                            if (expenses.isNotEmpty() && !notificationsViewed) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .align(Alignment.TopEnd)
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(Color(0xFF734656), Color(0xFF8A5B6E)),
                                                start = Offset(0f, 0f),
                                                end = Offset(Float.POSITIVE_INFINITY, 0f)
                                            ),
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }
                }
                if (expenses.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 16.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        color = Color.White,
                        shadowElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Welcome to ExpenSEEs!",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp
                                ),
                                color = Color(0xFF1F2937),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Start tracking your expenses today. Record your first expense to see your spending insights.",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp
                                ),
                                color = Color(0xFF4B5563),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { navController.navigate("record_expenses") },
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent
                                ),
                                contentPadding = PaddingValues(0.dp),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 4.dp,
                                    pressedElevation = 8.dp
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(Color(0xFF734656), Color(0xFF8A5B6E)),
                                                start = Offset(0f, 0f),
                                                end = Offset(Float.POSITIVE_INFINITY, 0f)
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = Color(0xFF8A5B6E).copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Record First Expense",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        color = Color.Transparent
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    try {
                                        loadDataWithBaseURL(
                                            null,
                                            """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js"></script>
                        <style>
                            html, body {
                                margin: 0;
                                padding: 0;
                                background: transparent;
                                width: 100%;
                                height: 100%;
                            }
                            #chartContainer {
                                width: 100%;
                                height: 100%;
                            }
                            #expenseChart {
                                max-width: 100%;
                                max-height: 100%;
                            }
                        </style>
                    </head>
                    <body>
                        <div id="chartContainer">
                            <canvas id="expenseChart"></canvas>
                        </div>
                        <script>
                            const chartData = [${chartData.joinToString { if (it == 0.0) "0.01" else it.toString() }}];
                            const ctx = document.getElementById('expenseChart').getContext('2d');
                            let chart;
                            function renderChart(type) {
                                if (chart) chart.destroy();
                                try {
                                    chart = new Chart(ctx, {
                                        type: type,
                                        data: {
                                            labels: ['${categories.joinToString("','")}'],
                                            datasets: [{
                                                data: chartData,
                                                backgroundColor: [
                                                    '${colorList.joinToString("','") { "#${it.toArgb().toUInt().toString(16).padStart(8, '0').substring(2)}" }}'
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
                                                    color: '#1F2937',
                                                    font: { size: 18, weight: 'bold' },
                                                    align: 'center'
                                                },
                                                tooltip: {
                                                    enabled: true,
                                                    callbacks: {
                                                        label: function(context) {
                                                            const value = context.raw || 0;
                                                            return context.label + ': ₱' + value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
                                                        }
                                                    }
                                                }
                                            },
                                            scales: type === 'bar' ? {
                                                x: {
                                                    ticks: {
                                                        font: { size: 12 },
                                                        color: '#1F2937',
                                                        autoSkip: true,
                                                        maxRotation: 45,
                                                        minRotation: 45
                                                    }
                                                },
                                                y: {
                                                    type: 'logarithmic',
                                                    min: 0.1,
                                                    ticks: {
                                                        callback: function(value) {
                                                            return '₱' + value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
                                                        },
                                                        font: { size: 12 },
                                                        color: '#1F2937',
                                                        autoSkip: true,
                                                        maxTicksLimit: 6
                                                    }
                                                }
                                            } : {},
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
                                } catch (e) {
                                    console.error('Chart creation failed: ' + e.message);
                                }
                            }
                            renderChart('$chartType');
                            window.android.onCategorySelected('', 0);
                        </script>
                    </body>
                    </html>
                    """.trimIndent(),
                                            "text/html",
                                            "UTF-8",
                                            null
                                        )
                                    } catch (e: Exception) {
                                        Toast.makeText(ctx, "Failed to load chart: ${e.message}", Toast.LENGTH_LONG).show()
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
                            update = { webView ->
                                webView.evaluateJavascript("renderChart('$chartType');", null)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .padding(8.dp)
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures { _, dragAmount ->
                                        if (dragAmount < -50f) {
                                            chartType = if (chartType == "pie") "bar" else "pie"
                                        }
                                    }
                                }
                        )
                    }
                    Text(
                        text = if (selectedChartCategory != null) {
                            "${selectedChartCategory} Expenses: ₱${numberFormat.format(selectedCategoryAmount.coerceAtLeast(0.0))}"
                        } else {
                            "Total Expenses: ₱${numberFormat.format(totalExpenses)}"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        ),
                        color = selectedChartCategory?.let { categoryColors[it] } ?: Color(0xFF1F2937),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Text(
                            text = "Your Top 5 Expenses",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            ),
                            color = Color(0xFF1F2937),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            categoryTotals.forEachIndexed { index, (category, amount) ->
                                val scale by animatedScale.getOrNull(index)?.asState() ?: remember { mutableStateOf(1f) }
                                val categoryColor = categoryColors[category] ?: Color(0xFF6B4E38)
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
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
                                                        categoryColor.copy(alpha = 0.1f),
                                                        categoryColor.copy(alpha = 0.03f)
                                                    ),
                                                    start = Offset(0f, 0f),
                                                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                                )
                                            )
                                            .border(
                                                1.dp,
                                                categoryColor.copy(alpha = 0.3f),
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
                                                color = categoryColor,
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "${index + 1}",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = category ?: "Unknown",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 14.sp
                                                    ),
                                                    color = Color(0xFF1F2937),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "₱${numberFormat.format(amount.coerceAtLeast(0.0))}",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontSize = 12.sp
                                                    ),
                                                    color = Color(0xFF4B5563),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = if (totalExpenses > 0) {
                                                    "${String.format("%.2f", (amount / totalExpenses) * 100)}%"
                                                } else {
                                                    "0.00%"
                                                },
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                ),
                                                color = categoryColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    NavigationButton(
                        icon = Icons.Default.Add,
                        label = "Record",
                        onClick = onListExpensesClick,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    NavigationButton(
                        icon = Icons.Default.RequestQuote,
                        label = "Request",
                        onClick = { navController.navigate("requested_budgets") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    NavigationButton(
                        icon = Icons.Default.Assignment,
                        label = "Report",
                        onClick = { navController.navigate("liquidation_reports") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (selectedCategory != null) {
                Dialog(
                    onDismissRequest = { selectedCategory = null },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    val dialogAlpha by animateFloatAsState(
                        targetValue = if (selectedCategory != null) 1f else 0f,
                        animationSpec = tween(300, easing = LinearOutSlowInEasing)
                    )
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .fillMaxHeight(0.95f)
                            .clip(RoundedCornerShape(16.dp))
                            .alpha(dialogAlpha),
                        color = Color(0xFFF5F5F5),
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${selectedCategory} Transactions",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 20.sp
                                    ),
                                    color = Color(0xFF1F2937)
                                )
                                IconButton(
                                    onClick = { selectedCategory = null },
                                    modifier = Modifier
                                        .size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close dialog",
                                        tint = Color(0xFF4B5563)
                                    )
                                }
                            }
                            val transactionsForCategory = expenses.filter { it.category == selectedCategory }
                            var selectedMonth by remember { mutableStateOf<String?>(null) }
                            val currentYear = LocalDate.now().year
                            val monthOptions = remember {
                                listOf(
                                    "All Months",
                                    "January $currentYear",
                                    "February $currentYear",
                                    "March $currentYear",
                                    "April $currentYear",
                                    "May $currentYear",
                                    "June $currentYear",
                                    "July $currentYear",
                                    "August $currentYear",
                                    "September $currentYear",
                                    "October $currentYear",
                                    "November $currentYear",
                                    "December $currentYear"
                                )
                            }
                            var expanded by remember { mutableStateOf(false) }

                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                TextField(
                                    value = selectedMonth ?: "All Months",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Select Month") },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = Color(0xFF734656)
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White,
                                        focusedIndicatorColor = Color(0xFF734656),
                                        unfocusedIndicatorColor = Color(0xFF734656).copy(alpha = 0.5f),
                                        focusedLabelColor = Color(0xFF734656),
                                        unfocusedLabelColor = Color(0xFF734656).copy(alpha = 0.5f),
                                        cursorColor = Color(0xFF734656)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.background(Color.White)
                                ) {
                                    monthOptions.forEach { month ->
                                        DropdownMenuItem(
                                            text = { Text(month, style = MaterialTheme.typography.bodyMedium) },
                                            onClick = {
                                                selectedMonth = if (month == "All Months") null else month
                                                expanded = false
                                            },
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }

                            if (showFullScreenImage) {
                                // ... (Existing full-screen image dialog code remains unchanged)
                            }

                            val filteredTransactions = if (selectedMonth != null) {
                                transactionsForCategory.filter { expense ->
                                    expense.createdAt?.let {
                                        try {
                                            val dateTime = LocalDateTime.parse(it)
                                            val date = dateTime.toLocalDate()
                                            date.format(DateTimeFormatter.ofPattern("MMMM yyyy")) == selectedMonth
                                        } catch (e: Exception) {
                                            Log.e("HomeScreen", "Failed to parse createdAt date: $it, error: ${e.message}")
                                            false
                                        }
                                    } ?: false
                                }
                            } else {
                                transactionsForCategory
                            }

                            if (filteredTransactions.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No transactions recorded for this period.",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                        color = Color(0xFF4B5563)
                                    )
                                }
                            } else {
                                val baseColor = categoryColors[selectedCategory]!!
                                val transactionColors = filteredTransactions.indices.map { index ->
                                    val factor = 0.8f - (index * 0.1f).coerceAtMost(0.5f)
                                    "#${baseColor.copy(alpha = factor).toArgb().toUInt().toString(16).padStart(8, '0').substring(2)}"
                                }
                                val validTransactions = filteredTransactions.filter { it.amount > 0 }
                                val transactionData = validTransactions.map { it.amount }
                                val transactionLabels = validTransactions.mapIndexed { index, expense ->
                                    "'${(expense.remarks ?: "").replace("'", "\\'")}'"
                                }

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(
                                            2.dp,
                                            Color(0xFF734656).copy(alpha = 0.3f),
                                            RoundedCornerShape(16.dp)
                                        ),
                                    color = Color.White,
                                    shadowElevation = 4.dp
                                ) {
                                    AndroidView(
                                        factory = { ctx ->
                                            WebView(ctx).apply {
                                                settings.javaScriptEnabled = true
                                                setBackgroundColor(Color.Transparent.toArgb())
                                                layoutParams = ViewGroup.LayoutParams(
                                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                                    ViewGroup.LayoutParams.MATCH_PARENT
                                                )
                                            }
                                        },
                                        update = { webView ->
                                            try {
                                                webView.loadDataWithBaseURL(
                                                    null,
                                                    """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js"></script>
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
                                const transactionData = [${transactionData.joinToString { it.toString() }}];
                                const ctx = document.getElementById('transactionChart').getContext('2d');
                                const chart = new Chart(ctx, {
                                    type: 'bar',
                                    data: {
                                        labels: [${transactionLabels.joinToString()}],
                                        datasets: [{
                                            data: transactionData,
                                            backgroundColor: [${transactionColors.joinToString() { "'$it'" }}],
                                            borderColor: '#D1D5DB',
                                            borderWidth: 1
                                        }]
                                    },
                                    options: {
                                        responsive: true,
                                        maintainAspectRatio: false,
                                        plugins: {
                                            legend: { display: false },
                                            title: {
                                                display: true,
                                                text: 'Transaction Amounts',
                                                color: '#1F2937',
                                                font: { size: 16, weight: 'bold' },
                                                align: 'center'
                                            },
                                            tooltip: {
                                                enabled: true,
                                                callbacks: {
                                                    label: function(context) {
                                                        const value = context.raw || 0;
                                                        return '₱' + value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
                                                    }
                                                }
                                            }
                                        },
                                        scales: {
                                            x: {
                                                ticks: { display: false },
                                                barPercentage: 0.8,
                                                categoryPercentage: 0.9
                                            },
                                            y: {
                                                type: 'logarithmic',
                                                min: 0.1,
                                                ticks: {
                                                    callback: function(value) {
                                                        return '₱' + value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
                                                    },
                                                    font: { size: 12 },
                                                    color: '#1F2937',
                                                    autoSkip: true,
                                                    maxTicksLimit: 6
                                                }
                                            }
                                        },
                                        animation: {
                                            duration: 800,
                                            easing: 'easeOutCubic'
                                        },
                                        hover: {
                                            mode: 'nearest',
                                            intersect: true,
                                            animationDuration: 400
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
                                                Toast.makeText(context, "Failed to load transaction chart", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Text(
                                    text = "Transaction Details",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 18.sp
                                    ),
                                    color = Color(0xFF1F2937),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    itemsIndexed(filteredTransactions) { index, expense ->
                                        val alpha by animateFloatAsState(
                                            targetValue = if (selectedCategory != null) 1f else 0f,
                                            animationSpec = tween(
                                                durationMillis = 200,
                                                delayMillis = index * 50,
                                                easing = LinearOutSlowInEasing
                                            )
                                        )
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .alpha(alpha)
                                                .clickable {
                                                    selectedTransaction = expense
                                                    selectedImagePath = expense.imagePaths?.firstOrNull()
                                                    showExpenseDialog = true
                                                },
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFF5F5F5),
                                            shadowElevation = 2.dp
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = expense.remarks ?: "",
                                                        style = MaterialTheme.typography.bodyLarge.copy(
                                                            fontWeight = FontWeight.Medium,
                                                            fontSize = 14.sp
                                                        ),
                                                        color = Color(0xFF1F2937),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = expense.createdAt?.let {
                                                            try {
                                                                val dateTime = LocalDateTime.parse(it)
                                                                dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                                                            } catch (e: Exception) {
                                                                ""
                                                            }
                                                        } ?: "",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFF4B5563),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Text(
                                                    text = "₱${numberFormat.format(expense.amount.coerceAtLeast(0.0))}",
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 14.sp
                                                    ),
                                                    color = categoryColors[expense.category] ?: Color(0xFF6B4E38),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { selectedCategory = null },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = List(2) { Color(0xFF734656) },
                                                start = Offset(0f, 0f),
                                                end = Offset(Float.POSITIVE_INFINITY, 0f)
                                            )
                                        )
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Close",
                                        fontSize = 16.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (showExpenseDialog && selectedTransaction != null) {
                AlertDialog(
                    onDismissRequest = {
                        showExpenseDialog = false
                        selectedTransaction = null
                        expenseImageBitmap = null
                        selectedImagePath = null
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .clip(RoundedCornerShape(16.dp)),
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(
                        color = Color(0xFFF5F5F5),
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${selectedTransaction?.category ?: "Receipt"}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 20.sp
                                ),
                                color = Color(0xFF734656),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            selectedImagePath?.let { imagePath ->
                                var imageLoadFailed by remember { mutableStateOf(false) }
                                if (tokenFetchFailed) {
                                    Text(
                                        text = "Authentication error: Please log in again",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF4B5563),
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                } else if (selectedTransaction?.expenseId?.startsWith("local_") == true) {
                                    val bitmap = try {
                                        val uri = Uri.parse(imagePath)
                                        BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
                                    } catch (e: Exception) {
                                        Log.e("HomeScreen", "Failed to load local image: $imagePath, error: ${e.message}")
                                        null
                                    }
                                    bitmap?.let {
                                        Image(
                                            bitmap = it.asImageBitmap(),
                                            contentDescription = "${selectedTransaction?.category ?: "Receipt"} receipt",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(240.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(
                                                    1.5.dp,
                                                    Color(0xFF734656),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    expenseImageBitmap = it
                                                    showFullScreenImage = true
                                                },
                                            contentScale = ContentScale.Crop
                                        )
                                    } ?: run {
                                        imageLoadFailed = true
                                        Text(
                                            text = "No receipt photo available",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF4B5563),
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )
                                    }
                                } else {
                                    val fullImageUrl = "${ApiConfig.BASE_URL}api/expenses/${selectedTransaction?.expenseId}/images"
                                    Log.d("HomeScreen", "Loading server image: $fullImageUrl with token: ${token?.take(20)}...")
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(fullImageUrl)
                                            .apply {
                                                if (token != null) {
                                                    addHeader("Authorization", "Bearer $token")
                                                } else {
                                                    imageLoadFailed = true
                                                }
                                            }
                                            .diskCacheKey(fullImageUrl)
                                            .memoryCacheKey(fullImageUrl)
                                            .build(),
                                        contentDescription = "${selectedTransaction?.category ?: "Receipt"} receipt",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(240.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(
                                                1.5.dp,
                                                Color(0xFF734656),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { showFullScreenImage = true },
                                        contentScale = ContentScale.Crop,
                                        onError = { error ->
                                            imageLoadFailed = true
                                            scope.launch {
                                                Log.e("HomeScreen", "Failed to load server image: $fullImageUrl, error: ${error.result.throwable.message}")
                                                Toast.makeText(context, "Failed to load receipt image: ${error.result.throwable.message}", Toast.LENGTH_LONG).show()
                                                if (error.result.throwable.message?.contains("401") == true && retryCount < 2) {
                                                    retryCount++
                                                    val tokenResult = authRepository.getValidToken()
                                                    if (tokenResult.isSuccess) {
                                                        token = tokenResult.getOrNull()
                                                        Log.d("HomeScreen", "Retry token fetched: ${token?.take(20)}...")
                                                    } else {
                                                        tokenFetchFailed = true
                                                        isLoggingOut = true
                                                        navController.navigate("login") {
                                                            popUpTo("home") { inclusive = true }
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        onSuccess = {
                                            Log.d("HomeScreen", "Successfully loaded server image: $fullImageUrl")
                                        }
                                    )
                                }
                            } ?: Text(
                                text = "No receipt photo available",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF4B5563),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = { showInfoDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .padding(horizontal = 4.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 4.dp,
                                        pressedElevation = 8.dp
                                    ),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(Color(0xFF734656), Color(0xFF8A5B6E)),
                                                    start = Offset(0f, 0f),
                                                    end = Offset(Float.POSITIVE_INFINITY, 0f)
                                                ),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = Color(0xFF8A5B6E).copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(10.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Info",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        selectedImagePath?.let { imagePath ->
                                            scope.launch {
                                                try {
                                                    if (selectedTransaction?.expenseId?.startsWith("local_") == true) {
                                                        val uri = Uri.parse(imagePath)
                                                        val inputStream = context.contentResolver.openInputStream(uri)
                                                        val fileName = "Expense_${selectedTransaction?.expenseId}_${System.currentTimeMillis()}.jpg"
                                                        val outputFile = File(
                                                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                                            fileName
                                                        )
                                                        inputStream?.use { input ->
                                                            FileOutputStream(outputFile).use { output ->
                                                                input.copyTo(output)
                                                            }
                                                        }
                                                        Toast.makeText(context, "Image saved to Downloads: $fileName", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        val fullImageUrl = "${ApiConfig.BASE_URL}api/expenses/${selectedTransaction?.expenseId}/images"
                                                        val fileName = "Expense_${selectedTransaction?.expenseId}_${System.currentTimeMillis()}.jpg"
                                                        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                                        val request = DownloadManager.Request(Uri.parse(fullImageUrl)).apply {
                                                            if (token != null) {
                                                                addRequestHeader("Authorization", "Bearer $token")
                                                            }
                                                            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                                                            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                                            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                                                        }
                                                        downloadManager.enqueue(request)
                                                        Toast.makeText(context, "Downloading image to Downloads: $fileName", Toast.LENGTH_SHORT).show()
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e("HomeScreen", "Failed to download image: ${e.message}")
                                                    Toast.makeText(context, "Failed to download image: ${e.message}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        } ?: Toast.makeText(context, "No image available to download", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .padding(horizontal = 4.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 4.dp,
                                        pressedElevation = 8.dp
                                    ),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(Color(0xFF734656), Color(0xFF8A5B6E)),
                                                    start = Offset(0f, 0f),
                                                    end = Offset(Float.POSITIVE_INFINITY, 0f)
                                                ),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = Color(0xFF8A5B6E).copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(10.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Download",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        showExpenseDialog = false
                                        selectedTransaction = null
                                        expenseImageBitmap = null
                                        selectedImagePath = null
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .padding(horizontal = 4.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 4.dp,
                                        pressedElevation = 8.dp
                                    ),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(Color(0xFF734656), Color(0xFF8A5B6E)),
                                                    start = Offset(0f, 0f),
                                                    end = Offset(Float.POSITIVE_INFINITY, 0f)
                                                ),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = Color(0xFF8A5B6E).copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(10.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Close",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (showFullScreenImage) {
                AlertDialog(
                    onDismissRequest = {
                        showFullScreenImage = false
                        selectedTransaction = null
                        expenseImageBitmap = null
                        selectedImagePath = null
                    },
                    modifier = Modifier.fillMaxSize(),
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false,
                        decorFitsSystemWindows = false
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        selectedImagePath?.let { imagePath ->
                            var imageLoadFailed by remember { mutableStateOf(false) }
                            var scale by remember { mutableStateOf(1f) }
                            var offset by remember { mutableStateOf(Offset.Zero) }
                            var imageSize by remember { mutableStateOf(IntSize.Zero) }
                            var containerSize by remember { mutableStateOf(IntSize.Zero) }

                            val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
                                val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                                val scaledWidth = imageSize.width * newScale
                                val scaledHeight = imageSize.height * newScale
                                val maxX = maxOf(0f, (scaledWidth - containerSize.width) / 2f)
                                val maxY = maxOf(0f, (scaledHeight - containerSize.height) / 2f)
                                val newOffsetX = (offset.x + offsetChange.x).coerceIn(-maxX, maxX)
                                val newOffsetY = (offset.y + offsetChange.y).coerceIn(-maxY, maxY)
                                if (newScale != scale || newOffsetX != offset.x || newOffsetY != offset.y) {
                                    scale = newScale
                                    offset = Offset(newOffsetX, newOffsetY)
                                }
                            }

                            if (tokenFetchFailed) {
                                Text(
                                    text = "Authentication error: Please log in again",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                    color = Color.White,
                                    modifier = Modifier.padding(16.dp)
                                )
                            } else if (selectedTransaction?.expenseId?.startsWith("local_") == true) {
                                val bitmap = try {
                                    val uri = Uri.parse(imagePath)
                                    BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
                                } catch (e: Exception) {
                                    Log.e("HomeScreen", "Failed to load local full-screen image: $imagePath, error: ${e.message}")
                                    null
                                }
                                bitmap?.let {
                                    Image(
                                        bitmap = it.asImageBitmap(),
                                        contentDescription = "Full screen expense photo",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .onGloballyPositioned { coordinates ->
                                                imageSize = coordinates.size
                                                containerSize = coordinates.parentLayoutCoordinates?.size ?: IntSize.Zero
                                            }
                                            .transformable(state = transformableState)
                                            .graphicsLayer(
                                                scaleX = scale,
                                                scaleY = scale,
                                                translationX = offset.x,
                                                translationY = offset.y
                                            )
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(
                                                2.dp,
                                                categoryColors[selectedTransaction?.category] ?: Color(0xFF6B4E38),
                                                RoundedCornerShape(12.dp)
                                            ),
                                        contentScale = ContentScale.Fit
                                    )
                                } ?: run {
                                    imageLoadFailed = true
                                    Text(
                                        text = "No image available",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                        color = Color.White,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            } else {
                                val fullImageUrl = "${ApiConfig.BASE_URL}api/expenses/${selectedTransaction?.expenseId}/images"
                                Log.d("HomeScreen", "Loading full-screen server image: $fullImageUrl with token: ${token?.take(20)}...")
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(fullImageUrl)
                                        .apply {
                                            if (token != null) {
                                                addHeader("Authorization", "Bearer $token")
                                            } else {
                                                imageLoadFailed = true
                                            }
                                        }
                                        .diskCacheKey(fullImageUrl)
                                        .memoryCacheKey(fullImageUrl)
                                        .build(),
                                    contentDescription = "Full screen expense photo",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .onGloballyPositioned { coordinates ->
                                            imageSize = coordinates.size
                                            containerSize = coordinates.parentLayoutCoordinates?.size ?: IntSize.Zero
                                        }
                                        .transformable(state = transformableState)
                                        .graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale,
                                            translationX = offset.x,
                                            translationY = offset.y
                                        )
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            2.dp,
                                            categoryColors[selectedTransaction?.category] ?: Color(0xFF6B4E38),
                                            RoundedCornerShape(12.dp)
                                        ),
                                    contentScale = ContentScale.Fit,
                                    onError = { error ->
                                        imageLoadFailed = true
                                        scope.launch {
                                            Log.e("HomeScreen", "Failed to load full-screen server image: $fullImageUrl, error: ${error.result.throwable.message}")
                                            Toast.makeText(context, "Failed to load full screen image: ${error.result.throwable.message}", Toast.LENGTH_LONG).show()
                                            if (error.result.throwable.message?.contains("401") == true && retryCount < 2) {
                                                retryCount++
                                                val tokenResult = authRepository.getValidToken()
                                                if (tokenResult.isSuccess) {
                                                    token = tokenResult.getOrNull()
                                                    Log.d("HomeScreen", "Retry token fetched: ${token?.take(20)}...")
                                                } else {
                                                    tokenFetchFailed = true
                                                    isLoggingOut = true
                                                    navController.navigate("login") {
                                                        popUpTo("home") { inclusive = true }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    onSuccess = {
                                        Log.d("HomeScreen", "Successfully loaded full-screen server image: $fullImageUrl")
                                    }
                                )
                            }
                        } ?: Text(
                            text = "No image available",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = Color.White,
                            modifier = Modifier.padding(16.dp)
                        )
                        IconButton(
                            onClick = {
                                showFullScreenImage = false
                                selectedTransaction = null
                                expenseImageBitmap = null
                                selectedImagePath = null
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .background(Color(0xFFE5E7EB), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close image",
                                tint = Color(0xFF1F2937)
                            )
                        }
                    }
                }
            }
            if (showInfoDialog && selectedTransaction != null) {
                Dialog(
                    onDismissRequest = {
                        showInfoDialog = false
                    },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .clip(RoundedCornerShape(16.dp)),
                        color = Color(0xFFF5F5F5),
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Expense Details",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = Color(0xFF1F2937),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                            append("Category: ")
                                        }
                                        append("${selectedTransaction?.category ?: "N/A"}")
                                    },
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                    color = Color(0xFF1F2937),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                            append("Amount: ")
                                        }
                                        append("₱${numberFormat.format(selectedTransaction?.amount?.coerceAtLeast(0.0) ?: 0.0)}")
                                    },
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                    color = Color(0xFF1F2937),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                            append("Date of Transaction: ")
                                        }
                                        append("${selectedTransaction?.dateOfTransaction ?: "N/A"}")
                                    },
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                    color = Color(0xFF1F2937),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                            append("Created At: ")
                                        }
                                        append(selectedTransaction?.createdAt?.let {
                                            val dateTime = java.time.LocalDateTime.parse(it)
                                            dateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                                        } ?: "N/A")
                                    },
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                    color = Color(0xFF1F2937),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                            append("Remarks: ")
                                        }
                                        append("${selectedTransaction?.remarks ?: "N/A"}")
                                    },
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                    color = Color(0xFF1F2937),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    showInfoDialog = false
                                },
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .height(40.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(Color(0xFFE5E7EB), Color(0xFFD1D5DB)),
                                                start = Offset(0f, 0f),
                                                end = Offset(Float.POSITIVE_INFINITY, 0f)
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Close",
                                        color = themeColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NavigationButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
    )
    Surface(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .scale(scale),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        shadowElevation = 4.dp,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF734656), Color(0xFF8A5B6E)),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, 0f)
                    )
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(28.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun DrawerItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}