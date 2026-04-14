package app.slotnow.slotnowpro.data.local.prefs

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

/**
 * Manages Bearer token and session expiry persistence.
 * Uses EncryptedSharedPreferences for secure storage.
 * Token is cleared on logout, but does NOT clear shop slug or language.
 */
@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "barber_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(token: String, expiresAt: Instant) {
        prefs.edit {
            putString(KEY_TOKEN, token)
                .putLong(KEY_EXPIRES_AT, expiresAt.toEpochMilli())
        }
    }

    fun getToken(): String? {
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)

        // Check if token is expired
        if (Instant.now().toEpochMilli() >= expiresAt) {
            clearToken()
            return null
        }
        return token
    }

    fun clearToken() {
        // Clears token only — does NOT clear shop slug or language
        prefs.edit {
            remove(KEY_TOKEN)
                .remove(KEY_EXPIRES_AT)
        }
    }

    companion object {
        private const val KEY_TOKEN = "barber_token"
        private const val KEY_EXPIRES_AT = "token_expires_at"
    }
}

