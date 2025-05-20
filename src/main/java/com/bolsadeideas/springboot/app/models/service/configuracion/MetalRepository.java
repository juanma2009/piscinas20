package com.bolsadeideas.springboot.app.models.service.configuracion;

import com.bolsadeideas.springboot.app.models.entity.configuracion.Metal;
import com.bolsadeideas.springboot.app.models.entity.configuracion.Servicio;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MetalRepository{


    Optional<Metal> findById(Long id);
    void deleteById(Long id);
    Metal save(Metal Metal);
    List<Metal> findAll();
    Optional<Metal> actualizar(Long id, Metal metal);
}
