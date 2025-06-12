package com.familystalking.app.di

import com.familystalking.app.BuildConfig
// Remove unused imports for repositories from here if they were only for the @Provides methods
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
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
        install(Realtime)
        // If you used KotlinXSerializer or a specific Ktor engine, add them back
        // defaultSerializer = KotlinXSerializer()
        // httpClientEngine = OkHttp.create() // Or your preferred engine
    }

    // NO @Provides for AuthenticationRepository here
    // NO @Provides for FamilyRepository here
}