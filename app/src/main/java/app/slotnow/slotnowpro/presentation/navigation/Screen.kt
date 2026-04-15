package app.slotnow.slotnowpro.presentation.navigation

sealed class Screen(val route: String) {
    data object OnboardingGraph : Screen("onboarding")
    data object AuthGraph : Screen("auth")
    data object MainGraph : Screen("main")

    data object Language : Screen("onboarding_language")
    data object ShopSetup : Screen("onboarding_shop")
    data object AuthPlaceholder : Screen("auth_placeholder")
    data object MainPlaceholder : Screen("main_placeholder")
}

