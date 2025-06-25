package com.example.expensees.network

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.*
import com.example.expensees.models.*
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.text.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.util.Locale

class AuthRepository(
    private val apiService: ApiService,
    private val context: Context
) {
    val userExpenses: SnapshotStateList<Expense> = mutableStateListOf()

    suspend fun login(usernameOrEmail: String, password: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AuthRepository", "Creating SignInRequest: usernameOrEmail='$usernameOrEmail', password='$password'")
                val request = SignInRequest(usernameOrEmail, password)
                val response = apiService.signIn(request)
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

    suspend fun addExpense(expense: Expense): Result<Expense> {
        return withContext(Dispatchers.IO) {
            try {
                val token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                    .getString("auth_token", null)
                Log.d("AuthRepository", "Retrieved token: $token")
                if (token.isNullOrEmpty()) {
                    Log.e("AuthRepository", "No token found in SharedPreferences")
                    return@withContext Result.failure(Exception("Not authenticated. Please log in."))
                }

                // Validate and convert nullable fields to RequestBody
                val category = expense.category?.toRequestBody("text/plain".toMediaType())
                    ?: throw Exception("Category is required")
                val amount = expense.amount.toString().toRequestBody("text/plain".toMediaType())
                val dateOfTransaction = expense.dateOfTransaction?.toRequestBody("text/plain".toMediaType())
                    ?: throw Exception("Date of transaction is required")
                val comments = expense.comments?.toRequestBody("text/plain".toMediaType())
                val createdAt = expense.createdAt?.toRequestBody("text/plain".toMediaType())
                    ?: throw Exception("Created at timestamp is required")

                // Log request details
                Log.d("AuthRepository", "Adding expense: category=${expense.category}, amount=${expense.amount}, dateOfTransaction=${expense.dateOfTransaction}, comments=${expense.comments}, createdAt=${expense.createdAt}, imagePath=${expense.imagePath}")

                val imagePart = expense.imagePath?.let { uri ->
                    val file = File(context.cacheDir, "expense_image_${System.currentTimeMillis()}.jpg")
                    try {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            file.outputStream().use { output -> input.copyTo(output) }
                        }
                        val requestFile = file.asRequestBody("image/jpeg".toMediaType())
                        MultipartBody.Part.createFormData("image", file.name, requestFile)
                    } catch (e: Exception) {
                        Log.e("AuthRepository", "Failed to process image: ${e.message}", e)
                        null
                    }
                }

                Log.d("AuthRepository", "Sending request to /api/expenses with token: Bearer $token")
                val response = apiService.addExpense(
                    token = "Bearer $token",
                    category = category,
                    amount = amount,
                    dateOfTransaction = dateOfTransaction,
                    comments = comments,
                    createdAt = createdAt,
                    image = imagePart
                )

                Log.d("AuthRepository", "Add expense response: HTTP ${response.code()}, body=${response.body()?.let { Gson().toJson(it) } ?: "null"}, errorBody=${response.errorBody()?.string() ?: "null"}")

                if (response.isSuccessful) {
                    response.body()?.let { returnedExpense ->
                        if (returnedExpense.id.isNullOrEmpty()) {
                            Log.e("AuthRepository", "Expense returned but has no ID: ${Gson().toJson(returnedExpense)}")
                            Result.failure(Exception("Expense not saved in database (missing ID)"))
                        } else {
                            userExpenses.add(returnedExpense)
                            Log.d("AuthRepository", "Expense added: id=${returnedExpense.id}")
                            Result.success(returnedExpense)
                        }
                    } ?: Result.failure(Exception("Empty response body"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("AuthRepository", "Add expense failed: HTTP ${response.code()}, body=$errorBody")
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
                            401 -> "Unauthorized: Invalid or expired token"
                            400 -> "Invalid expense data: check field formats"
                            422 -> "Validation error: check required fields"
                            else -> "Server error (${response.code()})"
                        }
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: HttpException) {
                Log.e("AuthRepository", "HTTP error: ${e.message()}, code=${e.code()}", e)
                Result.failure(Exception("Network error: ${e.message()}"))
            } catch (e: IOException) {
                Log.e("AuthRepository", "IO error: ${e.message}", e)
                Result.failure(Exception("No internet connection"))
            } catch (e: Exception) {
                Log.e("AuthRepository", "Add expense error: ${e.message}", e)
                Result.failure(Exception("Failed to add expense: ${e.message}"))
            }
        }
    }

    suspend fun deleteExpenses(expenses: List<Expense>) {
        withContext(Dispatchers.IO) {
            try {
                val token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                    .getString("auth_token", null)
                    ?: throw Exception("Not authenticated. Please log in.")
                expenses.forEach { expense ->
                    expense.id?.let { id ->
                        val response = apiService.deleteExpense("Bearer $token", id)
                        if (response.isSuccessful) {
                            userExpenses.remove(expense)
                        } else {
                            throw Exception("Failed to delete expense: HTTP ${response.code()}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthRepository", "Delete expenses error: ${e.message}", e)
                throw Exception("Failed to delete expenses: ${e.message}")
            }
        }
    }

    fun logout() {
        Log.d("AuthRepository", "Logging out: clearing expenses and SharedPreferences")
        userExpenses.clear()
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    fun isAuthenticated(): Boolean {
        val token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            .getString("auth_token", null)
        Log.d("AuthRepository", "Checking authentication: token=$token")
        return !token.isNullOrEmpty()
    }
}