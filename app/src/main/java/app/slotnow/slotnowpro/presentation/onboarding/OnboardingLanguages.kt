package app.slotnow.slotnowpro.presentation.onboarding

import java.util.Locale

data class LanguageOption(
    val code: String,
    val nativeLabel: String,
    val englishLabel: String,
    val snackbarLabel: String,
    val flagEmoji: String? = null
)

object OnboardingLanguages {
    private const val FALLBACK_LANGUAGE_CODE = "en"

    val options = listOf(
        LanguageOption(
            code = "en",
            nativeLabel = "English",
            englishLabel = "English",
            snackbarLabel = "English",
            flagEmoji = "🇬🇧"
        ),
        LanguageOption(
            code = "hi",
            nativeLabel = "हिंदी",
            englishLabel = "Hindi",
            snackbarLabel = "हिंदी",
            flagEmoji = "🇮🇳"
        ),
        LanguageOption(
            code = "mr",
            nativeLabel = "मराठी",
            englishLabel = "Marathi",
            snackbarLabel = "मराठी",
            flagEmoji = "🇮🇳"
        )
    )

    fun snackbarMessage(code: String?): String {
        val option = options.firstOrNull { it.code == code }
        return if (option != null) {
            "Language updated: ${option.snackbarLabel}"
        } else {
            "Language updated"
        }
    }

    fun resolveInitialSelection(
        persistedCode: String?,
        deviceLocale: Locale = Locale.getDefault()
    ): String {
        return if (isSupportedCode(persistedCode)) {
            persistedCode.orEmpty()
        } else {
            defaultCodeForLocale(deviceLocale)
        }
    }

    fun defaultCodeForLocale(locale: Locale = Locale.getDefault()): String {
        val languageCode = locale.language.lowercase(Locale.ROOT)
        return if (isSupportedCode(languageCode)) {
            languageCode
        } else {
            FALLBACK_LANGUAGE_CODE
        }
    }

    fun isSupportedCode(code: String?): Boolean {
        return code != null && options.any { it.code == code }
    }
}

