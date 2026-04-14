package app.slotnow.slotnowpro

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Hilt application entry point.
 * Enables dependency injection across the app.
 */
@HiltAndroidApp
class MainApplication : Application()

