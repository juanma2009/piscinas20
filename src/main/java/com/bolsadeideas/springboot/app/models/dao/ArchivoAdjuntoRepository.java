package com.bolsadeideas.springboot.app.models.dao;

import com.bolsadeideas.springboot.app.models.entity.ArchivoAdjunto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;

public interface ArchivoAdjuntoRepository extends JpaRepository<ArchivoAdjunto, Long> {
    List<ArchivoAdjunto> findByPedido_NpedidoIn(Collection<Long> npedidos);

}

