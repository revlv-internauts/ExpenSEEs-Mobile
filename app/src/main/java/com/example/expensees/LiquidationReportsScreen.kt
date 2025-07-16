package com.example.expensees.screens

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.expensees.models.LiquidationReportData
import com.example.expensees.network.AuthRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiquidationReportsScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: LiquidationViewModel,
    authRepository: AuthRepository
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val reportsState = authRepository.liquidationReports
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val detailedReports = remember { mutableStateMapOf<String, LiquidationReportData>() }
    val remarksLoading = remember { mutableStateMapOf<String, Boolean>() }
    val remarksErrors = remember { mutableStateMapOf<String, String?>() }
    var selectedFilter by remember { mutableStateOf("Pending") }
    val filterOptions = listOf("Pending", "Denied", "Liquidated")

    val statusColors = mapOf(
        "PENDING" to Color(0xFFCA8A04),
        "RELEASED" to Color(0xFF16A34A),
        "DENIED" to Color(0xFFDC2626),
        "LIQUIDATED" to Color(0xFF4CAF50)
    )

    LaunchedEffect(reportsState.size) {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            val result = authRepository.getLiquidationReports()
            if (result.isFailure) {
                errorMessage = result.exceptionOrNull()?.message ?: "Failed to load reports"
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                isLoading = false
                return@launch
            }

            val deferredResults = reportsState.map { report ->
                async {
                    remarksLoading[report.liquidationId] = true
                    val detailResult = authRepository.getLiquidationReport(report.liquidationId)
                    remarksLoading[report.liquidationId] = false
                    if (detailResult.isSuccess) {
                        detailedReports[report.liquidationId] = detailResult.getOrNull()!!
                    } else {
                        remarksErrors[report.liquidationId] = detailResult.exceptionOrNull()?.message ?: "Failed to load remarks"
                    }
                }
            }
            deferredResults.awaitAll()
            isLoading = false
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
    ) { innerPadding ->
        val focusManager = LocalFocusManager.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(horizontal = 16.dp)
                .padding(top = innerPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding())
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { focusManager.clearFocus() },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 50.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween // Ensures even spacing
            ) {
                IconButton(
                    onClick = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = false }
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to Home",
                        tint = Color(0xFF1F2937)
                    )
                }
                Text(
                    text = "Liquidation Reports",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = Color(0xFF1F2937),
                    modifier = Modifier
                        .weight(1f) // Takes available space
                        .wrapContentWidth(Alignment.CenterHorizontally), // Centers text horizontally
                    textAlign = TextAlign.Center
                )
                IconButton(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        coroutineScope.launch {
                            val result = authRepository.getLiquidationReports()
                            if (result.isFailure) {
                                errorMessage = result.exceptionOrNull()?.message ?: "Failed to load reports"
                                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                            }
                            isLoading = false
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Reports",
                        tint = Color(0xFF1F2937)
                    )
                }
            }

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                filterOptions.forEachIndexed { index, option ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = filterOptions.size),
                        onClick = { selectedFilter = option },
                        selected = selectedFilter == option,
                        label = {
                            Text(
                                text = option,
                                fontSize = 14.sp,
                                fontWeight = if (selectedFilter == option) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = Color(0xFF734656).copy(alpha = 0.1f),
                            activeContentColor = Color(0xFF734656),
                            activeBorderColor = Color(0xFF5E3645),
                            inactiveContainerColor = Color(0xFFF5F5F5),
                            inactiveContentColor = Color(0xFF4B5563),
                            inactiveBorderColor = Color(0xFF4B5563).copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.height(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = Color(0xFF734656)
                        )
                    }
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 18.sp
                            ),
                            color = Color(0xFFDC2626),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                isLoading = true
                                errorMessage = null
                                coroutineScope.launch {
                                    val result = authRepository.getLiquidationReports()
                                    if (result.isFailure) {
                                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to load reports"
                                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                        isLoading = false
                                        return@launch
                                    }

                                    val deferredResults = reportsState.map { report ->
                                        async {
                                            remarksLoading[report.liquidationId] = true
                                            val detailResult = authRepository.getLiquidationReport(report.liquidationId)
                                            remarksLoading[report.liquidationId] = false
                                            if (detailResult.isSuccess) {
                                                detailedReports[report.liquidationId] = detailResult.getOrNull()!!
                                            } else {
                                                remarksErrors[report.liquidationId] = detailResult.exceptionOrNull()?.message ?: "Failed to load remarks"
                                            }
                                        }
                                    }
                                    deferredResults.awaitAll()
                                    isLoading = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF734656)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Retry",
                                fontSize = 16.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                reportsState.isEmpty() -> {
                    Text(
                        text = "No liquidation reports generated yet.",
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                        color = Color(0xFF4B5563),
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    val filteredReports = reportsState
                        .filter { it.status == selectedFilter.uppercase(Locale.US) }
                        .sortedByDescending { report ->
                            try {
                                OffsetDateTime.parse(report.createdAt)
                            } catch (e: Exception) {
                                OffsetDateTime.MIN // Fallback to ensure invalid dates don't crash
                            }
                        }

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredReports) { report ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        viewModel.selectReport(report)
                                        navController.navigate("detailed_liquidation_report/${report.liquidationId}")
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Transparent,
                                border = BorderStroke(1.dp, Color(0xFF5E3645))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "Report: ${report.budgetName}",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 16.sp
                                            ),
                                            color = Color(0xFF1F2937)
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Status: ${report.status.lowercase(Locale.US).replaceFirstChar { it.uppercase() }}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 12.sp
                                                ),
                                                color = Color(0xFF4B5563)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .background(statusColors[report.status] ?: Color(0xFF4CAF50), CircleShape)
                                            )
                                        }
                                        if (remarksLoading[report.liquidationId] == true) {
                                            Text(
                                                text = "Loading remarks...",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 12.sp
                                                ),
                                                color = Color(0xFF4B5563)
                                            )
                                        } else if (remarksErrors[report.liquidationId] != null) {
                                            Text(
                                                text = "Remarks: Failed to load",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 12.sp
                                                ),
                                                color = Color(0xFFDC2626)
                                            )
                                        } else {
                                            Text(
                                                text = "Remarks: ${detailedReports[report.liquidationId]?.remarks ?: "No remarks provided"}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 12.sp
                                                ),
                                                color = Color(0xFF4B5563)
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
    }
}