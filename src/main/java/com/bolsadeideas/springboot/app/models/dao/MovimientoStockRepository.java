package com.bolsadeideas.springboot.app.models.dao;

import com.bolsadeideas.springboot.app.models.entity.MovimientoStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Long> {

    /** Movimientos de un producto ordenados por fecha desc */
    List<MovimientoStock> findByProductoIdOrderByFechaDesc(Long productoId);

    /** Salidas (consumo) en un periodo */
    @Query("SELECT COALESCE(SUM(m.cantidad), 0) FROM MovimientoStock m " +
           "WHERE m.tipo = 'SALIDA' AND m.fecha BETWEEN :desde AND :hasta")
    Double totalSalidasEnPeriodo(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** Salidas de un producto concreto en un periodo */
    @Query("SELECT COALESCE(SUM(m.cantidad), 0) FROM MovimientoStock m " +
           "WHERE m.tipo = 'SALIDA' AND m.producto.id = :pid AND m.fecha BETWEEN :desde AND :hasta")
    Double salidasPorProductoEnPeriodo(@Param("pid") Long pid, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** Últimos 20 movimientos */
    List<MovimientoStock> findTop20ByOrderByFechaDesc();

    /** Movimientos entre fechas */
    @Query("SELECT m FROM MovimientoStock m WHERE m.fecha BETWEEN :desde AND :hasta ORDER BY m.fecha DESC")
    List<MovimientoStock> findByFechaBetween(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** TOP productos más consumidos (SALIDA) en un periodo */
    @Query("SELECT m.producto.id, m.producto.nombre, SUM(m.cantidad) as total " +
           "FROM MovimientoStock m WHERE m.tipo = 'SALIDA' AND m.fecha BETWEEN :desde AND :hasta " +
           "GROUP BY m.producto.id, m.producto.nombre ORDER BY total DESC")
    List<Object[]> topProductosConsumidos(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
