package com.example.expensees.network

import com.example.expensees.models.Expense
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("api/auth/sign-in")
    suspend fun signIn(@Body credentials: SignInRequest): Response<SignInResponse>

    @POST("api/auth/refresh-token")
    suspend fun refreshToken(
        @Body refreshRequest: RefreshTokenRequest,
        @Header("refresh_token") authHeader: String? = null // Add if required
    ): Response<SignInResponse>


    @Multipart
    @POST("api/expenses")
    suspend fun addExpense(
        @Header("refresh_token") token: String,
        @Part("category") category: RequestBody,
        @Part("amount") amount: RequestBody,
        @Part("dateOfTransaction") dateOfTransaction: RequestBody,
        @Part("remarks") remarks: RequestBody?,
        @Part("createdAt") createdAt: RequestBody,
        @Part image: MultipartBody.Part?
    ): Response<Expense>

    @DELETE("expenses/{expenseId}")
    suspend fun deleteExpense(
        @Header("refresh_token") token: String,
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
    @SerializedName("refresh_token") val refreshToken: String // Changed from "refreshToken"
)
