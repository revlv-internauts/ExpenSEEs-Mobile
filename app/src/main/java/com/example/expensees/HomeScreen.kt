package com.example.expensees.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.expensees.ApiConfig
import com.example.expensees.models.Expense
import com.example.expensees.network.AuthRepository
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import android.util.Log
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    expenses: SnapshotStateList<Expense>,
    navController: NavController,
    authRepository: AuthRepository,
    onRecordExpensesClick: () -> Unit = {},
    onListExpensesClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
        isGroupingUsed = true
    }

    // Fetch username and email from SharedPreferences
    val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    val username by remember { mutableStateOf(prefs.getString("username", "User") ?: "User") }
    val email by remember { mutableStateOf(prefs.getString("email", "user@example.com") ?: "user@example.com") }

    val categories = listOf(
        "Utilities", "Food", "Transportation", "Gas", "Office Supplies",
        "Rent", "Parking", "Electronic Supplies", "Grocery", "Other Expenses"
    )

    val totalExpenses = expenses.sumOf { it.amount.coerceAtLeast(0.0) }
    val categoryTotals = expenses.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount.coerceAtLeast(0.0) } }
        .toList()
        .sortedByDescending { it.second }
        .take(5)
    val chartData = categories.map { category ->
        expenses.filter { it.category == category }.sumOf { it.amount.coerceAtLeast(0.0) }
    }

    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedTransaction by remember { mutableStateOf<Expense?>(null) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var expenseImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showFullScreenImage by remember { mutableStateOf(false) }
    var selectedImagePath by remember { mutableStateOf<String?>(null) }

    var selectedChartCategory by remember { mutableStateOf<String?>(null) }
    var selectedCategoryAmount by remember { mutableStateOf(0.0) }

    val colorList = listOf(
        Color(0xFF6B4E38), Color(0xFFE7685D), Color(0xFFFBBD92), Color(0xFF4CAF50),
        Color(0xFF2196F3), Color(0xFFFF9800), Color(0xFFE28743), Color(0xFF009688),
        Color(0xFFFF5722), Color(0xFF607D8B)
    )
    val categoryColors = categories.zip(colorList).toMap()

    val animatedScale = remember {
        SnapshotStateList<Animatable<Float, *>>().apply {
            repeat(categoryTotals.size) { add(Animatable(0f)) }
        }
    }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    LaunchedEffect(categoryTotals) {
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

    var token by remember { mutableStateOf<String?>(null) }
    var tokenFetchFailed by remember { mutableStateOf(false) }
    var retryCount by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        if (retryCount < 2) {
            try {
                val tokenResult = authRepository.getValidToken()
                if (tokenResult.isSuccess) {
                    token = tokenResult.getOrNull()
                    Log.d("HomeScreen", "Initial token fetched: $token")
                } else {
                    retryCount++
                    Log.e("HomeScreen", "Initial token retrieval failed: ${tokenResult.exceptionOrNull()?.message}")
                    tokenFetchFailed = true
                }
            } catch (e: Exception) {
                retryCount++
                Log.e("HomeScreen", "Initial token retrieval exception: ${e.message}")
                tokenFetchFailed = true
            }
        } else {
            tokenFetchFailed = true
            Toast.makeText(context, "Authentication error: Please log in again", Toast.LENGTH_SHORT).show()
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, dragAmount ->
                                if (drawerState.isOpen && dragAmount < 0) {
                                    scope.launch {
                                        drawerState.close()
                                    }
                                    change.consume()
                                }
                            }
                        )
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 50.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = username.firstOrNull()?.uppercase() ?: "U",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = username,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        label = { Text("Reset Password") },
                        selected = false,
                        onClick = {
                            navController.navigate("reset_password")
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Palette, contentDescription = null) },
                        label = { Text("Theme") },
                        selected = false,
                        onClick = {
                            Toast.makeText(context, "Theme clicked", Toast.LENGTH_SHORT).show()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Info, contentDescription = null) },
                        label = { Text("About") },
                        selected = false,
                        onClick = {
                            Toast.makeText(context, "About clicked", Toast.LENGTH_SHORT).show()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                onLogoutClick()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .padding(vertical = 12.dp)
                            .align(Alignment.CenterHorizontally),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(0.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color(0xFF4A2E3F), Color(0xFF6B4E56)),
                                        start = Offset(0f, 0f),
                                        end = Offset(Float.POSITIVE_INFINITY, 0f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFF8A5B6E).copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(vertical = 10.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sign Out",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 50.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { scope.launch { drawerState.open() } },
                    modifier = Modifier
                        .background(Color(0xFFE5E7EB), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open navigation drawer",
                        tint = Color(0xFF1F2937)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "ExpenSEEs",
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
            if (expenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No expenses recorded yet.",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp
                        ),
                        color = Color(0xFF4B5563),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    color = Color.Transparent
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                try {
                                    loadDataWithBaseURL(
                                        null,
                                        """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js"></script>
                        <style>
                            html, body {
                                margin: 0;
                                padding: 0;
                                background: transparent;
                                width: 100%;
                                height: 100%;
                            }
                            #chartContainer {
                                width: 100%;
                                height: 100%;
                            }
                            #expenseChart {
                                max-width: 100%;
                                max-height: 100%;
                            }
                        </style>
                    </head>
                    <body>
                        <div id="chartContainer">
                            <canvas id="expenseChart"></canvas>
                        </div>
                        <script>
                            const chartData = [${chartData.joinToString { if (it == 0.0) "0.01" else it.toString() }}];
                            const ctx = document.getElementById('expenseChart').getContext('2d');
                            try {
                                const chart = new Chart(ctx, {
                                    type: 'pie',
                                    data: {
                                        labels: ['${categories.joinToString("','")}'],
                                        datasets: [{
                                            data: chartData,
                                            backgroundColor: [
                                                '${colorList.joinToString("','") { "#${it.toArgb().toUInt().toString(16).padStart(8, '0').substring(2)}" }}'
                                            ],
                                            borderColor: ['#FFFFFF'],
                                            borderWidth: 1
                                        }]
                                    },
                                    options: {
                                        responsive: true,
                                        maintainAspectRatio: false,
                                        plugins: {
                                            legend: {
                                                display: false
                                            },
                                            title: {
                                                display: true,
                                                text: 'Expense Distribution by Category',
                                                color: '#1F2937',
                                                font: { size: 18, weight: 'bold' },
                                                align: 'center'
                                            },
                                            tooltip: {
                                                enabled: true,
                                                callbacks: {
                                                    label: function(context) {
                                                        const value = context.raw || 0;
                                                        return context.label + ': ₱' + value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
                                                    }
                                                }
                                            }
                                        },
                                        onClick: (event, elements, chart) => {
                                            if (elements.length > 0) {
                                                const index = elements[0].index;
                                                const label = chart.data.labels[index];
                                                chart.options.plugins.title.text = label;
                                                window.android.onCategorySelected(label, chart.data.datasets[0].data[index]);
                                            } else {
                                                chart.options.plugins.title.text = 'Expense Distribution by Category';
                                                window.android.onCategorySelected('', 0);
                                            }
                                            chart.update();
                                        }
                                    }
                                });
                            } catch (e) {
                                console.error('Chart creation failed: ' + e.message);
                            }
                            window.android.onCategorySelected('', 0);
                        </script>
                    </body>
                    </html>
                    """.trimIndent(),
                                        "text/html",
                                        "UTF-8",
                                        null
                                    )
                                } catch (e: Exception) {
                                    Toast.makeText(ctx, "Failed to load chart: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                                addJavascriptInterface(object : Any() {
                                    @JavascriptInterface
                                    fun onCategorySelected(category: String, amount: Double) {
                                        scope.launch {
                                            selectedChartCategory = if (category.isEmpty()) null else category
                                            selectedCategoryAmount = amount
                                        }
                                    }
                                }, "android")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .padding(8.dp)
                    )
                }
                Text(
                    text = if (selectedChartCategory != null) {
                        "${selectedChartCategory} Expenses: ₱${numberFormat.format(selectedCategoryAmount.coerceAtLeast(0.0))}"
                    } else {
                        "Total Expenses: ₱${numberFormat.format(totalExpenses)}"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    ),
                    color = selectedChartCategory?.let { categoryColors[it] } ?: Color(0xFF1F2937),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Text(
                        text = "Your Top 5 Expenses",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        ),
                        color = Color(0xFF1F2937),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        categoryTotals.forEachIndexed { index, (category, amount) ->
                            val scale by animatedScale.getOrNull(index)?.asState() ?: remember { mutableStateOf(1f) }
                            val categoryColor = categoryColors[category] ?: Color(0xFF6B4E38)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(scale)
                                    .clickable { selectedCategory = category },
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
                                                text = category ?: "Unknown",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp
                                                ),
                                                color = Color(0xFF1F2937),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "₱${numberFormat.format(amount.coerceAtLeast(0.0))}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 12.sp
                                                ),
                                                color = Color(0xFF4B5563),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = if (totalExpenses > 0) {
                                                "${String.format("%.2f", (amount / totalExpenses) * 100)}%"
                                            } else {
                                                "0.00%"
                                            },
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            ),
                                            color = categoryColor,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NavigationButton(
                    icon = Icons.Default.Add,
                    label = "Record",
                    onClick = onRecordExpensesClick,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                NavigationButton(
                    icon = Icons.Default.RequestQuote,
                    label = "Request",
                    onClick = { navController.navigate("fund_request") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                NavigationButton(
                    icon = Icons.Default.Assignment,
                    label = "Report",
                    onClick = { navController.navigate("liquidation_report") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (selectedCategory != null) {
            Dialog(
                onDismissRequest = { selectedCategory = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                val dialogAlpha by animateFloatAsState(
                    targetValue = if (selectedCategory != null) 1f else 0f,
                    animationSpec = tween(300, easing = LinearOutSlowInEasing)
                )
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
                                text = "${selectedCategory} Transactions",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 20.sp
                                ),
                                color = Color(0xFF1F2937)
                            )
                            IconButton(
                                onClick = { selectedCategory = null },
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
                        val transactionsForCategory = expenses.filter { it.category == selectedCategory }
                        if (transactionsForCategory.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No transactions recorded for this category.",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                    color = Color(0xFF4B5563)
                                )
                            }
                        } else {
                            val baseColor = categoryColors[selectedCategory]!!
                            val transactionColors = transactionsForCategory.indices.map { index ->
                                val factor = 0.8f - (index * 0.1f).coerceAtMost(0.5f)
                                "#${baseColor.copy(alpha = factor).toArgb().toUInt().toString(16).padStart(8, '0').substring(2)}"
                            }
                            val validTransactions = transactionsForCategory.filter { it.amount > 0 }
                            val transactionData = validTransactions.map { it.amount }
                            val transactionLabels = validTransactions.mapIndexed { index, expense ->
                                "'${(expense.remarks ?: "").replace("'", "\\'")}'"
                            }

                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        settings.javaScriptEnabled = true
                                        setBackgroundColor(Color.Transparent.toArgb())
                                        try {
                                            loadDataWithBaseURL(
                                                null,
                                                """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js"></script>
                        <style>
                            html, body {
                                margin: 0;
                                padding: 0;
                                background: transparent;
                                height: 100%;
                                width: 100%;
                            }
                            #transactionChart {
                                width: 100%;
                                height: 100%;
                            }
                        </style>
                    </head>
                    <body>
                        <canvas id="transactionChart"></canvas>
                        <script>
                            const transactionData = [${transactionData.joinToString { it.toString() }}];
                            const ctx = document.getElementById('transactionChart').getContext('2d');
                            const chart = new Chart(ctx, {
                                type: 'bar',
                                data: {
                                    labels: [${transactionLabels.joinToString()}],
                                    datasets: [{
                                        data: transactionData,
                                        backgroundColor: [${transactionColors.joinToString() { "'$it'" }}],
                                        borderColor: '#D1D5DB',
                                        borderWidth: 1
                                    }]
                                },
                                options: {
                                    responsive: true,
                                    maintainAspectRatio: false,
                                    plugins: {
                                        legend: { display: false },
                                        title: {
                                            display: true,
                                            text: 'Transaction Amounts',
                                            color: '#1F2937',
                                            font: { size: 16, weight: 'bold' },
                                            align: 'center'
                                        },
                                        tooltip: {
                                            enabled: true,
                                            callbacks: {
                                                label: function(context) {
                                                    const value = context.raw || 0;
                                                    return '₱' + value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
                                                }
                                            }
                                        }
                                    },
                                    scales: {
                                        x: {
                                            ticks: { display: false },
                                            barPercentage: 0.8,
                                            categoryPercentage: 0.9
                                        },
                                        y: {
                                            type: 'logarithmic',
                                            min: 0.1,
                                            ticks: {
                                                callback: function(value) {
                                                    return '₱' + value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
                                                },
                                                font: { size: 12 },
                                                color: '#1F2937',
                                                autoSkip: true,
                                                maxTicksLimit: 6
                                            }
                                        }
                                    },
                                    animation: {
                                        duration: 800,
                                        easing: 'easeOutCubic'
                                    },
                                    hover: {
                                        mode: 'nearest',
                                        intersect: true,
                                        animationDuration: 400
                                    }
                                }
                            });
                        </script>
                    </body>
                    </html>
                    """.trimIndent(),
                                                "text/html",
                                                "UTF-8",
                                                null
                                            )
                                        } catch (e: Exception) {
                                            Toast.makeText(ctx, "Failed to load transaction chart", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        1.dp,
                                        Color(0xFFE5E7EB),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(8.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Transaction Details",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 18.sp
                                ),
                                color = Color(0xFF1F2937),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(transactionsForCategory) { index, expense ->
                                    val alpha by animateFloatAsState(
                                        targetValue = if (selectedCategory != null) 1f else 0f,
                                        animationSpec = tween(
                                            durationMillis = 200,
                                            delayMillis = index * 50,
                                            easing = LinearOutSlowInEasing
                                        )
                                    )
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .alpha(alpha)
                                            .clickable {
                                                selectedTransaction = expense
                                                selectedImagePath = expense.imagePaths?.firstOrNull()
                                                showExpenseDialog = true
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF5F5F5),
                                        shadowElevation = 4.dp
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = expense.remarks ?: "",
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        fontWeight = FontWeight.Medium,
                                                        fontSize = 16.sp
                                                    ),
                                                    color = Color(0xFF1F2937),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = expense.dateOfTransaction ?: "",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color(0xFF4B5563),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = "₱${numberFormat.format(expense.amount.coerceAtLeast(0.0))}",
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 16.sp
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
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { selectedCategory = null },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
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
                                        )
                                    )
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "Close",
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
        }
        if (showExpenseDialog && selectedTransaction != null) {
            AlertDialog(
                onDismissRequest = {
                    showExpenseDialog = false
                    selectedTransaction = null
                    expenseImageBitmap = null
                    selectedImagePath = null
                },
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
                            text = "${selectedTransaction?.category ?: "Receipt"}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 20.sp
                            ),
                            color = Color(0xFF3B82F6),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        selectedImagePath?.let { imagePath ->
                            var imageLoadFailed by remember { mutableStateOf(false) }
                            if (tokenFetchFailed) {
                                Text(
                                    text = "Authentication error: Please log in again",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF4B5563),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            } else if (selectedTransaction?.expenseId?.startsWith("local_") == true) {
                                val bitmap = try {
                                    val uri = Uri.parse(imagePath)
                                    BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
                                } catch (e: Exception) {
                                    Log.e("HomeScreen", "Failed to load local image: $imagePath, error: ${e.message}")
                                    null
                                }
                                bitmap?.let {
                                    Image(
                                        bitmap = it.asImageBitmap(),
                                        contentDescription = "${selectedTransaction?.category ?: "Receipt"} receipt",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(240.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(
                                                1.5.dp,
                                                Color(0xFF3B82F6),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                expenseImageBitmap = it
                                                showFullScreenImage = true
                                            },
                                        contentScale = ContentScale.Crop
                                    )
                                } ?: run {
                                    imageLoadFailed = true
                                    Text(
                                        text = "No receipt photo available",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF4B5563),
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                }
                            } else {
                                val fullImageUrl = "${ApiConfig.BASE_URL}api/expenses/${selectedTransaction?.expenseId}/images"
                                Log.d("HomeScreen", "Loading server image: $fullImageUrl with token: $token")
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(fullImageUrl)
                                        .apply {
                                            if (token != null) {
                                                addHeader("Authorization", "Bearer $token")
                                            } else {
                                                imageLoadFailed = true
                                            }
                                        }
                                        .diskCacheKey(fullImageUrl)
                                        .memoryCacheKey(fullImageUrl)
                                        .build(),
                                    contentDescription = "${selectedTransaction?.category ?: "Receipt"} receipt",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(240.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            1.5.dp,
                                            Color(0xFF3B82F6),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { showFullScreenImage = true },
                                    contentScale = ContentScale.Crop,
                                    onError = { error ->
                                        imageLoadFailed = true
                                        scope.launch {
                                            Log.e("HomeScreen", "Failed to load server image: $fullImageUrl, error: ${error.result.throwable.message}")
                                            Toast.makeText(context, "Failed to load receipt image: ${error.result.throwable.message}", Toast.LENGTH_LONG).show()
                                            if (error.result.throwable.message?.contains("401") == true && retryCount < 2) {
                                                retryCount++
                                                val tokenResult = authRepository.getValidToken()
                                                if (tokenResult.isSuccess) {
                                                    token = tokenResult.getOrNull()
                                                    Log.d("HomeScreen", "Retry token fetched: $token")
                                                } else {
                                                    tokenFetchFailed = true
                                                    Toast.makeText(context, "Authentication error: Please log in again", Toast.LENGTH_SHORT).show()
                                                    navController.navigate("login") {
                                                        popUpTo("home") { inclusive = true }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    onSuccess = {
                                        Log.d("HomeScreen", "Successfully loaded server image: $fullImageUrl")
                                    }
                                )
                            }
                        } ?: Text(
                            text = "No receipt photo available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF4B5563),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = { showInfoDialog = true },
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
                                    text = "Info",
                                    color = Color(0xFF3B82F6),
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                            TextButton(
                                onClick = {
                                    showExpenseDialog = false
                                    selectedTransaction = null
                                    expenseImageBitmap = null
                                    selectedImagePath = null
                                },
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
                                    text = "Close",
                                    color = Color(0xFF3B82F6),
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
        if (showInfoDialog && selectedTransaction != null) {
            AlertDialog(
                onDismissRequest = {
                    showInfoDialog = false
                    selectedTransaction = null
                    expenseImageBitmap = null
                    selectedImagePath = null
                },
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
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Expense Details",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 20.sp
                            ),
                            color = Color(0xFF3B82F6),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "Category: ${selectedTransaction?.category ?: "N/A"}",
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                color = Color(0xFF1F2937),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Amount: ₱${numberFormat.format(selectedTransaction?.amount?.coerceAtLeast(0.0) ?: 0.0)}",
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                color = Color(0xFF1F2937),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Date of Transaction: ${selectedTransaction?.dateOfTransaction ?: "N/A"}",
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                color = Color(0xFF1F2937),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Created At: ${selectedTransaction?.createdAt ?: "N/A"}",
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                color = Color(0xFF1F2937),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Remarks: ${selectedTransaction?.remarks ?: "N/A"}",
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                color = Color(0xFF1F2937),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = {
                                showInfoDialog = false
                                selectedTransaction = null
                                expenseImageBitmap = null
                                selectedImagePath = null
                            },
                            modifier = Modifier
                                .align(Alignment.End)
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
                                text = "Close",
                                color = Color(0xFF3B82F6),
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
        if (showFullScreenImage) {
            AlertDialog(
                onDismissRequest = {
                    showFullScreenImage = false
                    selectedTransaction = null
                    expenseImageBitmap = null
                    selectedImagePath = null
                },
                modifier = Modifier.fillMaxSize(),
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF5F5F5))
                        .padding(
                            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                            start = 16.dp,
                            end = 16.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    selectedImagePath?.let { imagePath ->
                        var imageLoadFailed by remember { mutableStateOf(false) }
                        if (tokenFetchFailed) {
                            Text(
                                text = "Authentication error: Please log in again",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = Color(0xFF4B5563),
                                modifier = Modifier.padding(16.dp)
                            )
                        } else if (selectedTransaction?.expenseId?.startsWith("local_") == true) {
                            val bitmap = try {
                                val uri = Uri.parse(imagePath)
                                BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
                            } catch (e: Exception) {
                                Log.e("HomeScreen", "Failed to load local full-screen image: $imagePath, error: ${e.message}")
                                null
                            }
                            bitmap?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = "Full screen expense photo",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            2.dp,
                                            Color(0xFF3B82F6),
                                            RoundedCornerShape(12.dp)
                                        ),
                                    contentScale = ContentScale.Fit
                                )
                            } ?: run {
                                imageLoadFailed = true
                                Text(
                                    text = "No image available",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                    color = Color(0xFF4B5563),
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else {
                            val fullImageUrl = "${ApiConfig.BASE_URL}api/expenses/${selectedTransaction?.expenseId}/images"
                            Log.d("HomeScreen", "Loading full-screen server image: $fullImageUrl with token: $token")
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(fullImageUrl)
                                    .apply {
                                        if (token != null) {
                                            addHeader("Authorization", "Bearer $token")
                                        } else {
                                            imageLoadFailed = true
                                        }
                                    }
                                    .diskCacheKey(fullImageUrl)
                                    .memoryCacheKey(fullImageUrl)
                                    .build(),
                                contentDescription = "Full screen expense photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        2.dp,
                                        Color(0xFF3B82F6),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentScale = ContentScale.Fit,
                                onError = { error ->
                                    imageLoadFailed = true
                                    scope.launch {
                                        Log.e("HomeScreen", "Failed to load full-screen server image: $fullImageUrl, error: ${error.result.throwable.message}")
                                        Toast.makeText(context, "Failed to load full screen image: ${error.result.throwable.message}", Toast.LENGTH_LONG).show()
                                        if (error.result.throwable.message?.contains("401") == true && retryCount < 2) {
                                            retryCount++
                                            val tokenResult = authRepository.getValidToken()
                                            if (tokenResult.isSuccess) {
                                                token = tokenResult.getOrNull()
                                                Log.d("HomeScreen", "Retry token fetched: $token")
                                            } else {
                                                tokenFetchFailed = true
                                                Toast.makeText(context, "Authentication error: Please log in again", Toast.LENGTH_SHORT).show()
                                                navController.navigate("login") {
                                                    popUpTo("home") { inclusive = true }
                                                }
                                            }
                                        }
                                    }
                                },
                                onSuccess = {
                                    Log.d("HomeScreen", "Successfully loaded full-screen server image: $fullImageUrl")
                                }
                            )
                        }
                    } ?: Text(
                        text = "No image available",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = Color(0xFF4B5563),
                        modifier = Modifier.padding(16.dp)
                    )
                    IconButton(
                        onClick = {
                            showFullScreenImage = false
                            selectedTransaction = null
                            expenseImageBitmap = null
                            selectedImagePath = null
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(
                                Color(0xFFE5E7EB),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close image",
                            tint = Color(0xFF1F2937)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NavigationButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
    )
    Surface(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .scale(scale),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        shadowElevation = 4.dp,
        onClick = onClick
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(28.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}