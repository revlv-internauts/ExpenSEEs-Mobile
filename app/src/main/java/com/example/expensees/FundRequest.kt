package com.example.expensees.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.expensees.models.BudgetStatus
import com.example.expensees.models.ExpenseItem
import com.example.expensees.models.SubmittedBudget
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FundRequest(
    modifier: Modifier = Modifier,
    navController: NavController,
    submittedBudgets: MutableList<SubmittedBudget>
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
    }

    // Categories list (aligned with other screens)
    val categories = listOf(
        "Utilities", "Food", "Transportation", "Gas", "Office Supplies",
        "Rent", "Parking", "Electronic Supplies", "Grocery", "Other Expenses"
    )

    // Calculate total expenses
    val totalExpenses = expenses.sumOf { it.quantity * it.amountPerUnit }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Request Fund",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFEEECE1), // Consistent background color
                    titleContentColor = MaterialTheme.colorScheme.primary
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
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(12.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        content = {
                            Text(
                                text = snackbarData.visuals.message,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        },
                        action = {
                            snackbarData.visuals.actionLabel?.let { label ->
                                TextButton(
                                    onClick = { snackbarData.performAction() },
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.secondary)
                                ) {
                                    Text(
                                        text = label,
                                        color = MaterialTheme.colorScheme.onSecondary,
                                        style = MaterialTheme.typography.labelLarge
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
                    .background(Color(0xFFEEECE1)) // Consistent background color
            )
        },
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFEEECE1)) // Scaffold background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFEEECE1)) // Ensure Column has the same background
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
                label = { Text("Budget Name (Purpose)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp)),
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = { showDialog = true },
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Expense",
                        tint = MaterialTheme.colorScheme.primary
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
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
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
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "₱${numberFormat.format(expense.quantity * expense.amountPerUnit)}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
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
                            }
                            if (expense.remarks.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Remarks: ${expense.remarks}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Total Expenses
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
                    text = "Total Expenses: ₱${numberFormat.format(totalExpenses)}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = {
                    if (budgetName.isNotBlank() && expenses.isNotEmpty()) {
                        val submittedBudgetName = budgetName // Store budgetName before clearing
                        submittedBudgets.add(
                            SubmittedBudget(
                                name = budgetName,
                                expenses = expenses.toList(),
                                total = totalExpenses,
                                status = BudgetStatus.PENDING
                            )
                        )
                        coroutineScope.launch {
                            // Show snackbar
                            val job = coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Budget request for $submittedBudgetName submitted successfully!",
                                    actionLabel = "OK",
                                    duration = SnackbarDuration.Indefinite
                                )
                            }
                            // Delay for 2 seconds
                            delay(2000L)
                            // Dismiss the snackbar
                            job.cancel()
                            snackbarHostState.currentSnackbarData?.dismiss()
                        }
                        // Clear form after submission
                        budgetName = ""
                        expenses.clear()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(8.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                enabled = budgetName.isNotBlank() && expenses.isNotEmpty()
            ) {
                Text(
                    text = "Request Budget",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp)) // Additional spacing for bottom bar
        }

        // Add Expense Dialog
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = {
                    Text(
                        text = "Add Expense",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.background(Color(0xFFEEECE1)) // Set dialog content background
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            OutlinedTextField(
                                value = category,
                                onValueChange = { }, // Keep empty since selection is via dropdown
                                label = { Text("Expense Category") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp)),
                                readOnly = true, // Prevent keyboard input
                                interactionSource = remember { MutableInteractionSource() }.also { interactionSource ->
                                    LaunchedEffect(interactionSource) {
                                        interactionSource.interactions.collect { interaction ->
                                            if (interaction is PressInteraction.Press) {
                                                expanded = true // Open dropdown on any press
                                            }
                                        }
                                    }
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = if (expanded) "Collapse" else "Expand",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFEEECE1)) // Set dropdown background
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
                            value = quantity,
                            onValueChange = { quantity = it.filter { c -> c.isDigit() } },
                            label = { Text("Quantity") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp)),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = amountPerUnit,
                            onValueChange = { amountPerUnit = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Amount per Unit (₱)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp)),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = remarks,
                            onValueChange = { remarks = it },
                            label = { Text("Remarks") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp)),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Total: ₱${numberFormat.format(
                                (quantity.toIntOrNull() ?: 0) * (amountPerUnit.toDoubleOrNull() ?: 0.0))}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                confirmButton = {
                    Button(
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
                        enabled = category.isNotBlank() && quantity.isNotBlank() && amountPerUnit.isNotBlank(),
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDialog = false },
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = "Cancel",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFEEECE1)) // Set dialog background
            )
        }
    }
}