package com.familystalking.app.di

import com.familystalking.app.data.repository.AuthenticationRepositoryImpl
import com.familystalking.app.domain.repository.AuthenticationRepository
import com.familystalking.app.data.repository.ProfileRepositoryImpl
import com.familystalking.app.data.repository.ProfileRepository
import com.familystalking.app.data.repository.FamilyRepositoryImpl
import com.familystalking.app.domain.repository.FamilyRepository
import com.familystalking.app.data.repository.AgendaRepositoryImpl
import com.familystalking.app.domain.repository.AgendaRepository
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
    ): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindFamilyRepository(
        familyRepositoryImpl: FamilyRepositoryImpl
    ): FamilyRepository

    @Binds
    @Singleton
    abstract fun bindAgendaRepository(
        agendaRepositoryImpl: AgendaRepositoryImpl
    ): AgendaRepository
}