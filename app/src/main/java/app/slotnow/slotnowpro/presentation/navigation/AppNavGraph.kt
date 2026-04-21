package app.slotnow.slotnowpro.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import app.slotnow.slotnowpro.presentation.auth.AuthNavigationEvent
import app.slotnow.slotnowpro.presentation.auth.AuthViewModel
import app.slotnow.slotnowpro.presentation.auth.LoginScreen
import app.slotnow.slotnowpro.presentation.auth.OtpScreen
import app.slotnow.slotnowpro.presentation.dashboard.DashboardRoute
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
                            Screen.Login.route
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
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.OnboardingGraph.route) { inclusive = true }
                        }
                    },
                    lastUsedShopSlug = viewModel.getSavedShopSlug(),
                    onUseLastShopId = viewModel::updateShopSlugInput
                )
            }
        }

        navigation(
            startDestination = Screen.Login.route,
            route = Screen.AuthGraph.route
        ) {
            composable(Screen.Login.route) {
                val parentEntry = remember(navController) {
                    navController.getBackStackEntry(Screen.AuthGraph.route)
                }
                val viewModel: AuthViewModel = hiltViewModel(parentEntry)
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(viewModel) {
                    viewModel.navigationEvents.collect { event ->
                        when (event) {
                            AuthNavigationEvent.ToOtp -> {
                                navController.navigate(Screen.Otp.route)
                            }

                            AuthNavigationEvent.ToMain -> {
                                navController.navigate(Screen.MainGraph.route) {
                                    popUpTo(Screen.AuthGraph.route) { inclusive = true }
                                }
                            }
                        }
                    }
                }

                LoginScreen(
                    uiState = uiState,
                    onPhoneChange = viewModel::updatePhoneInput,
                    onSendOtpClick = viewModel::requestOtpFromLogin,
                    onMissingShopContext = {
                        navController.navigate(Screen.ShopSetup.route) {
                            popUpTo(Screen.AuthGraph.route) { inclusive = true }
                        }
                    },
                    onRedirectHandled = viewModel::consumeShopSetupRedirect
                )
            }

            composable(Screen.Otp.route) {
                val parentEntry = remember(navController) {
                    navController.getBackStackEntry(Screen.AuthGraph.route)
                }
                val viewModel: AuthViewModel = hiltViewModel(parentEntry)
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(viewModel) {
                    viewModel.navigationEvents.collect { event ->
                        when (event) {
                            AuthNavigationEvent.ToOtp -> Unit
                            AuthNavigationEvent.ToMain -> {
                                navController.navigate(Screen.MainGraph.route) {
                                    popUpTo(Screen.AuthGraph.route) { inclusive = true }
                                }
                            }
                        }
                    }
                }

                OtpScreen(
                    uiState = uiState,
                    onOtpChange = viewModel::updateOtpInput,
                    onVerifyClick = viewModel::verifyOtp,
                    onResendClick = viewModel::resendOtp
                )
            }
        }

        navigation(
            startDestination = Screen.Dashboard.route,
            route = Screen.MainGraph.route
        ) {
            composable(Screen.Dashboard.route) {
                DashboardRoute()
            }
        }
    }
}

