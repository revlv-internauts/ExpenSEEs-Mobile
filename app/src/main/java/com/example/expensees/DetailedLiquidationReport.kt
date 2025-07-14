package com.example.expensees.screens

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.expensees.models.LiquidationReportData
import com.example.expensees.network.AuthRepository
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DetailedLiquidationReport(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: LiquidationViewModel,
    authRepository: AuthRepository,
    liquidationId: String
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var report by remember { mutableStateOf<LiquidationReportData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
        isGroupingUsed = true
    }

    val dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:ss")

    val statusColors = mapOf(
        "PENDING" to Color(0xFFCA8A04),
        "RELEASED" to Color(0xFF16A34A),
        "DENIED" to Color(0xFFDC2626),
        "LIQUIDATED" to Color(0xFF6B7280)
    )

    // Fetch report charter on composition
    LaunchedEffect(liquidationId) {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            val result = authRepository.getLiquidationReport(liquidationId)
            if (result.isSuccess) {
                report = result.getOrNull()
                report?.let { viewModel.selectReport(it) }
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "Failed to fetch report"
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                navController.popBackStack()
            }
            isLoading = false
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = Color(0xFF3B82F6)
            )
        }
        return
    }

    if (report == null || errorMessage != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = errorMessage ?: "Report not found",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp
                ),
                color = Color(0xFFDC2626),
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val totalBudgeted = report!!.totalSpent + report!!.remainingBalance
    val totalSpent = report!!.totalSpent
    val totalRemaining = report!!.remainingBalance

    Scaffold(
        modifier = modifier.fillMaxSize().background(Color(0xFFF8FAFC)),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
                    .background(Color(0xFFF8FAFC)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.navigate("liquidation_reports") { popUpTo("liquidation_reports") { inclusive = false } } },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to Liquidation Reports",
                        tint = Color(0xFF111827)
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
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = { navController.navigate("home") { popUpTo("home") { inclusive = false } } },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Go to Home",
                        tint = Color(0xFF111827)
                    )
                }
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
                    start = 8.dp,
                    end = 8.dp
                ),
            horizontalAlignment = Alignment.Start
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "Status: ${report!!.status.lowercase(Locale.US).replaceFirstChar { it.uppercase() }}",
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
                            .clip(CircleShape)
                            .background(statusColors[report!!.status] ?: Color(0xFF6B7280))
                    )
                }
                Text(
                    text = "Remarks: ${report!!.remarks ?: "No remarks provided"}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = report!!.budgetName ?: "N/A",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = Color(0xFF111827)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Generated: ${report!!.createdAt.format(dateFormatter)}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp
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
                text = "Receipt Details",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp
                ),
                color = Color(0xFF111827),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                stickyHeader {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F5F9))
                            .padding(horizontal = 4.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Date",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = Color(0xFF111827),
                            modifier = Modifier.weight(2f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = Color(0xFF111827),
                            modifier = Modifier.weight(2f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Receipt",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = Color(0xFF111827),
                            modifier = Modifier.weight(3f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Amount",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = Color(0xFF111827),
                            modifier = Modifier.weight(2f),
                            textAlign = TextAlign.Center
                        )
                    }
                    Divider(
                        color = Color(0xFFE5E7EB),
                        thickness = 1.dp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(report!!.expenses) { expense ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = expense.dateOfTransaction ?: "N/A",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = Color(0xFF6B7280),
                            modifier = Modifier.weight(2f),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                        Text(
                            text = expense.category ?: "Unknown",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = Color(0xFF111827),
                            modifier = Modifier.weight(2f),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                        Text(
                            text = expense.remarks ?: "No remarks",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = Color(0xFF6B7280),
                            modifier = Modifier.weight(3f),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                        Text(
                            text = "₱${numberFormat.format(expense.amount)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = Color(0xFF6B7280),
                            modifier = Modifier.weight(2f),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                    Divider(color = Color(0xFFE5E7EB), thickness = 0.5.dp)
                }
                item {
                    if (report!!.expenses.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No receipts found.",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 18.sp
                                ),
                                color = Color(0xFF4B5563),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .background(Color(0xFFF8FAFC)),
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
                    text = "Total Spent: ₱${numberFormat.format(totalSpent)}",
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