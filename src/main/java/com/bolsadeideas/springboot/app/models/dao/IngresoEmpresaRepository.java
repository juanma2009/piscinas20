package com.bolsadeideas.springboot.app.models.dao;

import com.bolsadeideas.springboot.app.models.entity.IngresoEmpresa;
import org.springframework.data.repository.CrudRepository;
import java.util.List;

public interface IngresoEmpresaRepository extends CrudRepository<IngresoEmpresa, Long> {
    List<IngresoEmpresa> findByEmpresaId(Long empresaId);
}
