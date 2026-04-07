package com.bolsadeideas.springboot.app.models.dao;

import com.bolsadeideas.springboot.app.models.entity.IncidenciaApp;
import org.springframework.data.repository.CrudRepository;
import java.util.List;

public interface IncidenciaAppRepository extends CrudRepository<IncidenciaApp, Long> {
    List<IncidenciaApp> findByEstado(String estado);
}
