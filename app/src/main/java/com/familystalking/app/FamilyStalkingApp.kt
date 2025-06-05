package com.familystalking.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Main Application class for the Family Stalking Android application.
 * 
 * This class serves as the entry point for the application and is responsible for
 * initializing application-wide dependencies and configurations. It is annotated
 * with [HiltAndroidApp] to enable Dagger-Hilt dependency injection throughout
 * the application.
 * 
 * The Family Stalking app allows family members to share their real-time locations
 * and coordinate activities through location tracking and calendar features.
 */
@HiltAndroidApp
class FamilyStalkingApp : Application() 