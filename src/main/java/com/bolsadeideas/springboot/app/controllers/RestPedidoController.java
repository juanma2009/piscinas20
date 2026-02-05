package com.bolsadeideas.springboot.app.controllers;


import com.bolsadeideas.springboot.app.apigoogledrice.GoogleDriveService;
import com.bolsadeideas.springboot.app.models.dto.mapper.PedidoMapper;
import com.bolsadeideas.springboot.app.models.entity.Pedido;
import com.bolsadeideas.springboot.app.models.service.*;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.logging.Log;
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
@Log4j2
public class RestPedidoController {

    Log logger = org.apache.commons.logging.LogFactory.getLog(getClass());
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

@RequestMapping(value = "/listarPedidos", method = RequestMethod.GET)
@ResponseBody
public Map<String, Object> listar(@RequestParam(name = "page", defaultValue = "0") int page) {
    Pageable pageRequest = PageRequest.of(page, Integer.MAX_VALUE);
    Page<Pedido> pedidos = pedidoService.findAllPedidos(pageRequest);
    log.info(pedidos.getTotalElements() + " pedidos encontrados");

    List<Map<String, Object>> pedidosData = pedidos.getContent().stream().map(p -> {
        Map<String, Object> pedidoData = new HashMap<>();
        pedidoData.put("npedido", p.getNpedido());
        pedidoData.put("ref", p.getRef());
        pedidoData.put("cliente", p.getCliente() != null ? p.getCliente().getNombre() : "Cliente no disponible");
        pedidoData.put("tipoPedido", p.getTipoPedido());
        pedidoData.put("estado", p.getEstado());
        pedidoData.put("fechaFinalizado", p.getFechaFinalizado());
        pedidoData.put("fechaEntrega", p.getFechaEntrega());
        pedidoData.put("metal", p.getGrupo());
        pedidoData.put("pieza", p.getPieza());
        pedidoData.put("tipo", p.getTipo());
        pedidoData.put("cobrado", p.getCobrado());
        return pedidoData;
    }).collect(Collectors.toList());

    Map<String, Object> response = new HashMap<>();
    response.put("recordsTotal", pedidos.getTotalElements());
    response.put("recordsFiltered", pedidos.getTotalElements());
    response.put("data", pedidosData);

    return response;
}

