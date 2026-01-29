package com.bolsadeideas.springboot.app.controllers;

import com.bolsadeideas.springboot.app.models.entity.Cliente;
import com.bolsadeideas.springboot.app.models.service.IClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cliente")
public class RestClienteController {

    @Autowired
    private IClienteService clienteService;

    @GetMapping("/listar")
    public Map<String, Object> listar(@RequestParam(name = "page", defaultValue = "0") int page) {
        Pageable pageRequest = PageRequest.of(page, Integer.MAX_VALUE);
        Page<Cliente> clientes = clienteService.findAll(pageRequest);

        List<Map<String, Object>> clientesData = clientes.getContent().stream().map(c -> {
            Map<String, Object> clienteData = new HashMap<>();
            clienteData.put("id", c.getId());
            clienteData.put("nombre", c.getNombre());
            clienteData.put("apellido", c.getApellido());
            clienteData.put("email", c.getEmail());
            clienteData.put("telefono", c.getTelefono());
            return clienteData;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("recordsTotal", clientes.getTotalElements());
        response.put("recordsFiltered", clientes.getTotalElements());
        response.put("data", clientesData);

        return response;
    }

    @GetMapping("/ver/{id}")
    public Map<String, Object> ver(@PathVariable Long id) {
        Cliente c = clienteService.findOne(id);
        if (c == null) return null;

        Map<String, Object> response = new HashMap<>();
        response.put("id", c.getId());
        response.put("nombre", c.getNombre());
        response.put("apellido", c.getApellido());
        response.put("email", c.getEmail());
        response.put("telefono", c.getTelefono());
        response.put("direccion", c.getDireccion());
        response.put("foto", c.getFoto());

        // Pedidos del cliente
        List<Map<String, Object>> pedidos = c.getPedido().stream().map(p -> {
            Map<String, Object> pMap = new HashMap<>();
            pMap.put("npedido", p.getNpedido());
            pMap.put("ref", p.getRef());
            pMap.put("estado", p.getEstado());
            return pMap;
        }).collect(Collectors.toList());
        response.put("pedidos", pedidos);

        // Facturas del cliente
        List<Map<String, Object>> facturas = c.getFacturas().stream().map(f -> {
            Map<String, Object> fMap = new HashMap<>();
            fMap.put("id", f.getId());
            fMap.put("total", f.getSubTotal());
            return fMap;
        }).collect(Collectors.toList());
        response.put("facturas", facturas);

        return response;
    }
}
