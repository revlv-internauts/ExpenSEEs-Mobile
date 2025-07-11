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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
    val liquidationViewModel: LiquidationViewModel = viewModel()

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
                modifier = modifier
            )
        }
        composable("requested_budgets") {
            RequestedBudgetsScreen(
                navController = navController,
                authRepository = authRepository,
                modifier = modifier
            )
        }
        composable(
            route = "liquidation_report/{budgetId}",
            arguments = listOf(navArgument("budgetId") { type = NavType.StringType; nullable = true })
        ) { backStackEntry ->
            val budgetId = backStackEntry.arguments?.getString("budgetId")
            LiquidationReport(
                navController = navController,
                authRepository = authRepository,
                viewModel = liquidationViewModel,
                modifier = modifier,
                budgetId = budgetId
            )
        }
        composable("detailed_liquidation_report/{budgetId}") { backStackEntry ->
            val budgetId = backStackEntry.arguments?.getString("budgetId")
            val budget = authRepository.submittedBudgets.find { it.budgetId == budgetId }
            if (budget != null) {
                DetailedLiquidationReport(
                    budget = budget,
                    selectedExpensesMap = liquidationViewModel.selectedExpensesMap,
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
        composable("notifications") {
            NotificationsScreen(
                navController = navController,
                modifier = modifier
            )
        }
        composable("liquidation_reports") {
            LiquidationReportsScreen(
                navController = navController,
                viewModel = liquidationViewModel, // Use shared ViewModel
                authRepository = authRepository, // Pass authRepository
                modifier = modifier
            )
        }
        composable("budget_details/{budgetId}") { backStackEntry ->
            val budgetId = backStackEntry.arguments?.getString("budgetId")
            if (budgetId != null) {
                BudgetDetailsScreen(
                    navController = navController,
                    authRepository = authRepository,
                    budgetId = budgetId
                )
            }
        }
    }
}