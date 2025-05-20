package com.bolsadeideas.springboot.app.models.service.configuracion;

import com.bolsadeideas.springboot.app.models.dao.configuracion.SubgrupoDao;
import com.bolsadeideas.springboot.app.models.entity.configuracion.Subgrupo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class SubgrupoService implements SubGrupoRepository {



    @Autowired
    private SubgrupoDao repository;


    @Override
    public Optional<Subgrupo> findById(Long id) {
        // This method should return an Optional of Subgrupo entity by its ID
        // You can use the repository to fetch the entity from the database
        return repository.findById(id);
    }

    @Override
    public void deleteById(Long id) {
        // This method should delete the Subgrupo entity by its ID
        // You can use the repository to delete the entity
        repository.deleteById(id);
        // No need to return anything as the method is void

    }

    @Override
    public Subgrupo save(Subgrupo subgrupo) {
        // This method should save the Subgrupo entity to the database
        // You can use the repository to save the entity
        return repository.save(subgrupo);
    }

    @Override
    public List<Subgrupo> findAll() {
        // This method should return a list of all Subgrupo entities
        // You can use the repository to fetch all records from the database
        // and return them as a list
         List<Subgrupo> subgrupos = (List<Subgrupo>) repository.findAll();
         if (subgrupos != null && !subgrupos.isEmpty()) {
             return subgrupos;
         }
        return Collections.emptyList();
    }

    @Override
    public Optional<Subgrupo> actualizar(Long id, Subgrupo subgrupo) {
        return repository.findById(id).map(existing -> {
            existing.setNombre(subgrupo.getNombre());
            existing.setDescription(subgrupo.getDescription());
            return repository.save(existing);
        });
    }


}

