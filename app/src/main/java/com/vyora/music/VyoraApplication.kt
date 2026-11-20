package com.vyora.music

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory

class VyoraApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        AppModule.init(this)
    }

    /** ImageLoader global: crossfade halus saat cover album/artis muncul. */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this).crossfade(true).build()
}
