package com.mojtijek.shared

import org.koin.dsl.module

val sharedModule = module {
    single { ApiClient() }
    single { UserRepository(get()) }
}
