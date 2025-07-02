package com.example.expensees.network

import android.content.Context
import android.net.Uri
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
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import id.zelory.compressor.Compressor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class AuthRepository(
    private val apiService: ApiService,
    private val context: Context
) {
    val userExpenses: SnapshotStateList<Expense> = mutableStateListOf()

    // Check network availability
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    // Login with username or email and password
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
                        if (signInResponse.token.isNullOrEmpty() || signInResponse.user_Id.isNullOrEmpty()) {
                            Log.e("AuthRepository", "Login failed: token or userId is null or empty")
                            return@withContext Result.failure(Exception("Invalid login response: token or userId is missing"))
                        }
                        Log.d("AuthRepository", "Login successful: token=${signInResponse.token.take(20)}..., userId=${signInResponse.user_Id}, refreshToken=${signInResponse.refreshToken?.take(20) ?: "null"}...")
                        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                            .edit()
                            .putString("auth_token", signInResponse.token)
                            .putString("user_id", signInResponse.user_Id)
                            .putString("refresh_token", signInResponse.refreshToken)
                            .apply()
                        // Verify token storage
                        val savedToken = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                            .getString("auth_token", null)
                        Log.d("AuthRepository", "Verified saved token: ${savedToken?.take(20) ?: "null"}...")

                        if (signInResponse.refreshToken.isNullOrEmpty()) {
                            Log.w("AuthRepository", "No refresh token provided by server. Token refresh will not be possible.")
                        }
                        try {
                            val decodedJWT = JWT.decode(signInResponse.token)
                            Log.d("AuthRepository", "Token claims: ${decodedJWT.claims}, expiresAt: ${decodedJWT.expiresAt}, raw exp: ${decodedJWT.getClaim("exp").asLong()}")
                        } catch (e: JWTDecodeException) {
                            Log.e("AuthRepository", "Failed to decode token: ${e.message}")
                        }

                        // Fetch expenses after successful login
                        val expenseResult = getExpenses()
                        expenseResult.onSuccess {
                            Log.d("AuthRepository", "Expenses fetched successfully after login")
                        }.onFailure { e ->
                            Log.e("AuthRepository", "Failed to fetch expenses after login: ${e.message}")
                            // Not failing login due to expense fetch failure
                        }
                        Result.success(Unit)
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

    // Fetch expenses from server
    suspend fun getExpenses(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AuthRepository", "Fetching expenses in coroutineContext=${currentCoroutineContext()}")
                if (!isNetworkAvailable(context)) {
                    Log.e("AuthRepository", "No network connection, using local expenses")
                    return@withContext Result.failure(Exception("No internet connection"))
                }

                val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                val token = prefs.getString("auth_token", null)
                Log.d("AuthRepository", "Stored auth_token: ${token?.take(20) ?: "null"}...")
                if (token.isNullOrEmpty()) {
                    Log.e("AuthRepository", "No token found in SharedPreferences")
                    return@withContext Result.failure(Exception("Not authenticated. Please log in."))
                }

                try {
                    val decodedJWT = JWT.decode(token)
                    val expiry = decodedJWT.expiresAt
                    val currentTime = Date()
                    Log.d("AuthRepository", "Token expiry: $expiry, currentTime: $currentTime, raw exp: ${decodedJWT.getClaim("exp").asLong()}, timezone: ${TimeZone.getDefault().id}, claims: ${decodedJWT.claims}")
                    if (expiry == null || expiry.before(currentTime)) {
                        Log.e("AuthRepository", "Access token expired or invalid")
                        return@withContext Result.failure(Exception("Unauthorized: Invalid or expired token. Please log in again."))
                    }
                } catch (e: JWTDecodeException) {
                    Log.e("AuthRepository", "Invalid JWT: ${e.message}", e)
                    return@withContext Result.failure(Exception("Unauthorized: Invalid token. Please log in again."))
                }

                Log.d("AuthRepository", "Sending request to /api/expenses with token: Bearer ${token.take(20)}...")
                val response = apiService.getExpenses("Bearer $token")
                Log.d("AuthRepository", "Get expenses response: HTTP ${response.code()}, body=${response.body()?.let { Gson().toJson(it) } ?: "null"}, errorBody=${response.errorBody()?.string() ?: "null"}, headers=${response.headers()}")

                if (response.isSuccessful) {
                    response.body()?.let { expenses ->
                        // Preserve local (unsynced) expenses
                        val localExpenses = userExpenses.filter { it.expenseId?.startsWith("local_") == true }
                        // Clear existing server expenses and add new ones
                        userExpenses.removeAll { it.expenseId?.startsWith("local_") != true }
                        userExpenses.addAll(expenses)
                        // Re-add local expenses
                        userExpenses.addAll(localExpenses)
                        Log.d("AuthRepository", "Expenses fetched: ${expenses.size}, total in userExpenses: ${userExpenses.size}")
                        Result.success(Unit)
                    } ?: Result.failure(Exception("Empty response body"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("AuthRepository", "Get expenses failed: HTTP ${response.code()}, body=$errorBody, headers=${response.headers()}")
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
                            401 -> "Unauthorized: Invalid or expired token. Please log in again."
                            400 -> "Bad request"
                            else -> "Server error (${response.code()}). Please try again."
                        }
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: HttpException) {
                Log.e("AuthRepository", "HTTP error in getExpenses: ${e.message()}, code=${e.code()}, response=${e.response()?.raw()}", e)
                val errorMessage = when (e.code()) {
                    401 -> "Unauthorized: Invalid or expired token. Please log in again."
                    else -> "Network error: ${e.message()}"
                }
                Result.failure(Exception(errorMessage))
            } catch (e: IOException) {
                Log.e("AuthRepository", "IO error in getExpenses: ${e.message}", e)
                Result.failure(Exception("No internet connection"))
            } catch (e: Exception) {
                Log.e("AuthRepository", "Get expenses error: ${e.message}", e)
                Result.failure(Exception("Failed to fetch expenses: ${e.message}"))
            }
        }
    }

    // Add a new expense
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
                var token = prefs.getString("auth_token", null)
                Log.d("AuthRepository", "Stored auth_token: ${token?.take(20) ?: "null"}...")
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
                    Log.d("AuthRepository", "Token expiry: $expiry, currentTime: $currentTime, raw exp: ${decodedJWT.getClaim("exp").asLong()}, timezone: ${TimeZone.getDefault().id}, claims: ${decodedJWT.claims}")
                    if (expiry == null || expiry.before(currentTime)) {
                        Log.d("AuthRepository", "Access token expired, attempting to refresh")
                        val refreshToken = prefs.getString("refresh_token", null)
                        if (refreshToken.isNullOrEmpty()) {
                            Log.e("AuthRepository", "No refresh token available")
                            val localExpense = expense.copy(expenseId = "local_${System.currentTimeMillis()}")
                            userExpenses.add(localExpense)
                            return@withContext Result.failure(Exception("Unauthorized: Invalid or expired token. Please log in again."))
                        }
                        val refreshResult = refreshToken(refreshToken)
                        if (refreshResult.isSuccess) {
                            token = prefs.getString("auth_token", null)
                            if (token.isNullOrEmpty()) {
                                Log.e("AuthRepository", "Failed to obtain new token after refresh")
                                val localExpense = expense.copy(expenseId = "local_${System.currentTimeMillis()}")
                                userExpenses.add(localExpense)
                                return@withContext Result.failure(Exception("Unauthorized: Failed to refresh token. Please log in again."))
                            }
                        } else {
                            Log.e("AuthRepository", "Token refresh failed: ${refreshResult.exceptionOrNull()?.message}")
                            val localExpense = expense.copy(expenseId = "local_${System.currentTimeMillis()}")
                            userExpenses.add(localExpense)
                            return@withContext Result.failure(Exception("Unauthorized: Invalid or expired token. Please log in again."))
                        }
                    }
                } catch (e: JWTDecodeException) {
                    Log.e("AuthRepository", "Invalid JWT: ${e.message}", e)
                    val localExpense = expense.copy(expenseId = "local_${System.currentTimeMillis()}")
                    userExpenses.add(localExpense)
                    return@withContext Result.failure(Exception("Unauthorized: Invalid token. Please log in again."))
                }

                return@withContext addExpenseWithToken(expense, token)
            } catch (e: HttpException) {
                Log.e("AuthRepository", "HTTP error in addExpense: ${e.message()}, code=${e.code()}, response=${e.response()?.raw()}", e)
                if (e.code() == 401 || e.code() == 415) {
                    Log.e("AuthRepository", "Unauthorized or unsupported media type: ${e.message()}")
                    val localExpense = expense.copy(expenseId = "local_${System.currentTimeMillis()}")
                    userExpenses.add(localExpense)
                    return@withContext Result.failure(Exception("Unauthorized: Invalid or expired token, or unsupported media type. Please log in again."))
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

    // Helper function to add expense with a valid token
    private suspend fun addExpenseWithToken(expense: Expense, token: String): Result<Expense> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AuthRepository", "Using token for request: Bearer ${token.take(20)}..., coroutineContext=${currentCoroutineContext()}")
                try {
                    val decodedJWT = JWT.decode(token)
                    Log.d("AuthRepository", "Token claims before request: ${decodedJWT.claims}, expiresAt: ${decodedJWT.expiresAt}, currentTime: ${Date()}, issuer: ${decodedJWT.issuer}, audience: ${decodedJWT.audience}")
                } catch (e: JWTDecodeException) {
                    Log.e("AuthRepository", "Invalid JWT before request: ${e.message}")
                    val localExpense = expense.copy(expenseId = "local_${System.currentTimeMillis()}")
                    userExpenses.add(localExpense)
                    return@withContext Result.failure(Exception("Unauthorized: Invalid token. Please log in again."))
                }

                // Validate date formats
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

                // Prepare multipart parts
                val categoryPart = expense.category?.toRequestBody("text/plain".toMediaTypeOrNull())
                    ?: throw IllegalArgumentException("Category is required")
                val amountPart = expense.amount.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val dateOfTransactionPart = expense.dateOfTransaction?.toRequestBody("text/plain".toMediaTypeOrNull())
                    ?: throw IllegalArgumentException("Date of transaction is required")
                val createdAtPart = expense.createdAt?.toRequestBody("text/plain".toMediaTypeOrNull())
                    ?: throw IllegalArgumentException("Created at timestamp is required")
                val remarksPart = expense.remarks?.toRequestBody("text/plain".toMediaTypeOrNull())

                // Prepare image part
                val imagePart = expense.imagePaths?.firstOrNull()?.let { uriString ->
                    try {
                        Log.d("AuthRepository", "Processing image URI: $uriString")
                        val uri = Uri.parse(uriString)
                        if (uri.scheme == null || !listOf("content", "file").contains(uri.scheme)) {
                            Log.e("AuthRepository", "Invalid URI scheme: ${uri.scheme} for URI: $uriString")
                            return@let null
                        }
                        // Verify URI accessibility
                        try {
                            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                                if (!cursor.moveToFirst()) {
                                    Log.e("AuthRepository", "URI is invalid or inaccessible: $uri")
                                    return@let null
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("AuthRepository", "Failed to query URI: ${e.message}", e)
                            return@let null
                        }
                        val inputStream = context.contentResolver.openInputStream(uri)
                        if (inputStream == null) {
                            Log.e("AuthRepository", "Failed to open input stream for URI: $uri")
                            return@let null
                        }
                        inputStream.use { input ->
                            // Create temp file
                            val tempFile = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
                            Log.d("AuthRepository", "Creating temp file: ${tempFile.absolutePath}")
                            tempFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                            if (!tempFile.exists() || tempFile.length() == 0L) {
                                Log.e("AuthRepository", "Temp file is empty or not created: ${tempFile.absolutePath}")
                                if (tempFile.exists()) {
                                    try {
                                        tempFile.delete()
                                        Log.d("AuthRepository", "Deleted empty temp file: ${tempFile.absolutePath}")
                                    } catch (e: Exception) {
                                        Log.e("AuthRepository", "Error deleting empty temp file: ${e.message}", e)
                                    }
                                }
                                return@let null
                            }
                            val imageFile = try {
                                Compressor.compress(context, tempFile)
                            } catch (e: Exception) {
                                Log.w("AuthRepository", "Compression failed: ${e.message}, using original file")
                                tempFile // Fallback to original file
                            }
                            try {
                                val bytes = imageFile.readBytes()
                                val fileName = uri.lastPathSegment?.let { segment ->
                                    if (segment.endsWith(".jpg", ignoreCase = true) || segment.endsWith(".jpeg", ignoreCase = true) || segment.endsWith(".png", ignoreCase = true)) {
                                        segment
                                    } else {
                                        "expense_image_${System.currentTimeMillis()}.jpg"
                                    }
                                } ?: "expense_image_${System.currentTimeMillis()}.jpg"
                                if (bytes.isEmpty()) {
                                    Log.e("AuthRepository", "Image bytes are empty for URI: $uri")
                                    return@let null
                                }
                                val mediaType = when {
                                    fileName.endsWith(".png", ignoreCase = true) -> "image/png"
                                    else -> "image/jpeg"
                                }.toMediaTypeOrNull()
                                Log.d("AuthRepository", "Image part created: fieldName=files, fileName=$fileName, size=${bytes.size} bytes, mediaType=$mediaType")
                                MultipartBody.Part.createFormData(
                                    "files", // Matches server expectation
                                    fileName,
                                    bytes.toRequestBody(mediaType)
                                )
                            } finally {
                                // Delete temp file
                                if (imageFile.exists()) {
                                    try {
                                        imageFile.delete()
                                        Log.d("AuthRepository", "Temp file deleted: ${imageFile.absolutePath}")
                                    } catch (e: Exception) {
                                        Log.e("AuthRepository", "Error deleting temp file: ${e.message}", e)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("AuthRepository", "Failed to prepare image part for URI $uriString: ${e.message}", e)
                        null
                    }
                }

                Log.d("AuthRepository", "Adding expense: category=${expense.category}, amount=${expense.amount}, dateOfTransaction=${expense.dateOfTransaction}, remarks=${expense.remarks}, createdAt=${expense.createdAt}, hasImage=${imagePart != null}")

                Log.d("AuthRepository", "Sending multipart request to /api/expenses with token: Bearer ${token.take(20)}...")
                val response = apiService.addExpense(
                    token = "Bearer $token",
                    category = categoryPart,
                    amount = amountPart,
                    dateOfTransaction = dateOfTransactionPart,
                    remarks = remarksPart,
                    createdAt = createdAtPart,
                    files = imagePart
                )

                Log.d("AuthRepository", "Add expense response: HTTP ${response.code()}, body=${response.body()?.let { Gson().toJson(it) } ?: "null"}, errorBody=${response.errorBody()?.string() ?: "null"}, headers=${response.headers()}")
                if (response.isSuccessful) {
                    response.body()?.let { returnedExpense ->
                        Log.d("AuthRepository", "Server returned expense with imagePaths: ${returnedExpense.imagePaths}")
                        if (returnedExpense.expenseId.isNullOrEmpty()) {
                            Log.e("AuthRepository", "Expense returned but has no expenseId: ${Gson().toJson(returnedExpense)}")
                            val localExpense = expense.copy(expenseId = "local_${System.currentTimeMillis()}")
                            userExpenses.add(localExpense)
                            Result.failure(Exception("Expense not saved on server (missing expenseId), saved locally"))
                        } else {
                            userExpenses.add(returnedExpense)
                            Log.d("AuthRepository", "Expense added: expenseId=${returnedExpense.expenseId}")
                            if (imagePart != null && returnedExpense.imagePaths.isNullOrEmpty()) {
                                Log.w("AuthRepository", "Image part was sent but server returned empty imagePaths")
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Expense added, but image upload may have failed", Toast.LENGTH_LONG).show()
                                }
                            }
                            Result.success(returnedExpense)
                        }
                    } ?: Result.failure(Exception("Empty response body"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("AuthRepository", "Add expense failed: HTTP ${response.code()}, body=$errorBody, headers=${response.headers()}")
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
                            401 -> "Unauthorized: Invalid or expired token. Please log in again."
                            415 -> "Unsupported media type: Server does not accept multipart/form-data."
                            400 -> "Invalid expense data: check field formats (e.g., amount, date)"
                            422 -> "Validation error: ensure all required fields are provided"
                            else -> "Server error (${response.code()}). Please try again."
                        }
                    val localExpense = expense.copy(expenseId = "local_${System.currentTimeMillis()}")
                    userExpenses.add(localExpense)
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: HttpException) {
                Log.e("AuthRepository", "HTTP error in addExpenseWithToken: ${e.message()}, code=${e.code()}, response=${e.response()?.raw()}", e)
                val localExpense = expense.copy(expenseId = "local_${System.currentTimeMillis()}")
                userExpenses.add(localExpense)
                val errorMessage = when (e.code()) {
                    401 -> "Unauthorized: Invalid or expired token."
                    415 -> "Unsupported media type: Server does not accept multipart/form-data."
                    else -> "Network error: ${e.message()}"
                }
                Result.failure(Exception(errorMessage))
            } catch (e: IOException) {
                Log.e("AuthRepository", "IO error in addExpenseWithToken: ${e.message}", e)
                val localExpense = expense.copy(expenseId = "local_${System.currentTimeMillis()}")
                userExpenses.add(localExpense)
                Result.failure(Exception("No internet connection"))
            } catch (e: Exception) {
                Log.e("AuthRepository", "Add expense error in addExpenseWithToken: ${e.message}", e)
                val localExpense = expense.copy(expenseId = "local_${System.currentTimeMillis()}")
                userExpenses.add(localExpense)
                Result.failure(Exception("Failed to add expense: ${e.message}"))
            }
        }
    }

    // Refresh access token using refresh token
    private suspend fun refreshToken(refreshToken: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AuthRepository", "Refresh request: ${Gson().toJson(RefreshTokenRequest(refreshToken))}, token=${refreshToken.take(20)}..., coroutineContext=${currentCoroutineContext()}")
                val response = apiService.refreshToken(RefreshTokenRequest(refreshToken))
                Log.d("AuthRepository", "Refresh response: HTTP ${response.code()}, body=${response.body()?.let { Gson().toJson(it) } ?: "null"}, errorBody=${response.errorBody()?.string() ?: "null"}, headers=${response.headers()}")
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
                            Log.d("AuthRepository", "Token refreshed successfully: new token=${signInResponse.token.take(20)}..., new refreshToken=${signInResponse.refreshToken?.take(20) ?: "null"}...")
                            Result.success(Unit)
                        }
                    } ?: Result.failure(Exception("Empty refresh token response"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("AuthRepository", "Refresh token failed: HTTP ${response.code()}, body=$errorBody, headers=${response.headers()}")
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
                Log.e("AuthRepository", "HTTP error refreshing token: ${e.message()}, code=${e.code()}, response=${e.response()?.raw()}", e)
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

    // Delete expenses
    suspend fun deleteExpenses(expenses: List<Expense>) {
        withContext(Dispatchers.IO) {
            try {
                val tokenResult = getValidToken()
                if (tokenResult.isFailure) {
                    Log.e("AuthRepository", "Failed to get valid token for deleteExpenses: ${tokenResult.exceptionOrNull()?.message}")
                    throw Exception("Not authenticated. Please log in.")
                }
                val token = tokenResult.getOrNull()!!
                expenses.forEach { expense ->
                    expense.expenseId?.let { expenseId ->
                        if (!expenseId.startsWith("local_")) {
                            Log.d("AuthRepository", "Deleting expense: expenseId=$expenseId")
                            val response = apiService.deleteExpense("Bearer $token", expenseId)
                            if (response.isSuccessful) {
                                userExpenses.remove(expense)
                                Log.d("AuthRepository", "Expense deleted: expenseId=$expenseId")
                            } else {
                                Log.e("AuthRepository", "Failed to delete expense: HTTP ${response.code()}, errorBody=${response.errorBody()?.string() ?: "null"}, headers=${response.headers()}")
                                throw Exception("Failed to delete expense: HTTP ${response.code()}")
                            }
                        } else {
                            Log.d("AuthRepository", "Removing local expense: expenseId=$expenseId")
                            userExpenses.remove(expense)
                        }
                    } ?: Log.w("AuthRepository", "Skipping expense with null expenseId: ${Gson().toJson(expense)}")
                }
            } catch (e: HttpException) {
                Log.e("AuthRepository", "HTTP error deleting expenses: ${e.message()}, code=${e.code()}, response=${e.response()?.raw()}", e)
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

    // Sync local expenses with server
    suspend fun syncLocalExpenses() {
        withContext(Dispatchers.IO) {
            try {
                val tokenResult = getValidToken()
                if (tokenResult.isFailure) {
                    Log.e("AuthRepository", "Failed to get valid token for syncLocalExpenses: ${tokenResult.exceptionOrNull()?.message}")
                    return@withContext
                }
                val token = tokenResult.getOrNull()!!
                val localExpenses = userExpenses.filter { it.expenseId?.startsWith("local_") == true }
                val serverExpenses = apiService.getExpenses("Bearer $token").body() ?: emptyList()
                val serverExpenseIds = serverExpenses.mapNotNull { it.expenseId }.toSet()

                for (localExpense in localExpenses) {
                    if (localExpense.expenseId !in serverExpenseIds) {
                        val result = addExpenseWithToken(localExpense, token)
                        if (result.isSuccess) {
                            userExpenses.remove(localExpense)
                            userExpenses.add(result.getOrNull()!!)
                            Log.d("AuthRepository", "Synced local expense: ${localExpense.expenseId}")
                        } else {
                            Log.e("AuthRepository", "Failed to sync local expense: ${localExpense.expenseId}, error: ${result.exceptionOrNull()?.message}")
                        }
                    } else {
                        Log.d("AuthRepository", "Skipping sync for local expense ${localExpense.expenseId}, already exists on server")
                        userExpenses.remove(localExpense)
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthRepository", "Sync local expenses error: ${e.message}", e)
            }
        }
    }

    // Logout and clear session
    fun logout() {
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        userExpenses.clear()
        Log.d("AuthRepository", "Logged out, cleared SharedPreferences and userExpenses")
    }

    // Check if user is authenticated
    fun isAuthenticated(): Boolean {
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("auth_token", null)
        return if (token.isNullOrEmpty()) {
            Log.d("AuthRepository", "Not authenticated: No token found")
            false
        } else {
            try {
                val decodedJWT = JWT.decode(token)
                val expiry = decodedJWT.expiresAt
                val currentTime = Date()
                val isValid = expiry != null && expiry.after(currentTime)
                Log.d("AuthRepository", "Checking authentication: tokenValid=$isValid, expiry=$expiry, currentTime=$currentTime")
                isValid
            } catch (e: JWTDecodeException) {
                Log.e("AuthRepository", "Invalid JWT: ${e.message}")
                false
            }
        }
    }

    // Get a valid token, refreshing if necessary
    suspend fun getValidToken(): Result<String> {
        return withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            var token = prefs.getString("auth_token", null)
            if (token.isNullOrEmpty()) {
                Log.e("AuthRepository", "No token found in SharedPreferences")
                return@withContext Result.failure(Exception("Not authenticated. Please log in."))
            }

            try {
                val decodedJWT = JWT.decode(token)
                val expiry = decodedJWT.expiresAt
                val currentTime = Date()
                if (expiry == null || expiry.before(currentTime)) {
                    Log.d("AuthRepository", "Access token expired, attempting to refresh")
                    val refreshToken = prefs.getString("refresh_token", null)
                    if (refreshToken.isNullOrEmpty()) {
                        Log.e("AuthRepository", "No refresh token available")
                        return@withContext Result.failure(Exception("Unauthorized: Invalid or expired token. Please log in again."))
                    }
                    val refreshResult = refreshToken(refreshToken)
                    if (refreshResult.isSuccess) {
                        token = prefs.getString("auth_token", null)
                        if (token.isNullOrEmpty()) {
                            Log.e("AuthRepository", "Failed to obtain new token after refresh")
                            return@withContext Result.failure(Exception("Unauthorized: Failed to refresh token. Please log in again."))
                        }
                    } else {
                        Log.e("AuthRepository", "Token refresh failed: ${refreshResult.exceptionOrNull()?.message}")
                        return@withContext Result.failure(Exception("Unauthorized: Invalid or expired token. Please log in again."))
                    }
                }
                Log.d("AuthRepository", "Valid token retrieved: ${token.take(20)}...")
                Result.success(token)
            } catch (e: JWTDecodeException) {
                Log.e("AuthRepository", "Invalid JWT: ${e.message}", e)
                Result.failure(Exception("Unauthorized: Invalid token. Please log in again."))
            }
        }
    }
}