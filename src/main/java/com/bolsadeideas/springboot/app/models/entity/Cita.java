package com.bolsadeideas.springboot.app.models.entity;

import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "citas")
@Data
@ToString(exclude = {"cliente", "pedido"})
@EqualsAndHashCode(exclude = {"cliente", "pedido"})
public class Cita implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "fecha_cita")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime fechaCita;

    @Column(length = 255)
    private String observacion;

    @Enumerated(EnumType.STRING)
    private TipoCita tipo;

    @Enumerated(EnumType.STRING)
    private EstadoCita estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    public enum TipoCita {
        ENTREGA_TRABAJO,  // El cliente trae algo
        RECOGIDA_PEDIDO   // El cliente se lleva algo
    }

    public enum EstadoCita {
        PROGRAMADA,
        COMPLETADA,
        CANCELADA,
        AUSENTE
    }

    private static final long serialVersionUID = 1L;
}
