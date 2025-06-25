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

    @Multipart
    @POST("expenses")
    suspend fun addExpense(
        @Header("Authorization") token: String,
        @Part("category") category: RequestBody,
        @Part("amount") amount: RequestBody,
        @Part("dateOfTransaction") dateOfTransaction: RequestBody,
        @Part("comments") comments: RequestBody?,
        @Part("createdAt") createdAt: RequestBody,
        @Part image: MultipartBody.Part?
    ): Response<Expense>

    @DELETE("expenses/{id}")
    suspend fun deleteExpense(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<Unit>
}

data class SignInRequest(
    @SerializedName("usernameOrEmail") val usernameOrEmail: String,
    @SerializedName("password") val password: String
)

data class SignInResponse(
    @SerializedName("token") val token: String,
    @SerializedName("userId") val userId: String
)

data class ErrorResponse(
    @SerializedName("message") val message: String?,
    @SerializedName("error") val error: String?
)