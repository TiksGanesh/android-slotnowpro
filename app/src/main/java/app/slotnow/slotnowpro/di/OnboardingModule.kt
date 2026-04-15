package app.slotnow.slotnowpro.di

import app.slotnow.slotnowpro.presentation.onboarding.AppCompatLocaleUpdater
import app.slotnow.slotnowpro.presentation.onboarding.LocaleUpdater
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OnboardingModule {

    @Binds
    @Singleton
    abstract fun bindLocaleUpdater(
        impl: AppCompatLocaleUpdater
    ): LocaleUpdater
}

