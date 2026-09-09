package com.mojtijek.doktor

import android.app.Application
import com.mojtijek.doktor.data.MojTijekDatabase
import com.mojtijek.doktor.data.MojTijekRepository
import com.mojtijek.shared.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.dsl.module

class DoktorApp : Application() {
    lateinit var repository: MojTijekRepository

    override fun onCreate() {
        super.onCreate()
        val db = MojTijekDatabase.getInstance(this)
        repository = MojTijekRepository(db)

        val localModule = module {
            single { repository }
        }

        startKoin {
            androidLogger()
            androidContext(this@DoktorApp)
            modules(sharedModule, localModule)
        }
    }
}
