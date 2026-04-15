package app.slotnow.slotnowpro.presentation.onboarding

data class LanguageOption(
    val code: String,
    val nativeLabel: String,
    val englishLabel: String,
    val snackbarLabel: String
)

object OnboardingLanguages {
    val options = listOf(
        LanguageOption(
            code = "en",
            nativeLabel = "English",
            englishLabel = "English",
            snackbarLabel = "English"
        ),
        LanguageOption(
            code = "hi",
            nativeLabel = "हिंदी",
            englishLabel = "Hindi",
            snackbarLabel = "हिंदी"
        ),
        LanguageOption(
            code = "mr",
            nativeLabel = "मराठी",
            englishLabel = "Marathi",
            snackbarLabel = "मराठी"
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
}

