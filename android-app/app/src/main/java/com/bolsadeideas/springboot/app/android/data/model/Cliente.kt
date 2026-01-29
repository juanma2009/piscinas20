package com.bolsadeideas.springboot.app.android.data.model

import com.google.gson.annotations.SerializedName

data class ClienteResponse(
    @SerializedName("data") val data: List<ClienteDto>,
    @SerializedName("recordsTotal") val recordsTotal: Int,
    @SerializedName("recordsFiltered") val recordsFiltered: Int
)

data class ClienteDto(
    @SerializedName("id") val id: Long,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("apellido") val apellido: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("telefono") val telefono: String?
)
