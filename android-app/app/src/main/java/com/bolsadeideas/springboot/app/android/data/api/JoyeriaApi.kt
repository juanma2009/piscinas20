package com.bolsadeideas.springboot.app.android.data.api

import com.bolsadeideas.springboot.app.android.data.model.ClienteDto
import com.bolsadeideas.springboot.app.android.data.model.ClienteResponse
import com.bolsadeideas.springboot.app.android.data.model.PageResponse
import com.bolsadeideas.springboot.app.android.data.model.PedidoDto
import com.bolsadeideas.springboot.app.android.data.model.PedidoFotoDto
import com.bolsadeideas.springboot.app.android.data.model.PedidoResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface JoyeriaApi {
    @GET("api/pedido/listarPedidos")
    suspend fun listarPedidos(
        @Query("page") page: Int = 0
    ): PedidoResponse

    @POST("api/pedido/guardar")
    suspend fun guardarPedido(@Body pedido: PedidoDto): Response<Unit>

    @GET("api/pedido/ver/{id}")
    suspend fun getPedidoDetalle(@Path("id") id: Long): Map<String, Any>

    @DELETE("api/pedido/eliminar/{id}")
    suspend fun eliminarPedido(@Path("id") id: Long): Response<Unit>

    @GET("api/cliente/listar")
    suspend fun listarClientes(
        @Query("page") page: Int = 0
    ): ClienteResponse

    @GET("api/cliente/ver/{id}")
    suspend fun getClienteDetalle(
        @Path("id") id: Long
    ): Map<String, Any>

    @POST("api/cliente/guardar")
    suspend fun guardarCliente(@Body cliente: ClienteDto): Response<Unit>

    @DELETE("api/cliente/eliminar/{id}")
    suspend fun eliminarCliente(@Path("id") id: Long): Response<Unit>

    // --- Albaranes ---
    @GET("api/albaran/VerAlbaranFinalizado/{idCliente}")
    suspend fun verAlbaranFinalizado(@Path("idCliente") idCliente: Long): Response<List<Map<String, Any>>>

    // --- Facturas ---
    @GET("api/factura/listarPorCliente")
    suspend fun listarFacturasPorCliente(
        @Query("clienteId") clienteId: Long,
        @Query("page") page: Int = 0
    ): Map<String, Any>

    // --- Búsqueda y Estadísticas de Pedidos ---
    @GET("api/pedido/listarPedidosClientes")
    suspend fun listarPedidosClientes(
        @Query("idCliente") idCliente: Long,
        @Query("page") page: Int = 0
    ): Map<String, Any>

    @POST("api/pedido/buscar")
    suspend fun buscarPedidos(
        @Query("page") page: Int = 0,
        @Query("cliente") clienteId: Int? = null,
        @Query("estado") estado: String? = null,
        @Query("tipoPedido") tipoPedido: String? = null,
        @Query("metal") metal: String? = null,
        @Query("pieza") pieza: String? = null,
        @Query("tipo") tipo: String? = null,
        @Query("ref") ref: String? = null,
        @Query("activo") activo: String? = null,
        @Query("fechaDesde") fechaDesde: String? = null,
        @Query("fechaHasta") fechaHasta: String? = null
    ): Map<String, Any>

    @GET("api/pedido/tipoPorPieza")
    suspend fun getTiposPorPieza(): Map<String, List<String>>

    @GET("api/pedido/estadisticas/facturacionanual")
    suspend fun getFacturacionAnual(@Query("clienteId") clienteId: Long): Map<String, Double>

    @GET("api/pedido/estadisticas/anios")
    suspend fun getAniosFacturacion(@Query("clienteId") clienteId: Long): List<Int>

    @GET("api/pedido/estadisticas/facturacion-mensual")
    suspend fun getFacturacionMensual(
        @Query("clienteId") clienteId: Long,
        @Query("anio") anio: Int
    ): Map<String, Double>

    @GET("api/pedido/estadisticas/pedidos-por-mes")
    suspend fun getPedidosPorMeses(
        @Query("clienteId") clienteId: Long,
        @Query("anio") anio: Int
    ): Map<String, Int>

    @GET("api/pedido/estadisticas/anios-disponibles")
    suspend fun getAniosDisponibles(@Query("clienteId") clienteId: Long): List<Int>

    // --- Gestión de Fotos de Pedidos ---
    @GET("api/pedidos/fotos")
    suspend fun buscarFotos(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("id") id: Int? = null,
        @Query("estado") estado: String? = null,
        @Query("activo") activo: Boolean? = null,
        @Query("fechaDesde") fechaDesde: String? = null,
        @Query("fechaHasta") fechaHasta: String? = null,
        @Query("tipoPedido") tipoPedido: String? = null,
        @Query("grupo") grupo: String? = null,
        @Query("pieza") pieza: String? = null,
        @Query("tipo") tipo: String? = null,
        @Query("ref") ref: String? = null
    ): PageResponse<PedidoFotoDto>
}
