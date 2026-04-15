package app.slotnow.slotnowpro.presentation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import app.slotnow.slotnowpro.R
import app.slotnow.slotnowpro.presentation.onboarding.LanguageSelectionScreen
import app.slotnow.slotnowpro.presentation.onboarding.OnboardingViewModel
import app.slotnow.slotnowpro.presentation.onboarding.ShopSetupScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String,
    onboardingStartDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        navigation(
            startDestination = onboardingStartDestination,
            route = Screen.OnboardingGraph.route
        ) {
            composable(Screen.Language.route) {
                val viewModel: OnboardingViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                LanguageSelectionScreen(
                    selectedLanguageCode = uiState.selectedLanguageCode,
                    onLanguageSelected = viewModel::selectLanguage,
                    onNextClick = {
                        viewModel.confirmLanguageSelection()
                        val nextRoute = if (viewModel.hasShopSlug()) {
                            Screen.AuthPlaceholder.route
                        } else {
                            Screen.ShopSetup.route
                        }
                        navController.navigate(nextRoute) {
                            popUpTo(Screen.Language.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.ShopSetup.route) {
                val viewModel: OnboardingViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                ShopSetupScreen(
                    slugInput = uiState.shopSlugInput,
                    validationState = uiState.shopValidationState,
                    onSlugChange = viewModel::updateShopSlugInput,
                    onContinueClick = viewModel::validateShopSlug,
                    onLooksGoodClick = {
                        navController.navigate(Screen.AuthPlaceholder.route) {
                            popUpTo(Screen.OnboardingGraph.route) { inclusive = true }
                        }
                    },
                    lastUsedShopSlug = viewModel.getSavedShopSlug(),
                    onUseLastShopId = viewModel::updateShopSlugInput
                )
            }
        }

        navigation(
            startDestination = Screen.AuthPlaceholder.route,
            route = Screen.AuthGraph.route
        ) {
            composable(Screen.AuthPlaceholder.route) {
                AuthPlaceholderScreen()
            }
        }

        navigation(
            startDestination = Screen.MainPlaceholder.route,
            route = Screen.MainGraph.route
        ) {
            composable(Screen.MainPlaceholder.route) {
                MainPlaceholderScreen()
            }
        }
    }
}

@Composable
private fun AuthPlaceholderScreen() {
    PlaceholderScaffold(
        title = stringResource(R.string.auth_placeholder_title),
        subtitle = stringResource(R.string.auth_placeholder_subtitle)
    )
}

@Composable
private fun MainPlaceholderScreen() {
    PlaceholderScaffold(
        title = stringResource(R.string.main_placeholder_title),
        subtitle = stringResource(R.string.main_placeholder_subtitle)
    )
}

@Composable
private fun PlaceholderScaffold(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


