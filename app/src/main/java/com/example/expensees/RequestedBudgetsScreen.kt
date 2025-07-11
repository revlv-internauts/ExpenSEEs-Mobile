package com.example.expensees.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.expensees.models.BudgetStatus
import com.example.expensees.models.SubmittedBudget
import com.example.expensees.network.AuthRepository
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestedBudgetsScreen(
    navController: NavController,
    authRepository: AuthRepository,
    modifier: Modifier = Modifier
) {
    // Number formatter for comma-separated amounts
    val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
        isGroupingUsed = true
    }

    // State management for category selection and all budgets view
    var selectedCategory by remember { mutableStateOf<BudgetStatus?>(null) }
    var showAllBudgets by remember { mutableStateOf(false) }

    // Status colors from LiquidationReport
    val statusColors = mapOf(
        BudgetStatus.PENDING to Color(0xFFD4A017),
        BudgetStatus.APPROVED to Color(0xFF388E3C),
        BudgetStatus.DENIED to Color(0xFFD32F2F)
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (selectedCategory != null) {
                            selectedCategory = null
                        } else if (showAllBudgets) {
                            showAllBudgets = false
                        } else {
                            navController.navigate("home") {
                                popUpTo("requested_budgets") { inclusive = true }
                            }
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
                    text = if (showAllBudgets) "All Budget Requests"
                    else if (selectedCategory != null) "${selectedCategory!!.name.lowercase(Locale.US).replaceFirstChar { it.uppercase() }} Budget Requests"
                    else "Requested Budgets",
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
                val pendingCount = authRepository.submittedBudgets.count { it.status == BudgetStatus.PENDING }
                val approvedCount = authRepository.submittedBudgets.count { it.status == BudgetStatus.APPROVED }
                val deniedCount = authRepository.submittedBudgets.count { it.status == BudgetStatus.DENIED }

                Button(
                    onClick = { selectedCategory = BudgetStatus.PENDING },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF78495B),
                        disabledContainerColor = Color(0xFF78495B).copy(alpha = 0.5f)
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

                Button(
                    onClick = { selectedCategory = BudgetStatus.APPROVED },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF78495B),
                        disabledContainerColor = Color(0xFF78495B).copy(alpha = 0.5f)
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

                Button(
                    onClick = { selectedCategory = BudgetStatus.DENIED },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF78495B),
                        disabledContainerColor = Color(0xFF78495B).copy(alpha = 0.5f)
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

                Button(
                    onClick = { showAllBudgets = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF78495B)
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
            } else {
                if (showAllBudgets || selectedCategory != null) {
                    Button(
                        onClick = {
                            selectedCategory = null
                            showAllBudgets = false
                        },
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
                }

                val budgetsToShow = when {
                    showAllBudgets -> authRepository.submittedBudgets
                    selectedCategory != null -> authRepository.submittedBudgets.filter { it.status == selectedCategory }
                    else -> authRepository.submittedBudgets
                }

                if (budgetsToShow.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when {
                                showAllBudgets -> "No budget requests available."
                                selectedCategory != null -> "No ${selectedCategory!!.name.lowercase(Locale.US).replaceFirstChar { it.uppercase() }} budget requests."
                                else -> "No budgets requested yet."
                            },
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
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(budgetsToShow) { budget ->
                            val index = budgetsToShow.indexOf(budget)
                            val scale by animatedScale.getOrNull(index)?.asState() ?: remember { mutableStateOf(1f) }
                            BudgetCard(
                                budget = budget,
                                numberFormat = numberFormat,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(scale)
                                    .clickable {
                                        if (budget.budgetId != null) {
                                            if (budget.status == BudgetStatus.APPROVED) {
                                                navController.navigate("liquidation_report/${budget.budgetId}")
                                            } else {
                                                navController.navigate("budget_details/${budget.budgetId}")
                                            }
                                        }
                                    },
                                statusColors = statusColors
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetCard(
    budget: SubmittedBudget,
    numberFormat: NumberFormat,
    modifier: Modifier = Modifier,
    statusColors: Map<BudgetStatus, Color>
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp)),
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
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                .border(
                    1.dp,
                    (statusColors[budget.status] ?: Color(0xFF6B4E38)).copy(alpha = 0.3f),
                    RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
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
                            text = budget.name.first().toString().uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = budget.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        ),
                        color = Color(0xFF1F2937),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "₱${numberFormat.format(budget.total)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 14.sp
                        ),
                        color = Color(0xFF4B5563)
                    )
                    Text(
                        text = "Status: ${budget.status.name.lowercase(Locale.US).replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 14.sp
                        ),
                        color = statusColors[budget.status] ?: Color(0xFF4B5563)
                    )
                }
            }
        }
    }
}