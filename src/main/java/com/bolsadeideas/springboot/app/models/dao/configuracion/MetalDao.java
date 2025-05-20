package com.bolsadeideas.springboot.app.models.dao.configuracion;

import com.bolsadeideas.springboot.app.models.entity.configuracion.Metal;
import com.bolsadeideas.springboot.app.models.entity.configuracion.Servicio;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MetalDao extends PagingAndSortingRepository<Metal, Long> {

}
