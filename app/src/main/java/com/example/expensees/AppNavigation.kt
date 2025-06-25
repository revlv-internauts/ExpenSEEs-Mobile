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
import com.example.expensees.models.Expense
import com.example.expensees.models.SubmittedBudget
import com.example.expensees.network.AuthRepository
import com.example.expensees.screens.*
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation(modifier: Modifier = Modifier, authRepository: AuthRepository) {
    val navController = rememberNavController()
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
                navController = navController,
                authRepository = authRepository
            )
        }
        composable("home") {
            HomeScreen(
                expenses = authRepository.userExpenses,
                navController = navController,
                onRecordExpensesClick = { navController.navigate("record_expenses") },
                onListExpensesClick = { navController.navigate("list_expenses") },
                onLogoutClick = {
                    authRepository.logout()
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
        composable("record_expenses") {
            RecordExpensesScreen(
                navController = navController,
                authRepository = authRepository,
                onLogoutClick = {
                    authRepository.logout()
                    navController.navigate("login") {
                        popUpTo("record_expenses") { inclusive = true }
                    }
                },
                modifier = modifier
            )
        }
        composable("list_expenses") {
            val scope = rememberCoroutineScope()
            ExpenseListScreen(
                navController = navController,
                expenses = authRepository.userExpenses,
                onDeleteExpenses = { expensesToDelete ->
                    scope.launch {
                        try {
                            authRepository.deleteExpenses(expensesToDelete)
                        } catch (e: Exception) {
                            // Optionally, handle errors here (e.g., log or notify UI via Snackbar in ExpenseListScreen)
                        }
                    }
                },
                onLogoutClick = {
                    authRepository.logout()
                    navController.navigate("login") {
                        popUpTo("list_expenses") { inclusive = true }
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
                expenses = authRepository.userExpenses,
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