package com.mojtijek.doktor

import android.app.Application
import com.mojtijek.shared.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class DoktorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@DoktorApp)
            modules(sharedModule)
        }
    }
}