    @RequestMapping(value = {"/listarPedidosClientes"}, method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> listarPorCliente(
            @RequestParam(name = "idCliente") Long idCliente,
            @RequestParam(name = "page", defaultValue = "0") int page) {

        Pageable pageRequest = PageRequest.of(page, Integer.MAX_VALUE);
        Page<Pedido> pedidos = pedidoService.getPedidosById(idCliente, pageRequest);

        List<Map<String, Object>> pedidosData = pedidos.getContent().stream().map(p -> {
            Map<String, Object> pedidoData = new HashMap<>();
            pedidoData.put("npedido", p.getNpedido());
            pedidoData.put("ref", p.getRef());
            pedidoData.put("cliente", p.getCliente().getNombre());
            pedidoData.put("tipoPedido", p.getTipoPedido());
            pedidoData.put("estado", p.getEstado());
            pedidoData.put("fechaFinalizado", p.getFechaFinalizado());
            pedidoData.put("fechaEntrega", p.getFechaEntrega());
            pedidoData.put("metal", p.getGrupo());
            pedidoData.put("pieza", p.getPieza());
            pedidoData.put("tipo", p.getTipo());
            pedidoData.put("cobrado", p.getCobrado());

            return pedidoData;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("recordsTotal", pedidos.getTotalElements());
        response.put("recordsFiltered", pedidos.getTotalElements());
        response.put("data", pedidosData);
        return response;
    }

    @PostMapping("/buscar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> buscar(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "cliente", defaultValue = "") Integer id,
            @RequestParam(name = "estado", defaultValue = "") String estado,
            @RequestParam(name = "tipoPedido", defaultValue = "") String tipoPedido,
            @RequestParam(name = "metal", defaultValue = "") String grupo,
            @RequestParam(name = "pieza", defaultValue = "") String pieza,
            @RequestParam(name = "tipo", defaultValue = "") String tipo,
            @RequestParam(name = "ref", defaultValue = "") String ref,
            @RequestParam(name = "activo", defaultValue = "") String activoStr,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            Pageable pageable) {

        Date fechaDesdeDate = (fechaDesde != null) ? Date.from(fechaDesde.atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;
        Date fechaHastaDate = (fechaHasta != null) ? Date.from(fechaHasta.atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;

        Pageable pageRequest = PageRequest.of(page, 6);
        Page<Pedido> pedidos = pedidoService.buscarPedidos(id, tipoPedido, estado, grupo, pieza,tipo,ref, fechaDesdeDate, fechaHastaDate, Boolean.valueOf(activoStr), pageRequest);
        
        List<Map<String, Object>> pedidosData = pedidos.getContent().stream().map(p -> {
            Map<String, Object> pedidoData = new HashMap<>();
            pedidoData.put("npedido", p.getNpedido());
            pedidoData.put("cliente", p.getCliente().getNombre());
            pedidoData.put("tipoPedido", p.getTipoPedido());
            pedidoData.put("estado", p.getEstado());
            pedidoData.put("fecha", p.getDfecha());
            pedidoData.put("metal", p.getGrupo());
            pedidoData.put("pieza", p.getPieza());
            pedidoData.put("tipo", p.getTipo());
            pedidoData.put("ref", p.getRef());
            pedidoData.put("cobrado", p.getCobrado());
            pedidoData.put("activo", p.isActivo());
             return pedidoData;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("draw", page);
        response.put("recordsTotal", pedidos.getTotalElements());
        response.put("recordsFiltered", pedidos.getTotalElements());
        response.put("data", pedidosData);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/tipoPorPieza")
    @ResponseBody
    public Map<String, List<String>> getTiposDePieza() {
        Map<String, List<String>> tiposPorPieza = new HashMap<>();
        tiposPorPieza.put("Anillo", Arrays.asList("Anillo", "Alianzas", "1/2 Alianzas", "Solitarios", "Sello"));
        tiposPorPieza.put("Colgante", Arrays.asList("Con Piedra", "Sin Piedra"));
        tiposPorPieza.put("Pulsera", List.of("Pulsera"));
        tiposPorPieza.put("Pendientes", Arrays.asList("Pendiente", "Criollas", "1/2 Criolla", "Aretes", "Largos"));
        tiposPorPieza.put("Aro", Arrays.asList("Aro", "Aro Entorcillado", "Cierre caja", "Aros"));
        tiposPorPieza.put("Broche", List.of("Broche"));
        tiposPorPieza.put("Otros", List.of("Otros"));
        return tiposPorPieza;
    }

    @GetMapping("/estadisticas/anios")
    public ResponseEntity<List<Integer>> getAniosFacturacion(@RequestParam Long clienteId) {
        return ResponseEntity.ok(pedidoService.obtenerAniosConFacturacion(clienteId));
    }

    @GetMapping("/estadisticas/facturacion-mensual")
    public ResponseEntity<Map<String, Double>> getFacturacionMensual(
            @RequestParam Long clienteId,
            @RequestParam int anio) {
        return ResponseEntity.ok(pedidoService.totalFacturadoPorMesAnio(clienteId, anio));
    }

    @GetMapping("/estadisticas/pedidos-por-mes")
    public ResponseEntity<Map<String, Integer>> getPedidosPorMeses(
            @RequestParam Long clienteId,
            @RequestParam int anio) {
        return ResponseEntity.ok(pedidoService.contarPedidosPorMesYAnio(clienteId, anio));
    }

    @GetMapping("/estadisticas/anios-disponibles")
    public ResponseEntity<List<Integer>> getAniosDisponibles(@RequestParam Long clienteId) {
        return ResponseEntity.ok(pedidoService.obtenerAniosConPedidos(clienteId));
    }
}
