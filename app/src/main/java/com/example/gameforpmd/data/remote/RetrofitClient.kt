package com.example.gameforpmd.data.remote

import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://www.cbr.ru/"

    val api: GoldApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(SimpleXmlConverterFactory.create())
            .build()
            .create(GoldApiService::class.java)
    }
}
