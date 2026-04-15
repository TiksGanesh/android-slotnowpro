package app.slotnow.slotnowpro.presentation.onboarding

import androidx.lifecycle.viewModelScope
import app.slotnow.slotnowpro.data.local.prefs.LanguageManager
import app.slotnow.slotnowpro.data.local.prefs.ShopManager
import app.slotnow.slotnowpro.domain.repository.OnboardingRepository
import app.slotnow.slotnowpro.presentation.BaseViewModel
import app.slotnow.slotnowpro.util.ApiResult
import app.slotnow.slotnowpro.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
    private val shopManager: ShopManager,
    private val languageManager: LanguageManager,
    private val localeUpdater: LocaleUpdater,
    appLogger: AppLogger
) : BaseViewModel(appLogger) {

    private companion object {
        private const val LOG_TAG = "OnboardingViewModel"
    }

    private val _uiState = MutableStateFlow(
        OnboardingUiState(
            selectedLanguageCode = OnboardingLanguages.resolveInitialSelection(
                persistedCode = languageManager.get()
            )
        )
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()


    fun selectLanguage(code: String) {
        logInfoDebug(LOG_TAG, "Language selected: $code")
        _uiState.update { it.copy(selectedLanguageCode = code) }
    }

    fun hasShopSlug(): Boolean = shopManager.getSlug() != null

    fun getSavedShopSlug(): String? = shopManager.getSlug()

    fun confirmLanguageSelection() {
        val code = _uiState.value.selectedLanguageCode
        logInfoDebug(LOG_TAG, "Confirming language selection: $code")
        languageManager.save(code)
        localeUpdater.apply(code)
    }

    fun updateShopSlugInput(rawInput: String) {
        val sanitized = sanitizeSlug(rawInput)
        logInfoDebug(LOG_TAG, "Shop slug input updated")
        _uiState.update {
            it.copy(
                shopSlugInput = sanitized,
                shopValidationState = ShopValidationState.Idle
            )
        }
    }

    fun validateShopSlug() {
        val slug = _uiState.value.shopSlugInput
        if (slug.isBlank()) {
            logWarnDebug(LOG_TAG, "Shop slug validation blocked: blank input")
            _uiState.update { it.copy(shopValidationState = ShopValidationState.InvalidInput) }
            return
        }

        viewModelScope.launch {
            logInfoDebug(LOG_TAG, "Validating shop slug: $slug")
            _uiState.update { it.copy(shopValidationState = ShopValidationState.Loading) }
            val nextState = when (val result = onboardingRepository.validateShopSlug(slug)) {
                is ApiResult.Success -> {
                    logInfoDebug(LOG_TAG, "Shop slug validation success")
                    shopManager.save(result.data)
                    ShopValidationState.Valid(result.data.shopName)
                }

                is ApiResult.ApiError -> when (result.code) {
                    "SHOP_NOT_FOUND" -> {
                        logWarnDebug(LOG_TAG, "Shop slug not found")
                        ShopValidationState.NotFound
                    }

                    "SHOP_INACTIVE" -> {
                        logWarnDebug(LOG_TAG, "Shop slug inactive")
                        ShopValidationState.Inactive
                    }

                    else -> {
                        logErrorDebug(LOG_TAG, "Shop slug validation api error: ${result.code}")
                        ShopValidationState.Error(result.message)
                    }
                }

                is ApiResult.NetworkError -> {
                    logErrorDebug(
                        tag = LOG_TAG,
                        message = "Shop slug validation network error",
                        throwable = result.cause
                    )
                    ShopValidationState.NetworkError
                }
            }
            _uiState.update { it.copy(shopValidationState = nextState) }
        }
    }

    private fun sanitizeSlug(rawInput: String): String {
        val lowered = rawInput.trim().lowercase()
        val normalized = buildString {
            lowered.forEach { char ->
                when {
                    char.isLetterOrDigit() -> append(char)
                    char == '-' || char == ' ' || char == '_' -> append('-')
                }
            }
        }

        return normalized
            .replace(Regex("-+"), "-")
            .trim('-')
    }
}

data class OnboardingUiState(
    val selectedLanguageCode: String,
    val shopSlugInput: String = "",
    val shopValidationState: ShopValidationState = ShopValidationState.Idle
)


sealed interface ShopValidationState {
    data object Idle : ShopValidationState
    data object Loading : ShopValidationState
    data class Valid(val shopName: String) : ShopValidationState
    data object InvalidInput : ShopValidationState
    data object NotFound : ShopValidationState
    data object Inactive : ShopValidationState
    data object NetworkError : ShopValidationState
    data class Error(val message: String) : ShopValidationState
}


