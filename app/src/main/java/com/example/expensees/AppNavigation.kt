package com.example.expensees.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.expensees.*
import com.example.expensees.models.Expense
import com.example.expensees.models.SubmittedBudget
import com.example.expensees.screens.ExpenseListScreen
import com.example.expensees.screens.ForgotPassword
import com.example.expensees.screens.FundRequest
import com.example.expensees.screens.HomeScreen
import com.example.expensees.screens.LiquidationReport
import com.example.expensees.screens.LoadingScreen
import com.example.expensees.screens.LoginScreen
import com.example.expensees.screens.RecordExpensesScreen
import com.example.expensees.screens.ResetPassword

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val expenses = remember { mutableStateListOf<Expense>() }
    val submittedBudgets = remember { mutableStateListOf<SubmittedBudget>() }

    NavHost(navController = navController, startDestination = "loading") {
        composable("loading") {
            LoadingScreen(
                modifier = modifier,
                onLoadingComplete = {
                    navController.navigate("login") {
                        popUpTo("loading") { inclusive = true }
                    }
                }
            )
        }
        composable("login") {
            LoginScreen(
                modifier = modifier,
                navController = navController,
                onLoginClick = { _, _ ->
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            HomeScreen(
                expenses = expenses,
                navController = navController,
                onRecordExpensesClick = { navController.navigate("record_expenses") },
                onListExpensesClick = { navController.navigate("list_expenses") },
                onLogoutClick = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                modifier = modifier
            )
        }
        composable("record_expenses") {
            RecordExpensesScreen(
                navController = navController,
                expenses = expenses,
                onLogoutClick = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                modifier = modifier
            )
        }
        composable("list_expenses") {
            ExpenseListScreen(
                navController = navController,
                expenses = expenses,
                onDeleteExpenses = { expensesToDelete ->
                    expenses.removeAll(expensesToDelete)
                },
                onLogoutClick = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                modifier = modifier
            )
        }
        composable("fund_request") {
            FundRequest(
                navController = navController,
                submittedBudgets = submittedBudgets,
                modifier = modifier
            )
        }
        composable("liquidation_report") {
            LiquidationReport(
                submittedBudgets = submittedBudgets,
                expenses = expenses,
                navController = navController,
                modifier = modifier
            )
        }
        composable("reset_password") {
            ResetPassword(
                navController = navController,
                modifier = modifier
            )
        }
        composable("forgot_password") {
            ForgotPassword(
                navController = navController,
                modifier = modifier
            )
        }
    }
}
