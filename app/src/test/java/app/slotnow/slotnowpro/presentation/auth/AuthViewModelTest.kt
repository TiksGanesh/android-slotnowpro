package app.slotnow.slotnowpro.presentation.auth

import app.slotnow.slotnowpro.data.local.prefs.ShopManager
import app.slotnow.slotnowpro.data.local.prefs.TokenManager
import app.slotnow.slotnowpro.domain.model.AuthSession
import app.slotnow.slotnowpro.domain.model.OtpRequestInfo
import app.slotnow.slotnowpro.domain.model.ShopInfo
import app.slotnow.slotnowpro.domain.repository.AuthRepository
import app.slotnow.slotnowpro.util.ApiResult
import app.slotnow.slotnowpro.util.AppLogger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val authRepository = mockk<AuthRepository>()
    private val shopManager = mockk<ShopManager>(relaxed = true)
    private val tokenManager = mockk<TokenManager>(relaxed = true)
    private val appLogger = mockk<AppLogger>(relaxed = true)

    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setUp() {
        // Create a StandardTestDispatcher that will be properly synchronized with runTest.
        // Each runTest block will use this dispatcher for Main, ensuring advanceTimeBy/runCurrent
        // can control time on the ViewModel's viewModelScope.
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state requests onboarding redirect when shop slug missing`() {
        every { shopManager.getName() } returns null
        every { shopManager.getSlug() } returns null
        every { shopManager.getLogoUrl() } returns null

        val viewModel = AuthViewModel(
            authRepository = authRepository,
            shopManager = shopManager,
            tokenManager = tokenManager,
            appLogger = appLogger
        )

        assertTrue(viewModel.uiState.value.requiresShopSetup)
    }

    @Test
    fun `requestOtp success emits OTP navigation and starts countdown`() = runTest {
        every { shopManager.getName() } returns "Modern Salon"
        every { shopManager.getSlug() } returns "modern-salon"
        every { shopManager.getLogoUrl() } returns null
        coEvery {
            authRepository.requestOtp("9876543210", "modern-salon")
        } returns ApiResult.Success(
            OtpRequestInfo(maskedPhone = "+91 98xxx x3210", otpExpiresInSeconds = 300)
        )

        val viewModel = AuthViewModel(
            authRepository = authRepository,
            shopManager = shopManager,
            tokenManager = tokenManager,
            appLogger = appLogger
        )
        viewModel.updatePhoneInput("9876543210")

        val eventDeferred = async { viewModel.navigationEvents.first() }
        viewModel.requestOtpFromLogin()
        runCurrent()

        assertEquals(AuthNavigationEvent.ToOtp, eventDeferred.await())
        assertEquals("+91 98xxx x3210", viewModel.uiState.value.maskedPhone)
        assertEquals(30, viewModel.uiState.value.resendCountdownSeconds)

        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(29, viewModel.uiState.value.resendCountdownSeconds)

        advanceUntilIdle()
    }

    @Test
    fun `verifyOtp blocks short code and does not call repository`() = runTest {
        every { shopManager.getName() } returns "Modern Salon"
        every { shopManager.getSlug() } returns "modern-salon"
        every { shopManager.getLogoUrl() } returns null

        val viewModel = AuthViewModel(
            authRepository = authRepository,
            shopManager = shopManager,
            tokenManager = tokenManager,
            appLogger = appLogger
        )
        viewModel.updatePhoneInput("9876543210")
        viewModel.updateOtpInput("12345")

        viewModel.verifyOtp()
        advanceUntilIdle()

        assertEquals(OtpError.InvalidCode, viewModel.uiState.value.otpError)
        coVerify(exactly = 0) { authRepository.verifyOtp(any(), any(), any()) }
    }

    @Test
    fun `verifyOtp success saves token and emits main navigation`() = runTest {
        every { shopManager.getName() } returns "Modern Salon"
        every { shopManager.getSlug() } returns "modern-salon"
        every { shopManager.getLogoUrl() } returns null
        every { tokenManager.saveToken(any(), any()) } just runs
        every { shopManager.save(any()) } just runs

        val expiresAt = Instant.now().plusSeconds(3600)
        val session = AuthSession(
            token = "token-123",
            expiresAt = expiresAt,
            shopInfo = ShopInfo(
                shopSlug = "modern-salon",
                shopName = "Modern Salon",
                shopTimezone = "Asia/Kolkata",
                logoUrl = null
            )
        )
        coEvery {
            authRepository.verifyOtp("9876543210", "123456", "modern-salon")
        } returns ApiResult.Success(session)

        val viewModel = AuthViewModel(
            authRepository = authRepository,
            shopManager = shopManager,
            tokenManager = tokenManager,
            appLogger = appLogger
        )
        viewModel.updatePhoneInput("9876543210")
        viewModel.updateOtpInput("123456")

        val eventDeferred = async { viewModel.navigationEvents.first() }
        viewModel.verifyOtp()
        advanceUntilIdle()

        assertEquals(AuthNavigationEvent.ToMain, eventDeferred.await())
        verify(exactly = 1) { tokenManager.saveToken("token-123", expiresAt) }
        verify(exactly = 1) { shopManager.save(session.shopInfo) }

        advanceUntilIdle()
    }
}


