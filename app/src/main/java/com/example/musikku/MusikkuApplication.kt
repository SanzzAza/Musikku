package com.example.musikku

import android.app.Application

class MusikkuApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppModule.init(this)
    }
}
