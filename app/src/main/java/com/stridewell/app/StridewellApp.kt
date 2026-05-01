package com.stridewell.app

import android.app.Application
import com.mapbox.common.MapboxOptions
import com.stridewell.BuildConfig
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class StridewellApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Register the Mapbox public access token globally. The token is injected
        // at build time from `local.properties` / `~/.gradle/gradle.properties` /
        // env (`MAPBOX_PUBLIC_TOKEN`). When empty, the SDK fails to load tiles at
        // runtime but the app still builds — useful for non-mapping CI builds.
        if (BuildConfig.MAPBOX_PUBLIC_TOKEN.isNotBlank()) {
            MapboxOptions.accessToken = BuildConfig.MAPBOX_PUBLIC_TOKEN
        }
    }
}
