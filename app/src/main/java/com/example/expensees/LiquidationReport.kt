package com.example.expensees.screens

import android.annotation.SuppressLint
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.expensees.models.BudgetStatus
import com.example.expensees.models.Expense
import com.example.expensees.models.ExpenseItem
import com.example.expensees.models.SubmittedBudget
import java.text.NumberFormat
import java.util.Locale
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiquidationReport(
    modifier: Modifier = Modifier,
    submittedBudgets: List<SubmittedBudget>,
    expenses: List<Expense>,
    navController: NavController
) {
    val context = LocalContext.current
    var selectedBudget by remember { mutableStateOf<SubmittedBudget?>(null) }
    var showExpenseSelectionDialog by remember { mutableStateOf(false) }
    var currentExpenseItem by remember { mutableStateOf<Pair<ExpenseItem, Int>?>(null) }
    val selectedExpensesMap = remember { mutableStateMapOf<Int, MutableList<Expense>>() }
    val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val generatedReports = remember { mutableStateListOf<String>() }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportContent by remember { mutableStateOf("") }
    var showAllReportsDialog by remember { mutableStateOf(false) }

    val statusColors = mapOf(
        BudgetStatus.PENDING to Color(0xFFFFCA28),
        BudgetStatus.APPROVED to Color(0xFF4CAF50),
        BudgetStatus.DENIED to Color(0xFFF44336)
    )

    val textColors = mapOf(
        BudgetStatus.PENDING to Color.Gray,
        BudgetStatus.APPROVED to Color.Black,
        BudgetStatus.DENIED to Color.Black
    )

    fun calculateTotalRemainingBalance(): Double {
        return submittedBudgets.sumOf { budget ->
            budget.expenses.withIndex().sumOf { (index, expense) ->
                val budgetedAmount = expense.quantity * expense.amountPerUnit
                val actualExpenseTotal = if (budget == selectedBudget) {
                    selectedExpensesMap[index]?.sumOf { it.amount } ?: 0.0
                } else {
                    0.0
                }
                budgetedAmount - actualExpenseTotal
            }
        }
    }

    val totalRemainingBalance by derivedStateOf {
        calculateTotalRemainingBalance()
    }

    BackHandler(enabled = true) {
        if (selectedBudget != null) {
            selectedBudget = null
        } else {
            if (navController.currentBackStackEntry?.destination?.route != "home") {
                navController.navigate("home") {
                    popUpTo("home") { inclusive = false }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Liquidation Report",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedBudget != null) {
                            selectedBudget = null
                        } else {
                            if (navController.currentBackStackEntry?.destination?.route != "home") {
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = false }
                                }
                            }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
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
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFEEECE1)) // Apply the background color here
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            if (submittedBudgets.isEmpty()) {
                Text(
                    text = "No budget requests submitted yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center
                )
            } else if (selectedBudget == null) {
                Text(
                    text = "Select a Budget Request",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(
                    onClick = { showAllReportsDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text(
                        text = "View All Reports",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onTertiary
                    )
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(submittedBudgets) { budget ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedBudget = budget },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = budget.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = textColors[budget.status] ?: MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(statusColors[budget.status] ?: Color.Gray)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Total: ₱${numberFormat.format(budget.total)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColors[budget.status] ?: MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Status: ${budget.status.name.lowercase(Locale.US).replaceFirstChar { it.uppercase() }}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = statusColors[budget.status] ?: MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                selectedBudget?.let { budget ->
                    Text(
                        text = "Liquidation Report: ${budget.name}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Status: ${budget.status.name.lowercase(Locale.US).replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = statusColors[budget.status] ?: MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(statusColors[budget.status] ?: Color.Gray)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Total Budgeted Expenses: ₱${numberFormat.format(budget.total)}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "Expense Details",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(budget.expenses.withIndex().toList()) { (index, expense) ->
                            val actualExpenseTotal = selectedExpensesMap[index]?.sumOf { it.amount } ?: 0.0
                            val budgetedAmount = expense.quantity * expense.amountPerUnit
                            val remainingBalance = budgetedAmount - actualExpenseTotal

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = expense.category,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "₱${numberFormat.format(budgetedAmount)}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    currentExpenseItem = expense to index
                                                    showExpenseSelectionDialog = true
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Upload receipt",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    selectedExpensesMap[index] = mutableListOf()
                                                    Toast.makeText(
                                                        context,
                                                        "All receipts removed for ${expense.category}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                },
                                                enabled = selectedExpensesMap[index]?.isNotEmpty() ?: false
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete receipts",
                                                    tint = if (selectedExpensesMap[index]?.isNotEmpty() ?: false)
                                                        MaterialTheme.colorScheme.error
                                                    else
                                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Quantity: ${expense.quantity}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Unit Price: ₱${numberFormat.format(expense.amountPerUnit)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Actual Expenses: ₱${numberFormat.format(actualExpenseTotal)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Remaining Balance: ₱${numberFormat.format(remainingBalance)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (remainingBalance >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                                    )
                                    if (expense.remarks.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Remarks: ${expense.remarks}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    selectedExpensesMap[index]?.let { selectedExpenses ->
                                        if (selectedExpenses.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Selected Receipts:",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            selectedExpenses.forEach { selected ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(start = 8.dp, top = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = selected.remarks ?: "No remarks",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "₱${numberFormat.format(selected.amount)}",
                                                        style = MaterialTheme.typography.bodySmall,
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
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = if (totalRemainingBalance >= 0) {
                                "Total Remaining Credit (All Budgets): ₱${numberFormat.format(totalRemainingBalance)}"
                            } else {
                                "Total Over Budget (All Budgets): ₱${numberFormat.format(-totalRemainingBalance)}"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (totalRemainingBalance >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { selectedBudget = null },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .padding(end = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text(
                                text = "Back to Budgets",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSecondary
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
                                reportBuilder.append("**Total Remaining Balance (This Budget)**: ₱${numberFormat.format(budgetRemainingBalance)}\n")
                                reportBuilder.append("**Total Remaining Balance (All Budgets)**: ₱${numberFormat.format(totalRemainingBalance)}\n\n")
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
                                                reportBuilder.append("  - ${selected.remarks ?: "No remarks"}: ₱${numberFormat.format(selected.amount)} (Date: ${selected.dateOfTransaction.let { OffsetDateTime.parse(it).format(dateFormatter) } ?: "Unknown"})\n")
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
                                .padding(start = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = "Generate Report",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        if (showExpenseSelectionDialog && currentExpenseItem != null) {
            val filteredExpenses = expenses.filter { it.category == currentExpenseItem!!.first.category }
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

            AlertDialog(
                onDismissRequest = {
                    showExpenseSelectionDialog = false
                    currentExpenseItem = null
                    checkedExpenses.clear()
                },
                title = { Text("Select Receipts for ${currentExpenseItem!!.first.category}") },
                text = {
                    Column {
                        if (filteredExpenses.isEmpty()) {
                            Text(
                                text = "No transactions recorded yet for ${currentExpenseItem!!.first.category}.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val potentialRemaining = budgetedAmount - potentialActualTotal.value
                            if (potentialRemaining < 0) {
                                Text(
                                    text = "Warning: Selection exceeds budget by ₱${numberFormat.format(-potentialRemaining)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp)
                            ) {
                                items(filteredExpenses) { expense ->
                                    val isUsed = selectedExpensesMap.any { (idx, exps) ->
                                        idx != currentExpenseItem!!.second && exps.contains(expense)
                                    }
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
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = checkedExpenses[expense] ?: false,
                                                onCheckedChange = { isChecked ->
                                                    checkedExpenses[expense] = isChecked
                                                },
                                                modifier = Modifier.padding(end = 8.dp),
                                                enabled = !isUsed || (checkedExpenses[expense] ?: false)
                                            )
                                            Column(
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = expense.remarks ?: "No remarks",
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color = if (isUsed && !(checkedExpenses[expense] ?: false))
                                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                        else
                                                            MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (isUsed) {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Icon(
                                                            imageVector = Icons.Default.CheckCircle,
                                                            contentDescription = "Receipt Used",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "₱${numberFormat.format(expense.amount)}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = "Category: ${expense.category}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    TextButton(
                        onClick = {
                            val expenseList = selectedExpensesMap.getOrPut(currentExpenseItem!!.second) { mutableListOf() }
                            expenseList.clear()
                            val newSelections = filteredExpenses.filter { checkedExpenses[it] == true }
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
                        },
                        enabled = filteredExpenses.isNotEmpty()
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showExpenseSelectionDialog = false
                            currentExpenseItem = null
                            checkedExpenses.clear()
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showReportDialog) {
            AlertDialog(
                onDismissRequest = { showReportDialog = false },
                title = { Text("Liquidation Report: ${selectedBudget?.name}") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = reportContent as String,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showReportDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        if (showAllReportsDialog) {
            AlertDialog(
                onDismissRequest = { showAllReportsDialog = false },
                title = { Text("All Generated Reports") },
                text = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        if (generatedReports.isEmpty()) {
                            item {
                                Text(
                                    text = "No reports generated yet.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            items(generatedReports.withIndex().toList()) { (index, report) ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    val budgetName = report.substringAfter("# Liquidation Report: ").substringBefore("\n").trim()
                                    Text(
                                        text = "Report: $budgetName",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    Text(
                                        text = report as String,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (index < generatedReports.size - 1) {
                                        Divider(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            thickness = 1.dp,
                                            modifier = Modifier.padding(top = 16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAllReportsDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}