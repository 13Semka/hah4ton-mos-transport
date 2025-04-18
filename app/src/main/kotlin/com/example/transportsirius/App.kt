package com.example.transportsirius

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Здесь можно инициализировать глобальные компоненты
    }
}