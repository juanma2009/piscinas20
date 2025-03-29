package com.bolsadeideas.springboot.app.controllers;


import com.bolsadeideas.springboot.app.apigoogledrice.GoogleDriveService;
import com.bolsadeideas.springboot.app.models.dto.PedidoDtos;
import com.bolsadeideas.springboot.app.models.dto.mapper.PedidoMapper;
import com.bolsadeideas.springboot.app.models.entity.ArchivoAdjunto;
import com.bolsadeideas.springboot.app.models.entity.Cliente;
import com.bolsadeideas.springboot.app.models.entity.Pedido;
import com.bolsadeideas.springboot.app.models.service.*;
import com.bolsadeideas.springboot.app.util.paginator.PageRender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Generated;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Generated(
        value = "org.mapstruct.ap.MappingProcessor",
        comments = "version: 1.3.1.Final, compiler: javac, environment: Java 1.8.0_381 (Oracle Corporation)"
)
@RestController
@RequestMapping("/api/pedido/")
public class RestPedidoController {


    @Autowired
    private IClienteService clienteService;

    @Autowired
    private ProveedorServiceImpl proveedorService;

    @Autowired
    private PedidoServiceImpl pedidoService;

    @Autowired
    private IUploadFileService uploadFileService;

    @Autowired
    private GoogleDriveService googleDriveService;

    @Autowired
    private ArchivoAdjuntoService archivoAdjuntoService;

    @Autowired
    private CloudinaryService cloudinaryService;


    @Autowired
    private PedidoMapper pedidoMapper;
    @RequestMapping(value = {"/listarPedidos"}, method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> listar(@RequestParam(name = "page", defaultValue = "0") int page) {
        Pageable pageRequest = PageRequest.of(page, Integer.MAX_VALUE);
        Page<Pedido> pedidos = pedidoService.findAll(pageRequest);

        List<Map<String, Object>> pedidosData = pedidos.getContent().stream().map(p -> {
            Map<String, Object> pedidoData = new HashMap<>();
            pedidoData.put("npedido", p.getNpedido());
            pedidoData.put("cliente", p.getCliente().getNombre()); // ✅ Siempre como String
            pedidoData.put("tipoPedido", p.getTipoPedido());
            pedidoData.put("estado", p.getEstado());
            pedidoData.put("fecha", p.getDfecha());
            pedidoData.put("grupo", p.getGrupo());
            pedidoData.put("subgrupo", p.getSubgrupo());
            return pedidoData;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("recordsTotal", pedidos.getTotalElements());
        response.put("recordsFiltered", pedidos.getTotalElements());
        response.put("data", pedidosData); // ✅ Misma estructura que en `buscar()`

        return response;
    }

    @PostMapping("/buscar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> buscar(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "cliente", defaultValue = "") String cliente,
            @RequestParam(name = "estado", defaultValue = "") String estado,
            @RequestParam(name = "tipoPedido", defaultValue = "") String tipoPedido,
            @RequestParam(name = "grupo", defaultValue = "") String grupo,
            @RequestParam(name = "subgrupo", defaultValue = "") String subgrupo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            Pageable pageable) {

        Date fechaDesdeDate = (fechaDesde != null) ? Date.from(fechaDesde.atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;
        Date fechaHastaDate = (fechaHasta != null) ? Date.from(fechaHasta.atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;

        Pageable pageRequest = PageRequest.of(page, 6);
        Page<Pedido> pedidos = pedidoService.buscarPedidos(cliente, tipoPedido, estado, grupo, subgrupo, fechaDesdeDate, fechaHastaDate, pageRequest);

        List<Map<String, Object>> pedidosData = pedidos.getContent().stream().map(p -> {
            Map<String, Object> pedidoData = new HashMap<>();
            pedidoData.put("npedido", p.getNpedido());
            pedidoData.put("cliente", p.getCliente().getNombre()); // ✅ Siempre como String
            pedidoData.put("tipoPedido", p.getTipoPedido());
            pedidoData.put("estado", p.getEstado());
            pedidoData.put("fecha", p.getDfecha());
            pedidoData.put("grupo", p.getGrupo());
            pedidoData.put("subgrupo", p.getSubgrupo());
            return pedidoData;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("draw", page);
        response.put("recordsTotal", pedidos.getTotalElements());
        response.put("recordsFiltered", pedidos.getTotalElements());
        response.put("data", pedidosData); // ✅ Misma estructura que en `listar()`

        return ResponseEntity.ok(response);
    }



}
