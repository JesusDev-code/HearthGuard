package com.example.tfg_apli.network.client

import com.example.tfg_apli.network.service.CimaApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object CimaRetrofitClient {
    private const val BASE_URL = "https://cima.aemps.es/cima/rest/"

    val apiService: CimaApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CimaApiService::class.java)
    }
}