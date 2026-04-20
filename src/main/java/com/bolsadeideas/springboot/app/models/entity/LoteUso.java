package com.bolsadeideas.springboot.app.models.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Registra cada vez que se consume parte de un lote (CompraInventario).
 * Un lote puede dividirse en múltiples usos (órdenes de producción distintas).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lote_uso")
@Filter(name = "tenantFilter", condition = "empresa_id = :tenantId")
public class LoteUso implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    /** Lote del que se está consumiendo (CompraInventario = Lote) */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "compra_id", nullable = false)
    private CompraInventario compra;

    /** Pedido/trabajo al que se destina este consumo (opcional) */
    @Column(name = "pedido_id")
    private Long pedidoId;

    /** Orden de producción asociada (opcional) */
    @Column(name = "orden_produccion_id")
    private Long ordenProduccionId;

    /** Gramos consumidos de este lote en este uso */
    @Column(nullable = false)
    private Double pesoUsadoGr;

    @Column(nullable = false)
    private LocalDateTime fechaUso;

    private String motivo;

    private String usuarioRegistro;

    @PrePersist
    public void prePersist() {
        if (fechaUso == null) fechaUso = LocalDateTime.now();
        if (this.empresa == null && com.bolsadeideas.springboot.app.util.TenantContext.getCurrentTenant() != null) {
            Empresa e = new Empresa();
            e.setId(com.bolsadeideas.springboot.app.util.TenantContext.getCurrentTenant());
            this.empresa = e;
        }
    }
}
