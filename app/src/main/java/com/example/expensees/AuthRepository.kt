package com.example.expensees.network

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.expensees.models.Expense
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class AuthRepository(
    private val apiService: ApiService,
    private val context: Context
) {
    val userExpenses: SnapshotStateList<Expense> = mutableStateListOf()

    suspend fun login(usernameOrEmail: String, password: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.signIn(SignInRequest(usernameOrEmail, password))
                if (response.isSuccessful) {
                    response.body()?.let { signInResponse ->
                        Log.d("AuthRepository", "Login successful: token=${signInResponse.token}, userId=${signInResponse.userId}")
                        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                            .edit()
                            .putString("auth_token", signInResponse.token)
                            .putString("user_id", signInResponse.userId)
                            .apply()
                        Result.success(Unit)
                    } ?: Result.failure(Exception("Empty response"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("AuthRepository", "Login failed: HTTP ${response.code()}, body=$errorBody")
                    val errorResponse = errorBody?.let {
                        try {
                            Gson().fromJson(it, ErrorResponse::class.java)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    val errorMessage = errorResponse?.message
                        ?: errorResponse?.error
                        ?: when (response.code()) {
                            401 -> "Invalid credentials"
                            400 -> "Bad request"
                            else -> "Server error (${response.code()})"
                        }
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: HttpException) {
                Log.e("AuthRepository", "HTTP error: ${e.message()}", e)
                Result.failure(Exception("Network error: ${e.message()}"))
            } catch (e: IOException) {
                Log.e("AuthRepository", "IO error: ${e.message}", e)
                Result.failure(Exception("No internet connection"))
            } catch (e: Exception) {
                Log.e("AuthRepository", "Unexpected error: ${e.message}", e)
                Result.failure(Exception("Login failed: ${e.message}"))
            }
        }
    }

    suspend fun addExpense(expense: Expense) {
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.addExpense(expense)
                if (response.isSuccessful) {
                    response.body()?.let { userExpenses.add(it) }
                } else {
                    throw Exception("Failed to add expense")
                }
            } catch (e: Exception) {
                throw Exception("Failed to add expense: ${e.message}")
            }
        }
    }

    suspend fun deleteExpenses(expenses: List<Expense>) {
        withContext(Dispatchers.IO) {
            try {
                expenses.forEach { expense ->
                    expense.id?.let { id ->
                        val response = apiService.deleteExpense(id)
                        if (response.isSuccessful) {
                            userExpenses.remove(expense)
                        }
                    }
                }
            } catch (e: Exception) {
                throw Exception("Failed to delete expenses: ${e.message}")
            }
        }
    }

    fun logout() {
        userExpenses.clear()
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}