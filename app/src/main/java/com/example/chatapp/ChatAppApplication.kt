package com.example.chatapp

import android.app.Application
import com.example.chatapp.di.authModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class ChatAppApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ChatAppApplication)
            androidLogger(level = Level.ERROR)

            modules(authModule)
        }
    }
}