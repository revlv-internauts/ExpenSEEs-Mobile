package com.example.expensees

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
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import android.graphics.drawable.Drawable
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target



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

    NavHost(navController = navController, startDestination = "loading") {
        composable("loading") {
            LoadingScreen(
                onLoadingComplete = {
                    navController.navigate("login") {
                        popUpTo("loading") { inclusive = true }
                    }
                },
                modifier = modifier
            )
        }
        composable("login") {
            LoginScreen(
                onLoginClick = { email, password ->
                    if (email == "andrew@gmail.com" && password == "123") {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                },
                modifier = modifier
            )
        }
        composable("home") {
            HomeScreen(
                expenses = expenses,
                onRecordExpensesClick = { navController.navigate("record_expenses") },
                onListExpensesClick = { navController.navigate("list_expenses") },
                onLogoutClick = { navController.navigate("login") { popUpTo("login") { inclusive = true } } },
                modifier = modifier
            )
        }
        composable("record_expenses") {
            RecordExpensesScreen(
                navController = navController,
                expenses = expenses,
                onLogoutClick = { navController.navigate("login") { popUpTo("login") { inclusive = true } } },
                modifier = modifier
            )
        }
        composable("list_expenses") {
            ExpenseListScreen(
                navController = navController,
                expenses = expenses,
                onLogoutClick = { navController.navigate("login") { popUpTo("login") { inclusive = true } } },
                modifier = modifier
            )
        }
    }
}

