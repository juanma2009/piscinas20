package com.bolsadeideas.springboot.app.models.service.configuracion;

import com.bolsadeideas.springboot.app.models.entity.configuracion.Metal;
import com.bolsadeideas.springboot.app.models.entity.configuracion.Servicio;
import com.bolsadeideas.springboot.app.models.entity.configuracion.Subgrupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubGrupoRepository  {


    Optional<Subgrupo> findById(Long id);
    void deleteById(Long id);
    Subgrupo save(Subgrupo Metal);
    List<Subgrupo> findAll();
    Optional<Subgrupo> actualizar(Long id, Subgrupo subgrupo);

}
