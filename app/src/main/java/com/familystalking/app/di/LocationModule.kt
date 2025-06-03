package com.familystalking.app.di

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient // Ensure this import is present
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocationModule { // Or 'class LocationModule'

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient { // <<< CORRECT RETURN TYPE
        return LocationServices.getFusedLocationProviderClient(context)
    }
}