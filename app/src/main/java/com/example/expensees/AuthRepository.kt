package com.example.expensees.network

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.auth0.jwt.JWT
import com.auth0.jwt.exceptions.JWTDecodeException
import com.example.expensees.ApiConfig
import com.example.expensees.models.Expense
import com.example.expensees.models.SubmittedBudget
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
import androidx.core.content.FileProvider
import com.example.expensees.models.LiquidationReportData
import id.zelory.compressor.Compressor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.EOFException
import java.io.File
import java.io.FileOutputStream

class AuthRepository(
    private val apiService: ApiService,
    private val context: Context
) {
    val userExpenses: SnapshotStateList<Expense> = mutableStateListOf()
    val submittedBudgets: SnapshotStateList<SubmittedBudget> = mutableStateListOf()

    // Check network availability
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    suspend fun resetPassword(
        email: String,
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            repeat(2) { attempt ->
                try {
                    Log.d("AuthRepository", "Attempting password reset for email: $email (Attempt ${attempt + 1})")
                    if (!isNetworkAvailable(context)) {
                        Log.e("AuthRepository", "No network connection")
                        return@withContext Result.failure(Exception("No internet connection"))
                    }

                    // Get logged-in user's email and token from SharedPreferences
                    val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                    val loggedInEmail = prefs.getString("email", null)
                    val token = prefs.getString("auth_token", null)
                    if (loggedInEmail.isNullOrEmpty() || token.isNullOrEmpty()) {
                        Log.e("AuthRepository", "No logged-in user or token found")
                        return@withContext Result.failure(Exception("Not authenticated. Please log in."))
                    }

                    // Validate email
                    if (email != loggedInEmail) {
                        Log.e("AuthRepository", "Input email does not match logged-in user's email")
                        return@withContext Result.failure(Exception("Email does not match the logged-in user's email"))
                    }

                    // Validate passwords
                    if (newPassword != confirmPassword) {
                        Log.e("AuthRepository", "New password and confirm password do not match")
                        return@withContext Result.failure(Exception("Passwords do not match"))
                    }

                    Log.d("AuthRepository", "Sending password reset request for email: $email")
                    val response = apiService.resetPassword(
                        "Bearer $token",
                        email,
                        ChangePassword(currentPassword, newPassword, confirmPassword)
                    )
                    Log.d("AuthRepository", "Reset password response: HTTP ${response.code()}, errorBody=${response.errorBody()?.string() ?: "null"}")

                    if (response.isSuccessful) {
                        Log.d("AuthRepository", "Password reset successful for email: $email")
                        return@withContext Result.success(Unit)
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("AuthRepository", "Password reset failed: HTTP ${response.code()}, body=$errorBody")
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
                                400 -> "Invalid request: Check email or password format"
                                401 -> "Unauthorized: Invalid or expired token"
                                404 -> "User not found"
                                500 -> "Server error: Unable to process request. Please try again later."
                                else -> "Server error (${response.code()}). Please try again."
                            }
                        return@withContext Result.failure(Exception(errorMessage))
                    }
                } catch (e: EOFException) {
                    Log.e("AuthRepository", "EOF error in resetPassword: ${e.message}", e)
                    if (attempt == 1) {
                        return@withContext Result.failure(Exception("Server response was incomplete. Please try again later."))
                    }
                    Log.w("AuthRepository", "Retrying due to EOFException: ${e.message}")
                } catch (e: HttpException) {
                    Log.e("AuthRepository", "HTTP error in resetPassword: ${e.message()}, code=${e.code()}", e)
                    val errorMessage = when (e.code()) {
                        400 -> "Invalid request: Check email or password format"
                        401 -> "Unauthorized: Invalid or expired token"
                        404 -> "User not found"
                        500 -> "Server error: Unable to process request"
                        else -> "Network error: ${e.message()}"
                    }
                    return@withContext Result.failure(Exception(errorMessage))
                } catch (e: IOException) {
                    Log.e("AuthRepository", "IO error in resetPassword: ${e.message}", e)
                    return@withContext Result.failure(Exception("No internet connection or server issue"))
                } catch (e: Exception) {
                    Log.e("AuthRepository", "Password reset error: ${e.message}", e)
                    return@withContext Result.failure(Exception("Failed to reset password: ${e.message}"))
                }
            }
            Result.failure(Exception("Failed to reset password after retries"))
        }
    }


    suspend fun getProfilePicture(): Result<Uri> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AuthRepository", "Fetching profile picture, coroutineContext=${currentCoroutineContext()}")
                if (!isNetworkAvailable(context)) {
                    Log.e("AuthRepository", "No network connection")
                    return@withContext Result.failure(Exception("No internet connection"))
                }

                val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                var token = prefs.getString("auth_token", null)
                val userId = prefs.getString("user_id", null)
                Log.d("AuthRepository", "Stored auth_token: ${token?.take(20) ?: "null"}..., userId: $userId")
                if (token.isNullOrEmpty() || userId.isNullOrEmpty()) {
                    Log.e("AuthRepository", "No token or userId found in SharedPreferences")
                    return@withContext Result.failure(Exception("Not authenticated. Please log in."))
                }

                try {
                    val decodedJWT = JWT.decode(token)
                    val expiry = decodedJWT.expiresAt
                    val currentTime = Date()
                    Log.d("AuthRepository", "Token expiry: $expiry, currentTime: $currentTime, raw exp: ${decodedJWT.getClaim("exp").asLong()}")
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
                } catch (e: JWTDecodeException) {
                    Log.e("AuthRepository", "Invalid JWT: ${e.message}", e)
                    return@withContext Result.failure(Exception("Unauthorized: Invalid token. Please log in again."))
                }

                Log.d("AuthRepository", "Sending profile picture fetch request to /api/users/$userId/profile-picture with token: Bearer ${token.take(20)}...")
                val response = apiService.getProfilePicture(
                    token = "Bearer $token",
                    userId = userId
                )

                Log.d("AuthRepository", "Get profile picture response: HTTP ${response.code()}, headers=${response.headers()}")
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        // Save the image to a temporary file
                        val tempFile = File(context.cacheDir, "profile_picture_${System.currentTimeMillis()}.jpg")
                        try {
                            responseBody.byteStream().use { inputStream ->
                                FileOutputStream(tempFile).use { outputStream ->
                                    inputStream.copyTo(outputStream)
                                }
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
                                return@withContext Result.failure(Exception("Failed to save profile picture"))
                            }

                            // Create a Uri for the file using FileProvider
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                tempFile
                            )
                            Log.d("AuthRepository", "Profile picture fetched and saved: ${tempFile.absolutePath}, Uri: $uri")
                            Result.success(uri)
                        } catch (e: IOException) {
                            Log.e("AuthRepository", "Failed to save profile picture: ${e.message}", e)
                            if (tempFile.exists()) {
                                try {
                                    tempFile.delete()
                                    Log.d("AuthRepository", "Deleted temp file on error: ${tempFile.absolutePath}")
                                } catch (deleteException: Exception) {
                                    Log.e("AuthRepository", "Error deleting temp file: ${deleteException.message}", deleteException)
                                }
                            }
                            Result.failure(Exception("Failed to save profile picture: ${e.message}"))
                        }
                    } ?: Result.failure(Exception("Empty response body"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("AuthRepository", "Get profile picture failed: HTTP ${response.code()}, body=$errorBody, headers=${response.headers()}")
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
                            404 -> "Profile picture not found."
                            else -> "Server error (${response.code()}). Please try again."
                        }
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: HttpException) {
                Log.e("AuthRepository", "HTTP error in getProfilePicture: ${e.message()}, code=${e.code()}, response=${e.response()?.raw()}", e)
                val errorMessage = when (e.code()) {
                    401 -> "Unauthorized: Invalid or expired token."
                    404 -> "Profile picture not found."
                    else -> "Network error: ${e.message()}"
                }
                Result.failure(Exception(errorMessage))
            } catch (e: IOException) {
                Log.e("AuthRepository", "IO error in getProfilePicture: ${e.message}", e)
                Result.failure(Exception("No internet connection"))
            } catch (e: Exception) {
                Log.e("AuthRepository", "Get profile picture error: ${e.message}", e)
                Result.failure(Exception("Failed to fetch profile picture: ${e.message}"))
            }
        }
    }

    // Upload profile picture
    suspend fun uploadProfilePicture(uri: Uri): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AuthRepository", "Uploading profile picture: uri=$uri, coroutineContext=${currentCoroutineContext()}")
                if (!isNetworkAvailable(context)) {
                    Log.e("AuthRepository", "No network connection")
                    return@withContext Result.failure(Exception("No internet connection"))
                }

                val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                var token = prefs.getString("auth_token", null)
                val userId = prefs.getString("user_id", null)
                Log.d("AuthRepository", "Stored auth_token: ${token?.take(20) ?: "null"}..., userId: $userId")
                if (token.isNullOrEmpty() || userId.isNullOrEmpty()) {
                    Log.e("AuthRepository", "No token or userId found in SharedPreferences")
                    return@withContext Result.failure(Exception("Not authenticated. Please log in."))
                }

                try {
                    val decodedJWT = JWT.decode(token)
                    val expiry = decodedJWT.expiresAt
                    val currentTime = Date()
                    Log.d("AuthRepository", "Token expiry: $expiry, currentTime: $currentTime, raw exp: ${decodedJWT.getClaim("exp").asLong()}, timezone: ${TimeZone.getDefault().id}")
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
                } catch (e: JWTDecodeException) {
                    Log.e("AuthRepository", "Invalid JWT: ${e.message}", e)
                    return@withContext Result.failure(Exception("Unauthorized: Invalid token. Please log in again."))
                }

                // Prepare image part
                val imagePart = try {
                    Log.d("AuthRepository", "Processing profile picture URI: $uri")
                    if (uri.scheme == null || !listOf("content", "file").contains(uri.scheme)) {
                        Log.e("AuthRepository", "Invalid URI scheme: ${uri.scheme} for URI: $uri")
                        return@withContext Result.failure(Exception("Invalid image URI"))
                    }
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (!cursor.moveToFirst()) {
                            Log.e("AuthRepository", "URI is invalid or inaccessible: $uri")
                            return@withContext Result.failure(Exception("Invalid or inaccessible image"))
                        }
                    }
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream == null) {
                        Log.e("AuthRepository", "Failed to open input stream for URI: $uri")
                        return@withContext Result.failure(Exception("Failed to access image"))
                    }
                    inputStream.use { input ->
                        val tempFile = File(context.cacheDir, "profile_image_${System.currentTimeMillis()}.jpg")
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
                            return@withContext Result.failure(Exception("Failed to create image file"))
                        }
                        val imageFile = try {
                            Compressor.compress(context, tempFile)
                        } catch (e: Exception) {
                            Log.w("AuthRepository", "Compression failed: ${e.message}, using original file")
                            tempFile
                        }
                        try {
                            val bytes = imageFile.readBytes()
                            val fileName = uri.lastPathSegment?.let { segment ->
                                if (segment.endsWith(".jpg", ignoreCase = true) || segment.endsWith(".jpeg", ignoreCase = true) || segment.endsWith(".png", ignoreCase = true)) {
                                    segment
                                } else {
                                    "profile_image_${System.currentTimeMillis()}.jpg"
                                }
                            } ?: "profile_image_${System.currentTimeMillis()}.jpg"
                            if (bytes.isEmpty()) {
                                Log.e("AuthRepository", "Image bytes are empty for URI: $uri")
                                return@withContext Result.failure(Exception("Empty image file"))
                            }
                            val mediaType = when {
                                fileName.endsWith(".png", ignoreCase = true) -> "image/png"
                                else -> "image/jpeg"
                            }.toMediaTypeOrNull()
                            Log.d("AuthRepository", "Image part created: fieldName=file, fileName=$fileName, size=${bytes.size} bytes, mediaType=$mediaType")
                            MultipartBody.Part.createFormData(
                                "file",
                                fileName,
                                bytes.toRequestBody(mediaType)
                            )
                        } finally {
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
                    Log.e("AuthRepository", "Failed to prepare image part for URI $uri: ${e.message}", e)
                    return@withContext Result.failure(Exception("Failed to prepare image: ${e.message}"))
                }

                Log.d("AuthRepository", "Sending profile picture upload request to /api/users/$userId/profile-picture with token: Bearer ${token.take(20)}...")
                val response = apiService.uploadProfilePicture(
                    token = "Bearer $token",
                    userId = userId,
                    file = imagePart
                )

                Log.d("AuthRepository", "Upload profile picture response: HTTP ${response.code()}, errorBody=${response.errorBody()?.string() ?: "null"}, headers=${response.headers()}")
                if (response.isSuccessful) {
                    Log.d("AuthRepository", "Profile picture uploaded successfully for userId=$userId")
                    Result.success(uri.toString()) // Return the URI as a success result
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("AuthRepository", "Upload profile picture failed: HTTP ${response.code()}, body=$errorBody, headers=${response.headers()}")
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
                            400 -> "Invalid request: check file format."
                            404 -> "User not found."
                            else -> "Server error (${response.code()}). Please try again."
                        }
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: HttpException) {
                Log.e("AuthRepository", "HTTP error in uploadProfilePicture: ${e.message()}, code=${e.code()}, response=${e.response()?.raw()}", e)
                val errorMessage = when (e.code()) {
                    401 -> "Unauthorized: Invalid or expired token."
                    415 -> "Unsupported media type: Server does not accept multipart/form-data."
                    else -> "Network error: ${e.message()}"
                }
                Result.failure(Exception(errorMessage))
            } catch (e: IOException) {
                Log.e("AuthRepository", "IO error in uploadProfilePicture: ${e.message}", e)
                Result.failure(Exception("No internet connection"))
            } catch (e: Exception) {
                Log.e("AuthRepository", "Upload profile picture error: ${e.message}", e)
                Result.failure(Exception("Failed to upload profile picture: ${e.message}"))
            }
        }
    }


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
                            .putString("username", signInResponse.username)
                            .putString("email", signInResponse.email)
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
                // Validate image presence
                if (expense.imagePaths.isNullOrEmpty()) {
                    Log.e("AuthRepository", "No image provided for expense")
                    return@withContext Result.failure(Exception("Image is required for expense"))
                }
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

                // Log input values for debugging
                Log.d("AuthRepository", "Validating expense: dateOfTransaction=${expense.dateOfTransaction}, createdAt=${expense.createdAt}")

                // Validate date formats
                try {
                    expense.dateOfTransaction?.let {
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        dateFormat.isLenient = false // Strict parsing
                        dateFormat.parse(it)
                            ?: throw IllegalArgumentException("Invalid dateOfTransaction format: $it, expected yyyy-MM-dd")
                    } ?: throw IllegalArgumentException("dateOfTransaction is required")
                    expense.createdAt?.let {
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
                        dateFormat.isLenient = false // Strict parsing
                        try {
                            dateFormat.parse(it)
                                ?: throw IllegalArgumentException("Invalid createdAt format: $it, expected yyyy-MM-dd'T'HH:mm:ss'Z'")
                        } catch (e: Exception) {
                            // Try alternative format without literal 'Z'
                            val altFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                            altFormat.timeZone = TimeZone.getTimeZone("UTC")
                            altFormat.isLenient = false
                            altFormat.parse(it)
                                ?: throw IllegalArgumentException("Invalid createdAt format: $it, tried yyyy-MM-dd'T'HH:mm:ss'Z' and yyyy-MM-dd'T'HH:mm:ss")
                        }
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
                                tempFile
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
                                    "files",
                                    fileName,
                                    bytes.toRequestBody(mediaType)
                                )
                            } finally {
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
                Log.d("AuthRepository", "Sending createdAt to server: ${expense.createdAt}")

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
                response.body()?.let { returnedExpense ->
                    Log.d("AuthRepository", "Server returned createdAt: ${returnedExpense.createdAt}")
                }
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
                                .putString("username", signInResponse.username)
                                .putString("email", signInResponse.email)
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
            val tokenResult = getValidToken()
            if (tokenResult.isFailure) {
                throw IOException("Failed to get valid token: ${tokenResult.exceptionOrNull()?.message}")
            }
            val token = tokenResult.getOrNull() ?: throw IOException("Token is null")

            expenses.forEach { expense ->
                if (expense.expenseId?.startsWith("local_") == true) {
                    // Remove local expense from userExpenses
                    userExpenses.remove(expense)
                    Log.d("AuthRepository", "Deleted local expense: ${expense.expenseId}")
                } else {
                    // Delete server-synced expense via API
                    try {
                        val response = apiService.deleteExpense("Bearer $token", expense.expenseId!!)
                        if (response.isSuccessful) {
                            // Remove from local list after successful server deletion
                            userExpenses.remove(expense)
                            Log.d("AuthRepository", "Deleted server expense: ${expense.expenseId}")
                        } else {
                            val errorBody = response.errorBody()?.string()
                            val errorMessage = errorBody?.let {
                                try {
                                    Gson().fromJson(it, ErrorResponse::class.java)?.message
                                        ?: "Server error (${response.code()})"
                                } catch (e: Exception) {
                                    "Server error (${response.code()})"
                                }
                            } ?: "Server error (${response.code()})"
                            throw IOException("Failed to delete expense ${expense.expenseId}: $errorMessage")
                        }
                    } catch (e: Exception) {
                        Log.e("AuthRepository", "Error deleting expense ${expense.expenseId}: ${e.message}")
                        throw e
                    }
                }
            }
        }
    }

    // Add a new budget
    suspend fun addBudget(budget: SubmittedBudget): Result<SubmittedBudget> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AuthRepository", "Adding budget: name=${budget.name}, total=${budget.total}, expenses=${budget.expenses.size}, coroutineContext=${currentCoroutineContext()}")
                if (!isNetworkAvailable(context)) {
                    Log.e("AuthRepository", "No network connection, saving locally")
                    val localBudget = budget.copy(budgetId = "local_${System.currentTimeMillis()}")
                    submittedBudgets.add(localBudget)
                    return@withContext Result.failure(Exception("No internet connection"))
                }

                val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                var token = prefs.getString("auth_token", null)
                Log.d("AuthRepository", "Stored auth_token: ${token?.take(20) ?: "null"}...")
                if (token.isNullOrEmpty()) {
                    Log.e("AuthRepository", "No token found in SharedPreferences")
                    val localBudget = budget.copy(budgetId = "local_${System.currentTimeMillis()}")
                    submittedBudgets.add(localBudget)
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
                            val localBudget = budget.copy(budgetId = "local_${System.currentTimeMillis()}")
                            submittedBudgets.add(localBudget)
                            return@withContext Result.failure(Exception("Unauthorized: Invalid or expired token. Please log in again."))
                        }
                        val refreshResult = refreshToken(refreshToken)
                        if (refreshResult.isSuccess) {
                            token = prefs.getString("auth_token", null)
                            if (token.isNullOrEmpty()) {
                                Log.e("AuthRepository", "Failed to obtain new token after refresh")
                                val localBudget = budget.copy(budgetId = "local_${System.currentTimeMillis()}")
                                submittedBudgets.add(localBudget)
                                return@withContext Result.failure(Exception("Unauthorized: Failed to refresh token. Please log in again."))
                            }
                        } else {
                            Log.e("AuthRepository", "Token refresh failed: ${refreshResult.exceptionOrNull()?.message}")
                            val localBudget = budget.copy(budgetId = "local_${System.currentTimeMillis()}")
                            submittedBudgets.add(localBudget)
                            return@withContext Result.failure(Exception("Unauthorized: Invalid or expired token. Please log in again."))
                        }
                    }
                } catch (e: JWTDecodeException) {
                    Log.e("AuthRepository", "Invalid JWT: ${e.message}", e)
                    val localBudget = budget.copy(budgetId = "local_${System.currentTimeMillis()}")
                    submittedBudgets.add(localBudget)
                    return@withContext Result.failure(Exception("Unauthorized: Invalid token. Please log in again."))
                }

                Log.d("AuthRepository", "Sending request to /api/budgets with token: Bearer ${token.take(20)}...")
                val response = apiService.addBudget("Bearer $token", budget)
                Log.d("AuthRepository", "Add budget response: HTTP ${response.code()}, body=${response.body()?.let { Gson().toJson(it) } ?: "null"}, errorBody=${response.errorBody()?.string() ?: "null"}, headers=${response.headers()}")

                if (response.isSuccessful) {
                    response.body()?.let { returnedBudget ->
                        if (returnedBudget.budgetId.isNullOrEmpty()) {
                            Log.e("AuthRepository", "Budget returned but has no budgetId: ${Gson().toJson(returnedBudget)}")
                            val localBudget = budget.copy(budgetId = "local_${System.currentTimeMillis()}")
                            submittedBudgets.add(localBudget)
                            Result.failure(Exception("Budget not saved on server (missing budgetId), saved locally"))
                        } else {
                            submittedBudgets.add(returnedBudget)
                            Log.d("AuthRepository", "Budget added: budgetId=${returnedBudget.budgetId}")
                            Result.success(returnedBudget)
                        }
                    } ?: Result.failure(Exception("Empty response body"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("AuthRepository", "Add budget failed: HTTP ${response.code()}, body=$errorBody, headers=${response.headers()}")
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
                            400 -> "Invalid budget data: check field formats"
                            422 -> "Validation error: ensure all required fields are provided"
                            else -> "Server error (${response.code()}). Please try again."
                        }
                    val localBudget = budget.copy(budgetId = "local_${System.currentTimeMillis()}")
                    submittedBudgets.add(localBudget)
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: HttpException) {
                Log.e("AuthRepository", "HTTP error in addBudget: ${e.message()}, code=${e.code()}, response=${e.response()?.raw()}", e)
                val localBudget = budget.copy(budgetId = "local_${System.currentTimeMillis()}")
                submittedBudgets.add(localBudget)
                val errorMessage = when (e.code()) {
                    401 -> "Unauthorized: Invalid or expired token."
                    else -> "Network error: ${e.message()}"
                }
                Result.failure(Exception(errorMessage))
            } catch (e: IOException) {
                Log.e("AuthRepository", "IO error in addBudget: ${e.message}", e)
                val localBudget = budget.copy(budgetId = "local_${System.currentTimeMillis()}")
                submittedBudgets.add(localBudget)
                Result.failure(Exception("No internet connection"))
            } catch (e: Exception) {
                Log.e("AuthRepository", "Add budget error: ${e.message}", e)
                val localBudget = budget.copy(budgetId = "local_${System.currentTimeMillis()}")
                submittedBudgets.add(localBudget) // Fixed: Replaced concurrentsubmittedBudgets with submittedBudgets
                Result.failure(Exception("Failed to add budget: ${e.message}"))
            }
        }
    }

    suspend fun getBudgets(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (!isNetworkAvailable(context)) {
                    Log.e("AuthRepository", "No network connection, returning local budgets")
                    return@withContext Result.success(Unit)
                }
                val tokenResult = getValidToken()
                if (tokenResult.isFailure) {
                    Log.e("AuthRepository", "Failed to get valid token: ${tokenResult.exceptionOrNull()?.message}")
                    return@withContext Result.failure(tokenResult.exceptionOrNull()!!)
                }
                val token = tokenResult.getOrNull()!!
                Log.d("AuthRepository", "Fetching budgets with token: Bearer ${token.take(20)}...")
                val response = apiService.getBudgets("Bearer $token")
                Log.d("AuthRepository", "Get budgets response: HTTP ${response.code()}, body=${response.body()?.let { Gson().toJson(it) } ?: "null"}, errorBody=${response.errorBody()?.string() ?: "null"}")
                if (response.isSuccessful) {
                    response.body()?.let { budgets ->
                        // Keep local budgets (e.g., unsynced with local_ prefix)
                        submittedBudgets.removeAll { !it.budgetId.orEmpty().startsWith("local_") }
                        submittedBudgets.addAll(budgets)
                        Log.d("AuthRepository", "Fetched ${budgets.size} budgets")
                        Result.success(Unit)
                    } ?: Result.failure(Exception("Empty response body"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("AuthRepository", "Get budgets failed: HTTP ${response.code()}, body=$errorBody")
                    val errorMessage = when (response.code()) {
                        401 -> "Unauthorized: Invalid or expired token."
                        400 -> "Invalid request: check API format."
                        else -> "Server error (${response.code()}): $errorBody"
                    }
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: HttpException) {
                Log.e("AuthRepository", "HTTP error in getBudgets: ${e.message()}, code=${e.code()}", e)
                Result.failure(Exception("Network error: ${e.message()}"))
            } catch (e: IOException) {
                Log.e("AuthRepository", "IO error in getBudgets: ${e.message}", e)
                Result.failure(Exception("No internet connection"))
            } catch (e: Exception) {
                Log.e("AuthRepository", "Get budgets error: ${e.message}", e)
                Result.failure(Exception("Failed to fetch budgets: ${e.message}"))
            }
        }
    }

    // Sync local budgets with server
    suspend fun syncLocalBudgets() {
        withContext(Dispatchers.IO) {
            try {
                val tokenResult = getValidToken()
                if (tokenResult.isFailure) {
                    Log.e("AuthRepository", "Failed to get valid token for syncLocalBudgets: ${tokenResult.exceptionOrNull()?.message}")
                    return@withContext
                }
                val token = tokenResult.getOrNull()!!
                val localBudgets = submittedBudgets.filter { it.budgetId?.startsWith("local_") == true }

                for (localBudget in localBudgets) {
                    val result = addBudget(localBudget)
                    if (result.isSuccess) {
                        submittedBudgets.remove(localBudget)
                        submittedBudgets.add(result.getOrNull()!!)
                        Log.d("AuthRepository", "Synced local budget: ${localBudget.budgetId}")
                    } else {
                        Log.e("AuthRepository", "Failed to sync local budget: ${localBudget.budgetId}, error: ${result.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthRepository", "Sync local budgets error: ${e.message}", e)
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
        submittedBudgets.clear()
        Log.d("AuthRepository", "Logged out, cleared SharedPreferences, userExpenses, and submittedBudgets")
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

    val liquidationReports: SnapshotStateList<LiquidationReportData> = mutableStateListOf()

    suspend fun getLiquidationReports(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (!isNetworkAvailable(context)) {
                    Log.e(
                        "AuthRepository",
                        "No network connection, returning local liquidation reports"
                    )
                    return@withContext Result.success(Unit)
                }
                val tokenResult = getValidToken()
                if (tokenResult.isFailure) {
                    Log.e(
                        "AuthRepository",
                        "Failed to get valid token: ${tokenResult.exceptionOrNull()?.message}"
                    )
                    return@withContext Result.failure(tokenResult.exceptionOrNull()!!)
                }
                val token = tokenResult.getOrNull()!!
                Log.d(
                    "AuthRepository",
                    "Fetching liquidation reports with token: Bearer ${token.take(20)}..."
                )
                val response = apiService.getLiquidationReports("Bearer $token")
                Log.d(
                    "AuthRepository",
                    "Get liquidation reports response: HTTP ${response.code()}, body=${
                        response.body()?.let { Gson().toJson(it) } ?: "null"
                    }, errorBody=${response.errorBody()?.string() ?: "null"}")
                if (response.isSuccessful) {
                    response.body()?.let { reportsResponse ->
                        liquidationReports.removeAll { !it.liquidationId.startsWith("local_") }
                        liquidationReports.addAll(reportsResponse.budgets) // Use budgets from the wrapper
                        Log.d(
                            "AuthRepository",
                            "Fetched ${reportsResponse.budgets.size} liquidation reports"
                        )
                        Result.success(Unit)
                    } ?: Result.failure(Exception("Empty response body"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(
                        "AuthRepository",
                        "Get liquidation reports failed: HTTP ${response.code()}, body=$errorBody"
                    )
                    val errorMessage = when (response.code()) {
                        401 -> "Unauthorized: Invalid or expired token."
                        400 -> "Invalid request: check API format."
                        else -> "Server error (${response.code()}): $errorBody"
                    }
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: HttpException) {
                Log.e(
                    "AuthRepository",
                    "HTTP error in getLiquidationReports: ${e.message()}, code=${e.code()}",
                    e
                )
                Result.failure(Exception("Network error: ${e.message()}"))
            } catch (e: IOException) {
                Log.e("AuthRepository", "IO error in getLiquidationReports: ${e.message}", e)
                Result.failure(Exception("No internet connection"))
            } catch (e: Exception) {
                Log.e("AuthRepository", "Get liquidation reports error: ${e.message}", e)
                Result.failure(Exception("Failed to fetch liquidation reports: ${e.message}"))
            }
        }
    }

    suspend fun submitLiquidationReport(report: LiquidationReportData): Result<LiquidationReportData> {
        return withContext(Dispatchers.IO) {
            try {
                if (!isNetworkAvailable(context)) {
                    Log.e("AuthRepository", "No network connection")
                    return@withContext Result.failure(Exception("No internet connection"))
                }
                val tokenResult = getValidToken()
                if (tokenResult.isFailure) {
                    Log.e(
                        "AuthRepository",
                        "Failed to get valid token: ${tokenResult.exceptionOrNull()?.message}"
                    )
                    return@withContext Result.failure(tokenResult.exceptionOrNull()!!)
                }
                val token = tokenResult.getOrNull()!!
                Log.d(
                    "AuthRepository",
                    "Submitting liquidation report for budgetId: ${report.budgetId} with token: Bearer ${
                        token.take(20)
                    }..."
                )

                // Collect all parts in a list
                val parts = mutableListOf<MultipartBody.Part>()

                // Add budgetId
                parts.add(
                    MultipartBody.Part.createFormData(
                        "budgetId",
                        report.budgetId
                    )
                )

                // Serialize expenses list to JSON, excluding imagePaths
                val gson = Gson()
                val expensesJson = gson.toJson(report.expenses.map { expense ->
                    mapOf(
                        "category" to expense.category,
                        "amount" to expense.amount,
                        "remarks" to (expense.remarks ?: ""),
                        "dateOfTransaction" to expense.dateOfTransaction
                    )
                })
                parts.add(
                    MultipartBody.Part.createFormData(
                        "expenses",
                        expensesJson
                    )
                )

                // Add image files for each expense
                var fileCount = 0
                report.expenses.forEachIndexed { expenseIndex, expense ->
                    expense.imagePaths?.forEachIndexed { fileIndex, imagePath ->
                        try {
                            // Skip server-side paths starting with "Uploads/"
                            if (imagePath.startsWith("Uploads/")) {
                                Log.w(
                                    "AuthRepository",
                                    "Skipping server-side image path: $imagePath"
                                )
                                return@forEachIndexed
                            }
                            val file = if (imagePath.startsWith("content://")) {
                                context.contentResolver.openInputStream(Uri.parse(imagePath))
                                    ?.use { input ->
                                        val tempFile = File.createTempFile(
                                            "expense_${expenseIndex}_$fileIndex",
                                            ".jpg",
                                            context.cacheDir
                                        )
                                        tempFile.outputStream().use { output ->
                                            input.copyTo(output)
                                        }
                                        tempFile
                                    }
                            } else {
                                File(imagePath)
                            }
                            if (file?.exists() == true) {
                                val requestFile =
                                    file.readBytes().toRequestBody("image/jpeg".toMediaType())
                                parts.add(
                                    MultipartBody.Part.createFormData(
                                        "files[$fileCount]", // Use a single files array to match backend
                                        file.name,
                                        requestFile
                                    )
                                )
                                fileCount++
                            } else {
                                Log.w("AuthRepository", "Image file not found: $imagePath")
                            }
                        } catch (e: Exception) {
                            Log.e(
                                "AuthRepository",
                                "Failed to process image $imagePath for expense $expenseIndex: ${e.message}"
                            )
                        }
                    }
                }

                Log.d(
                    "AuthRepository",
                    "Multipart request parts: ${parts.size}, files included: $fileCount"
                )

                // Make the API call
                val response = apiService.submitLiquidationReport(
                    authorization = "Bearer $token",
                    budgetId = report.budgetId,
                    parts = parts
                )
                Log.d(
                    "AuthRepository",
                    "Submit liquidation report response: HTTP ${response.code()}, body=${
                        response.body()?.let { Gson().toJson(it) } ?: "null"
                    }, errorBody=${
                        response.errorBody()?.string() ?: "null"
                    }, headers=${response.headers()}")

                if (response.isSuccessful) {
                    response.body()?.let { submittedReport ->
                        liquidationReports.add(submittedReport)
                        Log.d(
                            "AuthRepository",
                            "Liquidation report submitted: liquidationId=${submittedReport.liquidationId}"
                        )
                        Result.success(submittedReport)
                    } ?: Result.failure(Exception("Empty response body"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(
                        "AuthRepository",
                        "Submit liquidation report failed: HTTP ${response.code()}, body=$errorBody"
                    )
                    val errorResponse = errorBody?.let {
                        try {
                            Gson().fromJson(it, ErrorResponse::class.java)
                        } catch (e: Exception) {
                            Log.e("AuthRepository", "Failed to parse error body: ${e.message}")
                            null
                        }
                    }
                    val errorMessage = errorResponse?.error
                        ?: errorResponse?.message
                        ?: when (response.code()) {
                            400 -> "Invalid request: Check expense amount, category, or date format."
                            401 -> "Unauthorized: Invalid or expired token."
                            404 -> "Budget not found for ID: ${report.budgetId}"
                            422 -> "Validation error: Ensure all required fields and files are provided."
                            500 -> "Server error: Unable to process request. Contact support."
                            else -> "Failed to submit report: HTTP ${response.code()}"
                        }
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Log.e("AuthRepository", "Error in submitLiquidationReport: ${e.message}", e)
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    suspend fun getLiquidationReport(liquidationId: String): Result<LiquidationReportData> {
        return withContext(Dispatchers.IO) {
            try {
                if (!isNetworkAvailable(context)) {
                    Log.e("AuthRepository", "No network connection")
                    return@withContext Result.failure(Exception("No internet connection"))
                }
                val tokenResult = getValidToken()
                if (tokenResult.isFailure) {
                    Log.e("AuthRepository", "Failed to get valid token: ${tokenResult.exceptionOrNull()?.message}")
                    return@withContext Result.failure(tokenResult.exceptionOrNull()!!)
                }
                val token = tokenResult.getOrNull()!!
                Log.d("AuthRepository", "Fetching liquidation report for ID: $liquidationId with token: Bearer ${token.take(20)}...")
                val response = apiService.getLiquidationReport("Bearer $token", liquidationId)
                Log.d("AuthRepository", "Get liquidation report response: HTTP ${response.code()}, body=${response.body()?.let { Gson().toJson(it) } ?: "null"}, errorBody=${response.errorBody()?.string() ?: "null"}")
                if (response.isSuccessful) {
                    response.body()?.let { report ->
                        Result.success(report)
                    } ?: Result.failure(Exception("Empty response body"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("AuthRepository", "Get liquidation report failed: HTTP ${response.code()}, body=$errorBody")
                    val errorMessage = when (response.code()) {
                        401 -> "Unauthorized: Invalid or expired token."
                        404 -> "Report not found for ID: $liquidationId"
                        500 -> "Server error: $errorBody"
                        else -> "Failed to fetch report: HTTP ${response.code()}"
                    }
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: HttpException) {
                Log.e("AuthRepository", "HTTP error in getLiquidationReport: ${e.message()}, code=${e.code()}", e)
                Result.failure(Exception("Network error: ${e.message()}"))
            } catch (e: IOException) {
                Log.e("AuthRepository", "IO error in getLiquidationReport: ${e.message}", e)
                Result.failure(Exception("No internet connection"))
            } catch (e: Exception) {
                Log.e("AuthRepository", "Get liquidation report error: ${e.message}", e)
                Result.failure(Exception("Failed to fetch report: ${e.message}"))
            }
        }
    }

    suspend fun hasLiquidationReport(budgetId: String): Boolean {
        return try {
            val reportsResult = getLiquidationReports()
            if (reportsResult.isSuccess) {
                liquidationReports.any { it.budgetId == budgetId }
            } else {
                false // Assume no report if fetching fails
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error checking liquidation report for budget $budgetId: ${e.message}")
            false
        }
    }
}