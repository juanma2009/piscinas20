package com.bolsadeideas.springboot.app.models.dao.configuracion;

import com.bolsadeideas.springboot.app.models.entity.configuracion.Servicio;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;


import java.util.Optional;

@Repository
public interface ServicioDao extends PagingAndSortingRepository<Servicio, Long> {

    //crea el query para buscar el servicio por id
    @Query(value = "SELECT s FROM Servicio s WHERE s.id = ?1")
     Optional<Servicio> findById(Long id);

     void deleteById(Long id);

    //crea el query para actualizar el servicio
     @Query(value = "UPDATE Servicio s SET s.nombre = ?1, s.description = ?2 WHERE s.id = ?3")
     Optional<Servicio> actualizar(Long id, Servicio servicio);
}
