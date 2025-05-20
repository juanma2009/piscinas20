package com.bolsadeideas.springboot.app.models.service.configuracion;

import com.bolsadeideas.springboot.app.models.dao.configuracion.MetalDao;
import com.bolsadeideas.springboot.app.models.entity.configuracion.Metal;
import com.bolsadeideas.springboot.app.models.entity.configuracion.Servicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class MetalService implements MetalRepository {

    @Autowired
    private MetalDao repository;


    @Override
    public Optional<Metal> findById(Long id) {
        // This method should return an Optional of Metal entity by its ID
        // You can use the repository to fetch the entity from the database
        return repository.findById(id);
    }

    @Override
    public void deleteById(Long id) {
        // This method should delete the Metal entity by its ID
        // You can use the repository to delete the entity
        repository.deleteById(id);
        // No need to return anything as the method is void

    }

    @Override
    public Metal save(Metal Metal) {
        // This method should save the Metal entity to the database
        // You can use the repository to save the entity
        // and return the saved entity
        return repository.save(Metal);
    }


    @Override
    public List<Metal> findAll() {
        // This method should return a list of all Metal entities
        // You can use the repository to fetch all records from the database
        // and return them as a list
        List<Metal> metales = (List<Metal>) repository.findAll();
        if (metales != null && !metales.isEmpty()) {
            return metales;
        }
        // If no records are found, return an empty list
        // or handle it as per your requirement
        // For example, you can return an empty list:
         return Collections.emptyList();

    }

    @Override
    public Optional<Metal> actualizar(Long id, Metal metal) {
        Optional<Metal> metaloExistente = repository.findById(id);
        if (metaloExistente.isPresent()) {
            Metal estadoActualizado = metaloExistente.get();
            estadoActualizado.setNombre(metal.getNombre());
            estadoActualizado.setDescription(metal.getDescription());
            // Actualiza otros campos según sea necesario
            repository.save(estadoActualizado);
            return Optional.of(estadoActualizado);
        }
        return Optional.empty();
    }

    }


