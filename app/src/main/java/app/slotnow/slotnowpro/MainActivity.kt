package app.slotnow.slotnowpro

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.slotnow.slotnowpro.presentation.onboarding.LanguageSelectionScreen
import app.slotnow.slotnowpro.presentation.onboarding.OnboardingLanguages
import app.slotnow.slotnowpro.presentation.onboarding.OnboardingUiEvent
import app.slotnow.slotnowpro.presentation.onboarding.OnboardingViewModel
import app.slotnow.slotnowpro.ui.theme.SlotNowProTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SlotNowProTheme {
                OnboardingLanguageHost(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun OnboardingLanguageHost(modifier: Modifier = Modifier) {
    val viewModel: OnboardingViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            // TODO(1.4b): Move snackbar copy to string resources once ShopSetupScreen is in place.
            val message = when (event) {
                is OnboardingUiEvent.ShowLanguageUpdatedSnackbar ->
                    OnboardingLanguages.snackbarMessage(event.code)
            }
            snackbarHostState.showSnackbar(message = message)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        if (uiState.showLanguageSelection) {
            LanguageSelectionScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                selectedLanguageCode = uiState.selectedLanguageCode,
                showBackButton = false,
                onBack = {},
                onLanguageSelected = viewModel::selectLanguage,
                onNextClick = viewModel::confirmLanguageSelection
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // TODO(1.4d): Replace placeholder with AppNavGraph startDestination branching.
                Text(
                    text = stringResource(R.string.onboarding_language_skip_placeholder),
                    modifier = Modifier
                )
            }
        }
    }
}