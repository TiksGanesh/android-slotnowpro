package app.slotnow.slotnowpro.domain.model

import java.time.Instant

data class OtpRequestInfo(
    val maskedPhone: String,
    val otpExpiresInSeconds: Int
)

data class AuthSession(
    val token: String,
    val expiresAt: Instant,
    val shopInfo: ShopInfo
)

