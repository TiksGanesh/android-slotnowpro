package app.slotnow.slotnowpro.presentation.onboarding

import androidx.lifecycle.ViewModel
import app.slotnow.slotnowpro.data.local.prefs.LanguageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val languageManager: LanguageManager,
    private val localeUpdater: LocaleUpdater
) : ViewModel() {

    private val storedLanguageCode = languageManager.get()

    private val _uiState = MutableStateFlow(
        OnboardingUiState(
            selectedLanguageCode = languageManager.getOrDefault(),
            showLanguageSelection = storedLanguageCode == null
        )
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<OnboardingUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<OnboardingUiEvent> = _events.asSharedFlow()

    fun selectLanguage(code: String) {
        _uiState.update { it.copy(selectedLanguageCode = code) }
    }

    fun confirmLanguageSelection() {
        val code = _uiState.value.selectedLanguageCode
        languageManager.save(code)
        localeUpdater.apply(code)

        _uiState.update {
            it.copy(
                showLanguageSelection = false
            )
        }
        _events.tryEmit(OnboardingUiEvent.ShowLanguageUpdatedSnackbar(code))
    }
}

data class OnboardingUiState(
    val selectedLanguageCode: String,
    val showLanguageSelection: Boolean
)

sealed interface OnboardingUiEvent {
    data class ShowLanguageUpdatedSnackbar(val code: String) : OnboardingUiEvent
}


