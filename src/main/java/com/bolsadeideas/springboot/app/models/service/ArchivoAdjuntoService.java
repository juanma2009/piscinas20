package com.bolsadeideas.springboot.app.models.service;

import com.bolsadeideas.springboot.app.models.dao.ArchivoAdjuntoDao;
import com.bolsadeideas.springboot.app.models.entity.ArchivoAdjunto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class ArchivoAdjuntoService {

    private final ArchivoAdjuntoDao archivoAdjuntoRepository;

    @Autowired
    public ArchivoAdjuntoService(ArchivoAdjuntoDao archivoAdjuntoRepository) {
        this.archivoAdjuntoRepository = archivoAdjuntoRepository;
    }

    public void eliminarArchivoAdjunto(ArchivoAdjunto archivoAdjunto) {
        archivoAdjuntoRepository.delete(archivoAdjunto);
    }

    //metodo guarda
    public void guardar(ArchivoAdjunto archivoAdjunto) {
        archivoAdjuntoRepository.save(archivoAdjunto);
    }

    public List<ArchivoAdjunto> findArchivosAdjuntosByPedidoId(Long pedidoId) {
        return archivoAdjuntoRepository.findArchivoAdjuntoById(pedidoId);
    }

    public Optional<ArchivoAdjunto> findArchivosAdjuntosByPedidoIdOne(String fileId, Long pedidoId) {
        return archivoAdjuntoRepository.findArchivoAdjuntoByIdOne(fileId, Long.valueOf(String.valueOf(pedidoId)));
    }

    public List<String> findArchivosAdjuntos() {
        return archivoAdjuntoRepository.findArchivoAdjunto();
    }

    @Transactional
    public void guardarTodos(List<ArchivoAdjunto> archivos) {
        archivoAdjuntoRepository.saveAll(archivos); // Batch insert
    }



}
