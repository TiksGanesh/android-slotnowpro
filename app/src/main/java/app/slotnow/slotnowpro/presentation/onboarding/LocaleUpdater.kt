package app.slotnow.slotnowpro.presentation.onboarding

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import javax.inject.Inject

interface LocaleUpdater {
    fun apply(code: String)
}

class AppCompatLocaleUpdater @Inject constructor() : LocaleUpdater {
    override fun apply(code: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
    }
}

