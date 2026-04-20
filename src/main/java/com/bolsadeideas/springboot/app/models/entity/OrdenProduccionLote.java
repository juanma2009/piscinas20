package com.bolsadeideas.springboot.app.models.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Tabla intermedia N:M entre OrdenProduccion y CompraInventario (Lote).
 * Registra cuántos gramos de cada lote se usaron en cada orden de producción.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "orden_produccion_lote")
public class OrdenProduccionLote implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Orden de producción a la que pertenece este uso */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_produccion_id", nullable = false)
    private OrdenProduccion ordenProduccion;

    /** Lote (CompraInventario) del que se consume */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "compra_id", nullable = false)
    private CompraInventario compra;

    /** Gramos de este lote utilizados en esta orden */
    @Column(nullable = false)
    private Double pesoUsadoGr;
}
