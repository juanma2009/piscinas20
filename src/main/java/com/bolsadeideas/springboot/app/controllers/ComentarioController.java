package com.bolsadeideas.springboot.app.controllers;

import com.bolsadeideas.springboot.app.models.entity.Comentario;
import com.bolsadeideas.springboot.app.models.service.ComentarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comentarios")
public class ComentarioController {

    @Autowired
    private ComentarioService comentarioService;

    @GetMapping("/pedido/{npedido}")
    public ResponseEntity<List<Comentario>> listarComentarios(@PathVariable Long npedido) {
        List<Comentario> comentarios = comentarioService.obtenerComentariosPorPedido(npedido);
        return ResponseEntity.ok(comentarios);
    }

    @PostMapping
    public ResponseEntity<Comentario> crearComentario(@RequestBody Comentario comentario) {
        Comentario nuevoComentario = comentarioService.guardarComentario(comentario);
        return ResponseEntity.ok(nuevoComentario);
    }
}
