package com.familystalking.app.di

import android.content.Context
import com.familystalking.app.data.repository.SupabaseLocationRepository
import com.familystalking.app.domain.repository.LocationRepository
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocationModule {

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(
        @ApplicationContext context: Context
    ) = LocationServices.getFusedLocationProviderClient(context)

    @Provides
    @Singleton
    fun provideLocationRepository(
        supabaseLocationRepository: SupabaseLocationRepository
    ): LocationRepository = supabaseLocationRepository
} 