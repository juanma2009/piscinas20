package com.bolsadeideas.springboot.app.models.service.configuracion;

import com.bolsadeideas.springboot.app.models.entity.configuracion.Grupo;
import com.bolsadeideas.springboot.app.models.entity.configuracion.Servicio;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GrupoRepository {

    Grupo save(Grupo grupo);

    Optional<Grupo> findById(Long id);

    void deleteById(Long id);


    Optional<Grupo> actualizar(Long id, Grupo grupo);

    List<Grupo> findAll();


}
