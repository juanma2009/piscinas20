package com.bolsadeideas.springboot.app.models.dao.configuracion;

import com.bolsadeideas.springboot.app.models.entity.configuracion.Estado;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EstadoDao extends PagingAndSortingRepository<Estado,Long> {
}
