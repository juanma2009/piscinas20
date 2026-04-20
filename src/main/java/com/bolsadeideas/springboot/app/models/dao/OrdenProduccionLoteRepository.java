package com.bolsadeideas.springboot.app.models.dao;

import com.bolsadeideas.springboot.app.models.entity.OrdenProduccionLote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrdenProduccionLoteRepository extends JpaRepository<OrdenProduccionLote, Long> {

    /** Todos los lotes usados en una orden */
    List<OrdenProduccionLote> findByOrdenProduccionId(Long ordenId);

    /** Todas las órdenes que usaron un lote concreto */
    List<OrdenProduccionLote> findByCompraId(Long compraId);
}
