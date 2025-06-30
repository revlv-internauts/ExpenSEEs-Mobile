package com.example.expensees.screens

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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.expensees.models.Expense
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ExpenseListScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    expenses: SnapshotStateList<Expense>,
    onDeleteExpenses: (List<Expense>) -> Unit
) {
    var selectedExpenses by remember { mutableStateOf(setOf<Expense>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var selectedExpense by remember { mutableStateOf<Expense?>(null) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var expenseImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showFullScreenImage by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    // Calendar state
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val filteredExpenses = expenses.filter { expense ->
        try {
            expense.dateOfTransaction?.let {
                LocalDate.parse(it, dateFormatter) == selectedDate
            } ?: false
        } catch (e: DateTimeParseException) {
            false
        }
    }

    // Category colors
    val categories = listOf(
        "Utilities", "Food", "Transportation", "Gas", "Office Supplies",
        "Rent", "Parking", "Electronic Supplies", "Grocery", "Other Expenses"
    )
    val categoryColors = categories.zip(
        listOf(
            Color(0xFFEF476F), // Utilities
            Color(0xFF06D6A0), // Food
            Color(0xFF118AB2), // Transportation
            Color(0xFFFFD166), // Gas
            Color(0xFFF4A261), // Office Supplies
            Color(0xFF8D5524), // Rent
            Color(0xFFC9CBA3), // Parking
            Color(0xFF6B7280), // Electronic Supplies
            Color(0xFF2E7D32), // Grocery
            Color(0xFFFFA400)  // Other Expenses
        )
    ).toMap()

    // Category icons
    val categoryIcons = categories.zip(
        listOf(
            Icons.Default.Bolt, // Utilities
            Icons.Default.Restaurant, // Food
            Icons.Default.DirectionsCar, // Transportation
            Icons.Default.LocalGasStation, // Gas
            Icons.Default.Work, // Office Supplies
            Icons.Default.Home, // Rent
            Icons.Default.LocalParking, // Parking
            Icons.Default.Devices, // Electronic Supplies
            Icons.Default.ShoppingCart, // Grocery
            Icons.Default.Category // Other Expenses
        )
    ).toMap()

    // Function to generate shades
    fun generateColorShades(baseColor: Color, count: Int): List<Color> {
        if (count <= 0) return emptyList()
        val hsl = baseColor.toHsl()
        val shades = mutableListOf<Color>()
        val lightnessStep = if (count == 1) 0f else 0.3f / (count - 1)
        for (i in 0 until count) {
            val newLightness = (0.4f + i * lightnessStep).coerceIn(0.4f, 0.7f)
            shades.add(Color.hsl(hsl[0], hsl[1], newLightness))
        }
        return shades
    }

    // Generate shades for each category
    val categoryShades = remember(selectedDate, filteredExpenses) {
        categoryColors.mapValues { (category, baseColor) ->
            val categoryExpenses = filteredExpenses.filter { it.category == category }
            generateColorShades(baseColor, categoryExpenses.size)
        }
    }

    // Custom DatePickerDialog
    val calendar = Calendar.getInstance()
    val datePickerDialog = remember {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                showDatePickerDialog = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    LaunchedEffect(showDatePickerDialog) {
        if (showDatePickerDialog) {
            datePickerDialog.show()
        } else {
            datePickerDialog.dismiss()
        }
    }

    // Mini calendar for one month
    val startDate = selectedDate.withDayOfMonth(1)
    val daysInMonth = startDate.lengthOfMonth()
    val days = (0 until daysInMonth).map { startDate.plusDays(it.toLong()) }
    val lazyListState = rememberLazyListState()

    LaunchedEffect(selectedDate) {
        val index = days.indexOf(selectedDate).coerceAtLeast(0)
        lazyListState.animateScrollToItem(index)
    }

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
                    },
                    actions = {
                        IconButton(onClick = { showDatePickerDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Select date",
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
            ) {
                // Mini calendar
                LazyRow(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(days) { day ->
                        val isSelected = day == selectedDate
                        Card(
                            modifier = Modifier
                                .size(60.dp)
                                .clickable { selectedDate = day },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    categoryColors["Utilities"] ?: Color.Gray
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = if (!isSelected) {
                                BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                            } else {
                                null
                            }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = day.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = day.dayOfWeek.toString().substring(0, 3),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    }
                                )
                            }
                        }
                    }
                }

                // Expenses list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = if (selectedExpenses.isNotEmpty()) 80.dp else 16.dp)
                ) {
                    itemsIndexed(filteredExpenses) { index, expense ->
                        val categoryIndex = filteredExpenses
                            .filter { it.category == expense.category }
                            .indexOfFirst { it == expense }
                        val baseColor = categoryColors[expense.category] ?: Color.Gray
                        val shade = categoryShades[expense.category]?.getOrNull(categoryIndex) ?: baseColor
                        val isSelected = expense in selectedExpenses
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val scale by animateFloatAsState(
                            targetValue = if (isPressed || isSelected) 1.05f else 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        )
                        val elevation by animateDpAsState(
                            targetValue = if (isPressed || isSelected) 12.dp else 6.dp,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(scale)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) {
                                    selectedExpense = expense
                                    showExpenseDialog = true
                                }
                                .graphicsLayer {
                                    shadowElevation = elevation.toPx()
                                    spotShadowColor = shade.copy(alpha = 0.5f)
                                    ambientShadowColor = shade.copy(alpha = 0.3f)
                                    clip = true
                                    shape = RoundedCornerShape(16.dp)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                shade.copy(alpha = 0.8f),
                                                shade.copy(alpha = 0.4f)
                                            ),
                                            start = Offset(0f, 0f),
                                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                        )
                                    )
                                    .border(
                                        2.dp,
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                shade,
                                                shade.copy(alpha = 0.5f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .then(
                                        if (isSelected) Modifier.background(
                                            color = Color.White.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(16.dp)
                                        ) else Modifier
                                    )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Animated Checkbox
                                    val checkboxScale by animateFloatAsState(
                                        targetValue = if (isSelected) 1.2f else 1f,
                                        animationSpec = tween(durationMillis = 200)
                                    )
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            selectedExpenses = if (checked) {
                                                selectedExpenses + expense
                                            } else {
                                                selectedExpenses - expense
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = shade,
                                            uncheckedColor = shade.copy(alpha = 0.5f),
                                            checkmarkColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier
                                            .scale(checkboxScale)
                                            .padding(4.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = categoryIcons[expense.category] ?: Icons.Default.Category,
                                                contentDescription = null,
                                                tint = shade,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = expense.category ?: "",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                modifier = Modifier
                                                    .graphicsLayer {
                                                        shadowElevation = 4f
                                                        spotShadowColor = Color.Black.copy(alpha = 0.3f)
                                                        translationX = 2f
                                                        translationY = 2f
                                                    },
                                                color = shade
                                            )
                                        }
                                        Text(
                                            text = expense.remarks ?: "",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier
                                                .graphicsLayer {
                                                    shadowElevation = 4f
                                                    spotShadowColor = Color.Black.copy(alpha = 0.3f)
                                                    translationX = 2f
                                                    translationY = 2f
                                                },
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "₱${numberFormat.format(expense.amount)}",
                                            style = MaterialTheme.typography.headlineSmall.copy(
                                                fontWeight = FontWeight.ExtraBold
                                            ),
                                            color = shade
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    expense.imagePath?.let { imagePath ->
                                        var imageLoadFailed by remember { mutableStateOf(false) }
                                        if (imageLoadFailed) {
                                            Box(
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .border(
                                                        2.dp,
                                                        shade.copy(alpha = 0.7f),
                                                        RoundedCornerShape(12.dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "No Image",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = shade,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        } else if (expense.expenseId?.startsWith("local_") == true) {
                                            val bitmap = try {
                                                val uri = Uri.parse(imagePath)
                                                BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
                                            } catch (e: Exception) {
                                                null
                                            }
                                            if (bitmap != null) {
                                                Image(
                                                    bitmap = bitmap.asImageBitmap(),
                                                    contentDescription = "Expense receipt",
                                                    modifier = Modifier
                                                        .size(56.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .border(
                                                            2.dp,
                                                            shade.copy(alpha = 0.7f),
                                                            RoundedCornerShape(12.dp)
                                                        )
                                                        .scale(if (isPressed) 1.1f else 1f)
                                                        .animateContentSize(
                                                            animationSpec = tween(200)
                                                        ),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(56.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .border(
                                                            2.dp,
                                                            shade.copy(alpha = 0.7f),
                                                            RoundedCornerShape(12.dp)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "No Image",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = shade,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        } else {
                                            AsyncImage(
                                                model = imagePath,
                                                contentDescription = "Expense receipt",
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .border(
                                                        2.dp,
                                                        shade.copy(alpha = 0.7f),
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .scale(if (isPressed) 1.1f else 1f)
                                                    .animateContentSize(
                                                        animationSpec = tween(200)
                                                    ),
                                                contentScale = ContentScale.Crop,
                                                onError = {
                                                    imageLoadFailed = true
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            message = "Failed to load receipt image",
                                                            duration = SnackbarDuration.Short
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Button row at the bottom
                if (selectedExpenses.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                            .background(Color.Transparent, shape = RoundedCornerShape(8.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ExtendedFloatingActionButton(
                                onClick = { selectedExpenses = emptySet() },
                                containerColor = categoryColors["Utilities"] ?: Color.Gray,
                                contentColor = Color.White,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Deselect all")
                                Spacer(Modifier.width(8.dp))
                                Text("Deselect All")
                            }
                            Spacer(Modifier.width(8.dp))
                            ExtendedFloatingActionButton(
                                onClick = { showDeleteDialog = true },
                                containerColor = categoryColors["Transportation"] ?: Color.Gray,
                                contentColor = Color.White,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                                Spacer(Modifier.width(8.dp))
                                Text("Delete (${selectedExpenses.size})")
                            }
                        }
                    }
                }
            }

            // Delete confirmation dialog
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Delete Expenses") },
                    text = { Text("Are you sure you want to delete ${selectedExpenses.size} expense(s)?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        onDeleteExpenses(selectedExpenses.toList())
                                        selectedExpenses = emptySet()
                                        showDeleteDialog = false
                                        snackbarHostState.showSnackbar(
                                            message = "Expenses deleted",
                                            duration = SnackbarDuration.Short
                                        )
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar(
                                            message = "Failed to delete expenses: ${e.message}",
                                            duration = SnackbarDuration.Long
                                        )
                                    }
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

            // Expense details dialog
            if (showExpenseDialog && selectedExpense != null) {
                AlertDialog(
                    onDismissRequest = {
                        showExpenseDialog = false
                        selectedExpense = null
                        expenseImageBitmap = null
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(16.dp)),
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.graphicsLayer {
                            shadowElevation = 12.dp.toPx()
                            spotShadowColor = Color.Black.copy(alpha = 0.3f)
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "${selectedExpense?.category ?: "Expense"} Receipt",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = categoryColors[selectedExpense?.category] ?: Color.Gray,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            selectedExpense?.imagePath?.let { imagePath ->
                                var imageLoadFailed by remember { mutableStateOf(false) }
                                if (imageLoadFailed) {
                                    Text(
                                        text = "No receipt photo available",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                } else if (selectedExpense?.expenseId?.startsWith("local_") == true) {
                                    val bitmap = try {
                                        val uri = Uri.parse(imagePath)
                                        BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
                                    } catch (e: Exception) {
                                        null
                                    }
                                    bitmap?.let {
                                        Image(
                                            bitmap = it.asImageBitmap(),
                                            contentDescription = "${selectedExpense?.category ?: "Expense"} receipt",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(
                                                    1.5.dp,
                                                    categoryColors[selectedExpense?.category] ?: Color.Gray,
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
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                    }
                                } else {
                                    AsyncImage(
                                        model = imagePath,
                                        contentDescription = "${selectedExpense?.category ?: "Expense"} receipt",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(
                                                1.5.dp,
                                                categoryColors[selectedExpense?.category] ?: Color.Gray,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { showFullScreenImage = true },
                                        contentScale = ContentScale.Crop,
                                        onError = {
                                            imageLoadFailed = true
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message = "Failed to load receipt image",
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
                                        }
                                    )
                                }
                            } ?: Text(
                                text = "No receipt photo available",
                                style = MaterialTheme.typography.bodyLarge,
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
                                        color = categoryColors[selectedExpense?.category] ?: Color.Gray
                                    )
                                }
                                TextButton(onClick = {
                                    showExpenseDialog = false
                                    selectedExpense = null
                                    expenseImageBitmap = null
                                }) {
                                    Text(
                                        text = "Close",
                                        color = categoryColors[selectedExpense?.category] ?: Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Expense info dialog
            if (showInfoDialog && selectedExpense != null) {
                AlertDialog(
                    onDismissRequest = {
                        showInfoDialog = false
                        selectedExpense = null
                        expenseImageBitmap = null
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(16.dp)),
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.graphicsLayer {
                            shadowElevation = 12.dp.toPx()
                            spotShadowColor = Color.Black.copy(alpha = 0.3f)
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Expense Details",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = categoryColors[selectedExpense?.category] ?: Color.Gray,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                Text(
                                    text = "Remarks: ${selectedExpense?.remarks ?: ""}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    text = "Amount: ₱${numberFormat.format(selectedExpense?.amount ?: 0.0)}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    text = "Category: ${selectedExpense?.category ?: ""}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    text = "Date of Transaction: ${selectedExpense?.dateOfTransaction ?: ""}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    text = "Created At: ${selectedExpense?.createdAt ?: ""}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
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
                                    color = categoryColors[selectedExpense?.category] ?: Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            // Full-screen image dialog
            if (showFullScreenImage) {
                AlertDialog(
                    onDismissRequest = {
                        showFullScreenImage = false
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
                        selectedExpense?.imagePath?.let { imagePath ->
                            var imageLoadFailed by remember { mutableStateOf(false) }
                            if (imageLoadFailed) {
                                Text(
                                    text = "No image available",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            } else if (selectedExpense?.expenseId?.startsWith("local_") == true) {
                                val bitmap = try {
                                    val uri = Uri.parse(imagePath)
                                    BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
                                } catch (e: Exception) {
                                    null
                                }
                                bitmap?.let {
                                    Image(
                                        bitmap = it.asImageBitmap(),
                                        contentDescription = "Full screen receipt photo",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(
                                                2.dp,
                                                MaterialTheme.colorScheme.primary,
                                                RoundedCornerShape(12.dp)
                                            ),
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
                            } else {
                                AsyncImage(
                                    model = imagePath,
                                    contentDescription = "Full screen receipt photo",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            2.dp,
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(12.dp)
                                        ),
                                    contentScale = ContentScale.Fit,
                                    onError = {
                                        imageLoadFailed = true
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "Failed to load full screen image",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    }
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
        }
    }
}

// Extension function to convert Color to HSL
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