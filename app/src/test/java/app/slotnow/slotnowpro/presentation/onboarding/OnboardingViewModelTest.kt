package app.slotnow.slotnowpro.presentation.onboarding

import app.slotnow.slotnowpro.data.local.prefs.LanguageManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val languageManager = mockk<LanguageManager>(relaxed = true)
    private val localeUpdater = mockk<LocaleUpdater>(relaxed = true)

    @Test
    fun `initial state shows selection when language is missing`() {
        every { languageManager.getOrDefault() } returns "en"
        every { languageManager.get() } returns null

        val viewModel = OnboardingViewModel(languageManager, localeUpdater)

        val state = viewModel.uiState.value
        assertEquals("en", state.selectedLanguageCode)
        assertTrue(state.showLanguageSelection)
    }

    @Test
    fun `initial state skips selection when language already stored`() {
        every { languageManager.getOrDefault() } returns "mr"
        every { languageManager.get() } returns "mr"

        val viewModel = OnboardingViewModel(languageManager, localeUpdater)

        val state = viewModel.uiState.value
        assertEquals("mr", state.selectedLanguageCode)
        assertFalse(state.showLanguageSelection)
    }

    @Test
    fun `selectLanguage updates draft only without persistence or locale apply`() {
        every { languageManager.getOrDefault() } returns "en"
        every { languageManager.get() } returns null

        val viewModel = OnboardingViewModel(languageManager, localeUpdater)

        viewModel.selectLanguage("hi")

        assertEquals("hi", viewModel.uiState.value.selectedLanguageCode)
        assertTrue(viewModel.uiState.value.showLanguageSelection)

        verify(exactly = 0) { languageManager.save(any()) }
        verify(exactly = 0) { localeUpdater.apply(any()) }
    }

    @Test
    fun `confirmLanguageSelection persists choice applies locale and emits snackbar event`() = runTest {
        every { languageManager.getOrDefault() } returns "en"
        every { languageManager.get() } returns null

        val viewModel = OnboardingViewModel(languageManager, localeUpdater)
        val collectedEvents = mutableListOf<OnboardingUiEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { collectedEvents.add(it) }
        }

        viewModel.selectLanguage("hi")
        viewModel.confirmLanguageSelection()

        assertEquals("hi", viewModel.uiState.value.selectedLanguageCode)
        assertFalse(viewModel.uiState.value.showLanguageSelection)
        assertEquals(1, collectedEvents.size)
        assertEquals(OnboardingUiEvent.ShowLanguageUpdatedSnackbar("hi"), collectedEvents[0])

        verify(exactly = 1) { languageManager.save("hi") }
        verify(exactly = 1) { localeUpdater.apply("hi") }
        collector.cancel()
    }

    @Test
    fun `confirmLanguageSelection emits snackbar event on each confirmation`() = runTest {
        every { languageManager.getOrDefault() } returns "en"
        every { languageManager.get() } returns null

        val viewModel = OnboardingViewModel(languageManager, localeUpdater)
        val collectedEvents = mutableListOf<OnboardingUiEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { collectedEvents.add(it) }
        }

        viewModel.selectLanguage("hi")
        viewModel.confirmLanguageSelection()
        assertEquals(1, collectedEvents.size)
        assertEquals(OnboardingUiEvent.ShowLanguageUpdatedSnackbar("hi"), collectedEvents[0])

        viewModel.selectLanguage("mr")
        viewModel.confirmLanguageSelection()
        assertEquals(2, collectedEvents.size)
        assertEquals(OnboardingUiEvent.ShowLanguageUpdatedSnackbar("mr"), collectedEvents[1])
        assertEquals("mr", viewModel.uiState.value.selectedLanguageCode)

        verify(exactly = 1) { languageManager.save("hi") }
        verify(exactly = 1) { languageManager.save("mr") }
        verify(exactly = 1) { localeUpdater.apply("hi") }
        verify(exactly = 1) { localeUpdater.apply("mr") }
        collector.cancel()
    }
}

