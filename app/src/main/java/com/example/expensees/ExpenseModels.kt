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
    val createdAt: String?
)

data class LiquidationReportData(
    val reportId: String = java.util.UUID.randomUUID().toString(),
    val budgetId: String,
    val budgetName: String,
    val generatedAt: String,
    val totalBudgeted: Double,
    val totalActual: Double,
    val totalRemaining: Double,
    val expenses: Map<Int, List<Expense>> // Map of expense item index to selected expenses
)