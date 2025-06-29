package com.example.expensees.screens

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import coil.compose.AsyncImage
import com.example.expensees.models.Expense
import com.example.expensees.network.AuthRepository
import com.example.expensees.utils.createImageUri
import com.google.gson.Gson
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordExpensesScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    authRepository: AuthRepository,
    onLogoutClick: () -> Unit = {}
) {
    var remarks by remember { mutableStateOf("") }
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
    var showAuthErrorDialog by remember { mutableStateOf(false) }
    var pendingExpense by remember { mutableStateOf<Expense?>(null) }
    val categories = listOf(
        "Utilities", "Food", "Transportation", "Gas", "Office Supplies",
        "Rent", "Parking", "Electronic Supplies", "Grocery", "Other Expenses"
    )
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
                Log.e("RecordExpensesScreen", "Failed to decode image: ${e.message}", e)
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
                Log.e("RecordExpensesScreen", "Failed to decode image: ${e.message}", e)
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
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(12.dp)),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = {
                Text(
                    text = "Permission Required",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Text(
                    text = when (permissionType) {
                        "camera" -> "Camera access is needed to take photos of your expense receipts."
                        "storage" -> "Storage access is needed to select photos from your gallery."
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
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
                    Text(
                        text = "Grant Permission",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 16.sp
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationale = false }) {
                    Text(
                        text = "Cancel",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 16.sp
                    )
                }
            }
        )
    }

    if (showAuthErrorDialog) {
        AlertDialog(
            onDismissRequest = { showAuthErrorDialog = false },
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(12.dp)),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = {
                Text(
                    text = "Authentication Error",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Text(
                    text = "Your session has expired or is invalid. The expense has been saved locally. Please log in again to sync it with the server.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAuthErrorDialog = false
                        authRepository.logout()
                        navController.navigate("login") {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        }
                    }
                ) {
                    Text(
                        text = "Log In",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 16.sp
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showAuthErrorDialog = false }) {
                    Text(
                        text = "Cancel",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 16.sp
                    )
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
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(12.dp)),
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
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
                            text = "${selectedExpense?.category ?: ""} Receipt",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = { showExpenseDialog = false; selectedExpense = null },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close dialog",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    selectedExpense?.imagePath?.let { imagePath ->
                        var imageLoadFailed by remember { mutableStateOf(false) }
                        if (imageLoadFailed) {
                            Text(
                                text = "No receipt photo available",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        } else if (selectedExpense?.expenseId?.startsWith("local_") == true) {
                            val bitmap = try {
                                val uri = Uri.parse(imagePath)
                                BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
                            } catch (e: Exception) {
                                Log.e("RecordExpensesScreen", "Failed to load receipt image: ${e.message}, imagePath: $imagePath", e)
                                null
                            }
                            bitmap?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = "${selectedExpense?.category ?: ""} receipt",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        } else {
                            AsyncImage(
                                model = imagePath,
                                contentDescription = "${selectedExpense?.category ?: ""} receipt",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                    .clickable { showFullScreenImage = true },
                                contentScale = ContentScale.Crop,
                                onError = {
                                    Log.e("RecordExpensesScreen", "Failed to load server image: ${it.result.throwable.message}")
                                    imageLoadFailed = true
                                    scope.launch {
                                        Toast.makeText(context, "Failed to load receipt image", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    } ?: Text(
                        text = "No receipt photo available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { showInfoDialog = true }) {
                            Text(
                                text = "Info",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 16.sp
                            )
                        }
                        TextButton(onClick = {
                            showExpenseDialog = false
                            selectedExpense = null
                        }) {
                            Text(
                                text = "Close",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showInfoDialog && selectedExpense != null) {
        AlertDialog(
            onDismissRequest = {
                showInfoDialog = false
                selectedExpense = null
                expenseImageBitmap = null
            },
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(12.dp)),
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Expense Details",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Category: ${selectedExpense?.category ?: "N/A"}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Amount: ₱${String.format("%.2f", selectedExpense?.amount ?: 0.0)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Date of Transaction: ${selectedExpense?.dateOfTransaction ?: "N/A"}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Created At: ${selectedExpense?.createdAt ?: "N/A"}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Remarks: ${selectedExpense?.remarks ?: "N/A"}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = {
                            showInfoDialog = false
                            selectedExpense = null
                            expenseImageBitmap = null
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = "Close",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }

    if (showFullScreenImage) {
        AlertDialog(
            onDismissRequest = {
                showFullScreenImage = false
                selectedExpense = null
                expenseImageBitmap = null
            },
            modifier = Modifier.fillMaxSize(),
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f))
                    .padding(
                        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    ),
                contentAlignment = Alignment.Center
            ) {
                selectedExpense?.let { expense ->
                    var imageLoadFailed by remember { mutableStateOf(false) }
                    if (imageLoadFailed) {
                        Text(
                            text = "No image available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else if (expense.expenseId?.startsWith("local_") == true) {
                        expense.imagePath?.let { imagePath ->
                            val bitmap = try {
                                val uri = Uri.parse(imagePath)
                                BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
                            } catch (e: Exception) {
                                Log.e("RecordExpensesScreen", "Failed to load full screen image: ${e.message}, imagePath: $imagePath", e)
                                null
                            }
                            bitmap?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = "Full screen expense photo",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            } ?: run {
                                imageLoadFailed = true
                                Text(
                                    text = "No image available",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } ?: Text(
                            text = "No image available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        expense.imagePath?.let { imagePath ->
                            AsyncImage(
                                model = imagePath,
                                contentDescription = "Full screen expense photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Fit,
                                onError = {
                                    Log.e("RecordExpensesScreen", "Failed to load server full screen image: ${it.result.throwable.message}")
                                    imageLoadFailed = true
                                    scope.launch {
                                        Toast.makeText(context, "Failed to load full screen image", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        } ?: Text(
                            text = "No image available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } ?: Text(
                    text = "No image available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
                IconButton(
                    onClick = {
                        showFullScreenImage = false
                        selectedExpense = null
                        expenseImageBitmap = null
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close image",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (authRepository.isAuthenticated()) {
            scope.launch {
                Log.d("RecordExpensesScreen", "Fetching expenses in coroutineContext=${currentCoroutineContext()}")
                val fetchResult = authRepository.getExpenses()
                fetchResult.onSuccess {
                    Log.d("RecordExpensesScreen", "Expenses fetched successfully")
                    if (pendingExpense != null) {
                        Log.d("RecordExpensesScreen", "Retrying pending expense: ${Gson().toJson(pendingExpense)}")
                        val result = authRepository.addExpense(pendingExpense!!)
                        result.onSuccess { returnedExpense ->
                            Log.d("RecordExpensesScreen", "Pending expense added: expenseId=${returnedExpense.expenseId}")
                            if (returnedExpense.expenseId?.startsWith("local_") == true) {
                                pendingExpense = null
                            }
                            Toast.makeText(context, "Expense synced successfully", Toast.LENGTH_SHORT).show()
                        }.onFailure { e ->
                            Log.e("RecordExpensesScreen", "Failed to sync pending expense: ${e.message}", e)
                            when {
                                e.message?.contains("Unauthorized") == true || e.message?.contains("Not authenticated") == true -> {
                                    showAuthErrorDialog = true
                                }
                                e.message?.contains("No internet connection") == true -> {
                                    Toast.makeText(context, "No internet connection, expense saved locally", Toast.LENGTH_LONG).show()
                                }
                                else -> {
                                    Toast.makeText(context, "Failed to sync expense: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }.onFailure { e ->
                    Log.e("RecordExpensesScreen", "Failed to fetch expenses: ${e.message}", e)
                    when {
                        e.message?.contains("Unauthorized") == true || e.message?.contains("Not authenticated") == true -> {
                            showAuthErrorDialog = true
                        }
                        e.message?.contains("No internet connection") == true -> {
                            Toast.makeText(context, "No internet connection, showing local expenses", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            Toast.makeText(context, "Failed to fetch expenses: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        } else {
            Log.d("RecordExpensesScreen", "Not authenticated, showing local expenses")
            Toast.makeText(context, "Not logged in, showing local expenses", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp), // Consistent with HomeScreen
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp, top = 16.dp), // Match HomeScreen header spacing
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.navigate("home") }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back to home",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "Record Expenses",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold), // Match HomeScreen title style
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp) // Consistent spacing
        ) {
            OutlinedTextField(
                value = category,
                onValueChange = { },
                label = { Text("Category") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { expanded = !expanded }) {
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
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
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
                .padding(bottom = 12.dp), // Consistent spacing
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        OutlinedTextField(
            value = dateOfTransaction,
            onValueChange = { },
            label = { Text("Date of Transaction (YYYY-MM-DD)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp), // Consistent spacing
            readOnly = true,
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

        OutlinedTextField(
            value = remarks,
            onValueChange = { remarks = it },
            label = { Text("Remarks") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp), // Consistent spacing
            maxLines = 3
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp), // Consistent spacing
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
                    .height(50.dp) // Match HomeScreen button height
                    .padding(end = 4.dp), // Consistent button spacing
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
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
                    .height(50.dp) // Match HomeScreen button height
                    .padding(start = 4.dp), // Consistent button spacing
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
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
                    .padding(bottom = 12.dp) // Consistent spacing
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                    .clickable {
                        expenseImageBitmap = bitmap
                        showFullScreenImage = true
                    },
                contentScale = ContentScale.Crop
            )
        }

        Button(
            onClick = {
                if (remarks.isNotBlank() && amount.isNotBlank() && category.isNotBlank() && dateOfTransaction.isNotBlank()) {
                    val amountValue = amount.toDoubleOrNull()
                    if (amountValue != null) {
                        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }.format(Date())
                        val newExpense = Expense(
                            expenseId = null,
                            category = category,
                            amount = amountValue,
                            dateOfTransaction = dateOfTransaction,
                            remarks = remarks,
                            imagePath = selectedImageUri?.toString(),
                            createdAt = timestamp
                        )
                        if (!authRepository.isAuthenticated()) {
                            Log.d("RecordExpensesScreen", "Not authenticated, saving locally: ${Gson().toJson(newExpense)}")
                            val localExpense = newExpense.copy(expenseId = "local_${System.currentTimeMillis()}")
                            authRepository.userExpenses.add(localExpense)
                            pendingExpense = newExpense
                            Toast.makeText(context, "Expense saved locally. Please log in to sync.", Toast.LENGTH_LONG).show()
                            showAuthErrorDialog = true
                            remarks = ""
                            amount = ""
                            category = ""
                            dateOfTransaction = ""
                            selectedImageBitmap = null
                            selectedImageUri = null
                            return@Button
                        }
                        scope.launch {
                            try {
                                Log.d("RecordExpensesScreen", "Attempting to add expense: ${Gson().toJson(newExpense)}, coroutineContext=${currentCoroutineContext()}")
                                val result = authRepository.addExpense(newExpense)
                                result.onSuccess { returnedExpense ->
                                    Log.d("RecordExpensesScreen", "Expense added: expenseId=${returnedExpense.expenseId}, expense=${Gson().toJson(returnedExpense)}")
                                    remarks = ""
                                    amount = ""
                                    category = ""
                                    dateOfTransaction = ""
                                    selectedImageBitmap = null
                                    selectedImageUri = null
                                    pendingExpense = null
                                    Toast.makeText(context, "Expense added successfully", Toast.LENGTH_SHORT).show()
                                }.onFailure { e ->
                                    Log.e("RecordExpensesScreen", "Failed to add expense: ${e.message}", e)
                                    when {
                                        e.message?.contains("Unauthorized") == true || e.message?.contains("Not authenticated") == true -> {
                                            Log.d("RecordExpensesScreen", "Authentication error, saving locally: ${Gson().toJson(newExpense)}")
                                            val localExpense = newExpense.copy(expenseId = "local_${System.currentTimeMillis()}")
                                            authRepository.userExpenses.add(localExpense)
                                            pendingExpense = newExpense
                                            Toast.makeText(context, "Session expired. Expense saved locally, please log in to sync.", Toast.LENGTH_LONG).show()
                                            showAuthErrorDialog = true
                                        }
                                        e.message?.contains("Invalid expense data") == true || e.message?.contains("Validation error") == true -> {
                                            Toast.makeText(context, "Invalid data: check fields (e.g., date format) and try again", Toast.LENGTH_LONG).show()
                                        }
                                        e.message?.contains("missing expenseId") == true -> {
                                            Toast.makeText(context, "Expense saved locally due to server issue. Try syncing later.", Toast.LENGTH_LONG).show()
                                            val localExpense = newExpense.copy(expenseId = "local_${System.currentTimeMillis()}")
                                            authRepository.userExpenses.add(localExpense)
                                            pendingExpense = newExpense
                                        }
                                        e.message?.contains("No internet connection") == true -> {
                                            Toast.makeText(context, "No internet connection, expense saved locally", Toast.LENGTH_LONG).show()
                                            val localExpense = newExpense.copy(expenseId = "local_${System.currentTimeMillis()}")
                                            authRepository.userExpenses.add(localExpense)
                                            pendingExpense = newExpense
                                        }
                                        else -> {
                                            Toast.makeText(context, "Failed to add expense: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("RecordExpensesScreen", "Unexpected error: ${e.message}", e)
                                Toast.makeText(context, "Failed to add expense: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Invalid amount", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(bottom = 12.dp), // Consistent spacing
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(8.dp) // Match HomeScreen button shape
        ) {
            Text(
                text = "Add Expense",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(8.dp)) // Match HomeScreen section spacing

        Text(
            text = "Recent Transactions",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp), // Match HomeScreen card spacing
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp) // Match HomeScreen transaction spacing
                ) {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    val sortedExpenses = authRepository.userExpenses.sortedByDescending {
                        try {
                            it.createdAt?.let { createdAt -> dateFormat.parse(createdAt) } ?: Date(0)
                        } catch (e: Exception) {
                            Log.e("RecordExpensesScreen", "Failed to parse date: ${it.createdAt}, error: ${e.message}")
                            try {
                                it.dateOfTransaction?.let { dateOfTransaction ->
                                    SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateOfTransaction)
                                } ?: Date(0)
                            } catch (e: Exception) {
                                Log.e("RecordExpensesScreen", "Failed to parse dateOfTransaction: ${it.dateOfTransaction}, error: ${e.message}")
                                Date(0)
                            }
                        }
                    }.take(10)
                    items(sortedExpenses) { expense ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp) // Match HomeScreen transaction card spacing
                                .clickable {
                                    selectedExpense = expense
                                    showExpenseDialog = true
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp), // Match HomeScreen transaction padding
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = expense.remarks ?: "",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${expense.category ?: ""}${if (expense.expenseId?.startsWith("local_") == true) " (Not Synced)" else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = try {
                                            expense.createdAt?.let { createdAt ->
                                                val parsedDate = dateFormat.parse(createdAt)
                                                SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(parsedDate)
                                            } ?: expense.dateOfTransaction ?: "Unknown date"
                                        } catch (e: Exception) {
                                            Log.e("RecordExpensesScreen", "Failed to format date: ${expense.createdAt}, error: ${e.message}")
                                            expense.dateOfTransaction ?: "Unknown date"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp)) // Match HomeScreen spacing
                                Text(
                                    text = "₱${String.format("%.2f", expense.amount)}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.End,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f)) // Match HomeScreen footer spacing
    }
}