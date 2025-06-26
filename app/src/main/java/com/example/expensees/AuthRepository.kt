package com.example.expensees.network

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.auth0.jwt.JWT
import com.auth0.jwt.exceptions.JWTDecodeException
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AuthRepository(
    private val apiService: ApiService,
    private val context: Context
) {
    val userExpenses: SnapshotStateList<Expense> = mutableStateListOf()

    suspend fun login(usernameOrEmail: String, password: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AuthRepository", "Creating SignInRequest: usernameOrEmail='$usernameOrEmail'")
                val request = SignInRequest(usernameOrEmail, password)
                val response = apiService.signIn(request)
                val rawResponseBody = response.body()?.let { Gson().toJson(it) } ?: "null"
                val errorBody = response.errorBody()?.string() ?: "null"
                Log.d("AuthRepository", "Sign-in response: HTTP ${response.code()}, body=$rawResponseBody, errorBody=$errorBody")
                if (response.isSuccessful) {
                    response.body()?.let { signInResponse ->
                        Log.d("AuthRepository", "Response details: token=${signInResponse.token?.take(10) ?: "null"}..., userId=${signInResponse.user_Id}, refreshToken=${signInResponse.refreshToken?.take(10) ?: "null"}...")
                        if (signInResponse.token.isNullOrEmpty() || signInResponse.user_Id.isNullOrEmpty()) {
                            Log.e("AuthRepository", "Login failed: token or userId is null or empty")
                            Result.failure(Exception("Invalid login response: token or userId is missing"))
                        } else {
                            Log.d("AuthRepository", "Login successful: token=${signInResponse.token.take(10)}..., userId=${signInResponse.user_Id}")
                            context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                                .edit()
                                .putString("auth_token", signInResponse.token)
                                .putString("user_id", signInResponse.user_Id)
                                .putString("refresh_token", signInResponse.refreshToken)
                                .apply()
                            Result.success(Unit)
                        }
                    } ?: Result.failure(Exception("Empty response body"))
                } else {
                    val errorResponse = response.errorBody()?.string()?.let {
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
                    Log.e("AuthRepository", "Login failed: HTTP ${response.code()}, message=$errorMessage")
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: HttpException) {
                Log.e("AuthRepository", "HTTP error: ${e.message()}, code=${e.code()}", e)
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
                // Validate date formats
                try {
                    expense.dateOfTransaction?.let {
                        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(it)
                            ?: throw IllegalArgumentException("Invalid dateOfTransaction format: $it")
                    } ?: throw IllegalArgumentException("dateOfTransaction is required")
                    expense.createdAt?.let {
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).parse(it)
                            ?: throw IllegalArgumentException("Invalid createdAt format: $it")
                    } ?: throw IllegalArgumentException("createdAt is required")
                } catch (e: Exception) {
                    Log.e("AuthRepository", "Date validation error: ${e.message}", e)
                    return@withContext Result.failure(Exception("Invalid date format: ${e.message}"))
                }

                // Validate and convert fields to RequestBody
                val category = expense.category?.toRequestBody("text/plain".toMediaType())
                    ?: throw Exception("Category is required")
                val amount = expense.amount.toString().toRequestBody("text/plain".toMediaType())
                val dateOfTransaction = expense.dateOfTransaction?.toRequestBody("text/plain".toMediaType())
                    ?: throw Exception("Date of transaction is required")
                val remarks = expense.remarks?.toRequestBody("text/plain".toMediaType())
                val createdAt = expense.createdAt?.toRequestBody("text/plain".toMediaType())
                    ?: throw Exception("Created at timestamp is required")

                Log.d("AuthRepository", "Adding expense: category=${expense.category}, amount=${expense.amount}, dateOfTransaction=${expense.dateOfTransaction}, remarks=${expense.remarks}, createdAt=${expense.createdAt}, imagePath=${expense.imagePath}")

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

                Log.d("AuthRepository", "Sending request to /api/expense without token")
                val response = apiService.addExpense(
                    category = category,
                    amount = amount,
                    dateOfTransaction = dateOfTransaction,
                    remarks = remarks,
                    createdAt = createdAt,
                    image = imagePart
                )

                Log.d("AuthRepository", "Add expense response: HTTP ${response.code()}, body=${response.body()?.let { Gson().toJson(it) } ?: "null"}, errorBody=${response.errorBody()?.string() ?: "null"}")

                if (response.isSuccessful) {
                    response.body()?.let { returnedExpense ->
                        if (returnedExpense.expenseId.isNullOrEmpty()) {
                            Log.e("AuthRepository", "Expense returned but has no expenseId: ${Gson().toJson(returnedExpense)}")
                            Result.failure(Exception("Expense not saved in database (missing expenseId)"))
                        } else {
                            userExpenses.add(returnedExpense)
                            Log.d("AuthRepository", "Expense added: expenseId=${returnedExpense.expenseId}")
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
                            400 -> "Invalid expense data: check field formats (e.g., amount, date)"
                            422 -> "Validation error: ensure all required fields are provided"
                            else -> "Server error (${response.code()}). Please try again."
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

    private suspend fun refreshToken(refreshToken: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AuthRepository", "Attempting to refresh token with refreshToken: ${refreshToken.take(10)}...")
                val response = apiService.refreshToken(RefreshTokenRequest(refreshToken))
                Log.d("AuthRepository", "Refresh token response: HTTP ${response.code()}, body=${response.body()?.let { Gson().toJson(it) } ?: "null"}, errorBody=${response.errorBody()?.string() ?: "null"}")
                if (response.isSuccessful) {
                    response.body()?.let { signInResponse ->
                        if (signInResponse.token.isNullOrEmpty() || signInResponse.user_Id.isNullOrEmpty()) {
                            Log.e("AuthRepository", "Refresh token failed: token or userId is null or empty")
                            Result.failure(Exception("Invalid refresh token response"))
                        } else {
                            context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                                .edit()
                                .putString("auth_token", signInResponse.token)
                                .putString("user_id", signInResponse.user_Id)
                                .putString("refresh_token", signInResponse.refreshToken)
                                .apply()
                            Log.d("AuthRepository", "Token refreshed successfully: new token=${signInResponse.token.take(10)}...")
                            Result.success(Unit)
                        }
                    } ?: Result.failure(Exception("Empty refresh token response"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("AuthRepository", "Refresh token failed: HTTP ${response.code()}, body=$errorBody")
                    Result.failure(Exception("Failed to refresh token: HTTP ${response.code()}"))
                }
            } catch (e: HttpException) {
                Log.e("AuthRepository", "HTTP error refreshing token: ${e.message()}, code=${e.code()}", e)
                Result.failure(Exception("Network error: ${e.message()}"))
            } catch (e: IOException) {
                Log.e("AuthRepository", "IO error refreshing token: ${e.message}", e)
                Result.failure(Exception("No internet connection"))
            } catch (e: Exception) {
                Log.e("AuthRepository", "Refresh token error: ${e.message}", e)
                Result.failure(Exception("Failed to refresh token: ${e.message}"))
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
                    expense.expenseId?.let { expenseId ->
                        Log.d("AuthRepository", "Deleting expense: expenseId=$expenseId")
                        val response = apiService.deleteExpense("Bearer $token", expenseId)
                        if (response.isSuccessful) {
                            userExpenses.remove(expense)
                            Log.d("AuthRepository", "Expense deleted: expenseId=$expenseId")
                        } else {
                            Log.e("AuthRepository", "Failed to delete expense: HTTP ${response.code()}, errorBody=${response.errorBody()?.string() ?: "null"}")
                            throw Exception("Failed to delete expense: HTTP ${response.code()}")
                        }
                    } ?: Log.w("AuthRepository", "Skipping expense with null expenseId: ${Gson().toJson(expense)}")
                }
            } catch (e: HttpException) {
                Log.e("AuthRepository", "HTTP error deleting expenses: ${e.message()}, code=${e.code()}", e)
                throw Exception("Failed to delete expenses: HTTP ${e.code()}")
            } catch (e: IOException) {
                Log.e("AuthRepository", "IO error deleting expenses: ${e.message}", e)
                throw Exception("No internet connection")
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
        Log.d("AuthRepository", "Checking authentication: token=${token?.take(10) ?: "null"}...")
        if (token.isNullOrEmpty()) return false
        return try {
            val decodedJWT = JWT.decode(token)
            decodedJWT.expiresAt?.after(Date()) ?: false
        } catch (e: JWTDecodeException) {
            Log.e("AuthRepository", "Invalid JWT in isAuthenticated: ${e.message}")
            false
        }
    }
}