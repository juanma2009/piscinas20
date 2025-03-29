package com.bolsadeideas.springboot.app.models.entity;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

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

    @Temporal(TemporalType.DATE)  // @Temporal(TemporalType.DATE)
    private Date fecha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "npedido")
    private Pedido pedido;
/*
    @Column(name = "google_drive_file_id")
    private String googleDriveFileId; // Campo para el ID en Google Drive
*/
    private String urlCloudinary;

    private String setUrlDrive;

    public void setUrlDrive(String urlDrive) {
        this.setUrlDrive = urlDrive;  // Asigna la URL de Google Drive al campo
    }


}
