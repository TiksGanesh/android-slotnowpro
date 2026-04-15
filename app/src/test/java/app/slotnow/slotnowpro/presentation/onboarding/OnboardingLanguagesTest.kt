package app.slotnow.slotnowpro.presentation.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class OnboardingLanguagesTest {

    @Test
    fun `options contain expected onboarding language codes`() {
        val codes = OnboardingLanguages.options.map { it.code }

        assertEquals(listOf("en", "hi", "mr"), codes)
    }

    @Test
    fun `snackbarMessage uses shared label for known code`() {
        val message = OnboardingLanguages.snackbarMessage("hi")

        assertEquals("Language updated: हिंदी", message)
    }

    @Test
    fun `snackbarMessage falls back for unknown code`() {
        val message = OnboardingLanguages.snackbarMessage("xx")

        assertTrue(message == "Language updated")
    }

    @Test
    fun `defaultCodeForLocale uses supported device locale language`() {
        assertEquals("hi", OnboardingLanguages.defaultCodeForLocale(Locale.forLanguageTag("hi-IN")))
        assertEquals("mr", OnboardingLanguages.defaultCodeForLocale(Locale.forLanguageTag("mr-IN")))
    }

    @Test
    fun `defaultCodeForLocale falls back to english for unsupported locale`() {
        assertEquals("en", OnboardingLanguages.defaultCodeForLocale(Locale.forLanguageTag("fr-FR")))
    }

    @Test
    fun `resolveInitialSelection prefers persisted supported code`() {
        assertEquals(
            "mr",
            OnboardingLanguages.resolveInitialSelection(
                persistedCode = "mr",
                deviceLocale = Locale.forLanguageTag("hi-IN")
            )
        )
    }

    @Test
    fun `resolveInitialSelection falls back to device locale when persisted code is missing`() {
        assertEquals(
            "hi",
            OnboardingLanguages.resolveInitialSelection(
                persistedCode = null,
                deviceLocale = Locale.forLanguageTag("hi-IN")
            )
        )
    }
}

