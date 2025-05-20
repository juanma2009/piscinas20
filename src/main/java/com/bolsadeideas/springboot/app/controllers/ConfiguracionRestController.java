package com.bolsadeideas.springboot.app.controllers;

import com.bolsadeideas.springboot.app.models.entity.configuracion.Estado;
import com.bolsadeideas.springboot.app.models.entity.configuracion.Metal;
import com.bolsadeideas.springboot.app.models.entity.configuracion.Servicio;
import com.bolsadeideas.springboot.app.models.entity.configuracion.Subgrupo;
import com.bolsadeideas.springboot.app.models.service.configuracion.EstadoServiceImpl;
import com.bolsadeideas.springboot.app.models.service.configuracion.MetalService;
import com.bolsadeideas.springboot.app.models.service.configuracion.ServicioServiceImpl;
import com.bolsadeideas.springboot.app.models.service.configuracion.SubgrupoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import static org.checkerframework.checker.nullness.Opt.orElse;

@Slf4j
@RestController
@RequestMapping("/api/configuracion")
public class ConfiguracionRestController {

    @Autowired private ServicioServiceImpl servicioService;
    @Autowired private MetalService metalService;
    @Autowired private SubgrupoService subgrupoService;
    @Autowired private EstadoServiceImpl estadoService;

    // --- SERVICIOS ---
    @GetMapping("/servicios")
    public ResponseEntity<List<Servicio>> listarServicios() {
        Optional<List<Servicio>> servicios = Optional.of(servicioService.findAll());
      //mostrar todos los servicios
        log.info("Servicios encontrados: {}", servicios.get());
        return servicios.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }


    @GetMapping("/servicios/{id}")
    public ResponseEntity<Servicio> obtenerServicio(@PathVariable Long id) {
        return servicioService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/servicios")
    public ResponseEntity<Servicio> guardarServicio(@RequestBody Servicio servicio) {
        Servicio nuevo = servicioService.save(servicio);
        return ResponseEntity.ok(nuevo);
    }

    @PutMapping("/servicios/{id}")
    public ResponseEntity<Servicio> actualizarServicio(@PathVariable Long id, @RequestBody Servicio servicio) {
        return servicioService.actualizar(id, servicio)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/servicios/{id}")
    public ResponseEntity<?> eliminarServicio(@PathVariable Long id) {
        servicioService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // --- METALES ---
    @GetMapping("/metales")
    public List<Metal> listarMetales() {
        return metalService.findAll();
    }

    @GetMapping("/metales/{id}")
    public ResponseEntity<Metal> obtenerMetal(@PathVariable Long id) {
        return metalService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/metales")
    public ResponseEntity<Metal> guardarMetal(@RequestBody Metal metal) {
        Metal nuevo = metalService.save(metal);
        return ResponseEntity.ok(nuevo);
    }

    @PutMapping("/metales/{id}")
    public ResponseEntity<Metal> actualizarMetal(@PathVariable Long id, @RequestBody Metal metal) {
        return metalService.actualizar(id, metal)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/metales/{id}")
    public ResponseEntity<?> eliminarMetal(@PathVariable Long id) {
        metalService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // --- SUBGRUPOS ---
    @GetMapping("/subgrupos")
    public List<Subgrupo> listarSubgrupos() {
        return subgrupoService.findAll();
    }

    @GetMapping("/subgrupos/{id}")
    public ResponseEntity<Subgrupo> obtenerSubgrupo(@PathVariable Long id) {
        return subgrupoService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/subgrupos")
    public ResponseEntity<Subgrupo> guardarSubgrupo(@RequestBody Subgrupo subgrupo) {
        Subgrupo nuevo = subgrupoService.save(subgrupo);
        return ResponseEntity.ok(nuevo);
    }

    @PutMapping("/subgrupos/{id}")
    public ResponseEntity<Subgrupo> actualizarSubgrupo(@PathVariable Long id, @RequestBody Subgrupo subgrupo) {
        return subgrupoService.actualizar(id, subgrupo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/subgrupos/{id}")
    public ResponseEntity<?> eliminarSubgrupo(@PathVariable Long id) {
        subgrupoService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // --- ESTADOS ---
    @GetMapping("/estados")
    public List<Estado> listarEstados() {
        return estadoService.findAll();
    }

    @GetMapping("/estados/{id}")
    public ResponseEntity<Estado> obtenerEstado(@PathVariable Long id) {
        return estadoService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/estados")
    public ResponseEntity<Estado> guardarEstado(@RequestBody Estado estado) {
        Estado nuevo = estadoService.save(estado);
        return ResponseEntity.ok(nuevo);
    }

    @PutMapping("/estados/{id}")
    public ResponseEntity<Estado> actualizarEstado(@PathVariable Long id, @RequestBody Estado estado) {
        return estadoService.actualizar(id, estado)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/estados/{id}")
    public ResponseEntity<?> eliminarEstado(@PathVariable Long id) {
        estadoService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
