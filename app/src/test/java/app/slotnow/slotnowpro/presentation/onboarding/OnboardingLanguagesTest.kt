package app.slotnow.slotnowpro.presentation.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
}

