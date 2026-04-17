package app.slotnow.slotnowpro.di

import app.slotnow.slotnowpro.data.repository.AuthRepositoryImpl
import app.slotnow.slotnowpro.data.repository.OnboardingRepositoryImpl
import app.slotnow.slotnowpro.domain.repository.AuthRepository
import app.slotnow.slotnowpro.domain.repository.OnboardingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Structural placeholder for repository DI bindings.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindOnboardingRepository(
        impl: OnboardingRepositoryImpl
    ): OnboardingRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository
}

