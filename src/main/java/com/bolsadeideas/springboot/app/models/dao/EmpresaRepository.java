package com.bolsadeideas.springboot.app.models.dao;

import com.bolsadeideas.springboot.app.models.entity.Empresa;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface EmpresaRepository extends CrudRepository<Empresa, Long> {
    Optional<Empresa> findByNombre(String nombre);
}
