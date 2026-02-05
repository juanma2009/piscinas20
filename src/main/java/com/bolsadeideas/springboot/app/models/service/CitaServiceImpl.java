package com.bolsadeideas.springboot.app.models.service;

import com.bolsadeideas.springboot.app.models.dao.CitaHistorialDao;
import com.bolsadeideas.springboot.app.models.dao.CitaRepository;
import com.bolsadeideas.springboot.app.models.entity.Cita;
import com.bolsadeideas.springboot.app.models.entity.CitaHistorial;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CitaServiceImpl implements ICitaService {

    @Autowired
    private CitaRepository citaRepository;
    
    @Autowired
    private CitaHistorialDao citaHistorialDao;

    @Override
    @Transactional(readOnly = true)
    public List<Cita> findAll() {
        return (List<Cita>) citaRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cita> findAllWithEntities() {
        return citaRepository.findAllWithEntities();
    }

    @Override
    @Transactional(readOnly = true)
    public Cita findById(Long id) {
        return citaRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Cita findByIdWithEntities(Long id) {
        return citaRepository.findByIdWithEntities(id);
    }

    @Override
    @Transactional
    public Cita save(Cita cita) {
        return citaRepository.save(cita);
    }

    @Override
    @Transactional
    public Cita save(Cita cita, String username) {
        if (cita.getId() != null) {
            Cita citaAnterior = citaRepository.findById(cita.getId()).orElse(null);
            if (citaAnterior != null) {
                CitaHistorial historial = new CitaHistorial();
                historial.setCita(citaAnterior);
                historial.setFechaCita(citaAnterior.getFechaCita());
                historial.setTipo(citaAnterior.getTipo());
                historial.setEstado(citaAnterior.getEstado());
                historial.setObservacion(citaAnterior.getObservacion());
                historial.setFechaModificacion(LocalDateTime.now());
                historial.setUsuarioModificacion(username != null ? username : "Sistema");
                citaHistorialDao.save(historial);
            }
        }
        return citaRepository.save(cita);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        citaRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cita> findByCliente(Long clienteId) {
        return citaRepository.findByClienteId(clienteId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cita> findByPedido(Long pedidoId) {
        return citaRepository.findByPedidoId(pedidoId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cita> findByRangoFechas(LocalDateTime inicio, LocalDateTime fin) {
        return citaRepository.findByRangoFechas(inicio, fin);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CitaHistorial> getHistorialByCitaId(Long citaId) {
        return citaHistorialDao.findByCitaIdOrderByFechaModificacionDesc(citaId);
    }
}
