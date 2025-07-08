package com.example.expensees.navigation

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.expensees.models.Expense
import com.example.expensees.models.SubmittedBudget
import com.example.expensees.network.AuthRepository
import com.example.expensees.screens.*
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation(modifier: Modifier = Modifier, authRepository: AuthRepository) {
    val navController = rememberNavController()
    val submittedBudgets = remember { mutableStateListOf<SubmittedBudget>() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
                authRepository = authRepository,
                modifier = modifier
            )
        }
        composable("home") {
            HomeScreen(
                expenses = authRepository.userExpenses,
                navController = navController,
                authRepository = authRepository,
                onRecordExpensesClick = { navController.navigate("record_expenses") },
                onListExpensesClick = { navController.navigate("list_expenses") },
                onLogoutClick = {
                    scope.launch {
                        try {
                            authRepository.logout()
                            navController.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Logout failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = modifier
            )
        }
        composable("record_expenses") {
            RecordExpensesScreen(
                navController = navController,
                authRepository = authRepository,
                onLogoutClick = {
                    scope.launch {
                        try {
                            authRepository.logout()
                            navController.navigate("login") {
                                popUpTo("record_expenses") { inclusive = true }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Logout failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = modifier
            )
        }
        composable("list_expenses") {
            ExpenseListScreen(
                navController = navController,
                expenses = authRepository.userExpenses,
                authRepository = authRepository,
                onDeleteExpenses = { expensesToDelete ->
                    scope.launch {
                        try {
                            authRepository.deleteExpenses(expensesToDelete)
                            Toast.makeText(context, "Expenses deleted successfully", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to delete expenses: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = modifier
            )
        }
        composable("fund_request") {
            FundRequest(
                navController = navController,
                authRepository = authRepository,
                modifier = Modifier
            )
        }
        composable("liquidation_Report") {
            LiquidationReport(
                navController = navController,
                authRepository = authRepository
            )
        }
        composable("detailed_liquidation_report/{budgetId}") { backStackEntry ->
            val budgetId = backStackEntry.arguments?.getString("budgetId")
            val budget = authRepository.submittedBudgets.find { it.budgetId == budgetId }
            if (budget != null) {
                DetailedLiquidationReport(
                    budget = budget,
                    selectedExpensesMap = authRepository.submittedBudgets
                        .find { it.budgetId == budgetId }
                        ?.let { selectedBudget ->
                            selectedBudget.expenses.withIndex().associate { (index, _) ->
                                index to (authRepository.userExpenses.filter { expense ->
                                    expense.category == selectedBudget.expenses[index].category
                                })
                            }
                        } ?: emptyMap(),
                    navController = navController,
                    modifier = modifier
                )
            } else {
                Toast.makeText(context, "Budget not found", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            }
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