package com.bolsadeideas.springboot.app.models.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

// PedidoFotoDto.java
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class PedidoFotoDto {
    private Long npedido;
    private Integer clienteId;
    private String clienteNombre;

    private LocalDate dfecha;
    private LocalDate fechaEntrega;

    private String tipoPedido;
    private String estado;
    private String grupo;
    private String pieza;
    private String tipo;
    private String ref;

    private Boolean activo;
    private String imagenUrl;

    public PedidoFotoDto() {}

    // getters/setters ...
}

