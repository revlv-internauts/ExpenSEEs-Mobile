package com.example.expensees.models

import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val description: String,
    val amount: Double,
    val category: String,
    val photoUri: Uri? = null,
    val dateOfTransaction: String,
    val dateAdded: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
) {
    companion object {
        const val DEFAULT_CATEGORY = "Other"
    }
}
