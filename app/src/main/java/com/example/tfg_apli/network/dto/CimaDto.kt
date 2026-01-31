package com.example.tfg_apli.network.dto

data class MedicamentoCima(
    val nregistro: String,
    val nombre: String,
    val labtitular: String,
    val cpabellon: String?
)

data class ResultadoBusquedaCima(
    val resultados: List<MedicamentoCima>
)
