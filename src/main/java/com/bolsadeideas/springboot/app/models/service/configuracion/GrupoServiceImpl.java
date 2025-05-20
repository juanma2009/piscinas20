package com.bolsadeideas.springboot.app.models.service.configuracion;

import com.bolsadeideas.springboot.app.models.dao.configuracion.GrupoDao;
import com.bolsadeideas.springboot.app.models.entity.configuracion.Grupo;
import com.bolsadeideas.springboot.app.models.entity.configuracion.Servicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class GrupoServiceImpl implements GrupoRepository {

    @Autowired
    private GrupoDao grupoDao;

    @Override
    public Grupo save(Grupo grupo) {
        // Implement the logic to save the Grupo entity
        // For example, you might want to use a repository to save it to a database
        return grupoDao.save(grupo);
    }

    @Override
    public Optional<Grupo> findById(Long id) {
        // Implement the logic to find a Grupo entity by its ID
        // For example, you might want to use a repository to fetch it from a database
         return grupoDao.findById(id);

    }

    @Override
    public void deleteById(Long id) {
        // Implement the logic to delete a Grupo entity by its ID
        // For example, you might want to use a repository to delete it from a database
        grupoDao.deleteById(id);
        // No need to return anything as the method is void

    }


    @Override
    public Optional<Grupo> actualizar(Long id, Grupo grupo) {
        return grupoDao.findById(id).map(existing -> {
            existing.setNombre(grupo.getNombre());
            existing.setDescription(grupo.getDescription());
            return grupoDao.save(existing);
        });
    }

    @Override
    public List<Grupo> findAll() {
        // Implement the logic to find all Grupo entities
        // For example, you might want to use a repository to fetch all records from a database
        // and return them as a list
        List<Grupo> grupos = (List<Grupo>) grupoDao.findAll();
        if (!grupos.isEmpty()) {
            return grupos;
        }
        // If no records are found, return an empty list
        // or handle it according to your application's requirements
        // For example, you can return an empty list:
         return Collections.emptyList();

    }
}
