package com.familystalking.app.di

import com.familystalking.app.BuildConfig
import com.familystalking.app.data.repository.AuthenticationRepositoryImpl
import com.familystalking.app.domain.repository.AuthenticationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Auth)
        install(Postgrest)
    }

    @Provides
    @Singleton
    fun provideAuthenticationRepository(
        supabaseClient: SupabaseClient
    ): AuthenticationRepository = AuthenticationRepositoryImpl(supabaseClient)

    @Provides
    @Singleton
    fun provideFamilyRepository(
        supabaseClient: SupabaseClient
    ): com.familystalking.app.domain.repository.FamilyRepository =
        com.familystalking.app.data.repository.FamilyRepositoryImpl(supabaseClient)
} 