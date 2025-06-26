
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
import kotlinx.coroutines.currentCoroutineContext
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
import java.util.TimeZone
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class AuthRepository(
    private val apiService: ApiService,
    private val context: Context
) {
    val userExpenses: SnapshotStateList<Expense> = mutableStateListOf()

    suspend fun login(usernameOrEmail: String, password: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AuthRepository", "Creating SignInRequest: usernameOrEmail='$usernameOrEmail', coroutineContext=${currentCoroutineContext()}")
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
                            Log.d("AuthRepository", "Login successful: token=${signInResponse.token.take(10)}..., userId=${signInResponse.user_Id}, refreshToken=${signInResponse.refreshToken?.take(10) ?: "null"}...")
                            context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                                .edit()
                                .putString("auth_token", signInResponse.token)
                                .putString("user_id", signInResponse.user_Id)
                                .putString("refresh_token", signInResponse.refreshToken)
                                .apply()
                            if (signInResponse.refreshToken.isNullOrEmpty()) {
                                Log.w("AuthRepository", "No refresh token provided by server. Token refresh will not be possible.")
                            }
                            try {
                                val decodedJWT = JWT.decode(signInResponse.token)
                                Log.d("AuthRepository", "Token claims: ${decodedJWT.claims}, expiresAt: ${decodedJWT.expiresAt}, raw exp: ${decodedJWT.getClaim("exp").asLong()}")
                            } catch (e: JWTDecodeException) {
                                Log.e("AuthRepository", "Failed to decode token: ${e.message}")
                            }
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



    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    suspend fun addExpense(expense: Expense): Result<Expense> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AuthRepository", "Adding expense in coroutineContext=${currentCoroutineContext()}")
                if (!isNetworkAvailable(context)) {
                    Log.e("AuthRepository", "No network connection, saving locally")
                    val localExpense = expense.copy(expenseId = "local_${System.currentTimeMillis()}")
                    userExpenses.add(localExpense)
                    return@withContext Result.failure(Exception("No internet connection"))
                }

                val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                val token = prefs.getString("auth_token", null)
                val refreshToken = prefs.getString("refresh_token", null)
                Log.d("AuthRepository", "Stored auth_token: ${token?.take(10) ?: "null"}..., refresh_token: ${refreshToken?.take(10) ?: "null"}...")
                if (token.isNullOrEmpty()) {
                    Log.e("AuthRepository", "No token found in SharedPreferences")
                    val localExpense = expense.copy(expenseId = "local_${System.currentTimeMillis()}")
                    userExpenses.add(localExpense)
                    return@withContext Result.failure(Exception("Not authenticated. Please log in."))
                }

                try {
                    val decodedJWT = JWT.decode(token)
                    val expiry = decodedJWT.expiresAt
                    val currentTime = Date()
                    val fiveMinutes = 5 * 60 * 1000L
                    Log.d("AuthRepository", "Token expiry: $expiry, currentTime: $currentTime, raw exp: ${decodedJWT.getClaim("exp").asLong()}, timezone: ${TimeZone.getDefault().id}, claims: ${decodedJWT.claims}")
                    if (expiry != null && expiry.time - currentTime.time < fiveMinutes) {
                        Log.d("AuthRepository", "Token nearing expiry, refreshing proactively")
                        if (refreshToken != null) {
                            val refreshResult = refreshToken(refreshToken)
                            if (refreshResult.isSuccess) {
                                return@withContext addExpenseWithToken(expense, retryCount = 0)
                            } else {
                                Log.e("AuthRepository", "Proactive refresh failed: ${refreshResult.exceptionOrNull()?.message}")
                                val localExpense = expense.copy(expenseId = "local_${System.currentTimeMillis()}")
                                userExpenses.add(localExpense)
                                return@withContext Result.failure(Exception("Unauthorized: Failed to refresh token. Please log in again."))
                            }
                        } else {
                            Log.e("AuthRepository", "No refresh token available")
                            val localExpense = expense.copy(expenseId = "local_${System.currentTimeMillis()}")
                            userExpenses.add(localExpense)
                            return@withContext Result.failure(Exception("Unauthorized: No refresh token available. Please log in again."))
                        }
                    }
                } catch (e: JWTDecodeException) {
                    Log.e("AuthRepository", "Invalid JWT: ${e.message}", e)
                    val localExpense = expense.copy(expenseId = "local_${System.currentTimeMillis()}")
                    userExpenses.add(localExpense)
                    return@withContext Result.failure(Exception("Unauthorized: Invalid token. Please log in again."))
                }

                try {
                    expense.dateOfTransaction?.let {
                        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(it)
                            ?: throw IllegalArgumentException("Invalid dateOfTransaction format: $it")
                    } ?: throw IllegalArgumentException("dateOfTransaction is required")
                    expense.createdAt?.let {
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }.parse(it) ?: throw IllegalArgumentException("Invalid createdAt format: $it")
                    } ?: throw IllegalArgumentException("createdAt is required")
                } catch (e: Exception) {
                    Log.e("AuthRepository", "Date validation error: ${e.message}", e)
                    return@withContext Result.failure(Exception("Invalid date format: ${e.message}"))
                }

                return@withContext addExpenseWithToken(expense, retryCount = 0)
            } catch (e: HttpException) {
                Log.e("AuthRepository", "HTTP error in addExpense: ${e.message()}, code=${e.code()}", e)
                if (e.code() == 401) {
                    val refreshToken = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                        .getString("refresh_token", null)
                    if (refreshToken != null) {
                        Log.d("AuthRepository", "Retrying token refresh due to 401: ${refreshToken.take(10)}...")
                        val refreshResult = refreshToken(refreshToken)
                        if (refreshResult.isSuccess) {
                            Log.d("AuthRepository", "Token refreshed successfully, retrying addExpense")
                            return@withContext addExpenseWithToken(expense, retryCount = 0)
                        }
                    }
                    Log.e("AuthRepository", "No refresh token available for retry or refresh failed")
                    val localExpense = expense.copy(expenseId = "local_${System.currentTimeMillis()}")
                    userExpenses.add(localExpense)
                    return@withContext Result.failure(Exception("Unauthorized: Invalid or expired token. Please log in again."))
                } else {
                    return@withContext Result.failure(Exception("Network error: ${e.message()}"))
                }
            } catch (e: IOException) {
                Log.e("AuthRepository", "IO error in addExpense: ${e.message}", e)
                val localExpense = expense.copy(expenseId = "local_${System.currentTimeMillis()}")
                userExpenses.add(localExpense)
                return@withContext Result.failure(Exception("No internet connection"))
            } catch (e: Exception) {
                Log.e("AuthRepository", "Add expense error: ${e.message}", e)
                return@withContext Result.failure(Exception("Failed to add expense: ${e.message}"))
            }
        }
    }

    private suspend fun addExpenseWithToken(expense: Expense, retryCount: Int = 0): Result<Expense> {
        if (retryCount > 1) {
            Log.e("AuthRepository", "Max retry attempts reached for addExpenseWithToken")
            return Result.failure(Exception("Unauthorized: Max retry attempts reached. Please log in again."))
        }
        return withContext(Dispatchers.IO) {
            try {
                val token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                    .getString("auth_token", null)
                if (token.isNullOrEmpty()) {
                    Log.e("AuthRepository", "No token found after refresh")
                    return@withContext Result.failure(Exception("Not authenticated. Please log in."))
                }

                Log.d("AuthRepository", "Using token for request: Bearer ${token.take(10)}..., coroutineContext=${currentCoroutineContext()}")
                try {
                    val decodedJWT = JWT.decode(token)
                    Log.d("AuthRepository", "Token claims before request: ${decodedJWT.claims}, expiresAt: ${decodedJWT.expiresAt}, currentTime: ${Date()}")
                } catch (e: JWTDecodeException) {
                    Log.e("AuthRepository", "Invalid JWT before request: ${e.message}")
                }

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

                Log.d("AuthRepository", "Sending request to /api/expense with token: Bearer ${token.take(10)}...")
                val response = apiService.addExpense(
                    token = "Bearer $token",
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
                            Log.e("AuthRepository", "Failed to parse error body: ${e.message}")
                            null
                        }
                    }
                    val errorMessage = errorResponse?.message
                        ?: errorResponse?.error
                        ?: when (response.code()) {
                            401 -> {
                                if (retryCount < 1) {
                                    val refreshToken = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                                        .getString("refresh_token", null)
                                    if (refreshToken != null) {
                                        Log.d("AuthRepository", "Retrying token refresh due to 401: ${refreshToken.take(10)}...")
                                        val refreshResult = refreshToken(refreshToken)
                                        if (refreshResult.isSuccess) {
                                            Log.d("AuthRepository", "Token refreshed successfully, retrying addExpense")
                                            return@withContext addExpenseWithToken(expense, retryCount + 1)
                                        }
                                    }
                                    "Unauthorized: Invalid or expired token. Please log in again."
                                } else {
                                    "Unauthorized: Failed to refresh token after retry. Please log in again."
                                }
                            }
                            400 -> "Invalid expense data: check field formats (e.g., amount, date)"
                            422 -> "Validation error: ensure all required fields are provided"
                            else -> "Server error (${response.code()}). Please try again."
                        }
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: HttpException) {
                Log.e("AuthRepository", "HTTP error in addExpenseWithToken: ${e.message()}, code=${e.code()}", e)
                Result.failure(Exception("Network error: ${e.message()}"))
            } catch (e: IOException) {
                Log.e("AuthRepository", "IO error in addExpenseWithToken: ${e.message}", e)
                Result.failure(Exception("No internet connection"))
            } catch (e: Exception) {
                Log.e("AuthRepository", "Add expense error in addExpenseWithToken: ${e.message}", e)
                Result.failure(Exception("Failed to add expense: ${e.message}"))
            }
        }
    }

    private suspend fun refreshToken(refreshToken: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AuthRepository", "Refresh request: ${Gson().toJson(RefreshTokenRequest(refreshToken))}, coroutineContext=${currentCoroutineContext()}")
                val response = apiService.refreshToken(RefreshTokenRequest(refreshToken))
                Log.d("AuthRepository", "Refresh response: HTTP ${response.code()}, body=${response.body()?.let { Gson().toJson(it) } ?: "null"}, errorBody=${response.errorBody()?.string() ?: "null"}")
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
                            Log.d("AuthRepository", "Token refreshed successfully: new token=${signInResponse.token.take(10)}..., new refreshToken=${signInResponse.refreshToken?.take(10) ?: "null"}...")
                            Result.success(Unit)
                        }
                    } ?: Result.failure(Exception("Empty refresh token response"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("AuthRepository", "Refresh token failed: HTTP ${response.code()}, body=$errorBody")
                    val errorResponse = errorBody?.let {
                        try {
                            Gson().fromJson(it, ErrorResponse::class.java)
                        } catch (e: Exception) {
                            Log.e("AuthRepository", "Failed to parse error body: ${e.message}")
                            null
                        }
                    }
                    val errorMessage = errorResponse?.message
                        ?: errorResponse?.error
                        ?: when (response.code()) {
                            401 -> "Invalid or expired refresh token"
                            403 -> "Forbidden: Refresh token not allowed"
                            else -> "Refresh token error: HTTP ${response.code()}"
                        }
                    if (response.code() == 401 || response.code() == 403) {
                        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                            .edit()
                            .clear()
                            .apply()
                    }
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: HttpException) {
                Log.e("AuthRepository", "HTTP error refreshing token: ${e.message()}, code=${e.code()}", e)
                val errorMessage = when (e.code()) {
                    401 -> "Invalid or expired refresh token"
                    403 -> "Forbidden: Refresh token not allowed"
                    else -> "Network error: ${e.message()}"
                }
                if (e.code() == 401 || e.code() == 403) {
                    context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .clear()
                        .apply()
                }
                Result.failure(Exception(errorMessage))
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
                        if (!expenseId.startsWith("local_")) {
                            Log.d("AuthRepository", "Deleting expense: expenseId=$expenseId")
                            val response = apiService.deleteExpense("Bearer $token", expenseId)
                            if (response.isSuccessful) {
                                userExpenses.remove(expense)
                                Log.d("AuthRepository", "Expense deleted: expenseId=$expenseId")
                            } else {
                                Log.e("AuthRepository", "Failed to delete expense: HTTP ${response.code()}, errorBody=${response.errorBody()?.string() ?: "null"}")
                                throw Exception("Failed to delete expense: HTTP ${response.code()}")
                            }
                        } else {
                            Log.d("AuthRepository", "Removing local expense: expenseId=$expenseId")
                            userExpenses.remove(expense)
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

    suspend fun syncLocalExpenses(maxRetries: Int = 3): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AuthRepository", "Syncing local expenses")
                val localExpenses = userExpenses.filter { it.expenseId?.startsWith("local_") == true }.toList()
                Log.d("AuthRepository", "Syncing ${localExpenses.size} local expenses")
                var allSynced = true
                localExpenses.forEach { expense ->
                    var retryCount = 0
                    var result: Result<Expense>
                    do {
                        result = addExpense(expense.copy(expenseId = null))
                        result.onSuccess { syncedExpense ->
                            userExpenses.remove(expense)
                            userExpenses.add(syncedExpense)
                            Log.d("AuthRepository", "Synced local expense: expenseId=${syncedExpense.expenseId}")
                        }.onFailure { e ->
                            Log.e("AuthRepository", "Failed to sync local expense (attempt ${retryCount + 1}): ${e.message}")
                            allSynced = false
                            retryCount++
                        }
                    } while (result.isFailure && retryCount < maxRetries)
                }
                if (allSynced) Result.success(Unit) else Result.failure(Exception("Some local expenses failed to sync"))
            } catch (e: Exception) {
                Log.e("AuthRepository", "Sync local expenses error: ${e.message}", e)
                Result.failure(Exception("Failed to sync local expenses: ${e.message}"))
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
        val refreshToken = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            .getString("refresh_token", null)
        Log.d("AuthRepository", "Checking authentication: token=${token?.take(10) ?: "null"}..., refreshToken=${refreshToken?.take(10) ?: "null"}...")
        if (token.isNullOrEmpty()) return false
        return try {
            val decodedJWT = JWT.decode(token)
            val expiry = decodedJWT.expiresAt
            val currentTime = Date()
            val isValid = expiry?.after(currentTime) ?: false
            Log.d("AuthRepository", "Token valid: $isValid, expiresAt: $expiry, currentTime: $currentTime, raw exp: ${decodedJWT.getClaim("exp").asLong()}, timezone: ${TimeZone.getDefault().id}, claims: ${decodedJWT.claims}")
            isValid
        } catch (e: JWTDecodeException) {
            Log.e("AuthRepository", "Invalid JWT in isAuthenticated: ${e.message}")
            false
        }
    }
}
