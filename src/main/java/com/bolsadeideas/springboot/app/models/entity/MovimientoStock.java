package com.bolsadeideas.springboot.app.models.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Registra cada movimiento de stock (ENTRADA o SALIDA) de un material.
 * ENTRADA: compra de material
 * SALIDA:  uso en pedido o ajuste manual
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "movimientos_stock")
@Filter(name = "tenantFilter", condition = "empresa_id = :tenantId")
public class MovimientoStock implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum TipoMovimiento {
        ENTRADA,        // compra de material (existente)
        SALIDA,         // uso genérico (existente)
        AJUSTE,         // corrección manual (existente)
        // ─── Extensión joyería ───────────────────────────────
        CONSUMO_LOTE,   // uso trazado a un lote concreto en una orden de producción
        MERMA,          // pérdida irrecuperable (polvo, vapor, soldadura)
        RECICLAJE_OUT,  // el material sale como chatarra hacia un contenedor/tarro
        RECICLAJE_IN    // el material entra como lote reciclado (de LOTE o GENERAL)
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimiento tipo;

    /** Cantidad del movimiento (siempre positiva) */
    @Column(nullable = false)
    private Double cantidad;

    /** Fecha y hora del movimiento */
    @Column(nullable = false)
    private LocalDateTime fecha;

    /** Referencia del pedido asociado (si es SALIDA por uso en taller) */
    private String referenciaPedido;

    /** ID de la compra que originó este movimiento (si es ENTRADA) */
    private Long compraId;

    /** Motivo / descripción del ajuste */
    private String motivo;

    private String usuarioRegistro;

    /** Stock resultante tras el movimiento (snapshot) */
    private Double stockResultante;

    @PrePersist
    public void prePersist() {
        if (fecha == null) fecha = LocalDateTime.now();
        if (this.empresa == null && com.bolsadeideas.springboot.app.util.TenantContext.getCurrentTenant() != null) {
            Empresa e = new Empresa();
            e.setId(com.bolsadeideas.springboot.app.util.TenantContext.getCurrentTenant());
            this.empresa = e;
        }
    }
}
