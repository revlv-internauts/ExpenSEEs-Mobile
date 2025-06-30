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
import com.example.expensees.models.Expense
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    expenses: SnapshotStateList<Expense>,
    navController: NavController,
    onRecordExpensesClick: () -> Unit = {},
    onListExpensesClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
        isGroupingUsed = true // Ensure thousands separators for readability
    }

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

    val categoryColors = categories.zip(
        listOf(
            Color(0xFF6B7280), // Muted Slate Gray
            Color(0xFF9CA3AF), // Soft Gray-Blue
            Color(0xFF8B9DC3), // Muted Blue
            Color(0xFF7F9E9F), // Muted Teal
            Color(0xFFA7B4C5), // Pale Blue-Gray
            Color(0xFFB7C1C3), // Light Cyan
            Color(0xFF9E9E9E), // Neutral Gray
            Color(0xFFB0BEC5), // Light Slate
            Color(0xFFA5B4B6), // Muted Aqua
            Color(0xFFBCC1C5)  // Pale Gray
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
                        durationMillis = 400,
                        delayMillis = index * 100,
                        easing = FastOutSlowInEasing
                    )
                )
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false, // Disable all built-in swipe gestures
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFFF5F5F5), // Matte off-white
                drawerContentColor = Color(0xFF1F2937), // Dark gray
                modifier = Modifier.pointerInput(Unit) {
                    var startX = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            startX = offset.x
                        },
                        onDragEnd = {
                            if (drawerState.isOpen) {
                                scope.launch {
                                    drawerState.close()
                                }
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            if (drawerState.isOpen && dragAmount < 0) {
                                // Allow swipe left to close (negative drag amount)
                                scope.launch {
                                    drawerState.close()
                                }
                            }
                            // Ignore swipe right (positive drag amount) to prevent opening
                        }
                    )
                }
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
                            color = Color(0xFFD6D8DA) // Matte silver-gray
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "A",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color(0xFF1F2937), // Dark gray
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "User Profile",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF1F2937) // Dark gray
                            )
                            Text(
                                text = "user@example.com",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF4B5563) // Darker gray for contrast
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Color(0xFFE5E7EB) // Light gray
                    )
                    TextButton(
                        onClick = {
                            navController.navigate("reset_password")
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "Reset Password",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF3B82F6), // Blue 500
                            fontSize = 16.sp
                        )
                    }
                    TextButton(
                        onClick = {
                            Toast.makeText(context, "Theme clicked", Toast.LENGTH_SHORT).show()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "Theme",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF3B82F6), // Blue 500
                            fontSize = 16.sp
                        )
                    }
                    TextButton(
                        onClick = {
                            Toast.makeText(context, "About clicked", Toast.LENGTH_SHORT).show()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "About",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF3B82F6), // Blue 500
                            fontSize = 16.sp
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
                            .height(50.dp)
                            .padding(vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3B82F6) // Blue 500
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Logout",
                            fontSize = 18.sp,
                            color = Color(0xFFFFFFFF), // White
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE3F2FD), // Light blue (Blue 50)
                            Color(0xFFBBDEFB) // Slightly darker blue (Blue 100)
                        )
                    )
                )
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 50.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { scope.launch { drawerState.open() } },
                    modifier = Modifier
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open navigation drawer",
                        tint = Color(0xFF1F2937) // Dark gray
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Welcome to ExpenSEEs!",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF1F2937), // Dark gray
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
            if (expenses.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFDBEAFE) // Blue 50
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "No expenses recorded yet.",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = Color(0xFF1F2937), // Dark gray
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFDBEAFE) // Blue 50
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    shape = RoundedCornerShape(12.dp)
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
                                background: #DBEAFE; /* Blue 50 */
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
                            .height(240.dp)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(8.dp))
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
                    color = selectedChartCategory?.let { categoryColors[it] } ?: Color(0xFF1F2937), // Dark gray
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFDBEAFE) // Blue 50
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = "Your Top 5 Expenses",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp
                            ),
                            color = Color(0xFF1F2937), // Dark gray
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp), // Allow scrolling if content exceeds this height
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            itemsIndexed(categoryTotals) { index, (category, amount) ->
                                val scale by animatedScale.getOrNull(index)?.asState() ?: remember { mutableStateOf(1f) }
                                val categoryColor = categoryColors[category] ?: Color(0xFF3B82F6) // Fallback to Blue 500
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
                                                        categoryColor.copy(alpha = 0.15f),
                                                        categoryColor.copy(alpha = 0.05f)
                                                    ),
                                                    start = Offset(0f, 0f),
                                                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                                )
                                            )
                                            .border(
                                                1.dp,
                                                categoryColor.copy(alpha = 0.5f),
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
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = Color(0xFFFFFFFF), // White
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = category ?: "Unknown",
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 14.sp
                                                    ),
                                                    color = Color(0xFF1F2937), // Dark gray
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "₱${numberFormat.format(amount.coerceAtLeast(0.0))}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF4B5563), // Darker gray for contrast
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.widthIn(max = 120.dp) // Constrain amount text width
                                                )
                                            }
                                            Text(
                                                text = if (totalExpenses > 0) {
                                                    "${String.format("%.2f", (amount / totalExpenses) * 100)}%"
                                                } else {
                                                    "0.00%"
                                                },
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                ),
                                                color = categoryColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.widthIn(max = 60.dp) // Constrain percentage text width
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFDBEAFE) // Blue 50
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    NavigationButton(
                        icon = Icons.Default.Add,
                        label = "Record",
                        onClick = onRecordExpensesClick,
                        modifier = Modifier.weight(1f)
                    )
                    NavigationButton(
                        icon = Icons.Default.RequestQuote,
                        label = "Request",
                        onClick = { navController.navigate("fund_request") },
                        modifier = Modifier.weight(1f)
                    )
                    NavigationButton(
                        icon = Icons.Default.Assignment,
                        label = "Report",
                        onClick = { navController.navigate("liquidation_report") },
                        modifier = Modifier.weight(1f)
                    )
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
                    animationSpec = tween(300, easing = LinearOutSlowInEasing)
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.85f)
                        .clip(RoundedCornerShape(12.dp))
                        .alpha(dialogAlpha),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFDBEAFE) // Blue 50
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                                color = Color(0xFF1F2937) // Dark gray
                            )
                            IconButton(
                                onClick = { selectedCategory = null },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        Color(0xFFCED4DA), // Matte cool gray
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close dialog",
                                    tint = Color(0xFF4B5563) // Darker gray for contrast
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
                                    color = Color(0xFF1F2937) // Dark gray
                                )
                            }
                        } else {
                            val baseColor = categoryColors[selectedCategory]!!
                            val transactionColors = transactionsForCategory.indices.map { index ->
                                val factor = 0.8f - (index * 0.1f).coerceAtMost(0.5f)
                                "#${baseColor.copy(alpha = factor).toArgb().toUInt().toString(16).padStart(8, '0').substring(2)}"
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
                                background: #DBEAFE; /* Blue 50 */
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
                                                    "'${(expense.remarks ?: "").replace("'", "\\'")}'"
                                                }.joinToString()}],
                                    datasets: [{
                                        data: transactionData,
                                        backgroundColor: [${transactionColors.joinToString() { "'$it'" }}],
                                        borderColor: '#D1D5DB', // Light gray border for subtle contrast
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
                                            ticks: { display: false }
                                        },
                                        y: {
                                            beginAtZero: true,
                                            ticks: {
                                                callback: function(value) {
                                                    return '₱' + value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
                                                },
                                                font: { size: 12 },
                                                color: '#1F2937' // Dark gray for tick labels
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
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        1.dp,
                                        Color(0xFFE5E7EB), // Light gray
                                        RoundedCornerShape(8.dp)
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
                                color = Color(0xFF1F2937), // Dark gray
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
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .alpha(alpha)
                                            .clickable {
                                                selectedTransaction = expense
                                                selectedImagePath = expense.imagePath
                                                showExpenseDialog = true
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFDBEAFE) // Blue 50
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
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
                                                    text = expense.remarks ?: "",
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        fontWeight = FontWeight.Medium,
                                                        fontSize = 16.sp
                                                    ),
                                                    color = Color(0xFF1F2937), // Dark gray
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = expense.dateOfTransaction ?: "",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color(0xFF4B5563), // Darker gray for contrast
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
                                                color = categoryColors[expense.category] ?: Color(0xFF3B82F6), // Fallback to Blue 500
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.widthIn(max = 100.dp) // Constrain transaction amount width
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
                                containerColor = Color(0xFF3B82F6) // Blue 500
                            )
                        ) {
                            Text(
                                text = "Close",
                                fontSize = 16.sp,
                                color = Color(0xFFFFFFFF), // White
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
                    expenseImageBitmap = null
                    selectedImagePath = null
                },
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .clip(RoundedCornerShape(12.dp)),
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFDBEAFE) // Blue 50
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                            color = Color(0xFF3B82F6), // Blue 500
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        selectedImagePath?.let { imagePath ->
                            var imageLoadFailed by remember { mutableStateOf(false) }
                            if (imageLoadFailed) {
                                Text(
                                    text = "No receipt photo available",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF4B5563), // Darker gray for contrast
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            } else if (selectedTransaction?.expenseId?.startsWith("local_") == true) {
                                val bitmap = try {
                                    val uri = Uri.parse(imagePath)
                                    BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
                                } catch (e: Exception) {
                                    null
                                }
                                bitmap?.let {
                                    Image(
                                        bitmap = it.asImageBitmap(),
                                        contentDescription = "${selectedTransaction?.category ?: "Receipt"} receipt",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(220.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(
                                                1.5.dp,
                                                Color(0xFF3B82F6), // Blue 500
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
                                        color = Color(0xFF4B5563), // Darker gray for contrast
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                }
                            } else {
                                AsyncImage(
                                    model = imagePath,
                                    contentDescription = "${selectedTransaction?.category ?: "Receipt"} receipt",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            1.5.dp,
                                            Color(0xFF3B82F6), // Blue 500
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { showFullScreenImage = true },
                                    contentScale = ContentScale.Crop,
                                    onError = {
                                        imageLoadFailed = true
                                        scope.launch {
                                            Toast.makeText(context, "Failed to load receipt image", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        } ?: Text(
                            text = "No receipt photo available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF4B5563), // Darker gray for contrast
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = { showInfoDialog = true }) {
                                Text(
                                    text = "Info",
                                    color = Color(0xFF3B82F6), // Blue 500
                                    fontSize = 16.sp
                                )
                            }
                            TextButton(onClick = {
                                showExpenseDialog = false
                                selectedTransaction = null
                                expenseImageBitmap = null
                                selectedImagePath = null
                            }) {
                                Text(
                                    text = "Close",
                                    color = Color(0xFF3B82F6), // Blue 500
                                    fontSize = 16.sp
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
                    .clip(RoundedCornerShape(12.dp)),
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFDBEAFE) // Blue 50
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                            color = Color(0xFF3B82F6), // Blue 500
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "Category: ${selectedTransaction?.category ?: "N/A"}",
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                color = Color(0xFF1F2937), // Dark gray
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Amount: ₱${numberFormat.format(selectedTransaction?.amount?.coerceAtLeast(0.0) ?: 0.0)}",
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                color = Color(0xFF1F2937), // Dark gray
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Date of Transaction: ${selectedTransaction?.dateOfTransaction ?: "N/A"}",
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                color = Color(0xFF1F2937), // Dark gray
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Created At: ${selectedTransaction?.createdAt ?: "N/A"}",
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                color = Color(0xFF1F2937), // Dark gray
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Remarks: ${selectedTransaction?.remarks ?: "N/A"}",
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                color = Color(0xFF1F2937), // Dark gray
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
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                text = "Close",
                                color = Color(0xFF3B82F6), // Blue 500
                                fontSize = 16.sp
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
                        .background(Color(0xFFDBEAFE)) // Blue 50
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
                        if (imageLoadFailed) {
                            Text(
                                text = "No image available",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = Color(0xFF4B5563), // Darker gray for contrast
                                modifier = Modifier.padding(16.dp)
                            )
                        } else if (selectedTransaction?.expenseId?.startsWith("local_") == true) {
                            val bitmap = try {
                                val uri = Uri.parse(imagePath)
                                BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
                            } catch (e: Exception) {
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
                                            Color(0xFF3B82F6), // Blue 500
                                            RoundedCornerShape(12.dp)
                                        ),
                                    contentScale = ContentScale.Fit
                                )
                            } ?: run {
                                imageLoadFailed = true
                                Text(
                                    text = "No image available",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                    color = Color(0xFF4B5563), // Darker gray for contrast
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else {
                            AsyncImage(
                                model = imagePath,
                                contentDescription = "Full screen expense photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        2.dp,
                                        Color(0xFF3B82F6), // Blue 500
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentScale = ContentScale.Fit,
                                onError = {
                                    imageLoadFailed = true
                                    scope.launch {
                                        Toast.makeText(context, "Failed to load full screen image", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    } ?: Text(
                        text = "No image available",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = Color(0xFF4B5563), // Darker gray for contrast
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
                                Color(0xFFCED4DA), // Matte cool gray
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close image",
                            tint = Color(0xFF1F2937) // Dark gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NavigationButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
        color = Color(0xFFDBEAFE), // Blue 50
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(28.dp),
                tint = Color(0xFF3B82F6) // Blue 500
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color(0xFF3B82F6), // Blue 500
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}