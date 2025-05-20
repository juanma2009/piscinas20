package com.bolsadeideas.springboot.app.models.service.configuracion;


import com.bolsadeideas.springboot.app.models.entity.configuracion.Servicio;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServicioRepository   {

    Optional<Servicio> findById(Long id);
    void deleteById(Long id);
    Servicio save(Servicio servicio);
    List<Servicio> findAll();
    Optional<Servicio> actualizar(Long id, Servicio servicio);
}

