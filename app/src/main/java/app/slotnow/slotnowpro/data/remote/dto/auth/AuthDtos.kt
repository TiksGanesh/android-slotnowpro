package app.slotnow.slotnowpro.data.remote.dto.auth

import com.google.gson.annotations.SerializedName

/**
 * Request body for POST /api/v1/barber/auth/request-otp
 */
data class RequestOtpRequest(
    @SerializedName("phone")
    val phone: String,
    @SerializedName("shopSlug")
    val shopSlug: String
)

/**
 * Response data for POST /api/v1/barber/auth/request-otp
 */
data class RequestOtpData(
    @SerializedName("maskedPhone")
    val maskedPhone: String,
    @SerializedName("otpExpiresInSeconds")
    val otpExpiresInSeconds: Int
)

/**
 * Request body for POST /api/v1/barber/auth/verify-otp
 */
data class VerifyOtpRequest(
    @SerializedName("phone")
    val phone: String,
    @SerializedName("code")
    val code: String,
    @SerializedName("shopSlug")
    val shopSlug: String
)

/**
 * Response data for POST /api/v1/barber/auth/verify-otp
 */
data class VerifyOtpData(
    @SerializedName("token")
    val token: String,
    @SerializedName("expiresAt")
    val expiresAt: String,
    @SerializedName("barber")
    val barber: BarberProfileData
)

/**
 * Response data for POST /api/v1/barber/auth/refresh
 * Note: refresh endpoint uses Authorization: Bearer header, no request body
 */
data class RefreshTokenData(
    @SerializedName("token")
    val token: String,
    @SerializedName("expiresAt")
    val expiresAt: String
)

/**
 * Request body for POST /api/v1/barber/auth/update-profile
 */
data class UpdateProfileRequest(
    @SerializedName("name")
    val name: String,
    @SerializedName("phone")
    val phone: String
)

/**
 * Response data for GET /api/v1/barber/auth/me
 */
data class MeData(
    @SerializedName("barber")
    val barber: BarberProfileData,
    @SerializedName("session")
    val session: SessionData
)

/**
 * Barber profile information returned in auth endpoints
 */
data class BarberProfileData(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("phone")
    val phone: String,
    @SerializedName("designation")
    val designation: String? = null,
    @SerializedName("shopId")
    val shopId: String,
    @SerializedName("shopName")
    val shopName: String,
    @SerializedName("shopSlug")
    val shopSlug: String,
    @SerializedName("shopTimezone")
    val shopTimezone: String,
    @SerializedName("logoUrl")
    val logoUrl: String? = null,
    @SerializedName("brandColor")
    val brandColor: String? = null
)

/**
 * Session metadata
 */
data class SessionData(
    @SerializedName("expiresAt")
    val expiresAt: String
)

/**
 * Response data for POST /api/v1/barber/auth/update-profile
 */
data class UpdateProfileData(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("phone")
    val phone: String,
    @SerializedName("designation")
    val designation: String? = null
)

/**
 * Response data for POST /api/v1/barber/auth/logout
 */
data class LogoutData(
    @SerializedName("message")
    val message: String
)

