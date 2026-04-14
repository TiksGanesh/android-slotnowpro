package app.slotnow.slotnowpro.data.remote.dto.onboarding

import com.google.gson.annotations.SerializedName

/**
 * Response data for GET /api/v1/barber/shop/{slug}/validate
 */
data class ShopValidateData(
    @SerializedName("shopSlug")
    val shopSlug: String,
    @SerializedName("shopName")
    val shopName: String,
    @SerializedName("shopTimezone")
    val shopTimezone: String,
    @SerializedName("logoUrl")
    val logoUrl: String? = null
)

