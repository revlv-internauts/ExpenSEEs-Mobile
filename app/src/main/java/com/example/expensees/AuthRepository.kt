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
import java.util.Date

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
                val rawResponseBody = response.body()?.let { Gson().toJson(it) } ?: "null"
                val errorBody = response.errorBody()?.string() ?: "null"
                Log.d("AuthRepository", "Raw response: body=$rawResponseBody, HTTP ${response.code()}, errorBody=$errorBody")
                if (response.isSuccessful) {
                    response.body()?.let { signInResponse ->
                        Log.d("AuthRepository", "Response details: token=${signInResponse.token?.take(10) ?: "null"}..., userId=${signInResponse.user_Id}, refreshToken=${signInResponse.refreshToken?.take(10) ?: "null"}...")
                        if (signInResponse.token.isNullOrEmpty() || signInResponse.user_Id.isNullOrEmpty()) {
                            Log.e("AuthRepository", "Login failed: token or userId is null or empty - token=${signInResponse.token}, userId=${signInResponse.user_Id}")
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
                // Retrieve token and refresh token from SharedPreferences
                val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                val token = prefs.getString("auth_token", null)
                val refreshToken = prefs.getString("refresh_token", null)
                Log.d("AuthRepository", "Retrieved token: ${token?.take(10) ?: "null"}... (truncated), refreshToken: ${refreshToken?.take(10) ?: "null"}... (truncated)")
                if (token.isNullOrEmpty()) {
                    Log.e("AuthRepository", "No token found in SharedPreferences")
                    return@withContext Result.failure(Exception("Not authenticated. Please log in."))
                }

                // Validate token expiration
                try {
                    val decodedJWT = JWT.decode(token)
                    val expiry = decodedJWT.expiresAt
                    if (expiry != null && expiry.before(Date())) {
                        Log.e("AuthRepository", "Token expired: expiry=$expiry")
                        if (refreshToken != null) {
                            val refreshResult = refreshToken(refreshToken)
                            if (refreshResult.isSuccess) {
                                return@withContext addExpenseWithToken(expense)
                            } else {
                                return@withContext Result.failure(Exception("Unauthorized: Failed to refresh token"))
                            }
                        } else {
                            return@withContext Result.failure(Exception("Unauthorized: Invalid or expired token"))
                        }
                    }
                } catch (e: JWTDecodeException) {
                    Log.e("AuthRepository", "Invalid JWT: ${e.message}")
                    return@withContext Result.failure(Exception("Unauthorized: Invalid token"))
                }

                // Proceed with expense request
                return@withContext addExpenseWithToken(expense)
            } catch (e: HttpException) {
                Log.e("AuthRepository", "HTTP error: ${e.message()}, code=${e.code()}", e)
                if (e.code() == 401) {
                    val refreshToken = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                        .getString("refresh_token", null)
                    if (refreshToken != null) {
                        val refreshResult = refreshToken(refreshToken)
                        if (refreshResult.isSuccess) {
                            return@withContext addExpenseWithToken(expense)
                        }
                    }
                    return@withContext Result.failure(Exception("Unauthorized: Invalid or expired token. Please log in again."))
                } else {
                    return@withContext Result.failure(Exception("Network error: ${e.message()}"))
                }
            } catch (e: IOException) {
                Log.e("AuthRepository", "IO error: ${e.message}", e)
                return@withContext Result.failure(Exception("No internet connection"))
            } catch (e: Exception) {
                Log.e("AuthRepository", "Add expense error: ${e.message}", e)
                return@withContext Result.failure(Exception("Failed to add expense: ${e.message}"))
            }
        }
    }

    private suspend fun addExpenseWithToken(expense: Expense): Result<Expense> {
        return withContext(Dispatchers.IO) {
            try {
                val token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                    .getString("auth_token", null)
                if (token.isNullOrEmpty()) {
                    Log.e("AuthRepository", "No token found after refresh")
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

                // Handle image upload if imagePath is provided
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

                // Send request to /api/expenses
                Log.d("AuthRepository", "Sending request to /api/expenses with token: Bearer ${token.take(10)}... (truncated)")
                val response = apiService.addExpense(
                    token = "Bearer $token",
                    category = category,
                    amount = amount,
                    dateOfTransaction = dateOfTransaction,
                    comments = comments,
                    createdAt = createdAt,
                    image = imagePart
                )

                // Log response details
                Log.d("AuthRepository", "Add expense response: HTTP ${response.code()}, body=${response.body()?.let { Gson().toJson(it) } ?: "null"}, errorBody=${response.errorBody()?.string() ?: "null"}")

                // Handle response
                if (response.isSuccessful) {
                    response.body()?.let { returnedExpense ->
                        if (returnedExpense.id.isNullOrEmpty()) { // Changed from id to expenseId
                            Log.e("AuthRepository", "Expense returned but has no expenseId: ${Gson().toJson(returnedExpense)}")
                            Result.failure(Exception("Expense not saved in database (missing expenseId)"))
                        } else {
                            userExpenses.add(returnedExpense)
                            Log.d("AuthRepository", "Expense added: expenseId=${returnedExpense.id}")
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
                            401 -> "Unauthorized: Invalid or expired token. Please log in again."
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
                Log.d("AuthRepository", "Attempting to refresh token")
                val response = apiService.refreshToken(RefreshTokenRequest(refreshToken))
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
                            Log.d("AuthRepository", "Token refreshed successfully")
                            Result.success(Unit)
                        }
                    } ?: Result.failure(Exception("Empty refresh token response"))
                } else {
                    Log.e("AuthRepository", "Refresh token failed: HTTP ${response.code()}, body=${response.errorBody()?.string()}")
                    Result.failure(Exception("Failed to refresh token: HTTP ${response.code()}"))
                }
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
                    expense.id?.let { expenseId -> // Changed from id to expenseId
                        val response = apiService.deleteExpense("Bearer $token", expenseId)
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
        Log.d("AuthRepository", "Checking authentication: token=${token?.take(10) ?: "null"}... (truncated)")
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