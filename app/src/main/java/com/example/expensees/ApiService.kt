package com.example.expensees.network

import com.example.expensees.models.Expense
import com.example.expensees.models.SubmittedBudget
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
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

    @Multipart
    @POST("api/expenses")
    suspend fun addExpense(
        @Header("Authorization") token: String,
        @Part("category") category: RequestBody,
        @Part("amount") amount: RequestBody,
        @Part("dateOfTransaction") dateOfTransaction: RequestBody,
        @Part("remarks") remarks: RequestBody?,
        @Part("createdAt") createdAt: RequestBody,
        @Part files: MultipartBody.Part? // Changed from "image" to "files"
    ): Response<Expense>

    @DELETE("api/expenses/{expenseId}")
    suspend fun deleteExpense(
        @Header("Authorization") token: String,
        @Path("expenseId") expenseId: String
    ): Response<Unit>

    @GET("api/expenses")
    suspend fun getExpenses(
        @Header("Authorization") token: String
    ): Response<List<Expense>>

    @POST("api/budgets")
    suspend fun addBudget(
        @Header("Authorization") token: String,
        @Body budget: SubmittedBudget
    ): Response<SubmittedBudget>

    @GET("api/budgets")
    suspend fun getBudgets(
        @Header("Authorization") token: String
    ): Response<List<SubmittedBudget>>

    @Multipart
    @POST("api/users/{userId}/profile-picture")
    suspend fun uploadProfilePicture(
        @Header("Authorization") token: String,
        @Path("userId") userId: String,
        @Part file: MultipartBody.Part
    ): Response<Unit>

    @GET("api/users/{userId}/profile-picture")
    suspend fun getProfilePicture(
        @Header("Authorization") token: String,
        @Path("userId") userId: String
    ): Response<ResponseBody>
}

data class SignInRequest(
    @SerializedName("usernameOrEmail") val usernameOrEmail: String,
    @SerializedName("password") val password: String
)

data class SignInResponse(
    @SerializedName("access_token") val token: String?,
    @SerializedName("user_id") val user_Id: String?,
    @SerializedName("refresh_token") val refreshToken: String?,
    @SerializedName("username") val username: String?,
    @SerializedName("email") val email: String?
)

data class ResetPasswordRequest(
    @SerializedName("email") val email: String,
    @SerializedName("newPassword") val newPassword: String
)

data class ErrorResponse(
    @SerializedName("message") val message: String?,
    @SerializedName("error") val error: String?
)

data class RefreshTokenRequest(
    @SerializedName("refresh_token") val refreshToken: String
)

data class ExpenseRequest(
    @SerializedName("category") val category: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("dateOfTransaction") val dateOfTransaction: String,
    @SerializedName("remarks") val remarks: String?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("image") val image: String? = null
)