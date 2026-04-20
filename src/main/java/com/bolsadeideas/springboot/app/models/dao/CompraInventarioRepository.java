package com.bolsadeideas.springboot.app.models.dao;

import com.bolsadeideas.springboot.app.models.entity.CompraInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CompraInventarioRepository extends JpaRepository<CompraInventario, Long> {

    /** Compras entre dos fechas */
    @Query("SELECT c FROM CompraInventario c WHERE c.fechaCompra BETWEEN :desde AND :hasta ORDER BY c.fechaCompra DESC")
    List<CompraInventario> findByFechaCompraBetween(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** Compras de un producto concreto */
    List<CompraInventario> findByProductoIdOrderByFechaCompraDesc(Long productoId);

    /** Total gastado en un rango de fechas */
    @Query("SELECT COALESCE(SUM(c.precioTotal), 0) FROM CompraInventario c WHERE c.fechaCompra BETWEEN :desde AND :hasta")
    Double totalGastadoEnPeriodo(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** Cantidad total comprada de un producto en un periodo */
    @Query("SELECT COALESCE(SUM(c.cantidad), 0) FROM CompraInventario c WHERE c.producto.id = :pid AND c.fechaCompra BETWEEN :desde AND :hasta")
    Double cantidadCompradaPorProducto(@Param("pid") Long pid, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** Últimas 10 compras */
    List<CompraInventario> findTop10ByOrderByFechaCompraDesc();

    /** Últimas 50 compras — para el listado */
    List<CompraInventario> findTop50ByOrderByFechaCompraDesc();
}
