package com.example.tfg_apli.network.service

import com.example.tfg_apli.network.dto.ResultadoBusquedaCima
import retrofit2.http.GET
import retrofit2.http.Query

interface CimaApiService {


    @GET("medicamentos")
    suspend fun buscarMedicamentos(
        @Query("nombre") nombre: String = "",
        @Query("cn") cn: String = ""
    ): ResultadoBusquedaCima
}