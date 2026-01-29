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

    private Long pedidoAdjunto;  // Este campo almacenará el ID del pedido asociado

    private String nombre;  // Nombre original del archivo (el nombre que sube el usuario)

    @Temporal(TemporalType.DATE)
    private Date fecha;  // Fecha de subida del archivo (puedes usar la fecha actual si lo prefieres)


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "npedido")  // aquí está la FK
    private Pedido pedido;

    private String urlCloudinary;  // URL de Cloudinary donde se encuentra la imagen

    private String setUrlDrive;  // Si decides usar Google Drive, puedes almacenar aquí la URL

    public void setUrlDrive(String urlDrive) {
        this.setUrlDrive = urlDrive;  // Asigna la URL de Google Drive al campo
    }

    // Constructor sin argumentos
    public ArchivoAdjunto() {}

    // Constructor para los metadatos (opcional)
    public ArchivoAdjunto(Long pedidoAdjunto, String nombre, String urlCloudinary) {
        this.pedidoAdjunto = pedidoAdjunto;
        this.nombre = nombre;
        this.urlCloudinary = urlCloudinary;
        this.fecha = new Date();  // Fecha actual
    }
}
