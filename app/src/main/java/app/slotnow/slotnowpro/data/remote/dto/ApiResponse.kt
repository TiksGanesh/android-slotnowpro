package app.slotnow.slotnowpro.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Standard API response envelope matching backend contract.
 * Generic wrapper for all success/error responses from `/api/v1/barber/` endpoints.
 */
data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: T? = null,
    @SerializedName("error")
    val error: ApiErrorBody? = null,
    @SerializedName("meta")
    val meta: ApiMeta? = null
)

data class ApiErrorBody(
    @SerializedName("code")
    val code: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("hint")
    val hint: String? = null
)

data class ApiMeta(
    @SerializedName("timestamp")
    val timestamp: String? = null,
    @SerializedName("timezone")
    val timezone: String? = null
)

