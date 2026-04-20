package com.bolsadeideas.springboot.app.models.dao;

import com.bolsadeideas.springboot.app.models.entity.OrdenProduccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrdenProduccionRepository extends JpaRepository<OrdenProduccion, Long> {

    /** Órdenes de un pedido concreto */
    List<OrdenProduccion> findByPedidoIdOrderByFechaInicioDesc(Long pedidoId);

    /** Órdenes en proceso */
    List<OrdenProduccion> findByEstadoOrderByFechaInicioDesc(OrdenProduccion.EstadoOrden estado);

    /** Últimas 20 órdenes */
    List<OrdenProduccion> findTop20ByOrderByFechaInicioDesc();

    /** Merma total acumulada en un período (en gramos) */
    @Query("SELECT COALESCE(SUM(o.pesoMermaGr), 0) FROM OrdenProduccion o " +
           "WHERE o.estado = 'TERMINADO'")
    Double totalMermaAcumulada();

    /** Trazabilidad inversa: ¿qué órdenes usaron un lote concreto? */
    @Query("SELECT DISTINCT opl.ordenProduccion FROM OrdenProduccionLote opl " +
           "WHERE opl.compra.id = :compraId")
    List<OrdenProduccion> findOrdenesPorLote(@Param("compraId") Long compraId);
}
