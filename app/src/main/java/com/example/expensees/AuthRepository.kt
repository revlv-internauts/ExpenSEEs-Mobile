package com.example.expensees.network

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class AuthRepository(private val apiService: ApiService, private val context: Context) {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var isAuthenticated by mutableStateOf(false)
        private set

    suspend fun login(usernameOrEmail: String, password: String): Result<LoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.login(LoginRequest(usernameOrEmail, password))
                if (response.isSuccessful) {
                    response.body()?.let { loginResponse ->
                        if (loginResponse.success && !loginResponse.token.isNullOrEmpty()) {
                            val sharedPreferences = context.getSharedPreferences(
                                "auth_prefs", Context.MODE_PRIVATE
                            )
                            sharedPreferences.edit()
                                .putString("auth_token", loginResponse.token)
                                .apply()
                            isAuthenticated = true
                            showToast("Login successful")
                            Result.success(loginResponse)
                        } else {
                            val errorMessage = loginResponse.message.ifEmpty { "Invalid response from server" }
                            showToast(errorMessage)

                            Result.failure(Exception(errorMessage))
                        }
                    } ?: run {
                        showToast("Login failed: Empty response")
                        Result.failure(Exception("Login failed: Empty response"))
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = when (response.code()) {
                        400 -> {
                            try {
                                val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                                errorResponse?.message ?: errorResponse?.error ?: "Bad request: Invalid input"
                            } catch (e: Exception) {
                                "Bad request: ${errorBody ?: response.message()}"
                            }
                        }
                        401 -> "Invalid username or password"
                        500 -> "Server error: Please try again later"
                        else -> "Login failed: ${response.message()}"
                    }
                    Log.e("AuthRepository", "HTTP ${response.code()}: $errorBody")
                    showToast(errorMessage)
                    Result.failure(HttpException(response))
                }
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Network error: Unable to connect"
                Log.e("AuthRepository", "Login error", e)
                showToast(errorMessage)
                Result.failure(e)
            }
        }
    }

    fun logout() {
        val sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().remove("auth_token").apply()
        isAuthenticated = false
        showToast("Logged out successfully")
    }

    private fun showToast(message: String) {
        coroutineScope.launch {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}

data class ErrorResponse(
    @SerializedName("message") val message: String?,
    @SerializedName("error") val error: String?
)