package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.i18n.GameLocaleState
import com.example.image.HeroImageLoader

class PlumpHeroesApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        GameLocaleState.init(this)
    }

    override fun newImageLoader(): ImageLoader {
        return HeroImageLoader.getOrCreate(this)
    }
}
