package com.bolsadeideas.springboot.app.models.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Registra cada compra/entrada de material al inventario.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "compras_inventario")
@Filter(name = "tenantFilter", condition = "empresa_id = :tenantId")
public class CompraInventario implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    /** Cantidad comprada */
    @Column(nullable = false)
    private Double cantidad;

    /** Precio unitario de compra */
    @Column(nullable = false)
    private Double precioUnitario;

    /** Precio total = cantidad * precioUnitario */
    @Column(nullable = false)
    private Double precioTotal;

    /** Fecha y hora de la compra */
    @Column(nullable = false)
    private LocalDateTime fechaCompra;

    /** Número de albarán / factura del proveedor */
    private String numAlbaran;

    /** Notas adicionales */
    private String notas;

    /** Usuario que registró la compra */
    private String usuarioRegistro;

    // ─── EXTENSIÓN JOYERÍA Paso 1 ─────────────────────────────────────────────

    /**
     * Código único de lote generado automáticamente al registrar la compra.
     * Formato: TIPO-PUREZA-FECHA-SEQ  →  ORO-18K-20240420-001
     * Nulo para compras anteriores a esta extensión (compatibilidad garantizada).
     */
    @Column(unique = true)
    private String codigoLote;

    /** Gramos comprados (peso inicial del lote). Nulo si el material no se mide en gramos. */
    private Double pesoInicialGr;

    /** Gramos restantes en el lote (se resta con cada consumo). Empieza igual que pesoInicialGr. */
    private Double pesoActualGr;

    /** Pureza del material: 18k, 14k, 9k, 925, 950... Nulo si no aplica. */
    private String pureza;

    /** Tipo de material: ORO, PLATA, PLATINO, OTRO. Nulo si no aplica. */
    private String tipoMaterial;

    @PrePersist
    public void prePersist() {
        if (fechaCompra == null) fechaCompra = LocalDateTime.now();
        if (precioTotal == null && cantidad != null && precioUnitario != null) {
            precioTotal = cantidad * precioUnitario;
        }
        // Si hay lote en gramos, inicializar peso_actual = peso_inicial
        if (pesoActualGr == null && pesoInicialGr != null) {
            pesoActualGr = pesoInicialGr;
        }
        if (this.empresa == null && com.bolsadeideas.springboot.app.util.TenantContext.getCurrentTenant() != null) {
            Empresa e = new Empresa();
            e.setId(com.bolsadeideas.springboot.app.util.TenantContext.getCurrentTenant());
            this.empresa = e;
        }
    }
}
