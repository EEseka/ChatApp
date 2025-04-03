package com.example.chatapp

import android.app.Application
import android.util.Log
import com.example.chatapp.core.data.utils.CloudinaryManager
import com.example.chatapp.core.domain.util.onError
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
        // Initialize Cloudinary
        val cloudinaryManager = CloudinaryManager(this)

        val cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME
        val apiKey = BuildConfig.CLOUDINARY_API_KEY
        val apiSecret = BuildConfig.CLOUDINARY_API_SECRET

        cloudinaryManager.init(cloudName, apiKey, apiSecret)
            .onError { error ->
                Log.e("ChatAppApplication", "Failed to initialize Cloudinary: $error")
            }
    }
}