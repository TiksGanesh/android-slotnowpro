package app.slotnow.slotnowpro.presentation.onboarding

import app.slotnow.slotnowpro.data.local.prefs.LanguageManager
import app.slotnow.slotnowpro.data.local.prefs.ShopManager
import app.slotnow.slotnowpro.domain.model.ShopInfo
import app.slotnow.slotnowpro.domain.repository.OnboardingRepository
import app.slotnow.slotnowpro.util.ApiResult
import app.slotnow.slotnowpro.util.AppLogger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val onboardingRepository = mockk<OnboardingRepository>()
    private val shopManager = mockk<ShopManager>(relaxed = true)
    private val languageManager = mockk<LanguageManager>(relaxed = true)
    private val localeUpdater = mockk<LocaleUpdater>(relaxed = true)
    private val appLogger = mockk<AppLogger>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads persisted language code`() {
        every { languageManager.get() } returns "en"

        val viewModel = OnboardingViewModel(
            onboardingRepository,
            shopManager,
            languageManager,
            localeUpdater,
            appLogger
        )

        val state = viewModel.uiState.value
        assertEquals("en", state.selectedLanguageCode)
        assertEquals("", state.shopSlugInput)
        assertEquals(ShopValidationState.Idle, state.shopValidationState)
    }

    @Test
    fun `selectLanguage updates draft only without persistence or locale apply`() {
        every { languageManager.get() } returns "en"

        val viewModel = OnboardingViewModel(
            onboardingRepository,
            shopManager,
            languageManager,
            localeUpdater,
            appLogger
        )

        viewModel.selectLanguage("hi")

        assertEquals("hi", viewModel.uiState.value.selectedLanguageCode)

        verify(exactly = 0) { languageManager.save(any()) }
        verify(exactly = 0) { localeUpdater.apply(any()) }
    }

    @Test
    fun `confirmLanguageSelection persists choice and applies locale`() = runTest {
        every { languageManager.get() } returns "en"

        val viewModel = OnboardingViewModel(
            onboardingRepository,
            shopManager,
            languageManager,
            localeUpdater,
            appLogger
        )

        viewModel.selectLanguage("hi")
        viewModel.confirmLanguageSelection()

        assertEquals("hi", viewModel.uiState.value.selectedLanguageCode)

        verify(exactly = 1) { languageManager.save("hi") }
        verify(exactly = 1) { localeUpdater.apply("hi") }
    }

    @Test
    fun `updateShopSlugInput sanitizes slug and resets validation state`() {
        every { languageManager.get() } returns "en"

        val viewModel = OnboardingViewModel(
            onboardingRepository,
            shopManager,
            languageManager,
            localeUpdater,
            appLogger
        )

        // Spaces are replaced with hyphens so free-text input produces a valid slug format.
        viewModel.updateShopSlugInput("  CUTS By Raj  ")
        assertEquals("cuts-by-raj", viewModel.uiState.value.shopSlugInput)
        assertEquals(ShopValidationState.Idle, viewModel.uiState.value.shopValidationState)

        // Already-formatted slug passes through unchanged.
        viewModel.updateShopSlugInput("cuts-by-raj")
        assertEquals("cuts-by-raj", viewModel.uiState.value.shopSlugInput)
    }

    @Test
    fun `validateShopSlug maps success and persists shop context`() = runTest {
        every { languageManager.get() } returns "en"
        val expectedShop = ShopInfo(
            shopSlug = "cuts-by-raj",
            shopName = "Cuts by Raj",
            shopTimezone = "Asia/Kolkata",
            logoUrl = null
        )
        coEvery { onboardingRepository.validateShopSlug("cuts-by-raj") } returns ApiResult.Success(expectedShop)

        val viewModel = OnboardingViewModel(
            onboardingRepository,
            shopManager,
            languageManager,
            localeUpdater,
            appLogger
        )

        viewModel.updateShopSlugInput("cuts-by-raj")
        viewModel.validateShopSlug()
        advanceUntilIdle()

        assertEquals(ShopValidationState.Valid("Cuts by Raj"), viewModel.uiState.value.shopValidationState)
        coVerify(exactly = 1) { onboardingRepository.validateShopSlug("cuts-by-raj") }
        verify(exactly = 1) { shopManager.save(expectedShop) }
    }

    @Test
    fun `validateShopSlug maps api and network errors`() = runTest {
        every { languageManager.get() } returns "en"
        coEvery { onboardingRepository.validateShopSlug("missing") } returns ApiResult.ApiError(
            code = "SHOP_NOT_FOUND",
            message = "No shop",
            httpStatus = 404
        )
        coEvery { onboardingRepository.validateShopSlug("inactive") } returns ApiResult.ApiError(
            code = "SHOP_INACTIVE",
            message = "Inactive",
            httpStatus = 403
        )
        coEvery { onboardingRepository.validateShopSlug("network") } returns ApiResult.NetworkError(
            java.io.IOException("offline")
        )

        val viewModel = OnboardingViewModel(
            onboardingRepository,
            shopManager,
            languageManager,
            localeUpdater,
            appLogger
        )

        viewModel.updateShopSlugInput("missing")
        viewModel.validateShopSlug()
        advanceUntilIdle()
        assertEquals(ShopValidationState.NotFound, viewModel.uiState.value.shopValidationState)

        viewModel.updateShopSlugInput("inactive")
        viewModel.validateShopSlug()
        advanceUntilIdle()
        assertEquals(ShopValidationState.Inactive, viewModel.uiState.value.shopValidationState)

        viewModel.updateShopSlugInput("network")
        viewModel.validateShopSlug()
        advanceUntilIdle()
        assertEquals(ShopValidationState.NetworkError, viewModel.uiState.value.shopValidationState)
    }

    @Test
    fun `validateShopSlug marks empty input`() {
        every { languageManager.get() } returns "en"

        val viewModel = OnboardingViewModel(
            onboardingRepository,
            shopManager,
            languageManager,
            localeUpdater,
            appLogger
        )

        viewModel.updateShopSlugInput("   ")
        viewModel.validateShopSlug()

        assertEquals(ShopValidationState.InvalidInput, viewModel.uiState.value.shopValidationState)
        coVerify(exactly = 0) { onboardingRepository.validateShopSlug(any()) }
    }

    @Test
    fun `hasShopSlug reflects manager state`() {
        every { languageManager.get() } returns "en"
        every { shopManager.getSlug() } returns "cuts-by-raj"

        val viewModel = OnboardingViewModel(
            onboardingRepository,
            shopManager,
            languageManager,
            localeUpdater,
            appLogger
        )

        assertTrue(viewModel.hasShopSlug())
    }
}

