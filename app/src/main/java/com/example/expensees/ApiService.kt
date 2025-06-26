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

    @POST("api/auth/refresh")
    suspend fun refreshToken(@Body refreshRequest: RefreshTokenRequest): Response<SignInResponse>

    @Multipart
    @POST("api/expenses")
    suspend fun addExpense(
        @Header("Authorization") token: String, // Changed from expenseId to Authorization
        @Part("category") category: RequestBody,
        @Part("amount") amount: RequestBody,
        @Part("dateOfTransaction") dateOfTransaction: RequestBody,
        @Part("remarks") comments: RequestBody?,
        @Part("createdAt") createdAt: RequestBody,
        @Part image: MultipartBody.Part?
    ): Response<Expense>

    @DELETE("expenses/{expenseId}") // Changed from {id} to {expenseId}
    suspend fun deleteExpense(
        @Header("Authorization") token: String,
        @Path("expenseId") expenseId: String // Changed from id to expenseId
    ): Response<Unit>
}

data class SignInRequest(
    @SerializedName("usernameOrEmail") val usernameOrEmail: String,
    @SerializedName("password") val password: String
)

data class SignInResponse(
    @SerializedName("jwt") val token: String?,
    @SerializedName("user_id") val user_Id: String?,
    @SerializedName("refreshToken") val refreshToken: String?
)

data class ErrorResponse(
    @SerializedName("message") val message: String?,
    @SerializedName("error") val error: String?
)

data class RefreshTokenRequest(
    @SerializedName("refreshToken") val refreshToken: String
)