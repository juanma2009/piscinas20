package com.bolsadeideas.springboot.app.android.data.model

import com.google.gson.annotations.SerializedName

data class PedidoResponse(
    @SerializedName("data") val data: List<PedidoDto>,
    @SerializedName("recordsTotal") val recordsTotal: Int,
    @SerializedName("recordsFiltered") val recordsFiltered: Int
)

data class PedidoDto(
    @SerializedName("npedido") val npedido: Long,
    @SerializedName("ref") val ref: String?,
    @SerializedName("cliente") val cliente: String,
    @SerializedName("tipoPedido") val tipoPedido: String?,
    @SerializedName("estado") val estado: String?,
    @SerializedName("fechaFinalizado") val fechaFinalizado: String?,
    @SerializedName("fechaEntrega") val fechaEntrega: String?,
    @SerializedName("metal") val metal: String?,
    @SerializedName("pieza") val pieza: String?,
    @SerializedName("tipo") val tipo: String?,
    @SerializedName("cobrado") val cobrado: Double?,
    @SerializedName("imagenes") val imagenes: List<String>?
)
