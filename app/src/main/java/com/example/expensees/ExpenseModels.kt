package com.example.expensees.models

import android.net.Uri
import com.google.gson.annotations.SerializedName



enum class BudgetStatus {
    PENDING, APPROVED, DENIED
}

data class ExpenseItem(
    val category: String,
    val quantity: Int,
    val amountPerUnit: Double,
    val remarks: String = ""
)

data class SubmittedBudget(
    val name: String,
    val expenses: List<ExpenseItem>,
    val total: Double,
    val status: BudgetStatus = BudgetStatus.PENDING
)

data class Expense(
    @SerializedName("expense_id")
    val expenseId: String?,
    @SerializedName("category")
    val category: String?,
    @SerializedName("amount")
    val amount: Double,
    @SerializedName("date_of_transaction")
    val dateOfTransaction: String?,
    @SerializedName("remarks")
    val remarks: String?,
    @SerializedName("image_path")
    val imagePath: String?,
    @SerializedName("created_at")
    val createdAt: String?
)