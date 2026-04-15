package app.slotnow.slotnowpro.domain.repository

import app.slotnow.slotnowpro.domain.model.ShopInfo
import app.slotnow.slotnowpro.util.ApiResult

interface OnboardingRepository {
    suspend fun validateShopSlug(slug: String): ApiResult<ShopInfo>
}

