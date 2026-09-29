package com.example.musikku

import android.content.Context
import com.example.musikku.data.MusicRepository
import com.example.musikku.data.local.FavoritesStore
import com.example.musikku.data.remote.DeezerApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Service locator sederhana (tanpa Hilt supaya mudah dipahami). */
object AppModule {

    lateinit var favorites: FavoritesStore
        private set

    fun init(context: Context) {
        favorites = FavoritesStore(context.applicationContext)
    }

    private val okHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private val api: DeezerApi by lazy {
        Retrofit.Builder()
            .baseUrl(DeezerApi.BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DeezerApi::class.java)
    }

    val repository: MusicRepository by lazy { MusicRepository(api) }
}
