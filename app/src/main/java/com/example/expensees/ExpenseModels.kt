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
    @SerializedName("imagePath")
    val imagePaths: String?,
    @SerializedName("createdAt")
    val createdAt: String?
)