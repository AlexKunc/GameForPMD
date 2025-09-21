package com.example.gameforpmd.data.remote

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface GoldApiService {
    // Берём XML: https://www.cbr.ru/scripts/xml_metall.asp
    @GET("scripts/xml_metall.asp")
    fun getMetalRates(
        @Query("date_req1") dateFrom: String,
        @Query("date_req2") dateTo: String
    ): Call<MetalRates>
}
