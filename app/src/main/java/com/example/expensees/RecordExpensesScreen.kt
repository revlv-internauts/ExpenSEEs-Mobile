package com.example.expensees.screens

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.expensees.ApiConfig
import com.example.expensees.models.Expense
import com.example.expensees.network.AuthRepository
import com.example.expensees.utils.createImageUri
import com.google.gson.Gson
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
    var isAddingExpense by remember { mutableStateOf(false) }
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

    val takePictureLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && selectedImageUri != null) {
                selectedImageBitmap = try {
                    BitmapFactory.decodeStream(
                        context.contentResolver.openInputStream(
                            selectedImageUri!!
                        )
                    )
                } catch (e: Exception) {
                    Log.e("RecordExpensesScreen", "Failed to decode image: ${e.message}", e)
                    null
                }
            }
        }

    val pickImageLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
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
                    } ?: Toast.makeText(context, "Error creating image file", Toast.LENGTH_LONG)
                        .show()
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
        Dialog(
            onDismissRequest = { showPermissionRationale = false },
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
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Permission Required",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp
                        ),
                        color = Color(0xFF1F2937),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = when (permissionType) {
                            "camera" -> "Camera access is needed to take photos of your expense receipts."
                            "storage" -> "Storage access is needed to select photos from your gallery."
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                        color = Color(0xFF4B5563),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
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
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .padding(end = 8.dp),
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
                                            colors = listOf(Color(0xFF734656), Color(0xFF8A5B6E)),
                                            start = Offset(0f, 0f),
                                            end = Offset(Float.POSITIVE_INFINITY, 0f)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Grant Permission",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        Button(
                            onClick = { showPermissionRationale = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .padding(start = 8.dp),
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
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Cancel",
                                    color = Color(0xFF3B82F6),
                                    fontSize = 16.sp,
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

    if (showAuthErrorDialog) {
        Dialog(
            onDismissRequest = { showAuthErrorDialog = false },
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
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Authentication Error",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp
                        ),
                        color = Color(0xFF1F2937),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = "Your session has expired or is invalid. The expense has been saved locally. Please log in again to sync it with the server.",
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                        color = Color(0xFF4B5563),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                showAuthErrorDialog = false
                                authRepository.logout()
                                navController.navigate("login") {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = true
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .padding(end = 8.dp),
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
                                            colors = listOf(Color(0xFF734656), Color(0xFF8A5B6E)),
                                            start = Offset(0f, 0f),
                                            end = Offset(Float.POSITIVE_INFINITY, 0f)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Log In",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        Button(
                            onClick = { showAuthErrorDialog = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .padding(start = 8.dp),
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
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Cancel",
                                    color = Color(0xFF3B82F6),
                                    fontSize = 16.sp,
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

    if (showExpenseDialog && selectedExpense != null) {
        Dialog(
            onDismissRequest = {
                showExpenseDialog = false
                selectedExpense = null
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(16.dp)),
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
                            text = "${selectedExpense?.category ?: ""} Receipt",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 20.sp
                            ),
                            color = Color(0xFF1F2937)
                        )
                        IconButton(
                            onClick = { showExpenseDialog = false; selectedExpense = null },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFE5E7EB), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close dialog",
                                tint = Color(0xFF1F2937)
                            )
                        }
                    }
                    selectedExpense?.imagePaths?.firstOrNull()?.let { imagePath ->
                        var imageLoadFailed by remember { mutableStateOf(false) }
                        if (imageLoadFailed) {
                            Text(
                                text = "No receipt photo available",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                                color = Color(0xFF4B5563),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        } else if (selectedExpense?.expenseId?.startsWith("local_") == true) {
                            val bitmap = try {
                                val uri = Uri.parse(imagePath)
                                BitmapFactory.decodeStream(
                                    context.contentResolver.openInputStream(
                                        uri
                                    )
                                )
                            } catch (e: Exception) {
                                Log.e(
                                    "RecordExpensesScreen",
                                    "Failed to load receipt image: ${e.message}, imagePath: $imagePath",
                                    e
                                )
                                null
                            }
                            bitmap?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = "${selectedExpense?.category ?: ""} receipt",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            1.5.dp,
                                            Color(0xFF3B82F6),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            expenseImageBitmap = it
                                            showFullScreenImage = true
                                        },
                                    contentScale = ContentScale.Fit
                                )
                            } ?: run {
                                imageLoadFailed = true
                                Text(
                                    text = "No receipt photo available",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                                    color = Color(0xFF4B5563),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            }
                        } else {
                            val fullImageUrl = "${ApiConfig.BASE_URL}$imagePath"
                            Log.d("RecordExpensesScreen", "Loading server image: $fullImageUrl")
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(fullImageUrl)
                                    .addHeader(
                                        "Authorization",
                                        "Bearer ${
                                            context.getSharedPreferences(
                                                "auth_prefs",
                                                Context.MODE_PRIVATE
                                            ).getString("auth_token", "")
                                        }"
                                    )
                                    .build(),
                                contentDescription = "${selectedExpense?.category ?: ""} receipt",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.5.dp, Color(0xFF3B82F6), RoundedCornerShape(12.dp))
                                    .clickable { showFullScreenImage = true },
                                contentScale = ContentScale.Fit,
                                onError = {
                                    Log.e(
                                        "RecordExpensesScreen",
                                        "Failed to load server image: $fullImageUrl, error: ${it.result.throwable.message}"
                                    )
                                    imageLoadFailed = true
                                    scope.launch {
                                        Toast.makeText(
                                            context,
                                            "Failed to load receipt image",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                        }
                    } ?: Text(
                        text = "No receipt photo available",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                        color = Color(0xFF4B5563),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { showInfoDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .padding(end = 8.dp),
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
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Info",
                                    color = Color(0xFF3B82F6),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        Button(
                            onClick = { showExpenseDialog = false; selectedExpense = null },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .padding(start = 8.dp),
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
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Close",
                                    color = Color(0xFF3B82F6),
                                    fontSize = 16.sp,
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

    if (showInfoDialog && selectedExpense != null) {
        Dialog(
            onDismissRequest = {
                showInfoDialog = false
                selectedExpense = null
                expenseImageBitmap = null
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
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp
                        ),
                        color = Color(0xFF1F2937),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Category: ${selectedExpense?.category ?: "N/A"}",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                            color = Color(0xFF1F2937),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Amount: ₱${
                                String.format(
                                    "%.2f",
                                    selectedExpense?.amount ?: 0.0
                                )
                            }",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                            color = Color(0xFF1F2937),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Date of Transaction: ${selectedExpense?.dateOfTransaction ?: "N/A"}",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                            color = Color(0xFF1F2937),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Created At: ${
                                selectedExpense?.createdAt?.let { utcTime ->
                                    try {
                                        val utcFormat = SimpleDateFormat(
                                            "yyyy-MM-dd'T'HH:mm:ss'Z'",
                                            Locale.US
                                        ).apply {
                                            timeZone = TimeZone.getTimeZone("UTC")
                                        }
                                        val localFormat = SimpleDateFormat(
                                            "yyyy-MM-dd HH:mm:ss",
                                            Locale.US
                                        ).apply {
                                            timeZone =
                                                TimeZone.getDefault() // Use device's local timezone (e.g., UTC+8 for Philippines)
                                        }
                                        val date = utcFormat.parse(utcTime)
                                        date?.let { localFormat.format(it) } ?: "N/A"
                                    } catch (e: Exception) {
                                        Log.e(
                                            "RecordExpensesScreen",
                                            "Failed to parse createdAt: ${e.message}"
                                        )
                                        "N/A"
                                    }
                                } ?: "N/A"
                            }",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                            color = Color(0xFF1F2937),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Remarks: ${selectedExpense?.remarks ?: "N/A"}",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                            color = Color(0xFF1F2937),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            showInfoDialog = false
                            selectedExpense = null
                            expenseImageBitmap = null
                        },
                        modifier = Modifier
                            .align(Alignment.End)
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
                                        colors = listOf(Color(0xFFE5E7EB), Color(0xFFD1D5DB)),
                                        start = Offset(0f, 0f),
                                        end = Offset(Float.POSITIVE_INFINITY, 0f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Close",
                                color = Color(0xFF3B82F6),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFullScreenImage) {
        Dialog(
            onDismissRequest = {
                showFullScreenImage = false
                selectedExpense = null
                expenseImageBitmap = null
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5))
                    .padding(
                        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                        bottom = WindowInsets.navigationBars.asPaddingValues()
                            .calculateBottomPadding(),
                        start = 16.dp,
                        end = 16.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                selectedExpense?.let { expense ->
                    var imageLoadFailed by remember { mutableStateOf(false) }
                    if (imageLoadFailed) {
                        Text(
                            text = "No image available",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                            color = Color(0xFF4B5563),
                            modifier = Modifier.padding(16.dp)
                        )
                    } else if (expense.expenseId?.startsWith("local_") == true) {
                        expense.imagePaths?.firstOrNull()?.let { imagePath ->
                            val bitmap = try {
                                val uri = Uri.parse(imagePath)
                                BitmapFactory.decodeStream(
                                    context.contentResolver.openInputStream(
                                        uri
                                    )
                                )
                            } catch (e: Exception) {
                                Log.e(
                                    "RecordExpensesScreen",
                                    "Failed to load full screen image: ${e.message}, imagePath: $imagePath",
                                    e
                                )
                                null
                            }
                            bitmap?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = "Full screen expense photo",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(2.dp, Color(0xFF3B82F6), RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            } ?: run {
                                imageLoadFailed = true
                                Text(
                                    text = "No image available",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                    color = Color(0xFF4B5563),
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } ?: Text(
                            text = "No image available",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                            color = Color(0xFF4B5563),
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        expense.imagePaths?.firstOrNull()?.let { imagePath ->
                            val fullImageUrl = "${ApiConfig.BASE_URL}$imagePath"
                            Log.d(
                                "RecordExpensesScreen",
                                "Loading full-screen server image: $fullImageUrl"
                            )
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(fullImageUrl)
                                    .addHeader(
                                        "Authorization",
                                        "Bearer ${
                                            context.getSharedPreferences(
                                                "auth_prefs",
                                                Context.MODE_PRIVATE
                                            ).getString("auth_token", "")
                                        }"
                                    )
                                    .build(),
                                contentDescription = "Full screen expense photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(2.dp, Color(0xFF3B82F6), RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Fit,
                                onError = {
                                    Log.e(
                                        "RecordExpensesScreen",
                                        "Failed to load server full screen image: $fullImageUrl, error: ${it.result.throwable.message}"
                                    )
                                    imageLoadFailed = true
                                    scope.launch {
                                        Toast.makeText(
                                            context,
                                            "Failed to load full screen image",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                        } ?: Text(
                            text = "No image available",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                            color = Color(0xFF4B5563),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } ?: Text(
                    text = "No image available",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    color = Color(0xFF4B5563),
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

    LaunchedEffect(Unit) {
        if (authRepository.isAuthenticated()) {
            scope.launch {
                Log.d("RecordExpensesScreen", "Fetching expenses")
                val fetchResult = authRepository.getExpenses()
                fetchResult.onSuccess {
                    Log.d("RecordExpensesScreen", "Expenses fetched successfully")
                    if (pendingExpense != null) {
                        Log.d(
                            "RecordExpensesScreen",
                            "Retrying pending expense: ${Gson().toJson(pendingExpense)}"
                        )
                        val result = authRepository.addExpense(pendingExpense!!)
                        result.onSuccess { returnedExpense ->
                            Log.d(
                                "RecordExpensesScreen",
                                "Pending expense added: expenseId=${returnedExpense.expenseId}"
                            )
                            if (returnedExpense.expenseId?.startsWith("local_") != true) {
                                pendingExpense = null
                            }
                            Toast.makeText(
                                context,
                                "Expense synced successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                        }.onFailure { e ->
                            Log.e(
                                "RecordExpensesScreen",
                                "Failed to sync pending expense: ${e.message}",
                                e
                            )
                            when {
                                e.message?.contains("Unauthorized") == true || e.message?.contains("Not authenticated") == true -> {
                                    showAuthErrorDialog = true
                                }

                                e.message?.contains("No internet connection") == true -> {
                                    Toast.makeText(
                                        context,
                                        "No internet connection, expense saved locally",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                                else -> {
                                    Toast.makeText(
                                        context,
                                        "Failed to sync expense: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
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
                            Toast.makeText(
                                context,
                                "No internet connection, showing local expenses",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        else -> {
                            Toast.makeText(
                                context,
                                "Failed to fetch expenses: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        } else {
            Log.d("RecordExpensesScreen", "Not authenticated, showing local expenses")
            Toast.makeText(context, "Not logged in, showing local expenses", Toast.LENGTH_LONG)
                .show()
        }
    }
    val focusManager = LocalFocusManager.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .clickable(
                onClick = { focusManager.clearFocus() },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 50.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.navigate("home") },
                    modifier = Modifier
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to home",
                        tint = Color(0xFF1F2937)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Record Expense",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp
                        ),
                        color = Color(0xFF1F2937),
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(bottom = 12.dp),
                color = Color(0xFFE5E7EB)
            )

            // Category Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                    color = Color(0xFF1F2937),
                    modifier = Modifier
                        .width(100.dp)
                        .padding(end = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = false,
                        readOnly = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                        placeholder = {
                            Text(
                                "Select a category",
                                color = Color(0xFF4B5563),
                                fontSize = 14.sp
                            )
                        },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            disabledTextColor = Color(0xFF1F2937),
                            disabledBorderColor = Color(0xFFE5E7EB),
                            disabledPlaceholderColor = Color(0xFF4B5563),
                            disabledLabelColor = Color(0xFF4B5563),
                            disabledLeadingIconColor = Color(0xFF4B5563),
                            disabledTrailingIconColor = Color(0xFF4B5563)
                        ),
                        trailingIcon = {
                            IconButton(onClick = { expanded = !expanded }) {
                                Text(
                                    text = if (expanded) "▲" else "▼",
                                    color = Color(0xFF8A5B6E),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F5F5))
                    ) {
                        categories.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        option,
                                        color = Color(0xFF1F2937),
                                        fontSize = 14.sp
                                    )
                                },
                                onClick = {
                                    category = option
                                    expanded = false
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Amount Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Amount",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                    color = Color(0xFF1F2937),
                    modifier = Modifier
                        .width(100.dp)
                        .padding(end = 8.dp)
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            amount = newValue
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = LocalTextStyle.current.copy(
                        color = Color(0xFF1F2937),
                        fontSize = 14.sp
                    ),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFFE5E7EB),
                        cursorColor = Color(0xFF3B82F6)
                    ),
                    placeholder = {
                        Text(
                            "Enter amount (e.g., 50.00)",
                            color = Color(0xFF4B5563),
                            fontSize = 14.sp
                        )
                    }
                )
            }

            // Date Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Date",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                    color = Color(0xFF1F2937),
                    modifier = Modifier
                        .width(100.dp)
                        .padding(end = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { datePickerDialog.show() }
                ) {
                    OutlinedTextField(
                        value = dateOfTransaction,
                        onValueChange = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = false,
                        readOnly = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                        placeholder = {
                            Text(
                                "Select date (YYYY-MM-DD)",
                                color = Color(0xFF4B5563),
                                fontSize = 14.sp
                            )
                        },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            disabledTextColor = Color(0xFF1F2937),
                            disabledBorderColor = Color(0xFFE5E7EB),
                            disabledPlaceholderColor = Color(0xFF4B5563),
                            disabledLabelColor = Color(0xFF4B5563),
                            disabledLeadingIconColor = Color(0xFF4B5563),
                            disabledTrailingIconColor = Color(0xFF4B5563)
                        ),
                        trailingIcon = {
                            IconButton(onClick = { datePickerDialog.show() }) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Select date",
                                    tint = Color(0xFF8A5B6E)
                                )
                            }
                        }
                    )
                }
            }

            // Remarks Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "Remarks",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                    color = Color(0xFF1F2937),
                    modifier = Modifier
                        .width(100.dp)
                        .padding(end = 8.dp, top = 16.dp)
                )
                val focusManager = LocalFocusManager.current
                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        color = Color(0xFF1F2937),
                        fontSize = 14.sp
                    ),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFFE5E7EB),
                        cursorColor = Color(0xFF3B82F6)
                    ),
                    placeholder = {
                        Text(
                            "Enter remarks (e.g., Lunch at Cafe)",
                            color = Color(0xFF4B5563),
                            fontSize = 14.sp
                        )
                    }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
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
                            } ?: Toast.makeText(
                                context,
                                "Error creating image file",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .padding(end = 8.dp),
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
                                    colors = listOf(Color(0xFF734656), Color(0xFF8A5B6E)),
                                    start = Offset(0f, 0f),
                                    end = Offset(Float.POSITIVE_INFINITY, 0f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Take Photo",
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Button(
                    onClick = {
                        permissionType = "storage"
                        val storagePermission =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
                        .height(48.dp)
                        .padding(start = 8.dp),
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
                                    colors = listOf(Color(0xFF734656), Color(0xFF8A5B6E)),
                                    start = Offset(0f, 0f),
                                    end = Offset(Float.POSITIVE_INFINITY, 0f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Pick from Gallery",
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            selectedImageBitmap?.let { bitmap ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(2f)
                        .padding(bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Selected expense photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, Color(0xFF3B82F6), RoundedCornerShape(12.dp))
                            .clickable {
                                expenseImageBitmap = bitmap
                                showFullScreenImage = true
                                selectedExpense = Expense(
                                    expenseId = "local_${System.currentTimeMillis()}",
                                    category = category.takeIf { it.isNotBlank() } ?: "N/A",
                                    amount = amount.toDoubleOrNull() ?: 0.0,
                                    dateOfTransaction = dateOfTransaction.takeIf { it.isNotBlank() },
                                    remarks = remarks.takeIf { it.isNotBlank() },
                                    imagePaths = selectedImageUri?.let { listOf(it.toString()) },
                                    createdAt = SimpleDateFormat(
                                        "yyyy-MM-dd'T'HH:mm:ss",
                                        Locale.US
                                    ).apply {
                                        timeZone = TimeZone.getDefault()
                                    }.format(Date())
                                )
                            },
                        contentScale = ContentScale.Fit
                    )
                }
            } ?: Spacer(modifier = Modifier.weight(2f))

            // Add Expense Button
            Button(
                onClick = {
                    if (remarks.isNotBlank() && amount.isNotBlank() && category.isNotBlank() && dateOfTransaction.isNotBlank()) {
                        val amountValue = amount.toDoubleOrNull()
                        if (amountValue != null) {
                            // Use device's local time for createdAt
                            val localFormat =
                                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                                    timeZone = TimeZone.getDefault()
                                }
                            val timestamp = localFormat.format(Date())
                            Log.d("RecordExpensesScreen", "Selected image URI: $selectedImageUri")
                            val newExpense = Expense(
                                expenseId = null,
                                category = category,
                                amount = amountValue,
                                dateOfTransaction = dateOfTransaction,
                                remarks = remarks,
                                imagePaths = selectedImageUri?.let { listOf(it.toString()) },
                                createdAt = timestamp
                            )
                            if (!authRepository.isAuthenticated()) {
                                Log.d(
                                    "RecordExpensesScreen",
                                    "Not authenticated, saving locally: ${Gson().toJson(newExpense)}"
                                )
                                val localExpense =
                                    newExpense.copy(expenseId = "local_${System.currentTimeMillis()}")
                                authRepository.userExpenses.add(localExpense)
                                pendingExpense = newExpense
                                Toast.makeText(
                                    context,
                                    "Not logged in. Expense saved locally, please log in to sync.",
                                    Toast.LENGTH_LONG
                                ).show()
                                showAuthErrorDialog = true
                                remarks = ""
                                amount = ""
                                category = ""
                                dateOfTransaction = ""
                                selectedImageBitmap = null
                                selectedImageUri = null
                                return@Button
                            }
                            isAddingExpense = true
                            scope.launch {
                                try {
                                    Log.d(
                                        "RecordExpensesScreen",
                                        "Attempting to add expense: ${Gson().toJson(newExpense)}"
                                    )
                                    val result = authRepository.addExpense(newExpense)
                                    result.onSuccess { returnedExpense ->
                                        Log.d(
                                            "RecordExpensesScreen",
                                            "Expense added: expenseId=${returnedExpense.expenseId}, expense=${
                                                Gson().toJson(returnedExpense)
                                            }"
                                        )
                                        remarks = ""
                                        amount = ""
                                        category = ""
                                        dateOfTransaction = ""
                                        selectedImageBitmap = null
                                        selectedImageUri = null
                                        pendingExpense = null
                                        if (selectedImageUri != null && returnedExpense.imagePaths.isNullOrEmpty()) {
                                            Toast.makeText(
                                                context,
                                                "Expense added, but image upload failed. Please try again.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Expense added successfully",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }.onFailure { e ->
                                        Log.e(
                                            "RecordExpensesScreen",
                                            "Failed to add expense: ${e.message}",
                                            e
                                        )
                                        when {
                                            e.message?.contains("Unauthorized") == true || e.message?.contains(
                                                "Not authenticated"
                                            ) == true -> {
                                                Log.d(
                                                    "RecordExpensesScreen",
                                                    "Authentication error, saving locally: ${
                                                        Gson().toJson(newExpense)
                                                    }"
                                                )
                                                val localExpense =
                                                    newExpense.copy(expenseId = "local_${System.currentTimeMillis()}")
                                                authRepository.userExpenses.add(localExpense)
                                                pendingExpense = newExpense
                                                Toast.makeText(
                                                    context,
                                                    "Session expired. Expense saved locally, please log in to sync.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                showAuthErrorDialog = true
                                            }

                                            e.message?.contains("Invalid expense data") == true || e.message?.contains(
                                                "Validation error"
                                            ) == true -> {
                                                Toast.makeText(
                                                    context,
                                                    "Invalid data: check fields (e.g., date format) and try again",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }

                                            e.message?.contains("missing expenseId") == true -> {
                                                Toast.makeText(
                                                    context,
                                                    "Expense saved locally due to server issue. Try syncing later.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                val localExpense =
                                                    newExpense.copy(expenseId = "local_${System.currentTimeMillis()}")
                                                authRepository.userExpenses.add(localExpense)
                                                pendingExpense = newExpense
                                            }

                                            e.message?.contains("No internet connection") == true -> {
                                                Toast.makeText(
                                                    context,
                                                    "No internet connection, expense saved locally",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                val localExpense =
                                                    newExpense.copy(expenseId = "local_${System.currentTimeMillis()}")
                                                authRepository.userExpenses.add(localExpense)
                                                pendingExpense = newExpense
                                            }

                                            else -> {
                                                Toast.makeText(
                                                    context,
                                                    "Failed to add expense: ${e.message}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                val localExpense =
                                                    newExpense.copy(expenseId = "local_${System.currentTimeMillis()}")
                                                authRepository.userExpenses.add(localExpense)
                                                pendingExpense = newExpense
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e(
                                        "RecordExpensesScreen",
                                        "Unexpected error: ${e.message}",
                                        e
                                    )
                                    Toast.makeText(
                                        context,
                                        "Failed to add expense: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    val localExpense =
                                        newExpense.copy(expenseId = "local_${System.currentTimeMillis()}")
                                    authRepository.userExpenses.add(localExpense)
                                    pendingExpense = newExpense
                                } finally {
                                    isAddingExpense = false
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
                    .height(56.dp)
                    .padding(bottom = 8.dp),
                enabled = !isAddingExpense,
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
                                colors = listOf(Color(0xFF734656), Color(0xFF8A5B6E)),
                                start = Offset(0f, 0f),
                                end = Offset(Float.POSITIVE_INFINITY, 0f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isAddingExpense) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Add Expense",
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // View Expenses Button
            Button(
                onClick = { navController.navigate("list_expenses") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 8.dp),
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
                                colors = listOf(Color(0xFF734656), Color(0xFF8A5B6E)),
                                start = Offset(0f, 0f),
                                end = Offset(Float.POSITIVE_INFINITY, 0f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "View Expenses",
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