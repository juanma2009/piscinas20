package com.bolsadeideas.springboot.app.models.service.configuracion;

import com.bolsadeideas.springboot.app.models.entity.configuracion.Estado;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstadoRepository {


    Optional<Estado> findById(Long id);

    void deleteById(Long id);

    Estado save(Estado Metal);

    List<Estado> findAll();

    Optional<Estado> actualizar(Long id, Estado estado);
}
