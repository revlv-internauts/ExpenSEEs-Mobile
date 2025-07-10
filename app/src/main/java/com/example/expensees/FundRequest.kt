package com.example.expensees.screens

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.example.expensees.models.BudgetStatus
import com.example.expensees.models.ExpenseItem
import com.example.expensees.models.SubmittedBudget
import com.example.expensees.network.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

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
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fetch budgets when the screen is loaded
    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        try {
            authRepository.syncLocalBudgets()
            val result = authRepository.getBudgets()
            result.onSuccess {
                isLoading = false
                Log.d("FundRequest", "Budgets fetched successfully: ${authRepository.submittedBudgets.size}")
            }.onFailure { e ->
                isLoading = false
                errorMessage = e.message
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Failed to fetch budgets: ${e.message}",
                        actionLabel = "Retry",
                        duration = SnackbarDuration.Long
                    ).let { result ->
                        if (result == SnackbarResult.ActionPerformed) {
                            authRepository.getBudgets()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            isLoading = false
            errorMessage = e.message
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = "Unexpected error: ${e.message}",
                    actionLabel = "OK",
                    duration = SnackbarDuration.Long
                )
            }
        }
    }

    // Dialog state
    var category by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var amountPerUnit by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    // Number formatter
    val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
        isGroupingUsed = true
    }

    // Categories and colors
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

    // Calculate total expenses
    val totalExpenses = expenses.sumOf { it.quantity * it.amountPerUnit }

    // Animation for expense cards
    val animatedScale = remember {
        mutableStateListOf<Animatable<Float, *>>().apply {
            repeat(expenses.size) { add(Animatable(0f)) }
        }
    }
    val animatedOffset = remember {
        mutableStateListOf<Animatable<Float, *>>().apply {
            repeat(expenses.size) { add(Animatable(0f)) }
        }
    }
    val animatedAlpha = remember {
        mutableStateListOf<Animatable<Float, *>>().apply {
            repeat(expenses.size) { add(Animatable(1f)) }
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

    val textFieldColors = TextFieldDefaults.outlinedTextFieldColors(
        focusedBorderColor = Color(0xFF3B82F6),
        unfocusedBorderColor = Color(0xFFE5E7EB),
        focusedLabelColor = Color(0xFF3B82F6),
        unfocusedLabelColor = Color(0xFF4B5563),
        focusedTextColor = Color(0xFF1F2937),
        unfocusedTextColor = Color(0xFF1F2937),
        cursorColor = Color(0xFF3B82F6)
    )

    // Define deleteExpense function inside the composable
    fun deleteExpense(
        index: Int,
        expenses: MutableList<ExpenseItem>,
        animatedScale: MutableList<Animatable<Float, *>>,
        animatedOffset: MutableList<Animatable<Float, *>>,
        animatedAlpha: MutableList<Animatable<Float, *>>,
        snackbarHostState: SnackbarHostState,
        coroutineScope: CoroutineScope
    ) {
        coroutineScope.launch {
            // Animate out the card
            animatedAlpha.getOrNull(index)?.animateTo(
                targetValue = 0f,
                animationSpec = tween(200)
            )
            // Remove the expense from the local list
            val removedExpense = expenses.removeAt(index)
            // Remove animation states
            animatedScale.removeAt(index)
            animatedOffset.removeAt(index)
            animatedAlpha.removeAt(index)
            // Show snackbar with undo option
            snackbarHostState.showSnackbar(
                message = "Expense deleted",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            ).let { result ->
                if (result == SnackbarResult.ActionPerformed) {
                    // Restore the expense if undo is clicked
                    expenses.add(index, removedExpense)
                    animatedScale.add(index, Animatable(0f))
                    animatedOffset.add(index, Animatable(0f))
                    animatedAlpha.add(index, Animatable(1f))
                    // Animate the restored card back in
                    coroutineScope.launch {
                        animatedScale.getOrNull(index)?.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(400, easing = FastOutSlowInEasing)
                        )
                        animatedAlpha.getOrNull(index)?.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(400)
                        )
                    }
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFFF5F5F5),
                drawerContentColor = Color(0xFF1F2937)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            modifier = Modifier.size(56.dp),
                            color = Color(0xFFD6D8DA)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "A",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color(0xFF1F2937),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "User Profile",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF1F2937)
                            )
                            Text(
                                text = "user@example.com",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF4B5563)
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Color(0xFFE5E7EB)
                    )
                    TextButton(
                        onClick = { navController.navigate("reset_password") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "Reset Password",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF3B82F6),
                            fontSize = 16.sp
                        )
                    }
                    TextButton(
                        onClick = { /* Handle Theme click */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "Theme",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF3B82F6),
                            fontSize = 16.sp
                        )
                    }
                    TextButton(
                        onClick = { /* Handle About click */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "About",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF3B82F6),
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            authRepository.logout()
                            navController.navigate("login") {
                                popUpTo("fund_request") { inclusive = true }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color(0xFF734656), Color(0xFF8A5B6E)),
                                        start = Offset(0f, 0f),
                                        end = Offset(Float.POSITIVE_INFINITY, 0f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Logout",
                                fontSize = 16.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
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
                                    Button(
                                        onClick = { snackbarData.performAction() },
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp)),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.Transparent
                                        ),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    brush = Brush.linearGradient(
                                                        colors = listOf(Color(0xFFE5E7EB), Color(0xFFD1D5DB)),
                                                        start = Offset(0f, 0f),
                                                        end = Offset(Float.POSITIVE_INFINITY, 0f)
                                                    ),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                color = Color(0xFF3B82F6),
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                )
            },
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
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
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF734656)
                        )
                    }
                } else if (errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Error: $errorMessage",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 18.sp
                            ),
                            color = Color(0xFFD32F2F),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 50.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { navController.popBackStack() },
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
                            text = "Request Fund",
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
                    OutlinedTextField(
                        value = budgetName,
                        onValueChange = { budgetName = it },
                        label = { Text("Budget Name (Purpose)", color = Color(0xFF4B5563)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        singleLine = true,
                        colors = textFieldColors,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Expenses",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            ),
                            color = Color(0xFF1F2937)
                        )
                        IconButton(
                            onClick = { showDialog = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Expense",
                                tint = Color(0xFF1F2937)
                            )
                        }
                    }
                    if (expenses.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No expenses added yet.",
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
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(expenses) { expense ->
                                val index = expenses.indexOf(expense)
                                val scale by animatedScale.getOrNull(index)?.asState() ?: remember { mutableStateOf(1f) }
                                val offset by animatedOffset.getOrNull(index)?.asState() ?: remember { mutableStateOf(0f) }
                                val alpha by animatedAlpha.getOrNull(index)?.asState() ?: remember { mutableStateOf(1f) }
                                var dragOffset by remember { mutableStateOf(0f) }
                                val swipeThreshold = 150f
                                val hapticFeedback = LocalHapticFeedback.current

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset(x = offset.dp)
                                        .alpha(alpha)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(80.dp)
                                            .background(
                                                color = Color(0xFFE7685D).copy(alpha = (abs(dragOffset) / swipeThreshold).coerceIn(0f, 1f)),
                                                shape = RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color.White,
                                            modifier = Modifier
                                                .padding(end = 16.dp)
                                                .size(24.dp)
                                                .alpha((abs(dragOffset) / swipeThreshold).coerceIn(0f, 1f))
                                        )
                                    }
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .scale(scale)
                                            .offset(x = dragOffset.dp)
                                            .pointerInput(Unit) {
                                                detectHorizontalDragGestures(
                                                    onDragStart = {
                                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    },
                                                    onDragEnd = {
                                                        if (abs(dragOffset) >= swipeThreshold) {
                                                            deleteExpense(
                                                                index,
                                                                expenses,
                                                                animatedScale,
                                                                animatedOffset,
                                                                animatedAlpha,
                                                                snackbarHostState,
                                                                coroutineScope
                                                            )
                                                        } else {
                                                            coroutineScope.launch {
                                                                animatedOffset.getOrNull(index)?.animateTo(
                                                                    targetValue = 0f,
                                                                    animationSpec = tween(200)
                                                                )
                                                            }
                                                            dragOffset = 0f
                                                        }
                                                    },
                                                    onHorizontalDrag = { _, dragAmount ->
                                                        dragOffset = (dragOffset + dragAmount).coerceIn(-swipeThreshold, 0f)
                                                        coroutineScope.launch {
                                                            animatedOffset.getOrNull(index)?.animateTo(
                                                                targetValue = dragOffset,
                                                                animationSpec = spring(
                                                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                                                    stiffness = Spring.StiffnessMediumLow
                                                                )
                                                            )
                                                        }
                                                        if (abs(dragOffset) >= swipeThreshold && abs(dragOffset - dragAmount) < swipeThreshold) {
                                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        }
                                                    }
                                                )
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
                                                Surface(
                                                    shape = CircleShape,
                                                    color = categoryColors[expense.category] ?: Color(0xFF6B4E38),
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
                                                        text = "₱${numberFormat.format(expense.quantity * expense.amountPerUnit)}",
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontSize = 12.sp
                                                        ),
                                                        color = Color(0xFF4B5563),
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
                                                }
                                                Text(
                                                    text = "Qty: ${expense.quantity}",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    ),
                                                    color = categoryColors[expense.category] ?: Color(0xFF6B4E38),
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
                    Text(
                        text = "Total Expenses: ₱${numberFormat.format(totalExpenses)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        ),
                        color = Color(0xFF1F2937),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        textAlign = TextAlign.Center
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp, top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                if (budgetName.isBlank() && expenses.isEmpty()) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Please enter a budget name and add at least one expense",
                                            actionLabel = "OK",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                } else if (budgetName.isBlank()) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Please enter a budget name",
                                            actionLabel = "OK",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                } else if (expenses.isEmpty()) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Please add at least one expense",
                                            actionLabel = "OK",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                } else {
                                    val submittedBudgetName = budgetName
                                    val budget = SubmittedBudget(
                                        budgetId = null,
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
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .padding(horizontal = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(Color(0xFF734656), Color(0xFF8A5B6E)),
                                            start = Offset(0f, 0f),
                                            end = Offset(Float.POSITIVE_INFINITY, 0f)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RequestQuote,
                                        contentDescription = "Request",
                                        modifier = Modifier.size(20.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Request",
                                        fontSize = 16.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        Button(
                            onClick = { navController.navigate("requested_budgets") },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .padding(horizontal = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(Color(0xFF734656), Color(0xFF8A5B6E)),
                                            start = Offset(0f, 0f),
                                            end = Offset(Float.POSITIVE_INFINITY, 0f)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.List,
                                        contentDescription = "View Requested Budgets",
                                        modifier = Modifier.size(20.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "View Requests",
                                        fontSize = 16.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (showDialog) {
                Dialog(
                    onDismissRequest = { showDialog = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    val dialogAlpha by animateFloatAsState(
                        targetValue = if (showDialog) 1f else 0f,
                        animationSpec = tween(300, easing = LinearOutSlowInEasing)
                    )
                    val focusManager = LocalFocusManager.current
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .fillMaxHeight(0.85f)
                            .clip(RoundedCornerShape(16.dp))
                            .alpha(dialogAlpha)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { focusManager.clearFocus() },
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
                                    text = "Add Expense",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 20.sp
                                    ),
                                    color = Color(0xFF1F2937)
                                )
                                IconButton(
                                    onClick = { showDialog = false },
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
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
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
                                        ),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                        keyboardActions = KeyboardActions(
                                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
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
                                                text = {
                                                    Text(
                                                        text = option,
                                                        style = MaterialTheme.typography.bodyLarge.copy(
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.Medium
                                                        ),
                                                        color = Color(0xFF1F2937)
                                                    )
                                                },
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
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                    ),
                                    colors = textFieldColors,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                OutlinedTextField(
                                    value = amountPerUnit,
                                    onValueChange = { amountPerUnit = it.filter { c -> c.isDigit() || c == '.' } },
                                    label = { Text("Amount per Unit", color = Color(0xFF4B5563)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp)),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                    ),
                                    colors = textFieldColors,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                OutlinedTextField(
                                    value = remarks,
                                    onValueChange = { remarks = it },
                                    label = { Text("Remarks (Optional)", color = Color(0xFF4B5563)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp)),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(
                                        onDone = { focusManager.clearFocus() }
                                    ),
                                    colors = textFieldColors,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        showDialog = false
                                        category = ""
                                        quantity = ""
                                        amountPerUnit = ""
                                        remarks = ""
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
                                        text = "Cancel",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Button(
                                    onClick = {
                                        if (category.isBlank() || quantity.isBlank() || amountPerUnit.isBlank()) {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message = "Please fill in all required fields",
                                                    actionLabel = "OK",
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
                                        } else {
                                            val qty = quantity.toIntOrNull() ?: 0
                                            val amount = amountPerUnit.toDoubleOrNull() ?: 0.0
                                            if (qty <= 0 || amount <= 0.0) {
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        message = "Quantity and amount must be greater than zero",
                                                        actionLabel = "OK",
                                                        duration = SnackbarDuration.Short
                                                    )
                                                }
                                            } else {
                                                expenses.add(
                                                    ExpenseItem(
                                                        category = category,
                                                        quantity = qty,
                                                        amountPerUnit = amount,
                                                        remarks = remarks
                                                    )
                                                )
                                                animatedScale.add(Animatable(0f))
                                                animatedOffset.add(Animatable(0f))
                                                animatedAlpha.add(Animatable(1f))
                                                showDialog = false
                                                category = ""
                                                quantity = ""
                                                amountPerUnit = ""
                                                remarks = ""
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .padding(start = 8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF734656)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Add",
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
}