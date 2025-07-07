package com.example.expensees.screens

import android.annotation.SuppressLint
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.example.expensees.models.BudgetStatus
import com.example.expensees.models.Expense
import com.example.expensees.models.ExpenseItem
import com.example.expensees.models.SubmittedBudget
import com.example.expensees.network.AuthRepository
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiquidationReport(
    modifier: Modifier = Modifier,
    navController: NavController,
    authRepository: AuthRepository
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedBudget by remember { mutableStateOf<SubmittedBudget?>(null) }
    var selectedCategory by remember { mutableStateOf<BudgetStatus?>(null) }
    var showExpenseSelectionDialog by remember { mutableStateOf(false) }
    var currentExpenseItem by remember { mutableStateOf<Pair<ExpenseItem, Int>?>(null) }
    val selectedExpensesMap = remember { mutableStateMapOf<Int, MutableList<Expense>>() }

    LaunchedEffect(selectedBudget) {
        if (selectedBudget == null) {
            selectedExpensesMap.clear() // Clear the map when no budget is selected
        } else {
            // Optionally, clear only entries not relevant to the new budget
            selectedExpensesMap.clear()
        }
    }

    val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
        isGroupingUsed = true
    }
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val generatedReports = remember { mutableStateListOf<String>() }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportContent by remember { mutableStateOf("") }
    var showAllBudgets by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Category colors from FundRequest
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

    // Adjusted status colors for a matte appearance
    val statusColors = mapOf(
        BudgetStatus.PENDING to Color(0xFFD4A017), // Less vibrant yellow
        BudgetStatus.APPROVED to Color(0xFF388E3C), // Less vibrant green
        BudgetStatus.DENIED to Color(0xFFD32F2F)  // Less vibrant red
    )

    val textColors = mapOf(
        BudgetStatus.PENDING to Color(0xFF4B5563),
        BudgetStatus.APPROVED to Color(0xFF1F2937),
        BudgetStatus.DENIED to Color(0xFF1F2937)
    )

    // Animation for budget cards
    val animatedScale = remember {
        mutableStateListOf<Animatable<Float, *>>().apply {
            repeat(authRepository.submittedBudgets.size) { add(Animatable(0f)) }
        }
    }
    LaunchedEffect(authRepository.submittedBudgets.size) {
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

    // Fetch budgets and expenses
    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            val budgetResult = authRepository.getBudgets()
            val expenseResult = authRepository.getExpenses()
            if (budgetResult.isFailure) {
                errorMessage = budgetResult.exceptionOrNull()?.message ?: "Failed to fetch budgets"
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            }
            if (expenseResult.isFailure) {
                errorMessage = (errorMessage ?: "") + "\n" + (expenseResult.exceptionOrNull()?.message ?: "Failed to fetch expenses")
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            }
            isLoading = false
        }
    }

    fun calculateTotalRemainingBalance(): Double {
        selectedBudget?.let { budget ->
            return budget.expenses.withIndex().sumOf { (index, expense) ->
                val budgetedAmount = expense.quantity * expense.amountPerUnit
                val actualExpenseTotal = selectedExpensesMap[index]?.sumOf { it.amount } ?: 0.0
                budgetedAmount - actualExpenseTotal
            }
        } ?: return 0.0
    }

    val totalRemainingBalance by derivedStateOf {
        calculateTotalRemainingBalance()
    }

    BackHandler(enabled = true) {
        if (selectedBudget != null) {
            selectedBudget = null
        } else if (selectedCategory != null) {
            selectedCategory = null
        } else if (showAllBudgets) {
            showAllBudgets = false
        } else {
            if (navController.currentBackStackEntry?.destination?.route != "home") {
                navController.navigate("home") {
                    popUpTo("home") { inclusive = false }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        snackbarHost = {
            SnackbarHost(
                hostState = remember { SnackbarHostState() },
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
                        }
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(horizontal = 16.dp)
                .padding(top = innerPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 50.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (selectedBudget != null) {
                            selectedBudget = null
                        } else if (selectedCategory != null) {
                            selectedCategory = null
                        } else if (showAllBudgets) {
                            showAllBudgets = false
                        } else {
                            if (navController.currentBackStackEntry?.destination?.route != "home") {
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = false }
                                }
                            }
                        }
                    },
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
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Liquidation Report",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = Color(0xFF1F2937),
                    modifier = Modifier
                        .weight(1f)
                        .offset(x = (-18).dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterHorizontally),
                    color = Color(0xFF3B82F6)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading budgets...",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp
                    ),
                    color = Color(0xFF4B5563),
                    textAlign = TextAlign.Center
                )
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp
                    ),
                    color = Color(0xFFF44336),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        coroutineScope.launch {
                            val budgetResult = authRepository.getBudgets()
                            val expenseResult = authRepository.getExpenses()
                            if (budgetResult.isFailure) {
                                errorMessage = budgetResult.exceptionOrNull()?.message ?: "Failed to fetch budgets"
                                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                            }
                            if (expenseResult.isFailure) {
                                errorMessage = (errorMessage ?: "") + "\n" + (expenseResult.exceptionOrNull()?.message ?: "Failed to fetch expenses")
                                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                            }
                            isLoading = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF734656)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 2.dp
                    )
                ) {
                    Text(
                        text = "Retry",
                        fontSize = 16.sp,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (authRepository.submittedBudgets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No budget requests submitted PURPLE submitted yet.",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp
                        ),
                        color = Color(0xFF4B5563),
                        textAlign = TextAlign.Center
                    )
                }
            } else if (selectedBudget == null) {
                if (selectedCategory == null && !showAllBudgets) {
                    Text(
                        text = "Select a Budget Category",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        ),
                        color = Color(0xFF1F2937),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    // Category Buttons
                    val pendingCount = authRepository.submittedBudgets.count { it.status == BudgetStatus.PENDING }
                    val approvedCount = authRepository.submittedBudgets.count { it.status == BudgetStatus.APPROVED }
                    val deniedCount = authRepository.submittedBudgets.count { it.status == BudgetStatus.DENIED }

                    // Pending Budgets Button
                    Button(
                        onClick = { selectedCategory = BudgetStatus.PENDING },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD4A017), // Matte yellow
                            disabledContainerColor = Color(0xFFD4A017).copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = pendingCount > 0,
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 2.dp
                        )
                    ) {
                        Text(
                            text = "Pending Budgets ($pendingCount)",
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Approved Budgets Button
                    Button(
                        onClick = { selectedCategory = BudgetStatus.APPROVED },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF388E3C), // Matte green
                            disabledContainerColor = Color(0xFF388E3C).copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = approvedCount > 0,
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 2.dp
                        )
                    ) {
                        Text(
                            text = "Approved Budgets ($approvedCount)",
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Denied Budgets Button
                    Button(
                        onClick = { selectedCategory = BudgetStatus.DENIED },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F), // Matte red
                            disabledContainerColor = Color(0xFFD32F2F).copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = deniedCount > 0,
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 2.dp
                        )
                    ) {
                        Text(
                            text = "Denied Budgets ($deniedCount)",
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }

                    // View All Budgets Button
                    Button(
                        onClick = { showAllBudgets = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4C2F3A) // Matte version of 0xFF5D3A49
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 2.dp
                        )
                    ) {
                        Text(
                            text = "View All Budgets",
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (showAllBudgets) {
                    Text(
                        text = "All Budget Requests",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        ),
                        color = Color(0xFF1F2937),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Button(
                        onClick = { showAllBudgets = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(bottom = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF734656)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 2.dp
                        )
                    ) {
                        Text(
                            text = "Back to Categories",
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                    if (authRepository.submittedBudgets.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No budget requests available.",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 18.sp
                                ),
                                color = Color(0xFF4B5563),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(authRepository.submittedBudgets) { budget ->
                                val index = authRepository.submittedBudgets.indexOf(budget)
                                val scale by animatedScale.getOrNull(index)?.asState() ?: remember { mutableStateOf(1f) }
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .scale(scale)
                                        .then(
                                            if (budget.status == BudgetStatus.APPROVED) {
                                                Modifier.clickable { selectedBudget = budget }
                                            } else {
                                                Modifier
                                            }
                                        ),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Transparent
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(
                                                        (statusColors[budget.status] ?: Color(0xFF6B4E38)).copy(alpha = 0.1f),
                                                        (statusColors[budget.status] ?: Color(0xFF6B4E38)).copy(alpha = 0.03f)
                                                    ),
                                                    start = Offset(0f, 0f),
                                                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                                )
                                            )
                                            .border(
                                                1.dp,
                                                (statusColors[budget.status] ?: Color(0xFF6B4E38)).copy(alpha = 0.3f),
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
                                                color = statusColors[budget.status] ?: Color(0xFF6B4E38),
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
                                                    text = budget.name,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 14.sp
                                                    ),
                                                    color = Color(0xFF1F2937),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "₱${numberFormat.format(budget.total)}",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontSize = 12.sp
                                                    ),
                                                    color = Color(0xFF4B5563),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "Status: ${budget.status.name.lowercase(Locale.US).replaceFirstChar { it.uppercase() }}",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontSize = 12.sp
                                                    ),
                                                    color = Color(0xFF4B5563),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(statusColors[budget.status] ?: Color(0xFF6B4E38))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val category = selectedCategory!!
                    Text(
                        text = "${category.name.lowercase(Locale.US).replaceFirstChar { it.uppercase() }} Budget Requests",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        ),
                        color = Color(0xFF1F2937),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Button(
                        onClick = { selectedCategory = null },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(bottom = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF734656)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 2.dp
                        )
                    ) {
                        Text(
                            text = "Back to Categories",
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                    val filteredBudgets = authRepository.submittedBudgets.filter { it.status == category }
                    if (filteredBudgets.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No ${category.name.lowercase(Locale.US).replaceFirstChar { it.uppercase() }} budget requests.",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 18.sp
                                ),
                                color = Color(0xFF4B5563),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filteredBudgets) { budget ->
                                val index = filteredBudgets.indexOf(budget)
                                val scale by animatedScale.getOrNull(index)?.asState() ?: remember { mutableStateOf(1f) }
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .scale(scale)
                                        .then(
                                            if (budget.status == BudgetStatus.APPROVED) {
                                                Modifier.clickable { selectedBudget = budget }
                                            } else {
                                                Modifier
                                            }
                                        ),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Transparent
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(
                                                        (statusColors[budget.status] ?: Color(0xFF6B4E38)).copy(alpha = 0.1f),
                                                        (statusColors[budget.status] ?: Color(0xFF6B4E38)).copy(alpha = 0.03f)
                                                    ),
                                                    start = Offset(0f, 0f),
                                                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                                )
                                            )
                                            .border(
                                                1.dp,
                                                (statusColors[budget.status] ?: Color(0xFF6B4E38)).copy(alpha = 0.3f),
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
                                                color = statusColors[budget.status] ?: Color(0xFF6B4E38),
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
                                                    text = budget.name,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 14.sp
                                                    ),
                                                    color = Color(0xFF1F2937),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "₱${numberFormat.format(budget.total)}",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontSize = 12.sp
                                                    ),
                                                    color = Color(0xFF4B5563),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "Status: ${budget.status.name.lowercase(Locale.US).replaceFirstChar { it.uppercase() }}",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontSize = 12.sp
                                                    ),
                                                    color = Color(0xFF4B5563),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(statusColors[budget.status] ?: Color(0xFF6B4E38))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                selectedBudget?.let { budget ->
                    Text(
                        text = "Liquidation Report: ${budget.name}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        ),
                        color = Color(0xFF1F2937),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Status: ${budget.status.name.lowercase(Locale.US).replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            ),
                            color = Color(0xFF4B5563),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(statusColors[budget.status] ?: Color(0xFF6B4E38))
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Total Budgeted Expenses: ₱${numberFormat.format(budget.total)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        ),
                        color = Color(0xFF1F2937),
                        modifier = Modifier.padding(bottom = 6.dp),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Expense Details",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        ),
                        color = Color(0xFF1F2937),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(budget.expenses.withIndex().toList()) { (index, expense) ->
                            val actualExpenseTotal = selectedExpensesMap[index]?.sumOf { it.amount } ?: 0.0
                            val budgetedAmount = expense.quantity * expense.amountPerUnit
                            val remainingBalance = budgetedAmount - actualExpenseTotal
                            val categoryColor = categoryColors[expense.category] ?: Color(0xFF6B4E38)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(1f),
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
                                                text = expense.category,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp
                                                ),
                                                color = Color(0xFF1F2937),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Budgeted: ₱${numberFormat.format(budgetedAmount)}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 12.sp
                                                ),
                                                color = Color(0xFF4B5563),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Actual: ₱${numberFormat.format(actualExpenseTotal)}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 12.sp
                                                ),
                                                color = Color(0xFF4B5563),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Remaining: ₱${numberFormat.format(remainingBalance)}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 12.sp
                                                ),
                                                color = if (remainingBalance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (expense.remarks.isNotBlank()) {
                                                Text(
                                                    text = "Remarks: ${expense.remarks}",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontSize = 12.sp
                                                    ),
                                                    color = Color(0xFF4B5563),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            selectedExpensesMap[index]?.let { selectedExpenses ->
                                                if (selectedExpenses.isNotEmpty()) {
                                                    Text(
                                                        text = "Selected Receipts:",
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        ),
                                                        color = Color(0xFF1F2937)
                                                    )
                                                    selectedExpenses.forEach { selected ->
                                                        Text(
                                                            text = "${selected.remarks ?: "No remarks"}: ₱${numberFormat.format(selected.amount)}",
                                                            style = MaterialTheme.typography.bodySmall.copy(
                                                                fontSize = 12.sp
                                                            ),
                                                            color = Color(0xFF4B5563),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    currentExpenseItem = expense to index
                                                    showExpenseSelectionDialog = true
                                                },
                                                modifier = Modifier
                                                    .size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Upload receipt",
                                                    tint = Color(0xFF1F2937)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = {
                                                    selectedExpensesMap[index] = mutableListOf()
                                                    Toast.makeText(
                                                        context,
                                                        "All receipts removed for ${expense.category}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                },
                                                modifier = Modifier
                                                    .size(36.dp),
                                                enabled = selectedExpensesMap[index]?.isNotEmpty() ?: false
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete receipts",
                                                    tint = if (selectedExpensesMap[index]?.isNotEmpty() ?: false)
                                                        Color(0xFFF44336)
                                                    else
                                                        Color(0xFF4B5563).copy(alpha = 0.38f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            if (totalRemainingBalance >= 0) Color(0xFF4CAF50).copy(alpha = 0.1f)
                                            else Color(0xFFF44336).copy(alpha = 0.1f),
                                            if (totalRemainingBalance >= 0) Color(0xFF4CAF50).copy(alpha = 0.03f)
                                            else Color(0xFFF44336).copy(alpha = 0.03f)
                                        ),
                                        start = Offset(0f, 0f),
                                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                    )
                                )
                                .border(
                                    1.dp,
                                    if (totalRemainingBalance >= 0) Color(0xFF4CAF50).copy(alpha = 0.3f)
                                    else Color(0xFFF44336).copy(alpha = 0.3f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Text(
                                text = if (totalRemainingBalance >= 0) {
                                    "Total Remaining Credit: ₱${numberFormat.format(totalRemainingBalance)}"
                                } else {
                                    "Total Over Budget: ₱${numberFormat.format(-totalRemainingBalance)}"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 18.sp
                                ),
                                color = if (totalRemainingBalance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp, top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { selectedBudget = null },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .padding(horizontal = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE5E7EB),
                                contentColor = Color(0xFF3B82F6)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 2.dp
                            )
                        ) {
                            Text(
                                text = "Back to Budgets",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                        Button(
                            onClick = {
                                val reportBuilder = StringBuilder()
                                reportBuilder.append("# Liquidation Report: ${budget.name}\n\n")
                                reportBuilder.append("**Status**: ${budget.status.name.lowercase(Locale.US).replaceFirstChar { it.uppercase() }}\n")
                                reportBuilder.append("**Total Budgeted Amount**: ₱${numberFormat.format(budget.total)}\n")
                                val budgetRemainingBalance = budget.expenses.withIndex().sumOf { (index, expense) ->
                                    val budgetedAmount = expense.quantity * expense.amountPerUnit
                                    val actualExpenseTotal = selectedExpensesMap[index]?.sumOf { it.amount } ?: 0.0
                                    budgetedAmount - actualExpenseTotal
                                }
                                reportBuilder.append("**Total Remaining Balance**: ₱${numberFormat.format(budgetRemainingBalance)}\n\n")
                                reportBuilder.append("## Expense Details\n\n")

                                budget.expenses.forEachIndexed { index, expense ->
                                    val actualExpenseTotal = selectedExpensesMap[index]?.sumOf { it.amount } ?: 0.0
                                    val budgetedAmount = expense.quantity * expense.amountPerUnit
                                    val remainingBalance = budgetedAmount - actualExpenseTotal

                                    reportBuilder.append("### ${expense.category}\n")
                                    reportBuilder.append("- **Budgeted Amount**: ₱${numberFormat.format(budgetedAmount)}\n")
                                    reportBuilder.append("- **Quantity**: ${expense.quantity}\n")
                                    reportBuilder.append("- **Unit Price**: ₱${numberFormat.format(expense.amountPerUnit)}\n")
                                    reportBuilder.append("- **Actual Expenses**: ₱${numberFormat.format(actualExpenseTotal)}\n")
                                    reportBuilder.append("- **Remaining Balance**: ₱${numberFormat.format(remainingBalance)}\n")
                                    if (expense.remarks.isNotBlank()) {
                                        reportBuilder.append("- **Remarks**: ${expense.remarks}\n")
                                    }
                                    selectedExpensesMap[index]?.let { selectedExpenses ->
                                        if (selectedExpenses.isNotEmpty()) {
                                            reportBuilder.append("- **Uploaded Receipts**:\n")
                                            selectedExpenses.forEach { selected ->
                                                reportBuilder.append("  - ${selected.remarks ?: "No remarks"}: ₱${numberFormat.format(selected.amount)} (Date: ${selected.dateOfTransaction?.let { OffsetDateTime.parse(it).format(dateFormatter) } ?: "Unknown"})\n")
                                            }
                                        }
                                    }
                                    reportBuilder.append("\n")
                                }

                                reportContent = reportBuilder.toString()
                                generatedReports.add(reportContent)
                                showReportDialog = true
                                Toast.makeText(
                                    context,
                                    "Liquidation Report Generated for ${budget.name}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .padding(horizontal = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF734656)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 2.dp
                            )
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RequestQuote,
                                    contentDescription = "Generate Report",
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Generate Report",
                                    fontSize = 16.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showExpenseSelectionDialog && currentExpenseItem != null) {
            val filteredExpenses = authRepository.userExpenses.filter { it.category == currentExpenseItem!!.first.category }
            val checkedExpenses = remember { mutableStateMapOf<Expense, Boolean>() }
            LaunchedEffect(filteredExpenses, currentExpenseItem) {
                filteredExpenses.forEach { expense ->
                    checkedExpenses[expense] = selectedExpensesMap[currentExpenseItem!!.second]?.contains(expense) ?: false
                }
            }

            val currentIndex = currentExpenseItem!!.second
            val currentExpense = currentExpenseItem!!.first
            val budgetedAmount = currentExpense.quantity * currentExpense.amountPerUnit
            val potentialActualTotal = derivedStateOf {
                checkedExpenses.filter { it.value }.keys.sumOf { it.amount }
            }
            val dialogAlpha by animateFloatAsState(
                targetValue = if (showExpenseSelectionDialog) 1f else 0f,
                animationSpec = tween(300, easing = LinearOutSlowInEasing)
            )

            Dialog(
                onDismissRequest = {
                    showExpenseSelectionDialog = false
                    currentExpenseItem = null
                    checkedExpenses.clear()
                },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.85f)
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
                                text = "Select Receipts for ${currentExpenseItem!!.first.category}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 20.sp
                                ),
                                color = Color(0xFF3B82F6)
                            )
                            IconButton(
                                onClick = {
                                    showExpenseSelectionDialog = false
                                    currentExpenseItem = null
                                    checkedExpenses.clear()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        Color(0xFFE5E7EB),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close dialog",
                                    tint = Color(0xFF4B5563)
                                )
                            }
                        }
                        if (filteredExpenses.isEmpty()) {
                            Text(
                                text = "No transactions recorded yet for ${currentExpenseItem!!.first.category}.",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 18.sp
                                ),
                                color = Color(0xFF4B5563),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            val potentialRemaining = budgetedAmount - potentialActualTotal.value
                            if (potentialRemaining < 0) {
                                Text(
                                    text = "Warning: Selection exceeds budget by ₱${numberFormat.format(-potentialRemaining)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    ),
                                    color = Color(0xFFF44336),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(filteredExpenses) { expense ->
                                    val isUsed = selectedExpensesMap.any { (idx, exps) ->
                                        idx != currentExpenseItem!!.second && exps.contains(expense)
                                    }
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .scale(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.Transparent
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    brush = Brush.linearGradient(
                                                        colors = listOf(
                                                            (categoryColors[expense.category] ?: Color(0xFF6B4E38)).copy(alpha = 0.1f),
                                                            (categoryColors[expense.category] ?: Color(0xFF6B4E38)).copy(alpha = 0.03f)
                                                        ),
                                                        start = Offset(0f, 0f),
                                                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                                    )
                                                )
                                                .border(
                                                    1.dp,
                                                    (categoryColors[expense.category] ?: Color(0xFF6B4E38)).copy(alpha = 0.3f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = checkedExpenses[expense] ?: false,
                                                    onCheckedChange = { isChecked ->
                                                        checkedExpenses[expense] = isChecked
                                                    },
                                                    modifier = Modifier.padding(end = 8.dp),
                                                    enabled = !isUsed || (checkedExpenses[expense] ?: false),
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = Color(0xFF3B82F6),
                                                        uncheckedColor = Color(0xFF4B5563),
                                                        checkmarkColor = Color.White
                                                    )
                                                )
                                                Column(
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = expense.remarks ?: "No remarks",
                                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                                fontWeight = FontWeight.SemiBold,
                                                                fontSize = 14.sp
                                                            ),
                                                            color = if (isUsed && !(checkedExpenses[expense] ?: false))
                                                                Color(0xFF4B5563).copy(alpha = 0.6f)
                                                            else
                                                                Color(0xFF1F2937),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        if (isUsed) {
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Icon(
                                                                imageVector = Icons.Default.CheckCircle,
                                                                contentDescription = "Receipt Used",
                                                                tint = Color(0xFF3B82F6),
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = "₱${numberFormat.format(expense.amount)}",
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontSize = 12.sp
                                                        ),
                                                        color = Color(0xFF4B5563),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "Category: ${expense.category}",
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontSize = 12.sp
                                                        ),
                                                        color = Color(0xFF4B5563),
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
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(
                                onClick = {
                                    showExpenseSelectionDialog = false
                                    currentExpenseItem = null
                                    checkedExpenses.clear()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .padding(end = 8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF3B82F6)
                                ),
                                border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Close",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Button(
                                onClick = {
                                    val expenseList = selectedExpensesMap.getOrPut(currentExpenseItem!!.second) { mutableListOf() }
                                    expenseList.clear()
                                    val newSelections = filteredExpenses.filter { checkedExpenses[it] == true }
                                    // Check for conflicts with other budgets
                                    val conflictingExpenses = newSelections.filter { expense ->
                                        selectedExpensesMap.any { (idx, exps) ->
                                            idx != currentExpenseItem!!.second && exps.contains(expense)
                                        }
                                    }
                                    if (conflictingExpenses.isNotEmpty()) {
                                        Toast.makeText(
                                            context,
                                            "Cannot select receipts already used in other budgets",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        expenseList.addAll(newSelections)
                                        selectedExpensesMap[currentExpenseItem!!.second] = expenseList
                                        if (newSelections.isNotEmpty()) {
                                            Toast.makeText(
                                                context,
                                                "${newSelections.size} receipt(s) selected",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        showExpenseSelectionDialog = false
                                        currentExpenseItem = null
                                        checkedExpenses.clear()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .padding(start = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF734656),
                                    disabledContainerColor = Color(0xFF734656).copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                enabled = filteredExpenses.isNotEmpty()
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Confirm",
                                        modifier = Modifier.size(24.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Confirm",
                                        fontSize = 16.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showReportDialog) {
            val dialogAlpha by animateFloatAsState(
                targetValue = if (showReportDialog) 1f else 0f,
                animationSpec = tween(300, easing = LinearOutSlowInEasing)
            )
            Dialog(
                onDismissRequest = { showReportDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.85f)
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
                                text = "Liquidation Report: ${selectedBudget?.name}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 20.sp
                                ),
                                color = Color(0xFF3B82F6)
                            )
                            IconButton(
                                onClick = { showReportDialog = false },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        Color(0xFFE5E7EB),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close dialog",
                                    tint = Color(0xFF4B5563)
                                )
                            }
                        }
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            item {
                                Text(
                                    text = reportContent,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp
                                    ),
                                    color = Color(0xFF1F2937)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { showReportDialog = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF3B82F6)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Close",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}