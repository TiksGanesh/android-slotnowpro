package app.slotnow.slotnowpro

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import app.slotnow.slotnowpro.data.local.prefs.LanguageManager
import app.slotnow.slotnowpro.data.local.prefs.ShopManager
import app.slotnow.slotnowpro.data.local.prefs.TokenManager
import app.slotnow.slotnowpro.presentation.navigation.AppNavGraph
import app.slotnow.slotnowpro.presentation.navigation.Screen
import app.slotnow.slotnowpro.ui.theme.SlotNowProTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var languageManager: LanguageManager

    @Inject
    lateinit var shopManager: ShopManager

    @Inject
    lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SlotNowProTheme {
                val navController = rememberNavController()
                val launchConfig = remember {
                    when {
                        languageManager.get() == null -> LaunchConfig(
                            rootStartDestination = Screen.OnboardingGraph.route,
                            onboardingStartDestination = Screen.Language.route
                        )

                        shopManager.getSlug() == null -> LaunchConfig(
                            rootStartDestination = Screen.OnboardingGraph.route,
                            onboardingStartDestination = Screen.ShopSetup.route
                        )

                        tokenManager.getToken() == null -> LaunchConfig(
                            rootStartDestination = Screen.AuthGraph.route,
                            onboardingStartDestination = Screen.Language.route
                        )

                        else -> LaunchConfig(
                            rootStartDestination = Screen.MainGraph.route,
                            onboardingStartDestination = Screen.Language.route
                        )
                    }
                }

                AppNavGraph(
                    navController = navController,
                    startDestination = launchConfig.rootStartDestination,
                    onboardingStartDestination = launchConfig.onboardingStartDestination
                )
            }
        }
    }
}

private data class LaunchConfig(
    val rootStartDestination: String,
    val onboardingStartDestination: String
)
