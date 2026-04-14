package app.slotnow.slotnowpro.data.local.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

/**
 * Manages language preference persistence.
 * Uses regular SharedPreferences (not encrypted) — language is not sensitive data.
 * Language is NEVER cleared on logout — it is a device preference.
 */
@Singleton
class LanguageManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(
        "app_prefs",
        Context.MODE_PRIVATE
    )

    fun save(code: String) {
        prefs.edit { putString(KEY_LANG, code) }
    }

    fun get(): String? = prefs.getString(KEY_LANG, null)

    fun getOrDefault(): String = prefs.getString(KEY_LANG, null) ?: "en"

    companion object {
        private const val KEY_LANG = "language"
    }
}

