package com.bolsadeideas.springboot.app.controllers;

import com.bolsadeideas.springboot.app.models.entity.Factura;
import com.bolsadeideas.springboot.app.models.service.IClienteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Generated;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Generated(
        value = "org.mapstruct.ap.MappingProcessor",
        comments = "version: 1.3.1.Final, compiler: javac, environment: Java 1.8.0_381 (Oracle Corporation)"
)
@RestController
@RequestMapping("/api/factura/")
public class RestFacturaController {

    @Autowired
    private IClienteService clienteService;

    @RequestMapping(value = {"/listarPorCliente"}, method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> listarFacturasPorCliente(
            @RequestParam(name = "clienteId") Long clienteId,
            @RequestParam(name = "page", defaultValue = "0") int page) {

        Pageable pageRequest = PageRequest.of(page, Integer.MAX_VALUE);
        Page<Factura> facturas = clienteService.findFacturaByIdPage(clienteId, pageRequest);

        List<Map<String, Object>> facturasData = facturas.getContent().stream().map(f -> {
            Map<String, Object> facturaData = new HashMap<>();
            facturaData.put("id", f.getId());
            facturaData.put("dfechaAlbaran", f.getDfechaAlbaran());
            facturaData.put("cliente", Map.of("nombre", f.getCliente() != null ? f.getCliente().getNombre() : "---"));
            facturaData.put("tipoPedido", f.getTipoPedido());
            facturaData.put("subTotal", f.getSubTotal());
            return facturaData;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("recordsTotal", facturas.getTotalElements());
        response.put("recordsFiltered", facturas.getTotalElements());
        response.put("data", facturasData);

        return response;
    }

}
