package com.example.expensees

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.expensees.ui.theme.ExpenSEEsTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import android.os.Build
import android.os.Environment
import android.webkit.WebView
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import java.io.IOException

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

fun createImageUri(context: Context): Uri? {
    return try {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: throw IOException("External files directory not available")
        val imageFile = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    } catch (e: Exception) {
        e.printStackTrace()
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
fun LoginScreen(modifier: Modifier = Modifier, onLoginClick: (String, String) -> Unit = { _, _ -> }) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

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

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    errorMessage = "Please enter email and password"
                } else {
                    coroutineScope.launch {
                        onLoginClick(email, password)
                        if (email != "andrew@gmail.com" || password != "123") {
                            errorMessage = "Invalid email or password"
                            Toast.makeText(context, "Login failed", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()
                        }
                    }
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
                text = "Login",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onPrimary
            )
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
    val date: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
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
    var expanded by remember { mutableStateOf(false) }
    var selectedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var permissionType by remember { mutableStateOf("") }
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

    // Camera and gallery launchers
    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && selectedImageUri != null) {
            selectedImageBitmap = BitmapFactory.decodeStream(
                context.contentResolver.openInputStream(selectedImageUri!!)
            )
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            selectedImageBitmap = BitmapFactory.decodeStream(
                context.contentResolver.openInputStream(it)
            )
        }
    }

    // Permission launcher for multiple permissions
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
                        modifier = Modifier.fillMaxSize()

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
                    .padding(bottom = 16.dp)
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
                )
            }

            Button(
                onClick = {
                    if (description.isNotBlank() && amount.isNotBlank() && category.isNotBlank()) {
                        val amountValue = amount.toDoubleOrNull()
                        if (amountValue != null) {
                            expenses.add(Expense(description, amountValue, category, selectedImageUri))
                            description = ""
                            amount = ""
                            category = ""
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
                items(expenses) { expense ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
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
                                text = "$${String.format("%.2f", expense.amount)}",
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
    onRecordExpensesClick: () -> Unit = {},
    onListExpensesClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val quotes = listOf(
        "Save money, and money will save you.",
        "A penny saved is a penny earned.",
        "Budgeting is the key to financial freedom.",
        "Track your expenses to build your wealth."
    )
    val randomQuote by remember { mutableStateOf(quotes[Random.nextInt(quotes.size)]) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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
                }
            }
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
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
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "Welcome to ExpenSEEs!",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                Text(
                    text = randomQuote,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onListExpensesClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp),
                    shape = RectangleShape,
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
                        .height(60.dp),
                    shape = RectangleShape,
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
    var selectedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
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
    val categoryTotals = expenses.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }
        .take(5)
    val chartData = categories.map { category ->
        expenses.filter { it.category == category }.sumOf { it.amount }
    }

    LaunchedEffect(selectedExpense) {
        selectedExpense?.photoUri?.let { uri ->
            try {
                selectedImageBitmap = BitmapFactory.decodeStream(
                    context.contentResolver.openInputStream(uri)
                )
            } catch (e: Exception) {
                selectedImageBitmap = null
            }
        } ?: run { selectedImageBitmap = null }
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
                                            text = "$${String.format("%.2f", expense.amount)}",
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

            Text(
                text = "Expense Distribution",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Pie Chart
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        loadDataWithBaseURL(
                            null,
                            """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
                            </head>
                            <body>
                                <canvas id="expenseChart"></canvas>
                                <script>
                                    const ctx = document.getElementById('expenseChart').getContext('2d');
                                    new Chart(ctx, {
                                        type: 'pie',
                                        data: {
                                            labels: ['Utilities', 'Food', 'Transportation', 'Gas', 'Office Supplies', 'Rent', 'Parking', 'Electronic Supplies', 'Other Expenses'],
                                            datasets: [{
                                                data: [${chartData.joinToString()}],
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
                                                borderColor: ['#FFFFFF', '#FFFFFF', '#FFFFFF', '#FFFFFF', '#FFFFFF', '#FFFFFF', '#FFFFFF', '#FFFFFF', '#FFFFFF'],
                                                borderWidth: 1
                                            }]
                                        },
                                        options: {
                                            responsive: true,
                                            plugins: {
                                                legend: {
                                                    position: 'top',
                                                    labels: { color: '#333333' }
                                                },
                                                title: {
                                                    display: true,
                                                    text: 'Expense Distribution by Category',
                                                    color: '#333333',
                                                    font: { size: 16 }
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
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(3f) // 3/4 of screen height
                    .padding(bottom = 16.dp)
            )

            Text(
                text = "Your Top 5 Expense Categories",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (categoryTotals.isEmpty()) {
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
                        .weight(1f) // Remaining 1/4 of screen height
                ) {
                    items(categoryTotals) { (category, amount) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
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
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$${String.format("%.2f", amount)}",
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
                    title = { Text("Expense Details") },
                    text = {
                        Column {
                            selectedImageBitmap?.let { bitmap ->
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Expense photo",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .padding(bottom = 16.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                )
                            } ?: Text(
                                text = "No photo available",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Text(
                                text = "Description: ${expense.description}",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Category: ${expense.category}",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Amount: $${String.format("%.2f", expense.amount)}",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Date: ${expense.date}",
                                style = MaterialTheme.typography.bodyLarge
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
fun HomeScreenPreview() {
    ExpenSEEsTheme {
        HomeScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun RecordExpensesScreenPreview() {
    ExpenSEEsTheme {
        RecordExpensesScreen(navController = rememberNavController(), expenses = mutableListOf())
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