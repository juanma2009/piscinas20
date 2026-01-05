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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

//todo añadir los nuevos campos metal,pieza,tipo
@RequestMapping(value = "/listarPedidos", method = RequestMethod.GET)
@ResponseBody
public Map<String, Object> listar(@RequestParam(name = "page", defaultValue = "0") int page) {
    // Crear la paginación
    Pageable pageRequest = PageRequest.of(page, Integer.MAX_VALUE);  // Tamaño máximo de la página

    // Obtener los pedidos con cliente cargado
    Page<Pedido> pedidos = pedidoService.findAllPedidos(pageRequest);
    // Log para verificar el número de pedidos obtenidos
    log.info(pedidos.getTotalElements() + " pedidos encontrados");

    // Mapear los pedidos a JSON para enviar al frontend
    List<Map<String, Object>> pedidosData = pedidos.getContent().stream().map(p -> {

        // Log para verificar si el cliente es null o no
        if (p.getCliente() == null) {
            log.warn("Cliente es null para el pedido: " + p.getNpedido());
        } else {
            log.info("Cliente nombre: " + p.getCliente().getNombre()); // Si el cliente está cargado, muestra el nombre
        }

        // Crear el mapa de datos para cada pedido
        Map<String, Object> pedidoData = new HashMap<>();
        pedidoData.put("npedido", p.getNpedido()); // se utiliza para como id para entrar a editar y ver los detalles
        pedidoData.put("ref", p.getRef());
        // Asegúrate de que p.getCliente() no sea null
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

    // Crear la estructura de respuesta para DataTables
    Map<String, Object> response = new HashMap<>();
    response.put("recordsTotal", pedidos.getTotalElements()); // Total de registros
    response.put("recordsFiltered", pedidos.getTotalElements()); // Total de registros
    response.put("data", pedidosData);  // Los datos de la página

    return response;  // Devolver en formato JSON esperado por DataTables
}

    @GetMapping("/reactivar/{id}")
    public String reactivar(@PathVariable Long id, RedirectAttributes flash) {
        Optional<Pedido> optional = Optional.ofNullable(pedidoService.findOne(id));

        if (optional.isEmpty()) {
            flash.addFlashAttribute("error", "El pedido no existe.");
            return "redirect:/pedidos";
        }
        Pedido pedido = optional.get();

        if (pedido.isActivo()) {
            flash.addFlashAttribute("info", "El pedido ya está activo.");
        } else {
            pedido.setActivo(true);  // ← Lo reactivamos
            pedidoService.save(pedido);
            flash.addFlashAttribute("success", "¡Pedido reactivado con éxito!");
        }

        // Redirigimos al detalle del cliente o lista general
        Long idCliente = pedido.getCliente() != null ? pedido.getCliente().getId() : null;
        return  "/pedidos/pedidolistar";
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
        // Crear la respuesta

        Map<String, Object> response = new HashMap<>();
        response.put("recordsTotal", pedidos.getTotalElements());
        response.put("recordsFiltered", pedidos.getTotalElements());
        response.put("data", pedidosData);
        logger.info("respopne lista depedidos"+response);
        return response;
    }

//todo añadir los nuevos campos metal,pieza,tipo

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
            @RequestParam(name = "activo", defaultValue = "") String activoStr,  // Recibimos como String
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            Pageable pageable) {

        Date fechaDesdeDate = (fechaDesde != null) ? Date.from(fechaDesde.atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;
        Date fechaHastaDate = (fechaHasta != null) ? Date.from(fechaHasta.atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;

        Pageable pageRequest = PageRequest.of(page, 6);
        Page<Pedido> pedidos = pedidoService.buscarPedidos(id, tipoPedido, estado, grupo, pieza,tipo,ref, fechaDesdeDate, fechaHastaDate, Boolean.valueOf(activoStr), pageRequest);
        // Convertimos el parámetro "activo" a Boolean (null = no filtrar)
        Boolean activo = null;
        if (!activoStr.isEmpty()) {
            activo = "true".equalsIgnoreCase(activoStr) || "1".equals(activoStr);
        }
        List<Map<String, Object>> pedidosData = pedidos.getContent().stream().map(p -> {
            Map<String, Object> pedidoData = new HashMap<>();
            pedidoData.put("npedido", p.getNpedido());
            pedidoData.put("cliente", p.getCliente().getNombre()); // ✅ Siempre como String
            pedidoData.put("tipoPedido", p.getTipoPedido());
            pedidoData.put("estado", p.getEstado());
            pedidoData.put("fecha", p.getDfecha());
            pedidoData.put("metal", p.getGrupo());
            pedidoData.put("pieza", p.getPieza());
            pedidoData.put("tipo", p.getTipo());
            pedidoData.put("ref", p.getRef());
            pedidoData.put("cobrado", p.getCobrado());
            pedidoData.put("activo", p.isActivo()); // ← Booleano correcto (true/false)
             return pedidoData;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("draw", page);
        response.put("recordsTotal", pedidos.getTotalElements());
        response.put("recordsFiltered", pedidos.getTotalElements());
        response.put("data", pedidosData); // ✅ Misma estructura que en `listar()`

        return ResponseEntity.ok(response);
    }

    //CARGAR EL SELECT TIPO CUANDO SE CAMBIA UNA PIEZA EN LAS BUSQUEDAS
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
//    // OBTENEMOS LAS ESTADISTICAS DE LOS PEDIDOS
//    @GetMapping("/estadisticas/pedidos-por-mes")
//    @ResponseBody
//    public Map<String, Integer> getPedidosPorMes(@RequestParam Long clienteId) {
//        log.info("Cliente ID: {}", clienteId);
//        // Llama al servicio para obtener la cantidad de pedidos por mes
//        log.info("Cantidad de pedidos por mes: {}", pedidoService.contarPedidosPorMes(clienteId));
//        return pedidoService.contarPedidosPorMes(clienteId);
//
//    }

//    // OBTENEMOS LAS ESTADISTICAS DE LOS PEDIDOS
//    @GetMapping("/estadisticas/pedidos-por-mes")
//    @ResponseBody
//    public Map<String, Integer> getFacturacionPorMes(@RequestParam Long clienteId) {
//        log.info("Cliente ID: {}", clienteId);
//        // Llama al servicio para obtener la cantidad de pedidos por mes
//        log.info("Cantidad de pedidos por mes: {}", pedidoService.contarPedidosPorMes(clienteId));
//        return pedidoService.contarPedidosPorMes(clienteId);
//
//    }

     //OBTENEMOS LAS ESTADISTICAS DfacturacionE LOS FACTURACION-ANUAL
    @GetMapping("/estadisticas/facturacionanual")
    @ResponseBody
    public Map<String, Double> getFacturacionAnual(@RequestParam Long clienteId) {
        // Llama al servicio para obtener el total facturado por año
        log.info("Cantidad de pedidos por mes: {}", pedidoService.totalFacturadoPorMes(clienteId));
        return pedidoService.totalFacturadoPorMes(clienteId); // Devuelve {"2021": 12000.0, "2022": 18000.0, ...}
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
        log.info("Cliente ID: {}", clienteId);
        // Llama al servicio para obtener la cantidad de pedidos por mes y año
        log.info("Cantidad de pedidos por mes y año: {}", pedidoService.contarPedidosPorMesYAnio(clienteId, anio));
        return ResponseEntity.ok(pedidoService.contarPedidosPorMesYAnio(clienteId, anio));
    }
    @GetMapping("/estadisticas/anios-disponibles")
    public ResponseEntity<List<Integer>> getAniosDisponibles(@RequestParam Long clienteId) {
        List<Integer> anios = pedidoService.obtenerAniosConPedidos(clienteId);
        log.info("Años disponibles para el cliente {}: {}", clienteId, anios);
        return ResponseEntity.ok(anios);
    }

}
