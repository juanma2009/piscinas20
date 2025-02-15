package com.bolsadeideas.springboot.app.models.dao;

import com.bolsadeideas.springboot.app.models.entity.ArchivoAdjunto;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface ArchivoAdjuntoDao extends PagingAndSortingRepository<ArchivoAdjunto, Long> {

    @Query("select a from ArchivoAdjunto a where a.pedidoAdjunto = ?1")
    public List<ArchivoAdjunto> findArchivoAdjuntoById(Long pedidoId);

    @Query("select a.nombre from ArchivoAdjunto a")
    public List<String> findArchivoAdjunto();

    @Query("select a from ArchivoAdjunto a where  a.nombre = ?1 and a.pedidoAdjunto = ?2")
    public ArchivoAdjunto findArchivoAdjuntoByIdOne(String fileId,Long pedidoId);

}
