package com.bolsadeideas.springboot.app.controllers;

import com.bolsadeideas.springboot.app.models.entity.Cita;
import com.bolsadeideas.springboot.app.models.service.ICitaService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/citas")
@Log4j2
@Secured("ROLE_ADMIN")
public class RestCitaController {

    @Autowired
    private ICitaService citaService;

    @GetMapping("/listarPorCliente")
    public Map<String, Object> listarPorCliente(@RequestParam Long clienteId) {
        log.info("API: Listando citas para cliente ID: {}", clienteId);
        List<Cita> citas = citaService.findByCliente(clienteId);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        List<Map<String, Object>> citasData = citas.stream().map(c -> {
            Map<String, Object> citaData = new HashMap<>();
            citaData.put("id", c.getId());
            citaData.put("fechaCita", c.getFechaCita() != null ? c.getFechaCita().format(formatter) : "");
            citaData.put("tipo", c.getTipo() != null ? c.getTipo().name() : "");
            citaData.put("pedidoId", c.getPedido() != null ? c.getPedido().getNpedido() : null);
            citaData.put("estado", c.getEstado() != null ? c.getEstado().name() : "");
            citaData.put("observacion", c.getObservacion());
            return citaData;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("recordsTotal", citasData.size());
        response.put("recordsFiltered", citasData.size());
        response.put("data", citasData);

        return response;
    }
}
