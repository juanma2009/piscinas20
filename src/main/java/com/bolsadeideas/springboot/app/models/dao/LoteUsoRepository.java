package com.bolsadeideas.springboot.app.models.dao;

import com.bolsadeideas.springboot.app.models.entity.LoteUso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LoteUsoRepository extends JpaRepository<LoteUso, Long> {

    /** Todos los usos de un lote concreto */
    List<LoteUso> findByCompraIdOrderByFechaUsoDesc(Long compraId);

    /** Todos los usos vinculados a un pedido */
    @Query("SELECT u FROM LoteUso u WHERE u.pedido.npedido = :npedido ORDER BY u.fechaUso DESC")
    List<LoteUso> findByPedidoId(@Param("npedido") Long npedido);

    /** Usos de una orden de producción */
    List<LoteUso> findByOrdenProduccionId(Long ordenProduccionId);

    /** Total de gramos consumidos de un lote */
    @Query("SELECT COALESCE(SUM(u.pesoUsadoGr), 0) FROM LoteUso u WHERE u.compra.id = :compraId")
    Double totalConsumidoDeLote(@Param("compraId") Long compraId);
}
