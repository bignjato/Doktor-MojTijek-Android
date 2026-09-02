package com.mojtijek.app

import android.app.Application
import com.mojtijek.shared.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MojTijekApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@MojTijekApplication)
            modules(sharedModule)
        }
    }
}