@Composable
fun LoadingScreen(modifier: Modifier = Modifier, onLoadingComplete: () -> Unit = {}) {
    LaunchedEffect(Unit) {
        delay(3000L)
        onLoadingComplete()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ExpenSEEs",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
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
                .alpha(if (isLoginComplete) 0.5f else 1f) // Fade button after login
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
                                delay(1500L) // Simulate processing
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
            // No content shown after login completes
        }

        TextButton(
            onClick = { /* Handle forgot password */ },
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

    // Date picker for Date of Transaction
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

    // Camera and gallery launchers
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

    // Permission launcher for multiple permissions
    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when (permissionType) {
            "camera" -> {
                if (permissions[android.Manifest.permission.CAMERA] == true) {
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
                    android.Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                }
                if (permissions[storagePermission] == true) {
                    pickImageLauncher.launch("image/*")
                } else {
                    Toast.makeText(context, "Storage permission denied", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Permission rationale dialog
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
                            "camera" -> android.Manifest.permission.CAMERA
                            "storage" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                android.Manifest.permission.READ_MEDIA_IMAGES
                            } else {
                                android.Manifest.permission.READ_EXTERNAL_STORAGE
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

    // Expense details dialog
    if (showExpenseDialog && selectedExpense != null) {
        AlertDialog(
            onDismissRequest = {
                showExpenseDialog = false
                selectedExpense = null
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
                        text = "Category: ${selectedExpense!!.category}",
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
                    selectedExpense!!.photoUri?.let { uri ->
                        val bitmap = try {
                            BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
                        } catch (e: Exception) {
                            null
                        }
                        bitmap?.let {
                            Text(
                                text = "Receipt Photo:",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Expense receipt",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .padding(bottom = 8.dp)
                                    .clickable {
                                        selectedImageBitmap = it
                                        showFullScreenImage = true
                                    },
                            )
                        }
                    }
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
            dismissButton = {}
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
                    selectedImageBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Full screen expense photo",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentScale = ContentScale.Fit
                        )
                    } ?: selectedImageUri?.let { uri ->
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
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    TextButton(
                        onClick = {
                            Toast.makeText(context, "Change Password clicked", Toast.LENGTH_SHORT).show()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Change Password",
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

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Remarks") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

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

            // Date of Transaction Field with Fixed Icon
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
                            context.checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                        ) {
                            if (ActivityCompat.shouldShowRequestPermissionRationale(
                                    context as Activity,
                                    android.Manifest.permission.CAMERA
                                )
                            ) {
                                showPermissionRationale = true
                            } else {
                                multiplePermissionsLauncher.launch(arrayOf(android.Manifest.permission.CAMERA))
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
                            android.Manifest.permission.READ_MEDIA_IMAGES
                        } else {
                            android.Manifest.permission.READ_EXTERNAL_STORAGE
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
                        .clickable { showFullScreenImage = true }
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

    // Calculate category totals and top 5
    val totalExpenses = expenses.sumOf { it.amount }
    val categoryTotals = expenses.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }
        .take(5)
    val chartData = categories.map { category ->
        expenses.filter { it.category == category }.sumOf { it.amount }
    }

    // State for dragging the top 5 categories list
    val offsetY = remember { mutableStateOf(0f) }
    val maxDragHeight = with(LocalDensity.current) { 100.dp.toPx() }

    // State for showing transactions dialog
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedTransaction by remember { mutableStateOf<Expense?>(null) }
    val transactionsForCategory = expenses.filter { it.category == selectedCategory }

    // State for full-screen image dialog
    var showFullScreenImage by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

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
                            Toast.makeText(context, "Change Password clicked", Toast.LENGTH_SHORT).show()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Change Password",
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Main content (scrollable)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
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
                Spacer(modifier = Modifier.height(32.dp))
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
                    // Pie Chart Section
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
                            const totalExpenses = chartData.reduce((sum, value) => sum + value, 0);
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
                                            const value = chart.data.datasets[0].data[index];
                                            const percentage = totalExpenses > 0 ? ((value / totalExpenses) * 100).toFixed(2) : 0;
                                            chart.options.plugins.title.text = label + ': ' + percentage + '% of total expenses';
                                            chart.update();
                                        } else {
                                            chart.options.plugins.title.text = 'Expense Distribution by Category';
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
                                    Toast.makeText(ctx, "Failed to load chart", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .padding(vertical = 8.dp)
                    )
                    // Total Expenses Display
                    Text(
                        text = "Total Expenses: ₱${String.format("%.2f", totalExpenses)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    // Top 5 Categories Section
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Your Top 5 Expenses",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .offset { IntOffset(0, offsetY.value.roundToInt()) }
                                    .pointerInput(Unit) {
                                        detectDragGestures { _, dragAmount ->
                                            val newOffset = offsetY.value + dragAmount.y
                                            offsetY.value = newOffset.coerceIn(-maxDragHeight, 0f)
                                        }
                                    }
                            ) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    itemsIndexed(categoryTotals) { index, (category, amount) ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp, horizontal = 8.dp)
                                                .clickable { selectedCategory = category },
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                            ),
                                            elevation = CardDefaults.cardElevation(
                                                defaultElevation = 2.dp
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "${index + 1}. $category",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = if (totalExpenses > 0) {
                                                        "${String.format("%.2f", (amount / totalExpenses) * 100)}%"
                                                    } else {
                                                        "0.00%"
                                                    },
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
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
            // Buttons at the bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
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
        }
        // Transactions Dialog
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
                            // Pie Chart for Transactions
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
                            // Transaction List with Date
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
                                            .clickable { selectedTransaction = expense },
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
        // Transaction Details Dialog
        if (selectedTransaction != null) {
            AlertDialog(
                onDismissRequest = { selectedTransaction = null },
                title = {
                    Text(
                        text = "Transaction Details",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Category: ${selectedTransaction?.category ?: "N/A"}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Description: ${selectedTransaction?.description ?: "N/A"}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Amount: ₱${String.format("%.2f", selectedTransaction?.amount ?: 0.0)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Purchase Date: ${selectedTransaction?.dateOfTransaction ?: "N/A"}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Date Added: ${selectedTransaction?.dateAdded ?: "N/A"}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        selectedTransaction?.photoUri?.let { uri ->
                            Text(
                                text = "Receipt Photo:",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            AsyncImage(
                                model = uri,
                                contentDescription = "Receipt photo",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedImageUri = uri
                                        showFullScreenImage = true
                                    },
                                contentScale = ContentScale.Fit,
                                onError = {
                                    Toast.makeText(context, "Failed to load receipt photo", Toast.LENGTH_SHORT).show()
                                }
                            )
                        } ?: Text(
                            text = "No photo available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedTransaction = null }) {
                        Text("Close")
                    }
                }
            )
        }
        // Full-screen image dialog
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
                        selectedImageUri?.let { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = "Full screen receipt photo",
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



@Composable
fun ExpenseListScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    expenses: List<Expense>,
    onLogoutClick: () -> Unit = {}
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedExpense by remember { mutableStateOf<Expense?>(null) }
    var showFullScreenImage by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

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
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    TextButton(
                        onClick = {
                            Toast.makeText(context, "Change Password clicked", Toast.LENGTH_SHORT).show()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Change Password",
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
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "Your Expenses",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        ) {
                            items(expenses) { expense ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { selectedExpense = expense },
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
                    text = "List of Expenses",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (expenses.isEmpty()) {
                Text(
                    text = "No expenses recorded.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(expenses) { expense ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedExpense = expense },
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

            selectedExpense?.let { expense ->
                AlertDialog(
                    onDismissRequest = { selectedExpense = null },
                    title = {
                        Text(
                            text = "Expense Details",
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            expense.photoUri?.let { uri ->
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Expense photo",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .padding(bottom = 16.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedImageUri = uri
                                            showFullScreenImage = true
                                        },
                                    contentScale = ContentScale.Fit,
                                    onError = {
                                        Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            } ?: Text(
                                text = "No photo available",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Text(
                                text = "Description: ${expense.description}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Category: ${expense.category}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Amount: ₱${String.format("%.2f", expense.amount)}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Purchase Date: ${expense.dateOfTransaction ?: "N/A"}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Date Added: ${expense.dateAdded ?: "N/A"}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { selectedExpense = null }) {
                            Text("Close")
                        }
                    }
                )
            }

            // Full-screen image dialog
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
                            selectedImageUri?.let { uri ->
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
        LoginScreen()
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
            onLogoutClick = {}
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
            navController = rememberNavController(),
            expenses = listOf(),
            onLogoutClick = {},
            modifier = Modifier
        )
    }
}