package com.example.expensees.models

import android.net.Uri
import android.os.Parcelable
import com.google.gson.annotations.SerializedName




enum class BudgetStatus {
    PENDING, DENIED, RELEASED, LIQUIDATED
}

data class ExpenseItem(
    @SerializedName("expenseItemId")
    val expenseItemId: String? = null,
    @SerializedName("category")
    val category: String,
    @SerializedName("quantity")
    val quantity: Int,
    @SerializedName("amountPerUnit")
    val amountPerUnit: Double,
    @SerializedName("remarks")
    val remarks: String
)

data class SubmittedBudget(
    @SerializedName("budgetId") val budgetId: String?,
    @SerializedName("name") val name: String,
    @SerializedName("expenses") val expenses: List<ExpenseItem>,
    @SerializedName("total") val total: Double,
    @SerializedName("status") val status: BudgetStatus,
    @SerializedName("budgetDate") val budgetDate: String?
)


data class Expense(
    @SerializedName("expenseId")
    val expenseId: String?,
    @SerializedName("category")
    val category: String?,
    @SerializedName("amount")
    val amount: Double,
    @SerializedName("dateOfTransaction")
    val dateOfTransaction: String?,
    @SerializedName("remarks")
    val remarks: String?,
    @SerializedName("imagePaths")
    val imagePaths: List<String>?,
    @SerializedName("createdAt")
    val createdAt: String?,
    @SerializedName("updatedAt")
    val updatedAt: String? = null,
    @SerializedName("username")
    val username: String? = null,
    @SerializedName("isExpenseAdded")
    val isExpenseAdded: Boolean? = false
)



data class LiquidationReportData(
    @SerializedName("liquidationId") val liquidationId: String,
    @SerializedName("budgetId") val budgetId: String,
    @SerializedName("budgetName") val budgetName: String,
    @SerializedName("totalSpent") val totalSpent: Double,
    @SerializedName("remainingBalance") val remainingBalance: Double,
    @SerializedName("status") val status: String,
    @SerializedName("dateOfTransaction") val dateOfTransaction: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("expenses") val expenses: List<Expense>,
    @SerializedName("remarks") val remarks: String? = null // Added for detailed report response
)