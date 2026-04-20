package com.bolsadeideas.springboot.app.models.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa una orden de transformación de material en el taller.
 * Registra qué lotes se usaron, cuánto pesó el producto final,
 * la merma perdida y el reciclaje recuperado.
 *
 * Rule de balance: peso_entrada = peso_producto + peso_merma + peso_reciclaje
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "orden_produccion")
@Filter(name = "tenantFilter", condition = "empresa_id = :tenantId")
public class OrdenProduccion implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum EstadoOrden { EN_PROCESO, TERMINADO, CANCELADO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    /** Pedido del cliente al que corresponde esta producción (opcional) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    /** Helper: obtiene el ID del pedido sin cargar la entidad completa */
    public Long getPedidoId() {
        return pedido != null ? pedido.getNpedido() : null;
    }

    private String descripcion;

    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoOrden estado = EstadoOrden.EN_PROCESO;

    // ─── Pesos (balance de masa) ───────────────────────────────────────────────

    /** Total de gramos que entran en la orden (suma de todos los lotes usados) */
    @Column(nullable = false)
    private Double pesoEntradaGr;

    /** Gramos en el producto final entregado al cliente */
    private Double pesoProductoGr;

    /** Gramos perdidos de forma irrecuperable (polvo, vapor, ácidos...) */
    private Double pesoMermaGr;

    /** Gramos recuperables (limaduras, recortes...) */
    private Double pesoReciclajeGr;

    // ─── Reciclaje ─────────────────────────────────────────────────────────────

    /**
     * Tipo de reciclaje de la merma recuperable:
     *   LOTE    → viene de este trabajo concreto, mismo material/pureza → nuevo lote hijo
     *   GENERAL → mezcla de varios trabajos, sin trazabilidad de lote   → stock genérico
     *   NINGUNO → nada recuperable (todo es pérdida irrecuperable)
     */
    @Column(length = 20)
    private String tipoReciclaje = "NINGUNO";

    /**
     * Solo si tipoReciclaje = 'LOTE':
     * ID de la nueva CompraInventario (lote hijo) creada a partir del reciclaje.
     */
    private Long loteReciclajeId;

    /**
     * Solo si tipoReciclaje = 'GENERAL':
     * ID del Producto genérico de chatarra donde se acumula la merma
     * (ej: "Chatarra Oro General", "Limaduras Plata").
     */
    private Long productoMermaId;

    private String usuarioRegistro;

    // ─── Lotes utilizados ──────────────────────────────────────────────────────

    @OneToMany(mappedBy = "ordenProduccion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrdenProduccionLote> lotesUsados = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (fechaInicio == null) fechaInicio = LocalDateTime.now();
        if (estado == null) estado = EstadoOrden.EN_PROCESO;
        if (tipoReciclaje == null) tipoReciclaje = "NINGUNO";
        if (this.empresa == null && com.bolsadeideas.springboot.app.util.TenantContext.getCurrentTenant() != null) {
            Empresa e = new Empresa();
            e.setId(com.bolsadeideas.springboot.app.util.TenantContext.getCurrentTenant());
            this.empresa = e;
        }
    }

    /**
     * Valida que el balance de masa cuadre (tolerancia 0.01g por redondeos).
     */
    public boolean balanceCuadra() {
        if (pesoEntradaGr == null || pesoProductoGr == null ||
            pesoMermaGr == null || pesoReciclajeGr == null) return false;
        double salida = pesoProductoGr + pesoMermaGr + pesoReciclajeGr;
        return Math.abs(salida - pesoEntradaGr) <= 0.01;
    }
}
