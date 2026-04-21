package app.slotnow.slotnowpro.presentation.navigation

sealed class Screen(val route: String) {
    data object OnboardingGraph : Screen("onboarding")
    data object AuthGraph : Screen("auth")
    data object MainGraph : Screen("main")

    data object Language : Screen("onboarding_language")
    data object ShopSetup : Screen("onboarding_shop")
    data object Login : Screen("auth_login")
    data object Otp : Screen("auth_otp")
    data object Dashboard : Screen("main_dashboard")
}

