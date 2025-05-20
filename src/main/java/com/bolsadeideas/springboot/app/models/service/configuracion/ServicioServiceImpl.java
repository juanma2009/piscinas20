package com.bolsadeideas.springboot.app.models.service.configuracion;

import com.bolsadeideas.springboot.app.models.dao.configuracion.ServicioDao;
import com.bolsadeideas.springboot.app.models.entity.configuracion.Servicio;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Service
public class ServicioServiceImpl implements ServicioRepository {

    @Autowired
    private ServicioDao servicioDao;


    @Override
    public Optional<Servicio> findById(Long id) {

        return servicioDao.findById(id);
    }



    @Override
    public void deleteById(Long id) {
        // This method should delete the Servicio entity by its ID
        // You can use the servicioRepository to delete the entity
        servicioDao.deleteById(id);

    }

    @Override
    public Servicio save(Servicio servicio) {
        // This method should save the Servicio entity to the database
        // You can use the servicioRepository to save the entity
        return servicioDao.save(servicio);

    }
    @Override
    public List<Servicio> findAll() {
        return (List<Servicio>) servicioDao.findAll();
    }



    @Override
    public Optional<Servicio> actualizar(Long id, Servicio servicio) {
        return servicioDao.findById(id).map(existing -> {
            existing.setNombre(servicio.getNombre());
            existing.setDescription(servicio.getDescription());
            return servicioDao.save(existing);
        });
    }




}

