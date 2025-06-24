package com.example.expensees.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.expensees.models.Expense
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    expenses: List<Expense>,
    navController: NavController,
    onRecordExpensesClick: () -> Unit = {},
    onListExpensesClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    val categories = listOf(
        "Utilities", "Food", "Transportation", "Gas", "Office Supplies",
        "Rent", "Parking", "Electronic Supplies", "Other Expenses", "Grocery"
    )

    val totalExpenses = expenses.sumOf { it.amount }
    val categoryTotals = expenses.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }
        .take(5)
    val chartData = categories.map { category ->
        expenses.filter { it.category == category }.sumOf { it.amount }
    }

    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedTransaction by remember { mutableStateOf<Expense?>(null) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var expenseImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val transactionsForCategory = expenses.filter { it.category == selectedCategory }

    var showFullScreenImage by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }

    var selectedChartCategory by remember { mutableStateOf<String?>(null) }
    var selectedCategoryAmount by remember { mutableStateOf(0.0) }

    val categoryColors = categories.zip(
        listOf(
            Color(0xFFEF476F), Color(0xFF06D6A0), Color(0xFF118AB2), Color(0xFFFFD166),
            Color(0xFFF4A261), Color(0xFF8D5524), Color(0xFFC9CBA3), Color(0xFF6B7280),
            Color(0xFFFFA400), Color(0xFF2E7D32)
        )
    ).toMap()

    val animatedScale = remember { SnapshotStateList<Animatable<Float, *>>().apply {
        repeat(categoryTotals.size) { add(Animatable(0f)) }
    } }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    LaunchedEffect(categoryTotals) {
        animatedScale.forEachIndexed { index, animatable ->
            launch {
                animatable.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 300,
                        delayMillis = index * 100,
                        easing = FastOutSlowInEasing
                    )
                )
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
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
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "A",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "User Profile",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "user@example.com",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    TextButton(
                        onClick = {
                            navController.navigate("reset_password")
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Reset Password",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(
                        onClick = {
                            Toast.makeText(context, "Theme clicked", Toast.LENGTH_SHORT).show()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Theme",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(
                        onClick = {
                            Toast.makeText(context, "About clicked", Toast.LENGTH_SHORT).show()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "About",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                onLogoutClick()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(
                            text = "Logout",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp, top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ExpenSEEs",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = "Welcome to ExpenSEEs!",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (expenses.isEmpty()) {
                Text(
                    text = "No expenses recorded yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center
                )
            } else {
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
                                                '${categoryColors.values.joinToString("','") { "#${it.toArgb().toUInt().toString(16).padStart(8, '0').substring(2)}" }}'
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
                                                color: '#333333',
                                                font: { size: 18 },
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
                        .padding(vertical = 4.dp)
                        .background(Color.Transparent)
                )
                Text(
                    text = if (selectedChartCategory != null) {
                        "${selectedChartCategory} Expenses: ₱${numberFormat.format(selectedCategoryAmount)}"
                    } else {
                        "Total Expenses: ₱${numberFormat.format(totalExpenses)}"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = selectedChartCategory?.let { categoryColors[it] } ?: MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = "Your Top 5 Expenses",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            categoryTotals.forEachIndexed { index, (category, amount) ->
                                val scale by animatedScale.getOrNull(index)?.asState() ?: remember { mutableStateOf(1f) }
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
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
                                                        categoryColors[category]!!.copy(alpha = 0.2f),
                                                        categoryColors[category]!!.copy(alpha = 0.05f)
                                                    ),
                                                    start = Offset(0f, 0f),
                                                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                                )
                                            )
                                            .border(
                                                1.dp,
                                                categoryColors[category]!!,
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
                                                color = categoryColors[category]!!,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "${index + 1}",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onPrimary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = category,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = "₱${numberFormat.format(amount)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.weight(1f))
                                            Text(
                                                text = if (totalExpenses > 0) {
                                                    "${String.format("%.2f", (amount / totalExpenses) * 100)}%"
                                                } else {
                                                    "0.00%"
                                                },
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = categoryColors[category]!!,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .padding(horizontal = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent,
                    onClick = onRecordExpensesClick
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Record New Expense",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Record",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .padding(horizontal = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent,
                    onClick = onListExpensesClick
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "View List of Expenses",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "List",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .padding(horizontal = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent,
                    onClick = { navController.navigate("fund_request") }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.RequestQuote,
                                contentDescription = "Request Fund",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Request",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .padding(horizontal = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent,
                    onClick = { navController.navigate("liquidation_report") }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Assignment,
                                contentDescription = "View Liquidation Report",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Report",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .padding(horizontal = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent,
                    onClick = { scope.launch { drawerState.open() } }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Profile",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
        if (selectedCategory != null) {
            Dialog(
                onDismissRequest = { selectedCategory = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                val dialogAlpha by animateFloatAsState(
                    targetValue = if (selectedCategory != null) 1f else 0f,
                    animationSpec = tween(200, easing = LinearOutSlowInEasing)
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.85f)
                        .clip(RoundedCornerShape(12.dp))
                        .alpha(dialogAlpha),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
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
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = { selectedCategory = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close dialog",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (transactionsForCategory.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No transactions recorded for this category.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            val baseColor = categoryColors[selectedCategory]!!
                            val transactionColors = transactionsForCategory.indices.map { index ->
                                val factor = 1f - (index * 0.1f).coerceAtMost(0.5f)
                                val red = (baseColor.red * factor).coerceIn(0f, 1f) * 255
                                val green = (baseColor.green * factor).coerceIn(0f, 1f) * 255
                                val blue = (baseColor.blue * factor).coerceIn(0f, 1f) * 255
                                val r = red.toInt().coerceIn(0, 255)
                                val g = green.toInt().coerceIn(0, 255)
                                val b = blue.toInt().coerceIn(0, 255)
                                "#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}"
                            }

                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        settings.javaScriptEnabled = true
                                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                        try {
                                            loadDataWithBaseURL(
                                                null,
                                                """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
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
                            const transactionData = [${transactionsForCategory.map { it.amount }.joinToString()}];
                            const ctx = document.getElementById('transactionChart').getContext('2d');
                            const chart = new Chart(ctx, {
                                type: 'bar',
                                data: {
                                    labels: [${transactionsForCategory.mapIndexed { index, expense ->
                                                    "'${expense.description.replace("'", "\\'")}'"
                                                }.joinToString()}],
                                    datasets: [{
                                        data: transactionData,
                                        backgroundColor: [${transactionColors.joinToString { "'$it'" }}],
                                        borderColor: '#FFFFFF',
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
                                            text: 'Transaction Amounts',
                                            color: '#333333',
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
                                            ticks: {
                                                display: false
                                            }
                                        },
                                        y: {
                                            beginAtZero: true,
                                            ticks: {
                                                callback: function(value) {
                                                    return '₱' + value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
                                                },
                                                font: { size: 12 }
                                            }
                                        }
                                    },
                                    animation: {
                                        duration: 800,
                                        easing: 'easeOutCubic'
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
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                        RoundedCornerShape(8.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Transaction Details",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
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
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .alpha(alpha)
                                            .clickable {
                                                selectedTransaction = expense
                                                expense.photoUri?.let { uri ->
                                                    try {
                                                        expenseImageBitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
                                                    } catch (e: Exception) {
                                                        expenseImageBitmap = null
                                                        Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                selectedImageUri = expense.photoUri
                                                showFullScreenImage = true
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = expense.description,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = expense.dateOfTransaction,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = "₱${numberFormat.format(expense.amount)}",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
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
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = "Close",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Medium
                            )
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
                },
                title = { Text("${selectedTransaction!!.category} Receipt") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        selectedTransaction!!.photoUri?.let { uri ->
                            val bitmap = try {
                                BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
                            } catch (e: Exception) {
                                null
                            }
                            bitmap?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = "${selectedTransaction!!.category} receipt",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                        .padding(bottom = 8.dp)
                                        .clickable {
                                            expenseImageBitmap = it
                                            showFullScreenImage = true
                                        }
                                )
                            } ?: Text(
                                text = "No receipt photo available",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        } ?: Text(
                            text = "No receipt photo available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showExpenseDialog = false
                            selectedTransaction = null
                        }
                    ) {
                        Text("Close")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showInfoDialog = true
                        }
                    ) {
                        Text("Info")
                    }
                }
            )
        }
        if (showInfoDialog && selectedTransaction != null) {
            AlertDialog(
                onDismissRequest = {
                    showInfoDialog = false
                },
                title = { Text("Expense Details") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Category: ${selectedTransaction!!.category}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Amount: ₱${numberFormat.format(selectedTransaction!!.amount)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Date of Transaction: ${selectedTransaction!!.dateOfTransaction}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Date Added: ${selectedTransaction!!.dateAdded}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Remarks: ${selectedTransaction!!.description}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showInfoDialog = false
                        }
                    ) {
                        Text("Close")
                    }
                }
            )
        }
        if (showFullScreenImage) {
            AlertDialog(
                onDismissRequest = {
                    showFullScreenImage = false
                    selectedTransaction = null
                    expenseImageBitmap = null
                    selectedImageUri = null
                },
                modifier = Modifier.fillMaxSize(),
                confirmButton = {
                    TextButton(onClick = {
                        showFullScreenImage = false
                        selectedTransaction = null
                        expenseImageBitmap = null
                        selectedImageUri = null
                    }) {
                        Text("Close")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showInfoDialog = true
                    }) {
                        Text("Info")
                    }
                },
                text = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        expenseImageBitmap?.let { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Full screen expense photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } ?: selectedImageUri?.let { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = "Full screen expense photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit,
                                onError = {
                                    Toast.makeText(context, "Failed to load full screen image", Toast.LENGTH_SHORT).show()
                                }
                            )
                        } ?: Text(
                            text = "No image available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun HomeButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleMedium
        )
    }
}