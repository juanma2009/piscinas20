package com.bolsadeideas.springboot.app.android.data.api

import com.bolsadeideas.springboot.app.android.data.model.ClienteResponse
import com.bolsadeideas.springboot.app.android.data.model.PedidoResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface JoyeriaApi {
    @GET("api/pedido/listarPedidos")
    suspend fun listarPedidos(
        @Query("page") page: Int = 0
    ): PedidoResponse

    @GET("api/cliente/listar")
    suspend fun listarClientes(
        @Query("page") page: Int = 0
    ): ClienteResponse

    @GET("api/cliente/ver/{id}")
    suspend fun getClienteDetalle(
        @Path("id") id: Long
    ): Map<String, Any>
}
