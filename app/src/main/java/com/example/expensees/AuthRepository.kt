package com.example.expensees.network

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.expensees.models.Expense
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException

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

    suspend fun addExpense(expense: Expense) {
        withContext(Dispatchers.IO) {
            try {
                val token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                    .getString("auth_token", null)
                    ?: throw Exception("Not authenticated. Please log in.")

                // Validate and convert nullable fields to RequestBody
                val category = expense.category?.toRequestBody("text/plain".toMediaType())
                    ?: throw Exception("Category is required")
                val amount = expense.amount.toString().toRequestBody("text/plain".toMediaType())
                val dateOfTransaction = expense.dateOfTransaction?.toRequestBody("text/plain".toMediaType())
                    ?: throw Exception("Date of transaction is required")
                val comments = expense.comments?.toRequestBody("text/plain".toMediaType())
                val createdAt = expense.createdAt?.toRequestBody("text/plain".toMediaType())
                    ?: throw Exception("Created at timestamp is required")

                // Log expense details for debugging
                Log.d("AuthRepository", "Adding expense: category=${expense.category}, amount=${expense.amount}, dateOfTransaction=${expense.dateOfTransaction}, comments=${expense.comments}, createdAt=${expense.createdAt}, imagePath=${expense.imagePath}")

                val imagePart = expense.imagePath?.let { uri ->
                    val file = File(context.cacheDir, "expense_image_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    }
                    val requestFile = file.asRequestBody("image/jpeg".toMediaType())
                    MultipartBody.Part.createFormData("image", file.name, requestFile)
                }

                val response = apiService.addExpense(
                    token = "Bearer $token",
                    category = category,
                    amount = amount,
                    dateOfTransaction = dateOfTransaction,
                    comments = comments,
                    createdAt = createdAt,
                    image = imagePart
                )

                if (response.isSuccessful) {
                    response.body()?.let { returnedExpense ->
                        userExpenses.add(returnedExpense)
                        Log.d("AuthRepository", "Expense added: id=${returnedExpense.id}")
                    } ?: throw Exception("Empty response")
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
                            401 -> "Unauthorized: Please log in again"
                            400 -> "Invalid expense data"
                            else -> "Server error (${response.code()})"
                        }
                    throw Exception(errorMessage)
                }
            } catch (e: Exception) {
                Log.e("AuthRepository", "Add expense error: ${e.message}", e)
                throw Exception("Failed to add expense: ${e.message}")
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
        userExpenses.clear()
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}