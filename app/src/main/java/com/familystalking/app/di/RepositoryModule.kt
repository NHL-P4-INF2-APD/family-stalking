package com.familystalking.app.di

import com.familystalking.app.data.repository.AuthenticationRepositoryImpl
import com.familystalking.app.domain.repository.AuthenticationRepository
import com.familystalking.app.data.repository.ProfileRepositoryImpl
import com.familystalking.app.data.repository.ProfileRepository // Assuming interface is here, or import from domain
import com.familystalking.app.data.repository.FamilyRepositoryImpl // Your FamilyRepository Impl
import com.familystalking.app.domain.repository.FamilyRepository    // Your FamilyRepository Interface (from domain)
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthenticationRepository(
        authenticationRepositoryImpl: AuthenticationRepositoryImpl
    ): AuthenticationRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        profileRepositoryImpl: ProfileRepositoryImpl
    ): ProfileRepository // Ensure ProfileRepository interface is correctly located/imported

    @Binds
    @Singleton
    abstract fun bindFamilyRepository( // Add this binding
        familyRepositoryImpl: FamilyRepositoryImpl
    ): FamilyRepository
}