package app.slotnow.slotnowpro.presentation.auth

import androidx.lifecycle.viewModelScope
import app.slotnow.slotnowpro.data.local.prefs.ShopManager
import app.slotnow.slotnowpro.data.local.prefs.TokenManager
import app.slotnow.slotnowpro.domain.repository.AuthRepository
import app.slotnow.slotnowpro.presentation.BaseViewModel
import app.slotnow.slotnowpro.util.ApiResult
import app.slotnow.slotnowpro.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val shopManager: ShopManager,
    private val tokenManager: TokenManager,
    appLogger: AppLogger
) : BaseViewModel(appLogger) {

    private companion object {
        private const val LOG_TAG = "AuthViewModel"
        private const val OTP_LENGTH = 6
        private const val RESEND_COUNTDOWN_SECONDS = 30
    }

    private var countdownJob: Job? = null

    private val _uiState = MutableStateFlow(
        AuthUiState(
            shopName = shopManager.getName(),
            shopSlug = shopManager.getSlug(),
            shopLogoUrl = shopManager.getLogoUrl(),
            shopTimezone = shopManager.getTimezone(),
            requiresShopSetup = shopManager.getSlug().isNullOrBlank()
        )
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<AuthNavigationEvent>()
    val navigationEvents: SharedFlow<AuthNavigationEvent> = _navigationEvents.asSharedFlow()

    fun updatePhoneInput(rawInput: String) {
        val sanitized = sanitizePhoneInput(rawInput)
        _uiState.update {
            it.copy(
                phoneInput = sanitized,
                isPhoneValid = isPhoneValid(sanitized),
                phoneError = null
            )
        }
    }

    fun requestOtpFromLogin() {
        sendOtp(navigateToOtpOnSuccess = true)
    }

    fun resendOtp() {
        if (_uiState.value.resendCountdownSeconds > 0) {
            return
        }
        sendOtp(navigateToOtpOnSuccess = false)
    }

    fun updateOtpInput(rawInput: String) {
        val sanitized = rawInput.filter { char -> char.isDigit() }.take(OTP_LENGTH)
        _uiState.update {
            it.copy(
                otpInput = sanitized,
                otpError = null
            )
        }
    }

    fun verifyOtp() {
        val state = _uiState.value
        val shopSlug = state.shopSlug
        if (shopSlug.isNullOrBlank()) {
            _uiState.update { it.copy(requiresShopSetup = true) }
            return
        }

        val otpCode = state.otpInput
        if (otpCode.length != OTP_LENGTH) {
            _uiState.update { it.copy(otpError = OtpError.InvalidCode) }
            return
        }

        viewModelScope.launch {
            logInfoDebug(LOG_TAG, "Verifying OTP")
            _uiState.update { it.copy(isVerifyLoading = true, otpError = null) }
            when (val result = authRepository.verifyOtp(state.phoneInput, otpCode, shopSlug)) {
                is ApiResult.Success -> {
                    tokenManager.saveToken(result.data.token, result.data.expiresAt)
                    shopManager.save(result.data.shopInfo)
                    _uiState.update { it.copy(isVerifyLoading = false) }
                    _navigationEvents.emit(AuthNavigationEvent.ToMain)
                }

                is ApiResult.ApiError -> {
                    val nextError = when (result.code) {
                        "OTP_INVALID",
                        "OTP_EXPIRED" -> OtpError.InvalidCode

                        "OTP_LOCKED" -> OtpError.Locked
                        else -> OtpError.Generic(result.message)
                    }
                    _uiState.update { it.copy(isVerifyLoading = false, otpError = nextError) }
                }

                is ApiResult.NetworkError -> {
                    _uiState.update {
                        it.copy(
                            isVerifyLoading = false,
                            otpError = OtpError.Network
                        )
                    }
                }
            }
        }
    }

    fun consumeShopSetupRedirect() {
        _uiState.update { it.copy(requiresShopSetup = false) }
    }

    private fun sendOtp(navigateToOtpOnSuccess: Boolean) {
        val state = _uiState.value
        val shopSlug = state.shopSlug
        if (shopSlug.isNullOrBlank()) {
            _uiState.update { it.copy(requiresShopSetup = true) }
            return
        }

        val phone = state.phoneInput
        if (!state.isPhoneValid) {
            _uiState.update { it.copy(phoneError = PhoneError.InvalidFormat) }
            return
        }

        viewModelScope.launch {
            logInfoDebug(LOG_TAG, "Requesting OTP")
            _uiState.update { it.copy(isRequestOtpLoading = true, phoneError = null) }
            when (val result = authRepository.requestOtp(phone, shopSlug)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isRequestOtpLoading = false,
                            maskedPhone = result.data.maskedPhone,
                            otpInput = "",
                            otpError = null,
                            resendCountdownSeconds = RESEND_COUNTDOWN_SECONDS
                        )
                    }
                    startResendCountdown(RESEND_COUNTDOWN_SECONDS)
                    if (navigateToOtpOnSuccess) {
                        _navigationEvents.emit(AuthNavigationEvent.ToOtp)
                    }
                }

                is ApiResult.ApiError -> {
                    val nextError = when (result.code) {
                        "INVALID_PHONE" -> PhoneError.InvalidFormat
                        "BARBER_NOT_FOUND" -> PhoneError.NotRegistered
                        "SHOP_NOT_FOUND",
                        "SHOP_INACTIVE" -> PhoneError.ShopUnavailable

                        else -> PhoneError.Generic(result.message)
                    }
                    _uiState.update { it.copy(isRequestOtpLoading = false, phoneError = nextError) }
                }

                is ApiResult.NetworkError -> {
                    _uiState.update {
                        it.copy(
                            isRequestOtpLoading = false,
                            phoneError = PhoneError.Network
                        )
                    }
                }
            }
        }
    }

    private fun startResendCountdown(initialSeconds: Int) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var remaining = initialSeconds
            while (remaining > 0) {
                delay(1_000)
                remaining -= 1
                _uiState.update { it.copy(resendCountdownSeconds = remaining) }
            }
        }
    }

    private fun sanitizePhoneInput(rawInput: String): String {
        return rawInput.filter { char -> char.isDigit() }.take(10)
    }

    private fun isPhoneValid(phone: String): Boolean {
        return phone.length == 10
    }
}

data class AuthUiState(
    val shopName: String?,
    val shopSlug: String?,
    val shopLogoUrl: String?,
    val shopTimezone: String? = null,
    val phoneInput: String = "",
    val isPhoneValid: Boolean = false,
    val maskedPhone: String = "",
    val otpInput: String = "",
    val resendCountdownSeconds: Int = 0,
    val isRequestOtpLoading: Boolean = false,
    val isVerifyLoading: Boolean = false,
    val phoneError: PhoneError? = null,
    val otpError: OtpError? = null,
    val requiresShopSetup: Boolean = false
)

sealed interface PhoneError {
    data object InvalidFormat : PhoneError
    data object NotRegistered : PhoneError
    data object ShopUnavailable : PhoneError
    data object Network : PhoneError
    data class Generic(val message: String) : PhoneError
}

sealed interface OtpError {
    data object InvalidCode : OtpError
    data object Locked : OtpError
    data object Network : OtpError
    data class Generic(val message: String) : OtpError
}

sealed interface AuthNavigationEvent {
    data object ToOtp : AuthNavigationEvent
    data object ToMain : AuthNavigationEvent
}

