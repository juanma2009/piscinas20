package com.bolsadeideas.springboot.app.models.entity;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Table(name = "archivos_adjuntos")
public class ArchivoAdjunto implements Serializable {

    private static final long serialVersionUID = 1456456L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long pedidoAdjunto;

    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "npedido")
    private Pedido pedido;

    @Column(name = "google_drive_file_id")
    private String googleDriveFileId; // Campo para el ID en Google Drive

    private String urlCloudinary;

    private String setUrlDrive;

    public void setUrlDrive(String urlDrive) {
        this.setUrlDrive = urlDrive;  // Asigna la URL de Google Drive al campo
    }


}
