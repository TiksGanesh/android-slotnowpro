package app.slotnow.slotnowpro

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import app.slotnow.slotnowpro.data.local.prefs.LanguageManager
import dagger.hilt.android.HiltAndroidApp

/**
 * Hilt application entry point.
 * Enables dependency injection across the app.
 */
@HiltAndroidApp
class MainApplication : Application() {
	override fun onCreate() {
		// Apply persisted locale before the first Activity is created.
		val languageManager = LanguageManager(this)
		val lang = languageManager.getOrDefault()
		AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))

		super.onCreate()
	}
}

