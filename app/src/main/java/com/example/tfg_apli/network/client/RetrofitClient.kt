package com.example.tfg_apli.network.client

import android.util.Log
import com.example.tfg_apli.network.service.ApiService
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.tasks.Tasks
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "http://health-guard-backedn-ttc9gz-66d657-51-77-145-58.traefik.me/"

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            val user = FirebaseAuth.getInstance().currentUser

            if (user != null) {
                try {
                    // Timeout de 5 segundos para obtener el token y no bloquear
                    val task = user.getIdToken(false)
                    val tokenResult = Tasks.await(task, 5, TimeUnit.SECONDS)
                    val token = tokenResult.token

                    if (!token.isNullOrEmpty()) {
                        requestBuilder.addHeader("Authorization", "Bearer $token")
                    }
                } catch (e: Exception) {
                    Log.e("RetrofitClient", "Error token: ${e.message}")
                }
            }
            chain.proceed(requestBuilder.build())
        }
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}