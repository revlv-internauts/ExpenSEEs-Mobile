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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expensees.models.LiquidationReportData

// ViewModel to hold selectedExpensesMap state
data class ReportRecord(val budgetName: String, val timestamp: String)

class LiquidationViewModel : ViewModel() {
    val selectedExpensesMap = mutableStateMapOf<Int, MutableList<Expense>>()
    val generatedReports = mutableStateListOf<ReportRecord>()

    fun clearSelections() {
        selectedExpensesMap.clear()
    }

    fun updateSelections(index: Int, expenses: List<Expense>) {
        selectedExpensesMap[index] = mutableListOf<Expense>().apply { addAll(expenses) }
        if (expenses.isEmpty()) {
            selectedExpensesMap.remove(index)
        }
    }

    fun addGeneratedReport(budgetName: String, timestamp: String) {
        generatedReports.add(ReportRecord(budgetName, timestamp))
    }
}

@SuppressLint("UnrememberedMutableState")
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiquidationReport(
    modifier: Modifier = Modifier,
    navController: NavController,
    authRepository: AuthRepository,
    viewModel: LiquidationViewModel = viewModel(),
    budgetId: String? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedBudget by remember { mutableStateOf<SubmittedBudget?>(null) }
    var showExpenseSelectionDialog by remember { mutableStateOf(false) }
    var currentExpenseItem by remember { mutableStateOf<Pair<ExpenseItem, Int>?>(null) }
    val selectedExpensesMap = viewModel.selectedExpensesMap
    val checkedExpenses = remember { mutableStateMapOf<Expense, Boolean>() }

    LaunchedEffect(budgetId) {
        if (budgetId != null) {
            val budget = authRepository.submittedBudgets.find { it.budgetId == budgetId }
            if (budget != null && budget.status == BudgetStatus.RELEASED) {
                selectedBudget = budget
                viewModel.clearSelections()
            } else {
                Toast.makeText(context, "Budget not found or not released", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            }
        }
    }

    LaunchedEffect(selectedBudget) {
        if (selectedBudget == null) {
            viewModel.clearSelections()
        }
    }

    val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
        isGroupingUsed = true
    }
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    var showReportDialog by remember { mutableStateOf(false) }
    var reportContent by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val categories = listOf(
        "Utilities", "Food", "Transportation", "Gas", "Office Supplies",
        "Rent", "Parking", "Electronic Supplies", "Grocery", "Other Expenses"
    )
    val colorList = listOf(
        Color(0xFF6B4E38), Color(0xFFE7685D), Color(0xFFFBBD92), Color(0xFF4CAF50),
        Color(0xFF2196F3), Color(0xFFFF9800), Color(0xFFE28743), Color(0xFF009688),
        Color(0xFFFF5722), Color(0xFF607D8B)
    )
    val categoryColors = categories.zip(colorList).toMap()

    val statusColors = mapOf(
        BudgetStatus.PENDING to Color(0xFFD4A017),
        BudgetStatus.RELEASED to Color(0xFF388E3C),
        BudgetStatus.DENIED to Color(0xFFD32F2F)
    )

    val textColors = mapOf(
        BudgetStatus.PENDING to Color(0xFF4B5563),
        BudgetStatus.RELEASED to Color(0xFF1F2937),
        BudgetStatus.DENIED to Color(0xFF1F2937)
    )

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
        selectedExpensesMap.entries.size
        calculateTotalRemainingBalance()
    }

    BackHandler(enabled = true) {
        if (showExpenseSelectionDialog) {
            showExpenseSelectionDialog = false
            currentExpenseItem = null
            checkedExpenses.clear()
        } else {
            navController.navigate("requested_budgets") {
                popUpTo("requested_budgets") { inclusive = false }
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 50.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        navController.navigate("requested_budgets") {
                            popUpTo("requested_budgets") { inclusive = false }
                        }
                    },
                    modifier = Modifier
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
                        text = "No budget requests submitted yet.",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp
                        ),
                        color = Color(0xFF4B5563),
                        textAlign = TextAlign.Center
                    )
                }
            } else if (selectedBudget == null) {
                Text(
                    text = "Select a Budget",
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
                    items(authRepository.submittedBudgets) { budget ->
                        val index = authRepository.submittedBudgets.indexOf(budget)
                        val scale by animatedScale.getOrNull(index)?.asState() ?: remember { mutableStateOf(1f) }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(scale)
                                .then(
                                    if (budget.status == BudgetStatus.RELEASED) {
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
                                                    viewModel.updateSelections(index, emptyList())
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
                                                        Color(0xFF4B5563).copy(alpha = 0.5f)
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
                        OutlinedButton(
                            onClick = {
                                navController.navigate("requested_budgets") {
                                    popUpTo("requested_budgets") { inclusive = false }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .padding(horizontal = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF1F2937),
                                disabledContentColor = Color(0xFF1F2937).copy(alpha = 0.5f)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF734656)),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp
                            )
                        ) {
                            Text(
                                text = "Back to Budgets",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                color = Color(0xFF1F2937)
                            )
                        }
                        Button(
                            onClick = {
                                if (selectedExpensesMap.isEmpty() || selectedExpensesMap.all { it.value.isEmpty() }) {
                                    Toast.makeText(
                                        context,
                                        "Please upload at least one receipt to generate the report",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    isLoading = true
                                    coroutineScope.launch {
                                        try {
                                            // Create LiquidationReportData
                                            val totalActual = selectedExpensesMap.values.flatten().sumOf { it.amount }
                                            val reportData = LiquidationReportData(
                                                budgetId = budget.budgetId!!,
                                                budgetName = budget.name,
                                                generatedAt = OffsetDateTime.now().format(dateFormatter),
                                                totalBudgeted = budget.total,
                                                totalActual = totalActual,
                                                totalRemaining = totalRemainingBalance,
                                                expenses = selectedExpensesMap
                                            )

                                            // Submit report to server
                                            val result = authRepository.submitLiquidationReport(reportData)
                                            if (result.isSuccess) {
                                                viewModel.addGeneratedReport(
                                                    budgetName = budget.name,
                                                    timestamp = reportData.generatedAt
                                                )
                                                println("Navigating to DetailedLiquidationReport with selectedExpensesMap: $selectedExpensesMap")
                                                navController.navigate("detailed_liquidation_report/${budget.budgetId}")
                                                Toast.makeText(
                                                    context,
                                                    "Liquidation Report for ${budget.name} submitted successfully",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } else {
                                                val errorMessage = result.exceptionOrNull()?.message ?: "Failed to submit report"
                                                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .padding(horizontal = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF734656),
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFF734656).copy(alpha = 0.5f),
                                disabledContentColor = Color.White.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isLoading
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.animateContentSize()
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Generating...",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                } else {
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
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showExpenseSelectionDialog && currentExpenseItem != null) {
            val currentIndex = currentExpenseItem!!.second
            val currentExpense = currentExpenseItem!!.first
            // Filter expenses to show only those not assigned to other budgets or already selected for this index
            val filteredExpenses = authRepository.userExpenses.filter { expense ->
                val isAssignedToOtherBudget = selectedExpensesMap.any { (idx, exps) ->
                    idx != currentIndex && exps.contains(expense)
                }
                expense.category == currentExpense.category && !isAssignedToOtherBudget
            }
            LaunchedEffect(filteredExpenses, currentExpenseItem, selectedBudget) {
                checkedExpenses.clear()
                // Initialize checkedExpenses only with expenses selected for the current index
                filteredExpenses.forEach { expense ->
                    checkedExpenses[expense] = selectedExpensesMap[currentIndex]?.contains(expense) ?: false
                }
                println("Dialog initialized with checkedExpenses: ${checkedExpenses.filter { it.value }.keys}")
            }

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
                                text = "Select Receipts for ${currentExpense.category}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 20.sp
                                ),
                                color = Color(0xFF1F2937)
                            )
                            IconButton(
                                onClick = {
                                    showExpenseSelectionDialog = false
                                    currentExpenseItem = null
                                    checkedExpenses.clear()
                                },
                                modifier = Modifier.size(36.dp)
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
                                text = "No unassigned transactions available for ${currentExpense.category}.",
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
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                checkedExpenses[expense] = !(checkedExpenses[expense] ?: false)
                                            },
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
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = Color(0xFF3B82F6),
                                                        uncheckedColor = Color(0xFF4B5563),
                                                        checkmarkColor = Color.White
                                                    )
                                                )
                                                Column(
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(
                                                        text = expense.remarks ?: "No remarks",
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = 14.sp
                                                        ),
                                                        color = Color(0xFF1F2937),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
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
                                        contentColor = Color(0xFF1F2937)
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFF734656)),
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
                                        val newSelections = filteredExpenses.filter { checkedExpenses[it] == true }
                                        println("Confirming selections: $newSelections")
                                        viewModel.updateSelections(currentIndex, newSelections)
                                        println("Updated selectedExpensesMap: $selectedExpensesMap")
                                        if (newSelections.isNotEmpty()) {
                                            Toast.makeText(
                                                context,
                                                "${newSelections.size} receipt(s) selected",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            viewModel.updateSelections(currentIndex, emptyList())
                                            Toast.makeText(
                                                context,
                                                "No receipts selected",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        showExpenseSelectionDialog = false
                                        currentExpenseItem = null
                                        checkedExpenses.clear()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .padding(start = 8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF734656),
                                        disabledContainerColor = Color(0xFF734656).copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = filteredExpenses.isNotEmpty()
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Confirm",
                                            modifier = Modifier.size(16.dp),
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