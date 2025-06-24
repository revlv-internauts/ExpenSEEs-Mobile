package com.example.expensees.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class LoginRequest(
    @SerializedName("usernameOrEmail") val usernameOrEmail: String,
    @SerializedName("password") val password: String
)

data class LoginResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?, // Changed to nullable
    @SerializedName("token") val token: String?
)

interface ApiService {
    @POST("api/auth/sign-in")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}