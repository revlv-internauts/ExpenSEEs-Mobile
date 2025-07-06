package com.example.expensees.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.example.expensees.models.BudgetStatus
import com.example.expensees.models.ExpenseItem
import com.example.expensees.models.SubmittedBudget
import com.example.expensees.network.AuthRepository
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FundRequest(
    modifier: Modifier = Modifier,
    navController: NavController,
    authRepository: AuthRepository
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
    var expanded by remember { mutableStateOf(false) }

    // Number formatter for comma-separated amounts
    val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
        isGroupingUsed = true
    }

    // Categories and colors (aligned with HomeScreen)
    val categories = listOf(
        "Utilities", "Food", "Transportation", "Gas", "Office Supplies",
        "Rent", "Parking", "Electronic Supplies", "Grocery", "Other Expenses"
    )
    val colorList = listOf(
        Color(0xFF6B4E38), // Brown
        Color(0xFFE7685D), // Coral Red
        Color(0xFFFBBD92), // Light Peach
        Color(0xFF4CAF50), // Green
        Color(0xFF2196F3), // Blue
        Color(0xFFFF9800), // Orange
        Color(0xFFE28743), // Orange
        Color(0xFF009688), // Teal
        Color(0xFFFF5722), // Deep Orange
        Color(0xFF607D8B)  // Blue Grey
    )
    val categoryColors = categories.zip(colorList).toMap()

    // Calculate total expenses
    val totalExpenses = expenses.sumOf { it.quantity * it.amountPerUnit }

    // Animation for expense cards
    val animatedScale = remember {
        mutableStateListOf<Animatable<Float, *>>().apply {
            repeat(expenses.size) { add(Animatable(0f)) }
        }
    }
    LaunchedEffect(expenses.size) {
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

    // Define common text field colors
    val textFieldColors = TextFieldDefaults.outlinedTextFieldColors(
        focusedBorderColor = Color(0xFF3B82F6),
        unfocusedBorderColor = Color(0xFFE5E7EB),
        focusedLabelColor = Color(0xFF3B82F6),
        unfocusedLabelColor = Color(0xFF4B5563),
        focusedTextColor = Color(0xFF1F2937),
        unfocusedTextColor = Color(0xFF1F2937) // Optional if you want same color
    )


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Request Fund",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 28.sp
                        ),
                        color = Color(0xFF1F2937),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .background(Color(0xFFE5E7EB), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF1F2937)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF5F5F5),
                    titleContentColor = Color(0xFF1F2937)
                )
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                snackbar = { snackbarData ->
                    Snackbar(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF5F5F5))
                            .padding(12.dp),
                        containerColor = Color(0xFFF5F5F5),
                        contentColor = Color(0xFF1F2937),
                        shape = RoundedCornerShape(12.dp),
                        content = {
                            Text(
                                text = snackbarData.visuals.message,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp
                                ),
                                color = Color(0xFF1F2937),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        },
                        action = {
                            snackbarData.visuals.actionLabel?.let { label ->
                                TextButton(
                                    onClick = { snackbarData.performAction() },
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(Color(0xFF734656), Color(0xFF8A5B6E)),
                                                start = Offset(0f, 0f),
                                                end = Offset(Float.POSITIVE_INFINITY, 0f)
                                            )
                                        )
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontSize = 16.sp
                                        )
                                    )
                                }
                            }
                        }
                    )
                }
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color(0xFFF5F5F5))
            )
        },
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
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
                label = { Text("Budget Name (Purpose)", color = Color(0xFF4B5563)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                singleLine = true,
                colors = textFieldColors,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
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
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    ),
                    color = Color(0xFF1F2937)
                )
                IconButton(
                    onClick = { showDialog = true },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFFE5E7EB))
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Expense",
                        tint = Color(0xFF1F2937)
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
                    val index = expenses.indexOf(expense)
                    val scale by animatedScale.getOrNull(index)?.asState() ?: remember { mutableStateOf(1f) }
                    val categoryColor = categoryColors[expense.category] ?: Color(0xFF6B4E38)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(scale)
                            .clip(RoundedCornerShape(12.dp)),
                        color = Color(0xFFF5F5F5),
                        shadowElevation = 4.dp
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
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = expense.category,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 16.sp
                                        ),
                                        color = Color(0xFF1F2937),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "₱${numberFormat.format(expense.quantity * expense.amountPerUnit)}",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 16.sp
                                        ),
                                        color = categoryColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Quantity: ${expense.quantity}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.sp
                                        ),
                                        color = Color(0xFF4B5563)
                                    )
                                    Text(
                                        text = "Unit Price: ₱${numberFormat.format(expense.amountPerUnit)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.sp
                                        ),
                                        color = Color(0xFF4B5563)
                                    )
                                }
                                if (expense.remarks.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Remarks: ${expense.remarks}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.sp
                                        ),
                                        color = Color(0xFF4B5563),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Total Expenses
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp)),
                color = Color(0xFFF5F5F5),
                shadowElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF734656).copy(alpha = 0.1f),
                                    Color(0xFF8A5B6E).copy(alpha = 0.03f)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            )
                        )
                        .border(
                            1.dp,
                            Color(0xFFE5E7EB),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Total Expenses: ₱${numberFormat.format(totalExpenses)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        ),
                        color = Color(0xFF1F2937),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Request Budget Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp)),
                color = Color.Transparent,
                shadowElevation = 4.dp,
                onClick = {
                    if (budgetName.isNotBlank() && expenses.isNotEmpty()) {
                        val submittedBudgetName = budgetName
                        val budget = SubmittedBudget(
                            budgetId = null, // Server assigns budgetId
                            name = budgetName,
                            expenses = expenses.toList(),
                            total = totalExpenses,
                            status = BudgetStatus.PENDING
                        )
                        coroutineScope.launch {
                            try {
                                val result = authRepository.addBudget(budget)
                                result.onSuccess { returnedBudget ->
                                    snackbarHostState.showSnackbar(
                                        message = "Budget request for $submittedBudgetName submitted successfully!",
                                        actionLabel = "OK",
                                        duration = SnackbarDuration.Short
                                    )
                                    budgetName = ""
                                    expenses.clear()
                                    navController.popBackStack()
                                }.onFailure { e ->
                                    snackbarHostState.showSnackbar(
                                        message = "Failed to submit budget: ${e.message}",
                                        actionLabel = "OK",
                                        duration = SnackbarDuration.Long
                                    )
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(
                                    message = "Failed to submit budget: ${e.message}",
                                    actionLabel = "OK",
                                    duration = SnackbarDuration.Long
                                )
                            }
                        }
                    }
                }
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
                    Text(
                        text = "Request Budget",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        ),
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Add Expense Dialog
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
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
                            text = "Add Expense",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 20.sp
                            ),
                            color = Color(0xFF3B82F6),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = category,
                                    onValueChange = { },
                                    label = { Text("Expense Category", color = Color(0xFF4B5563)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp)),
                                    readOnly = true,
                                    interactionSource = remember { MutableInteractionSource() }.also { interactionSource ->
                                        LaunchedEffect(interactionSource) {
                                            interactionSource.interactions.collect { interaction ->
                                                if (interaction is PressInteraction.Press) {
                                                    expanded = true
                                                }
                                            }
                                        }
                                    },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                            contentDescription = if (expanded) "Collapse" else "Expand",
                                            tint = Color(0xFF1F2937)
                                        )
                                    },
                                    colors = textFieldColors,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
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
                                            text = { Text(
                                                text = option,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                color = Color(0xFF1F2937)
                                            ) },
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
                                value = quantity,
                                onValueChange = { quantity = it.filter { c -> c.isDigit() } },
                                label = { Text("Quantity", color = Color(0xFF4B5563)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp)),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = textFieldColors,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            OutlinedTextField(
                                value = amountPerUnit,
                                onValueChange = { amountPerUnit = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("Amount per Unit (₱)", color = Color(0xFF4B5563)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp)),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = textFieldColors,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            OutlinedTextField(
                                value = remarks,
                                onValueChange = { remarks = it },
                                label = { Text("Remarks", color = Color(0xFF4B5563)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp)),
                                colors = textFieldColors,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = "Total: ₱${numberFormat.format(
                                    (quantity.toIntOrNull() ?: 0) * (amountPerUnit.toDoubleOrNull() ?: 0.0))}",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color(0xFF1F2937)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = { showDialog = false },
                                modifier = Modifier
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(Color(0xFFE5E7EB), Color(0xFFD1D5DB)),
                                            start = Offset(0f, 0f),
                                            end = Offset(Float.POSITIVE_INFINITY, 0f)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Cancel",
                                    color = Color(0xFF3B82F6),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 16.sp
                                    )
                                )
                            }
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp)),
                                color = Color.Transparent,
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
                                enabled = category.isNotBlank() && quantity.isNotBlank() && amountPerUnit.isNotBlank()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(Color(0xFF734656), Color(0xFF8A5B6E)),
                                                start = Offset(0f, 0f),
                                                end = Offset(Float.POSITIVE_INFINITY, 0f)
                                            )
                                        )
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Add",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium
                                        )
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