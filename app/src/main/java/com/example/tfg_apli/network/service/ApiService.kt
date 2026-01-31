package com.example.tfg_apli.network.service

import com.example.tfg_apli.network.dto.*
import retrofit2.http.*

interface ApiService {

    // --- AUTH & PERFIL ---

    @POST("api/usuarios/registro")
    suspend fun registro(@Body registroDTO: UsuarioRegistroDTO): UsuarioResponseDTO

    @GET("api/usuarios/perfil")
    suspend fun miPerfil(): UsuarioResponseDTO

    @GET("api/usuarios/{id}")
    suspend fun obtenerUsuarioPorId(@Path("id") usuarioId: Long): UsuarioResponseDTO

    @PUT("api/usuarios/perfil")
    suspend fun actualizarPerfil(@Body datos: UsuarioUpdateDTO): UsuarioResponseDTO

    // --- CUIDADORES & VINCULACIÓN ---

    @GET("api/cuidadores/{id}/pacientes")
    suspend fun listarPacientes(@Path("id") cuidadorId: Long): List<PacienteResumenDTO>

    @POST("api/cuidadores/{id}/vincular")
    suspend fun vincularPaciente(@Path("id") cuidadorId: Long, @Body datos: VincularPacienteDTO)



    // Para que el CUIDADOR elimine un paciente de su lista
    @DELETE("api/usuarios/cuidadores/desvincular")
    suspend fun desvincularPaciente(@Query("seniorId") seniorId: Long)

    //Para que el SENIOR (paciente) se desvincule de su cuidador
    @DELETE("api/usuarios/mi-cuidador/desvincular")
    suspend fun desvincularMiCuidador()

    // --- IA ---

    @GET("api/ia/consejo")
    suspend fun getConsejoIA(): ConsejoIAResponseDTO

    // --- SALUD (PATOLOGÍAS) ---

    @POST("api/salud/usuario/{usuarioId}/registrar")
    suspend fun registrarNuevaPatologia(
        @Path("usuarioId") usuarioId: Long,
        @Body datos: NuevaPatologiaDTO
    ): UsuarioPatologia

    @POST("api/salud/asignar/{usuarioId}")
    suspend fun asignarPatologia(
        @Path("usuarioId") usuarioId: Long,
        @Body asignarRequest: AsignarRequest
    ): UsuarioPatologia

    @PUT("api/salud/curar/{registroId}")
    suspend fun curar(@Path("registroId") registroId: Long)

    @GET("api/salud/perfil/{usuarioId}")
    suspend fun getPerfilSalud(@Path("usuarioId") usuarioId: Long): List<UsuarioPatologia>

    @GET("api/salud/historial/{usuarioId}")
    suspend fun getHistorialSalud(@Path("usuarioId") usuarioId: Long): List<UsuarioPatologia>

    @GET("api/salud/buscar")
    suspend fun buscarPatologia(@Query("query") query: String): List<Patologia>

    // --- MEDICAMENTOS ---

    @POST("api/medicamentos")
    suspend fun crearMedicamento(@Body medicamentoDTO: MedicamentoRequestDTO): MedicamentoResponseDTO

    @GET("api/medicamentos/senior/{id}")
    suspend fun listarMedicamentosPorSenior(@Path("id") seniorId: Long): List<MedicamentoResponseDTO>

    @GET("api/medicamentos/buscar-por-cn")
    suspend fun buscarMedicamentoPorCN(@Query("cn") codigoNacional: String): MedicamentoResponseDTO

    @DELETE("api/medicamentos/{id}")
    suspend fun borrarMedicamento(@Path("id") id: Long)

    // --- TOMAS (CONTROL DE MEDICACIÓN) ---

    @PATCH("api/tomas/{id}/registrar")
    suspend fun registrarToma(
        @Path("id") tomaId: Long,
        @Query("estado") estado: String
    ): TomaResponseDTO

    @GET("api/tomas/verificar")
    suspend fun verificarToma(@Query("codigo_barras") codigoBarras: String): VerificacionTomaResponseDTO

    @GET("api/tomas/senior/{id}/informe")
    suspend fun obtenerInforme(@Path("id") seniorId: Long): InformeCumplimientoDTO

    @GET("api/tomas/historial")
    suspend fun getTomasPorFecha(
        @Query("seniorId") seniorId: Long,
        @Query("fecha") fecha: String // Formato "YYYY-MM-DD"
    ): List<TomaResponseDTO>

    // --- ANALÍTICAS & EMERGENCIAS ---

    @POST("api/analiticas")
    suspend fun registrarAnalitica(@Body analiticaDTO: AnaliticaRequestDTO): AnaliticaResponseDTO

    @GET("api/analiticas/senior/{seniorId}/grafica")
    suspend fun obtenerDatosGrafica(
        @Path("seniorId") seniorId: Long,
        @Query("tipo") tipo: String
    ): List<AnaliticaResponseDTO>

    @POST("api/emergencias/reportar")
    suspend fun reportarEmergencia(
        @Query("tipo") tipo: String,
        @Query("gps") gps: String?
    ): LogEmergencia

    @GET("api/emergencias/usuario/{usuarioId}")
    suspend fun listarAlertas(@Path("usuarioId") usuarioId: Long): List<LogEmergencia>

    // --- CITAS MÉDICAS ---

    @GET("api/citas/senior/{id}")
    suspend fun listarCitas(@Path("id") seniorId: Long): List<CitaMedicaDTO>

    @POST("api/citas")
    suspend fun crearCita(@Body cita: CitaMedicaDTO): CitaMedicaDTO

    @DELETE("api/citas/{id}")
    suspend fun borrarCita(@Path("id") citaId: Long)

    @GET("api/analiticas/senior/{seniorId}")
    suspend fun obtenerAnaliticasSenior(@Path("seniorId") seniorId: Long): List<AnaliticaResponseDTO>
}
