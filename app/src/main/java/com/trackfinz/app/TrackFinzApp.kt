package com.trackfinz.app

import android.app.Application
import android.util.Log
import com.trackfinz.app.di.SeedDataInitializer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TrackFinzApp : Application() {

    @Inject lateinit var seedDataInitializer: SeedDataInitializer

    override fun onCreate() {
        super.onCreate()
        try {
            seedDataInitializer.seedIfEmpty()
        } catch (e: Exception) {
            Log.e("TrackFinzApp", "seedIfEmpty failed", e)
        }
    }
}
