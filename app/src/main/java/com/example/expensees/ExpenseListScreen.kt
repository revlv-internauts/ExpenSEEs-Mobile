package com.example.expensees.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntSize
import java.io.File
import android.app.DownloadManager
import android.os.Environment
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.graphics.graphicsLayer
import java.io.FileOutputStream
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ExpenseListScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    expenses: SnapshotStateList<Expense>,
    authRepository: AuthRepository,
    onDeleteExpenses: (List<Expense>) -> Unit
) {
    var selectedExpenses by remember { mutableStateOf(setOf<Expense>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var selectedExpense by remember { mutableStateOf<Expense?>(null) }
    var selectedImagePath by remember { mutableStateOf<String?>(null) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showFullScreenImage by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var token by remember { mutableStateOf<String?>(null) }
    var tokenFetchFailed by remember { mutableStateOf(false) }
    var retryCount by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedSortCategory by remember { mutableStateOf<String?>(null) }
    var showAllExpenses by remember { mutableStateOf(false) }
    var showCheckboxes by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    val themeColor = Color(0xFF734656)

    // Fetch token on initialization
    LaunchedEffect(Unit) {
        if (retryCount < 2) {
            try {
                val tokenResult = authRepository.getValidToken()
                if (tokenResult.isSuccess) {
                    token = tokenResult.getOrNull()
                    Log.d("ExpenseListScreen", "Initial token fetched: ${token?.take(20)}...")
                } else {
                    retryCount++
                    Log.e("ExpenseListScreen", "Initial token retrieval failed: ${tokenResult.exceptionOrNull()?.message}")
                    tokenFetchFailed = true
                }
            } catch (e: Exception) {
                retryCount++
                Log.e("ExpenseListScreen", "Initial token retrieval exception: ${e.message}")
                tokenFetchFailed = true
            }
        } else {
            tokenFetchFailed = true
            Toast.makeText(context, "Authentication error: Please log in again", Toast.LENGTH_SHORT).show()
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val filteredExpenses = expenses.filter { expense ->
        try {
            val matchesDate = showAllExpenses || expense.createdAt?.let {
                val createdAtDate = LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                createdAtDate == selectedDate
            } ?: false
            val matchesSearch = searchQuery.isEmpty() ||
                    expense.category?.contains(searchQuery, ignoreCase = true) == true ||
                    expense.remarks?.contains(searchQuery, ignoreCase = true) == true
            val matchesCategory = selectedSortCategory == null || expense.category == selectedSortCategory
            matchesDate && matchesSearch && matchesCategory
        } catch (e: DateTimeParseException) {
            false
        }
    }.sortedWith(
        compareByDescending<Expense> { expense ->
            try {
                expense.createdAt?.let { java.time.LocalDateTime.parse(it) } ?: java.time.LocalDateTime.MIN
            } catch (e: DateTimeParseException) {
                java.time.LocalDateTime.MIN
            }
        }
    )

    LaunchedEffect(selectedDate, showAllExpenses) {
        selectedExpenses = emptySet()
        showCheckboxes = false
        selectedExpense = null
        selectedImagePath = null
        showExpenseDialog = false
        showFullScreenImage = false
        showInfoDialog = false
        Log.d("ExpenseListScreen", "Date changed to $selectedDate, resetting selectedExpense and dialogs")
    }


    val categories = listOf(
        "Utilities", "Food", "Transportation", "Gas", "Office Supplies",
        "Rent", "Parking", "Electronic Supplies", "Grocery", "Other Expenses"
    )
    val categoryColors = categories.zip(
        listOf(
            Color(0xFF734656),
            Color(0xFF734656),
            Color(0xFF734656),
            Color(0xFF734656),
            Color(0xFF734656),
            Color(0xFF734656),
            Color(0xFF734656),
            Color(0xFF734656),
            Color(0xFF734656),
            Color(0xFF734656)
        )
    ).toMap()

    val categoryIcons = categories.zip(
        listOf(
            Icons.Default.Bolt,
            Icons.Default.Restaurant,
            Icons.Default.DirectionsCar,
            Icons.Default.LocalGasStation,
            Icons.Default.Work,
            Icons.Default.Home,
            Icons.Default.LocalParking,
            Icons.Default.Devices,
            Icons.Default.ShoppingCart,
            Icons.Default.Category
        )
    ).toMap()

    val calendar = Calendar.getInstance()
    val datePickerDialog = remember(selectedDate) {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                showDatePickerDialog = false
            },
            selectedDate.year,
            selectedDate.monthValue - 1, // Month is 0-based in DatePickerDialog
            selectedDate.dayOfMonth
        ).apply {
            setOnCancelListener { showDatePickerDialog = false }
            setOnDismissListener { showDatePickerDialog = false }
        }
    }

    LaunchedEffect(showDatePickerDialog) {
        if (showDatePickerDialog) {
            datePickerDialog.show()
        } else {
            datePickerDialog.dismiss()
        }
    }

    val startDate = selectedDate.withDayOfMonth(1)
    val daysInMonth = startDate.lengthOfMonth()
    val days = (0 until daysInMonth).map { startDate.plusDays(it.toLong()) }
    val lazyListState = rememberLazyListState()

    LaunchedEffect(selectedDate) {
        val index = days.indexOf(selectedDate).coerceAtLeast(0)
        lazyListState.animateScrollToItem(index)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
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
                    onClick = { navController.navigate("home") },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to home",
                        tint = Color(0xFF1F2937)
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    Text(
                        text = searchQuery,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = Color(0xFF1F2937),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    )
                } else {
                    Text(
                        text = "List of Expenses",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = Color(0xFF1F2937),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    )
                }
                Row {
                    IconButton(
                        onClick = { showSearchDialog = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search expenses",
                            tint = Color(0xFF734656),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    if (!showAllExpenses) {
                        IconButton(
                            onClick = { showDatePickerDialog = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Select date",
                                tint = Color(0xFF734656),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showAllExpenses = !showAllExpenses },
                    modifier = Modifier
                        .height(36.dp)
                        .padding(end = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, themeColor),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (showAllExpenses) "Filter by Date" else "All Expenses",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = themeColor
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier
                            .height(36.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, themeColor),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = selectedSortCategory ?: "Sort by Category",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = themeColor
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Sort dropdown",
                            tint = themeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            {
                                Text(
                                    "All Categories",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            onClick = {
                                selectedSortCategory = null
                                expanded = false
                            }
                        )
                        categories.forEach { category ->
                            DropdownMenuItem(
                                {
                                    Text(
                                        category,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                onClick = {
                                    selectedSortCategory = category
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(bottom = 12.dp),
                color = Color(0xFFE5E7EB)
            )

            if (!showAllExpenses) {
                Text(
                    text = selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    ),
                    color = Color(0xFF1F2937),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center
                )

                LazyRow(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(days) { day ->
                        val hasExpenses = expenses.any { expense ->
                            try {
                                val matchesDate = expense.createdAt?.let {
                                    val createdAtDate = LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                    createdAtDate == day
                                } ?: false
                                val matchesCategory = selectedSortCategory == null || expense.category == selectedSortCategory
                                val matchesSearch = searchQuery.isEmpty() ||
                                        expense.category?.contains(searchQuery, ignoreCase = true) == true ||
                                        expense.remarks?.contains(searchQuery, ignoreCase = true) == true
                                matchesDate && matchesCategory && matchesSearch
                            } catch (e: DateTimeParseException) {
                                false
                            }
                        }
                        val isSelected = day == selectedDate
                        Surface(
                            modifier = Modifier
                                .size(60.dp)
                                .clickable { selectedDate = day }
                                .clip(RoundedCornerShape(12.dp)),
                            color = if (isSelected) Color(0xFF734656) else Color(0xFFE5E7EB),
                            shape = RoundedCornerShape(12.dp),
                            shadowElevation = if (isSelected) 8.dp else 4.dp
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = day.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (isSelected) Color.White else Color(0xFF1F2937)
                                )
                                Text(
                                    text = day.dayOfWeek.toString().substring(0, 3),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = if (isSelected) Color.White.copy(alpha = 0.7f) else Color(0xFF4B5563)
                                )
                                if (hasExpenses) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .offset(y = 4.dp)
                                            .background(
                                                color = Color(0xFF734656),
                                                shape = CircleShape
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = Color.White.copy(alpha = 0.8f),
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = if (selectedExpenses.isNotEmpty()) 80.dp else 16.dp)
            ) {
                if (filteredExpenses.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No recorded expenses yet",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Color(0xFF4B5563),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    itemsIndexed(filteredExpenses, key = { _, expense -> expense.expenseId ?: UUID.randomUUID().toString() }) { index, expense ->
                        val isSelected = expense in selectedExpenses
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val scale by animateFloatAsState(
                            targetValue = if (isPressed || isSelected) 1.05f else 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        )
                        val elevation by animateDpAsState(
                            targetValue = if (isPressed || isSelected) 8.dp else 4.dp,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        )

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(scale)
                                .pointerInput(expense.expenseId) {
                                    detectTapGestures(
                                        onTap = {
                                            if (showCheckboxes) {
                                                selectedExpenses = if (isSelected) {
                                                    selectedExpenses - expense
                                                } else {
                                                    selectedExpenses + expense
                                                }
                                            } else {
                                                // Clear previous state and set new expense data
                                                selectedExpense = null
                                                selectedImagePath = null
                                                showFullScreenImage = false
                                                showInfoDialog = false
                                                selectedExpense = expense
                                                selectedImagePath = expense.imagePaths?.firstOrNull()
                                                showExpenseDialog = true
                                                Log.d(
                                                    "ExpenseListScreen",
                                                    "Clicked expense at index $index: ID=${ expenses[index].expenseId}, Category=${expense.category}, ImagePath=$selectedImagePath"
                                                )
                                            }
                                        },
                                        onLongPress = {
                                            showCheckboxes = true
                                            selectedExpenses = selectedExpenses + expense
                                        }
                                    )
                                }
                                .clip(RoundedCornerShape(16.dp)),
                            color = Color.White,
                            shadowElevation = elevation
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (showCheckboxes) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            selectedExpenses = if (checked) {
                                                selectedExpenses + expense
                                            } else {
                                                selectedExpenses - expense
                                            }
                                            if (selectedExpenses.isEmpty()) {
                                                showCheckboxes = false
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = Color(0xFF734656),
                                            uncheckedColor = Color(0xFF4B5563),
                                            checkmarkColor = Color.White
                                        ),
                                        modifier = Modifier.padding(4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = categoryIcons[expense.category]
                                                ?: Icons.Default.Category,
                                            contentDescription = null,
                                            tint = Color(0xFF734656),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = expense.category ?: "",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                            ),
                                            color = Color(0xFF1F2937)
                                        )
                                    }
                                    Text(
                                        text = expense.remarks ?: "",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        ),
                                        color = Color(0xFF1F2937),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "₱${numberFormat.format(expense.amount)}",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        ),
                                        color = Color(0xFF734656)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                expense.imagePaths?.firstOrNull()?.let { imagePath ->
                                    var imageLoadFailed by remember { mutableStateOf(false) }
                                    var isImageLoading by remember { mutableStateOf(true) }
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.5.dp, themeColor, RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (tokenFetchFailed) {
                                            Text(
                                                text = "Auth Error",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                                color = Color(0xFF4B5563),
                                                textAlign = TextAlign.Center
                                            )
                                        } else if (expense.expenseId?.startsWith("local_") == true) {
                                            val bitmap = try {
                                                val uri = Uri.parse(imagePath)
                                                BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
                                            } catch (e: Exception) {
                                                Log.e("ExpenseListScreen", "Failed to load local image: $imagePath, error: ${e.message}")
                                                null
                                            }
                                            if (bitmap != null) {
                                                isImageLoading = false
                                                Image(
                                                    bitmap = bitmap.asImageBitmap(),
                                                    contentDescription = "Expense receipt",
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .animateContentSize(animationSpec = tween(200)),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                isImageLoading = false
                                                imageLoadFailed = true
                                                Text(
                                                    text = "No Image",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                                    color = Color(0xFF4B5563),
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        } else {
                                            val fullImageUrl = "${ApiConfig.BASE_URL}api/expenses/${expense.expenseId}/images"
                                            Log.d("ExpenseListScreen", "Loading server image: $fullImageUrl with token: ${token?.take(20)}...")
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(fullImageUrl)
                                                    .apply {
                                                        if (token != null) {
                                                            addHeader("Authorization", "Bearer $token")
                                                        } else {
                                                            imageLoadFailed = true
                                                            isImageLoading = false
                                                        }
                                                    }
                                                    .diskCacheKey(fullImageUrl)
                                                    .memoryCacheKey(fullImageUrl)
                                                    .build(),
                                                contentDescription = "Expense receipt",
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .animateContentSize(animationSpec = tween(200)),
                                                contentScale = ContentScale.Crop,
                                                onLoading = { isImageLoading = true },
                                                onError = { error ->
                                                    isImageLoading = false
                                                    imageLoadFailed = true
                                                    scope.launch {
                                                        Log.e("ExpenseListScreen", "Failed to load server image: $fullImageUrl, error: ${error.result.throwable.message}")
                                                        snackbarHostState.showSnackbar(
                                                            message = "Failed to load receipt image",
                                                            duration = SnackbarDuration.Short
                                                        )
                                                        if (error.result.throwable.message?.contains("401") == true && retryCount < 2) {
                                                            retryCount++
                                                            val tokenResult = authRepository.getValidToken()
                                                            if (tokenResult.isSuccess) {
                                                                token = tokenResult.getOrNull()
                                                                Log.d("ExpenseListScreen", "Retry token fetched: ${token?.take(20)}...")
                                                            } else {
                                                                tokenFetchFailed = true
                                                                Toast.makeText(context, "Authentication error: Please log in again", Toast.LENGTH_SHORT).show()
                                                                navController.navigate("login") {
                                                                    popUpTo("home") { inclusive = true }
                                                                }
                                                            }
                                                        }
                                                    }
                                                },
                                                onSuccess = {
                                                    isImageLoading = false
                                                    Log.d("ExpenseListScreen", "Successfully loaded server image: $fullImageUrl")
                                                }
                                            )
                                        }
                                        if (isImageLoading && !imageLoadFailed) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = themeColor
                                            )
                                        }
                                    }
                                } ?: Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.5.dp, themeColor, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No Image",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                        color = Color(0xFF4B5563),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (selectedExpenses.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            selectedExpenses = emptySet()
                            showCheckboxes = false
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
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
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Deselect All",
                                color = themeColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Button(
                        onClick = {
                            selectedExpenses = filteredExpenses.toSet()
                            showCheckboxes = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .padding(horizontal = 4.dp),
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
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Select All",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Button(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
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
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Delete (${selectedExpenses.size})",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Add button in bottom-right corner
        if (!showCheckboxes) {
            FloatingActionButton(
                onClick = { navController.navigate("record_expenses") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 8.dp, bottom = 16.dp) // Adjusted padding to move closer to right
                    .size(48.dp), // Changed to square button, smaller size
                shape = CircleShape, // Changed to circular shape
                containerColor = Color.Transparent,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 8.dp
                )
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
                            shape = CircleShape // Match shape with FAB
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add expense",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    if (showSearchDialog) {
        Dialog(
            onDismissRequest = { showSearchDialog = false },
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
                        text = "Search Expenses",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp
                        ),
                        color = Color(0xFF1F2937),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        label = { Text("Search by category or remarks") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = { showSearchDialog = false }
                        ),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = themeColor,
                            unfocusedBorderColor = Color(0xFFE5E7EB),
                            cursorColor = themeColor
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                searchQuery = ""
                                showSearchDialog = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
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
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Clear",
                                    color = themeColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        Button(
                            onClick = { showSearchDialog = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
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
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Search",
                                    color = Color.White,
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

    if (showDeleteDialog) {
        Dialog(
            onDismissRequest = { showDeleteDialog = false },
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
                        text = "Delete Expenses",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp
                        ),
                        color = Color(0xFF1F2937),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = "Are you sure you want to delete ${selectedExpenses.size} expense(s)? This action cannot be undone.",
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
                                scope.launch {
                                    try {
                                        onDeleteExpenses(selectedExpenses.toList())
                                        selectedExpenses = emptySet()
                                        showDeleteDialog = false
                                        showCheckboxes = false
                                        snackbarHostState.showSnackbar(
                                            message = "${selectedExpenses.size} expense(s) deleted successfully",
                                            duration = SnackbarDuration.Short
                                        )
                                    } catch (e: Exception) {
                                        Log.e("ExpenseListScreen", "Deletion failed: ${e.message}")
                                        showDeleteDialog = false
                                        snackbarHostState.showSnackbar(
                                            message = "Failed to delete expenses: ${e.message}",
                                            duration = SnackbarDuration.Long
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
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
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Delete",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        Button(
                            onClick = { showDeleteDialog = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
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
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Cancel",
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

    if (showExpenseDialog && selectedExpense != null) {
        Dialog(
            onDismissRequest = {
                showExpenseDialog = false
                selectedExpense = null
                selectedImagePath = null
                showFullScreenImage = false
                showInfoDialog = false
                Log.d("ExpenseListScreen", "Expense dialog dismissed, states cleared")
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
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${selectedExpense?.category ?: "Receipt"}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp
                        ),
                        color = Color(0xFF734656),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    selectedImagePath?.let { imagePath ->
                        var imageLoadFailed by remember { mutableStateOf(false) }
                        var isImageLoading by remember { mutableStateOf(true) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.5.dp, Color(0xFF734656), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (tokenFetchFailed) {
                                isImageLoading = false
                                Text(
                                    text = "Authentication error: Please log in again",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF4B5563),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            } else if (selectedExpense?.expenseId?.startsWith("local_") == true) {
                                val bitmap = try {
                                    val uri = Uri.parse(imagePath)
                                    BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
                                } catch (e: Exception) {
                                    Log.e("ExpenseListScreen", "Failed to load local image: $imagePath, error: ${e.message}")
                                    null
                                }
                                if (bitmap != null) {
                                    isImageLoading = false
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "${selectedExpense?.category ?: "Receipt"} receipt",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable { showFullScreenImage = true },
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    isImageLoading = false
                                    imageLoadFailed = true
                                    Text(
                                        text = "No receipt photo available",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF4B5563),
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                }
                            } else {
                                val fullImageUrl = "${ApiConfig.BASE_URL}api/expenses/${selectedExpense?.expenseId}/images"
                                Log.d("ExpenseListScreen", "Loading server image: $fullImageUrl with token: ${token?.take(20)}...")
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(fullImageUrl)
                                        .apply {
                                            if (token != null) {
                                                addHeader("Authorization", "Bearer $token")
                                            } else {
                                                imageLoadFailed = true
                                                isImageLoading = false
                                            }
                                        }
                                        .diskCacheKey(fullImageUrl)
                                        .memoryCacheKey(fullImageUrl)
                                        .build(),
                                    contentDescription = "${selectedExpense?.category ?: "Receipt"} receipt",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable { showFullScreenImage = true },
                                    contentScale = ContentScale.Crop,
                                    onLoading = { isImageLoading = true },
                                    onError = { error ->
                                        isImageLoading = false
                                        imageLoadFailed = true
                                        scope.launch {
                                            Log.e("ExpenseListScreen", "Failed to load server image: $fullImageUrl, error: ${error.result.throwable.message}")
                                            snackbarHostState.showSnackbar(
                                                message = "Failed to load receipt image: ${error.result.throwable.message}",
                                                duration = SnackbarDuration.Long
                                            )
                                            if (error.result.throwable.message?.contains("401") == true && retryCount < 2) {
                                                retryCount++
                                                val tokenResult = authRepository.getValidToken()
                                                if (tokenResult.isSuccess) {
                                                    token = tokenResult.getOrNull()
                                                    Log.d("ExpenseListScreen", "Retry token fetched: ${token?.take(20)}...")
                                                } else {
                                                    tokenFetchFailed = true
                                                    Toast.makeText(context, "Authentication error: Please log in again", Toast.LENGTH_SHORT).show()
                                                    navController.navigate("login") {
                                                        popUpTo("home") { inclusive = true }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    onSuccess = {
                                        isImageLoading = false
                                        Log.d("ExpenseListScreen", "Successfully loaded server image: $fullImageUrl")
                                    }
                                )
                            }
                            if (isImageLoading && !imageLoadFailed) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    color = themeColor,
                                    strokeWidth = 4.dp
                                )
                            }
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
                            onClick = {
                                if (selectedExpense != null) {
                                    showInfoDialog = true
                                    Log.d("ExpenseListScreen", "Info button clicked for expense: ID=${selectedExpense?.expenseId}, Category=${selectedExpense?.category}")
                                } else {
                                    Log.e("ExpenseListScreen", "Info button clicked but selectedExpense is null")
                                    Toast.makeText(context, "Error: No expense selected", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .padding(horizontal = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
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
                                            if (selectedExpense?.expenseId?.startsWith("local_") == true) {
                                                val uri = Uri.parse(imagePath)
                                                val inputStream = context.contentResolver.openInputStream(uri)
                                                val fileName = "Expense_${selectedExpense?.expenseId}_${System.currentTimeMillis()}.jpg"
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
                                                val fullImageUrl = "${ApiConfig.BASE_URL}api/expenses/${selectedExpense?.expenseId}/images"
                                                val fileName = "Expense_${selectedExpense?.expenseId}_${System.currentTimeMillis()}.jpg"
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
                                            Log.e("ExpenseListScreen", "Failed to download image: ${e.message}")
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
                                selectedExpense = null
                                selectedImagePath = null
                                showFullScreenImage = false
                                showInfoDialog = false
                                Log.d("ExpenseListScreen", "Close button clicked, expense dialog dismissed")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .padding(horizontal = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
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
        Dialog(
            onDismissRequest = {
                showFullScreenImage = false
                selectedExpense = null
                selectedImagePath = null
                showInfoDialog = false
                Log.d("ExpenseListScreen", "Full-screen image dialog dismissed, states cleared")
            },
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
                    var isImageLoading by remember { mutableStateOf(true) }
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

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (tokenFetchFailed) {
                            isImageLoading = false
                            Text(
                                text = "Authentication error: Please log in again",
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                color = Color.White,
                                modifier = Modifier.padding(16.dp)
                            )
                        } else if (selectedExpense?.expenseId?.startsWith("local_") == true) {
                            val bitmap = try {
                                val uri = Uri.parse(imagePath)
                                BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
                            } catch (e: Exception) {
                                Log.e("ExpenseListScreen", "Failed to load local full-screen image: $imagePath, error: ${e.message}")
                                null
                            }
                            if (bitmap != null) {
                                isImageLoading = false
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Full screen receipt photo",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .transformable(state = transformableState)
                                        .graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale,
                                            translationX = offset.x,
                                            translationY = offset.y
                                        )
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(2.dp, themeColor, RoundedCornerShape(12.dp))
                                        .onGloballyPositioned { coordinates ->
                                            imageSize = coordinates.size
                                            containerSize = coordinates.parentLayoutCoordinates?.size ?: IntSize.Zero
                                        },
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                isImageLoading = false
                                imageLoadFailed = true
                                Text(
                                    text = "No image available",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                    color = Color.White,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else {
                            val fullImageUrl = "${ApiConfig.BASE_URL}api/expenses/${selectedExpense?.expenseId}/images"
                            Log.d("ExpenseListScreen", "Loading full-screen server image: $fullImageUrl with token: ${token?.take(20)}...")
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(fullImageUrl)
                                    .apply {
                                        if (token != null) {
                                            addHeader("Authorization", "Bearer $token")
                                        } else {
                                            imageLoadFailed = true
                                            isImageLoading = false
                                        }
                                    }
                                    .diskCacheKey(fullImageUrl)
                                    .memoryCacheKey(fullImageUrl)
                                    .build(),
                                contentDescription = "Full screen receipt photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .transformable(state = transformableState)
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offset.x,
                                        translationY = offset.y
                                    )
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(2.dp, themeColor, RoundedCornerShape(12.dp))
                                    .onGloballyPositioned { coordinates ->
                                        imageSize = coordinates.size
                                        containerSize = coordinates.parentLayoutCoordinates?.size ?: IntSize.Zero
                                    },
                                contentScale = ContentScale.Fit,
                                onLoading = { isImageLoading = true },
                                onError = { error ->
                                    isImageLoading = false
                                    imageLoadFailed = true
                                    scope.launch {
                                        Log.e("ExpenseListScreen", "Failed to load full-screen server image: $fullImageUrl, error: ${error.result.throwable.message}")
                                        snackbarHostState.showSnackbar(
                                            message = "Failed to load full screen image",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (error.result.throwable.message?.contains("401") == true && retryCount < 2) {
                                            retryCount++
                                            val tokenResult = authRepository.getValidToken()
                                            if (tokenResult.isSuccess) {
                                                token = tokenResult.getOrNull()
                                                Log.d("ExpenseListScreen", "Retry token fetched: ${token?.take(20)}...")
                                            } else {
                                                tokenFetchFailed = true
                                                Toast.makeText(context, "Authentication error: Please log in again", Toast.LENGTH_SHORT).show()
                                                navController.navigate("login") {
                                                    popUpTo("home") { inclusive = true }
                                                }
                                            }
                                        }
                                    }
                                },
                                onSuccess = {
                                    isImageLoading = false
                                    Log.d("ExpenseListScreen", "Successfully loaded full-screen server image: $fullImageUrl")
                                }
                            )
                        }
                        if (isImageLoading && !imageLoadFailed) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(64.dp)
                                    .align(Alignment.Center),
                                color = themeColor,
                                strokeWidth = 6.dp
                            )
                        }
                    }
                } ?: Text(
                    text = "No image available",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
                IconButton(
                    onClick = {
                        showFullScreenImage = false
                        selectedExpense = null
                        selectedImagePath = null
                        showInfoDialog = false
                        Log.d("ExpenseListScreen", "Full-screen image dialog closed via button")
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

    // Info Dialog Implementation
    if (showInfoDialog && selectedExpense != null) {
        Dialog(
            onDismissRequest = {
                showInfoDialog = false
                Log.d("ExpenseListScreen", "Info dialog dismissed for expense: ${selectedExpense?.expenseId}")
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
                        .padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Expense Details",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp
                        ),
                        color = Color(0xFF734656),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = "Category: ${selectedExpense?.category ?: "N/A"}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                        color = Color(0xFF1F2937),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Remarks: ${selectedExpense?.remarks ?: "N/A"}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                        color = Color(0xFF1F2937),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Amount: ₱${selectedExpense?.amount?.let { numberFormat.format(it) } ?: "N/A"}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                        color = Color(0xFF1F2937),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Date of Transaction: ${selectedExpense?.createdAt?.let {
                            try {
                                val parsedDate = LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                parsedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))
                            } catch (e: Exception) {
                                "N/A"
                            }
                        } ?: "N/A"}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                        color = Color(0xFF1F2937),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Created At: ${selectedExpense?.createdAt?.let {
                            try {
                                val parsedDate = LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                parsedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))
                            } catch (e: Exception) {
                                "N/A"
                            }
                        } ?: "N/A"}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                        color = Color(0xFF1F2937),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            showInfoDialog = false
                            Log.d("ExpenseListScreen", "Info dialog closed via button for expense: ${selectedExpense?.expenseId}")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
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

    if (showFullScreenImage) {
        Dialog(
            onDismissRequest = {
                showFullScreenImage = false
                selectedExpense = null
                selectedImagePath = null
                showInfoDialog = false
                Log.d("ExpenseListScreen", "Full-screen image dialog dismissed, states cleared")
            },
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
                    var isImageLoading by remember { mutableStateOf(true) }
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

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (tokenFetchFailed) {
                            isImageLoading = false
                            Text(
                                text = "Authentication error: Please log in again",
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                color = Color.White,
                                modifier = Modifier.padding(16.dp)
                            )
                        } else if (selectedExpense?.expenseId?.startsWith("local_") == true) {
                            val bitmap = try {
                                val uri = Uri.parse(imagePath)
                                BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
                            } catch (e: Exception) {
                                Log.e("ExpenseListScreen", "Failed to load local full-screen image: $imagePath, error: ${e.message}")
                                null
                            }
                            if (bitmap != null) {
                                isImageLoading = false
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Full screen receipt photo",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .transformable(state = transformableState)
                                        .graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale,
                                            translationX = offset.x,
                                            translationY = offset.y
                                        )
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(2.dp, themeColor, RoundedCornerShape(12.dp))
                                        .onGloballyPositioned { coordinates ->
                                            imageSize = coordinates.size
                                            containerSize = coordinates.parentLayoutCoordinates?.size ?: IntSize.Zero
                                        },
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                isImageLoading = false
                                imageLoadFailed = true
                                Text(
                                    text = "No image available",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                    color = Color.White,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else {
                            val fullImageUrl = "${ApiConfig.BASE_URL}api/expenses/${selectedExpense?.expenseId}/images"
                            Log.d("ExpenseListScreen", "Loading full-screen server image: $fullImageUrl with token: ${token?.take(20)}...")
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(fullImageUrl)
                                    .apply {
                                        if (token != null) {
                                            addHeader("Authorization", "Bearer $token")
                                        } else {
                                            imageLoadFailed = true
                                            isImageLoading = false
                                        }
                                    }
                                    .diskCacheKey(fullImageUrl)
                                    .memoryCacheKey(fullImageUrl)
                                    .build(),
                                contentDescription = "Full screen receipt photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .transformable(state = transformableState)
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offset.x,
                                        translationY = offset.y
                                    )
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(2.dp, themeColor, RoundedCornerShape(12.dp))
                                    .onGloballyPositioned { coordinates ->
                                        imageSize = coordinates.size
                                        containerSize = coordinates.parentLayoutCoordinates?.size ?: IntSize.Zero
                                    },
                                contentScale = ContentScale.Fit,
                                onLoading = { isImageLoading = true },
                                onError = { error ->
                                    isImageLoading = false
                                    imageLoadFailed = true
                                    scope.launch {
                                        Log.e("ExpenseListScreen", "Failed to load full-screen server image: $fullImageUrl, error: ${error.result.throwable.message}")
                                        snackbarHostState.showSnackbar(
                                            message = "Failed to load full screen image",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (error.result.throwable.message?.contains("401") == true && retryCount < 2) {
                                            retryCount++
                                            val tokenResult = authRepository.getValidToken()
                                            if (tokenResult.isSuccess) {
                                                token = tokenResult.getOrNull()
                                                Log.d("ExpenseListScreen", "Retry token fetched: ${token?.take(20)}...")
                                            } else {
                                                tokenFetchFailed = true
                                                Toast.makeText(context, "Authentication error: Please log in again", Toast.LENGTH_SHORT).show()
                                                navController.navigate("login") {
                                                    popUpTo("home") { inclusive = true }
                                                }
                                            }
                                        }
                                    }
                                },
                                onSuccess = {
                                    isImageLoading = false
                                    Log.d("ExpenseListScreen", "Successfully loaded full-screen server image: $fullImageUrl")
                                }
                            )
                        }
                        if (isImageLoading && !imageLoadFailed) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(64.dp)
                                    .align(Alignment.Center),
                                color = themeColor,
                                strokeWidth = 6.dp
                            )
                        }
                    }
                } ?: Text(
                    text = "No image available",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
                IconButton(
                    onClick = {
                        showFullScreenImage = false
                        selectedExpense = null
                        selectedImagePath = null
                        showInfoDialog = false
                        Log.d("ExpenseListScreen", "Full-screen image dialog closed via button")
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

    fun Color.toHsl(): FloatArray {
        val r = this.red
        val g = this.green
        val b = this.blue
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val l = (max + min) / 2f
        val h: Float
        val s: Float

        if (max == min) {
            h = 0f
            s = 0f
        } else {
            val d = max - min
            s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
            h = when (max) {
                r -> (g - b) / d + (if (g < b) 6f else 0f)
                g -> (b - r) / d + 2f
                else -> (r - g) / d + 4f
            } * 60f
        }
        return floatArrayOf(h, s, l)
    }
}