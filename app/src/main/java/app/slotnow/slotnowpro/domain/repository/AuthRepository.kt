package app.slotnow.slotnowpro.domain.repository

import app.slotnow.slotnowpro.domain.model.AuthSession
import app.slotnow.slotnowpro.domain.model.OtpRequestInfo
import app.slotnow.slotnowpro.util.ApiResult

interface AuthRepository {
    suspend fun requestOtp(phone: String, shopSlug: String): ApiResult<OtpRequestInfo>

    suspend fun verifyOtp(phone: String, code: String, shopSlug: String): ApiResult<AuthSession>
}

