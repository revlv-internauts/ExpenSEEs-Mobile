package com.example.expensees.network

import com.example.expensees.models.Expense
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("api/auth/sign-in")
    suspend fun signIn(@Body credentials: SignInRequest): Response<SignInResponse>

    @POST("api/auth/refresh-token")
    suspend fun refreshToken(
        @Body refreshRequest: RefreshTokenRequest,
        @Header("refresh_token") authHeader: String? = null
    ): Response<SignInResponse>

    @POST("api/expenses")
    suspend fun addExpense(
        @Header("Authorization") token: String,
        @Body expense: ExpenseRequest
    ): Response<Expense>

    @DELETE("expenses/{expenseId}")
    suspend fun deleteExpense(
        @Header("Authorization") token: String,
        @Path("expenseId") expenseId: String
    ): Response<Unit>
}

data class SignInRequest(
    @SerializedName("usernameOrEmail") val usernameOrEmail: String,
    @SerializedName("password") val password: String
)

data class SignInResponse(
    @SerializedName("access_token") val token: String?,
    @SerializedName("user_id") val user_Id: String?,
    @SerializedName("refresh_token") val refreshToken: String?
)

data class ErrorResponse(
    @SerializedName("message") val message: String?,
    @SerializedName("error") val error: String?
)

data class RefreshTokenRequest(
    @SerializedName("refresh_token") val refreshToken: String
)

// New DTO for JSON request body
data class ExpenseRequest(
    @SerializedName("category") val category: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("dateOfTransaction") val dateOfTransaction: String,
    @SerializedName("remarks") val remarks: String?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("image") val image: String? = null // Base64 string if server supports images
)