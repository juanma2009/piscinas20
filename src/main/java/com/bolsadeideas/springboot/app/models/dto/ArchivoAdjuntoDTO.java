package com.bolsadeideas.springboot.app.models.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ArchivoAdjuntoDTO {
    private Long id; // El ID del archivo adjunto
    private String nombre; // El nombre del archivo adjunto
    private Long pedidoId; // El ID del pedido asociado
    private String urlCloudinary;

    private String setUrlDrive;

}
