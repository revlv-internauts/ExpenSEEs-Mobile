package com.example.expensees.network

import com.example.expensees.models.Expense
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @POST("api/auth/sign-in")
    suspend fun signIn(@Body credentials: SignInRequest): Response<SignInResponse>

    @POST("expenses")
    suspend fun addExpense(@Body expense: Expense): Response<Expense>

    @DELETE("expenses/{id}")
    suspend fun deleteExpense(@Path("id") id: String): Response<Unit>
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