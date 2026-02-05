package com.bolsadeideas.springboot.app.models.service;

import com.bolsadeideas.springboot.app.models.entity.Cita;
import com.bolsadeideas.springboot.app.models.entity.CitaHistorial;

import java.time.LocalDateTime;
import java.util.List;

public interface ICitaService {
    List<Cita> findAll();
    List<Cita> findAllWithEntities();
    Cita findById(Long id);
    Cita findByIdWithEntities(Long id);
    Cita save(Cita cita);
    Cita save(Cita cita, String username);
    void delete(Long id);
    List<Cita> findByCliente(Long clienteId);
    List<Cita> findByPedido(Long pedidoId);
    List<Cita> findByRangoFechas(LocalDateTime inicio, LocalDateTime fin);
    List<CitaHistorial> getHistorialByCitaId(Long citaId);
}
