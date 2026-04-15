package app.slotnow.slotnowpro.data.local.prefs

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import app.slotnow.slotnowpro.domain.model.ShopInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores shop context selected during onboarding.
 * Shop data is cleared on logout while language remains intact.
 */
@Singleton
class ShopManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "shop_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun save(shopInfo: ShopInfo) {
        prefs.edit {
            putString(KEY_SLUG, shopInfo.shopSlug)
            putString(KEY_NAME, shopInfo.shopName)
            putString(KEY_TIMEZONE, shopInfo.shopTimezone)
            putString(KEY_LOGO_URL, shopInfo.logoUrl)
        }
    }

    fun getSlug(): String? = prefs.getString(KEY_SLUG, null)

    fun getName(): String? = prefs.getString(KEY_NAME, null)

    fun getTimezone(): String? = prefs.getString(KEY_TIMEZONE, null)

    fun getLogoUrl(): String? = prefs.getString(KEY_LOGO_URL, null)

    fun clear() {
        prefs.edit {
            remove(KEY_SLUG)
            remove(KEY_NAME)
            remove(KEY_TIMEZONE)
            remove(KEY_LOGO_URL)
        }
    }

    companion object {
        private const val KEY_SLUG = "shop_slug"
        private const val KEY_NAME = "shop_name"
        private const val KEY_TIMEZONE = "shop_timezone"
        private const val KEY_LOGO_URL = "shop_logo_url"
    }
}

