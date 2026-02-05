package com.bolsadeideas.springboot.app.models.dao;

import com.bolsadeideas.springboot.app.models.entity.CitaHistorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CitaHistorialDao extends JpaRepository<CitaHistorial, Long> {
    
    @Query("SELECT h FROM CitaHistorial h WHERE h.cita.id = :citaId ORDER BY h.fechaModificacion DESC")
    List<CitaHistorial> findByCitaIdOrderByFechaModificacionDesc(@Param("citaId") Long citaId);
}
