package app.slotnow.slotnowpro.di

import app.slotnow.slotnowpro.data.repository.AuthRepositoryImpl
import app.slotnow.slotnowpro.data.repository.BookingsRepositoryImpl
import app.slotnow.slotnowpro.data.repository.OnboardingRepositoryImpl
import app.slotnow.slotnowpro.data.repository.WorkflowRepositoryImpl
import app.slotnow.slotnowpro.domain.repository.AuthRepository
import app.slotnow.slotnowpro.domain.repository.BookingsRepository
import app.slotnow.slotnowpro.domain.repository.OnboardingRepository
import app.slotnow.slotnowpro.domain.repository.WorkflowRepository
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

    @Binds
    @Singleton
    abstract fun bindBookingsRepository(
        impl: BookingsRepositoryImpl
    ): BookingsRepository

    @Binds
    @Singleton
    abstract fun bindWorkflowRepository(
        impl: WorkflowRepositoryImpl
    ): WorkflowRepository
}

