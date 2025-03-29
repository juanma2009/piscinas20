package com.bolsadeideas.springboot.app.models.service;

import com.bolsadeideas.springboot.app.models.dao.ComentarioRepository;
import com.bolsadeideas.springboot.app.models.entity.Comentario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ComentarioService {

    @Autowired
    private ComentarioRepository comentarioRepository;

    public List<Comentario> obtenerComentariosPorPedido(Long pedidoId) {
        return comentarioRepository.findByPedidoId(pedidoId);
    }

    public Comentario guardarComentario(Comentario comentario) {
        return comentarioRepository.save(comentario);
    }
}
