package com.bolsadeideas.springboot.app.models.entity;

import lombok.Data;
import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "cita_historial")
@Data
public class CitaHistorial implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id", nullable = false)
    private Cita cita;

    @Column(name = "fecha_cita")
    private LocalDateTime fechaCita;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private Cita.TipoCita tipo;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private Cita.EstadoCita estado;

    @Column(length = 500)
    private String observacion;

    @Column(name = "fecha_modificacion", nullable = false)
    private LocalDateTime fechaModificacion;

    @Column(name = "usuario_modificacion", length = 100)
    private String usuarioModificacion;

    private static final long serialVersionUID = 1L;
}
