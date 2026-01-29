package com.bolsadeideas.springboot.app.android.data.api

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // IMPORTANTE: Reemplaza esta URL con la de tu servidor desplegado en Heroku.
    // Ej: "https://nombre-de-tu-app.herokuapp.com/"
    private const val BASE_URL = "https://taller-92fe56841c15.herokuapp.com/"

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    val api: JoyeriaApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(client)
            .build()
            .create(JoyeriaApi::class.java)
    }
}
