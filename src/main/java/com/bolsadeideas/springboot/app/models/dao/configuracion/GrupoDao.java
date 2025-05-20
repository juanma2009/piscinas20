package com.bolsadeideas.springboot.app.models.dao.configuracion;

import com.bolsadeideas.springboot.app.models.entity.configuracion.Grupo;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GrupoDao extends PagingAndSortingRepository<Grupo, Long> {

}
