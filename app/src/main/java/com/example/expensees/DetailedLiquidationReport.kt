package com.example.expensees.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.expensees.models.BudgetStatus
import com.example.expensees.models.Expense
import com.example.expensees.models.SubmittedBudget
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DetailedLiquidationReport(
    budget: SubmittedBudget,
    selectedExpensesMap: Map<Int, List<Expense>>,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
        isGroupingUsed = true
    }
    val totalBudgeted = budget.total
    val totalActual = budget.expenses.withIndex().sumOf { (index, _) ->
        selectedExpensesMap[index]?.sumOf { it.amount } ?: 0.0
    }
    val totalRemaining = totalBudgeted - totalActual

    // Status colors for consistency
    val statusColors = mapOf(
        BudgetStatus.PENDING to Color(0xFFCA8A04), // Professional yellow
        BudgetStatus.APPROVED to Color(0xFF16A34A), // Professional green
        BudgetStatus.DENIED to Color(0xFFDC2626)  // Professional red
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)), // Light neutral background
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
                    .background(Color(0xFFF8FAFC)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to Budgets",
                        tint = Color(0xFF111827) // Darker neutral for contrast
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Liquidation Report",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 26.sp
                    ),
                    color = Color(0xFF111827),
                    modifier = Modifier
                        .weight(1f)
                        .offset(x = (-18).dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding(),
                    start = 8.dp, // Added minimal side padding
                    end = 8.dp    // Added minimal side padding
                ),
            horizontalAlignment = Alignment.Start
        ) {
            // Budget Info Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "Status: ${budget.status.name.lowercase(Locale.US).replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        ),
                        color = Color(0xFF6B7280)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(statusColors[budget.status] ?: Color(0xFF6B7280))
                    )
                }
                Text(
                    text = budget.name,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = Color(0xFF111827)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Total Cash Requested: ₱${numberFormat.format(totalBudgeted)}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    ),
                    color = Color(0xFF6B7280)
                )
                Divider(
                    color = Color(0xFFE5E7EB),
                    thickness = 1.dp,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Text(
                text = "Expense Details",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp
                ),
                color = Color(0xFF111827),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                stickyHeader {

                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F5F9))
                            .padding(horizontal = 4.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = Color(0xFF111827),
                            modifier = Modifier.weight(2.5f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Actual",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = Color(0xFF111827),
                            modifier = Modifier.weight(1.5f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Qty",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = Color(0xFF111827),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = Color(0xFF111827),
                            modifier = Modifier.weight(1.5f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Balance",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = Color(0xFF111827),
                            modifier = Modifier.weight(1.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                    Divider(
                        color = Color(0xFFE5E7EB),
                        thickness = 1.dp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                // Expense Items
                items(budget.expenses.withIndex().toList()) { (index, expense) ->
                    val requestedBudget = expense.quantity * expense.amountPerUnit
                    val actualPrice = selectedExpensesMap[index]?.sumOf { it.amount } ?: 0.0
                    val total = actualPrice
                    val balance = requestedBudget - total
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = expense.category,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = Color(0xFF111827),
                            modifier = Modifier.weight(2.5f),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                        Text(
                            text = "₱${numberFormat.format(actualPrice)}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp
                            ),
                            color = Color(0xFF6B7280),
                            modifier = Modifier.weight(1.5f),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                        Text(
                            text = "${expense.quantity}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp
                            ),
                            color = Color(0xFF6B7280),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                        Text(
                            text = "₱${numberFormat.format(total)}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp
                            ),
                            color = Color(0xFF6B7280),
                            modifier = Modifier.weight(1.5f),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                        Text(
                            text = "₱${numberFormat.format(balance)}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp
                            ),
                            color = if (balance >= 0) Color(0xFF16A34A) else Color(0xFFDC2626),
                            modifier = Modifier.weight(1.5f),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                    Divider(color = Color(0xFFE5E7EB), thickness = 0.5.dp)
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Total Budgeted: ₱${numberFormat.format(totalBudgeted)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp
                            ),
                            color = Color(0xFF111827)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (totalRemaining >= 0) {
                                "Total Remaining: ₱${numberFormat.format(totalRemaining)}"
                            } else {
                                "Total Over Budget: ₱${numberFormat.format(-totalRemaining)}"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp
                            ),
                            color = if (totalRemaining >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                        )
                    }
                }
            }
        }
    }
}