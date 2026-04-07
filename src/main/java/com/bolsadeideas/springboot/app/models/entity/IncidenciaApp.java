package com.bolsadeideas.springboot.app.models.entity;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "incidencias_app")
@Getter
@Setter
public class IncidenciaApp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    private String asunto;
    private String descripcion;
    private String estado; // Pendiente, En Proceso, Cerrado
    private String prioridad; // Alta, Media, Baja

    @Temporal(TemporalType.TIMESTAMP)
    private Date fecha;

    @PrePersist
    public void prePersist() {
        if (this.fecha == null) this.fecha = new Date();
        if (this.estado == null) this.estado = "PENDIENTE";
    }
}
