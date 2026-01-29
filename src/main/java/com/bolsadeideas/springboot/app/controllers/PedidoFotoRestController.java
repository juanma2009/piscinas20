package com.bolsadeideas.springboot.app.controllers;
import com.bolsadeideas.springboot.app.models.dao.ArchivoAdjuntoRepository;
import com.bolsadeideas.springboot.app.models.dto.PedidoFotoDto;
import com.bolsadeideas.springboot.app.models.entity.ArchivoAdjunto;
import com.bolsadeideas.springboot.app.models.entity.Pedido;
import com.bolsadeideas.springboot.app.models.service.CloudinaryService;
import com.bolsadeideas.springboot.app.models.service.PedidoService;
import com.bolsadeideas.springboot.app.util.paginator.PageResponse;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/pedidos")
public class PedidoFotoRestController {

    private final PedidoService pedidoService;
    private final ArchivoAdjuntoRepository archivoAdjuntoRepository;
    private final CloudinaryService cloudinaryService;

    public PedidoFotoRestController(PedidoService pedidoService,
                                    ArchivoAdjuntoRepository archivoAdjuntoRepository,
                                    CloudinaryService cloudinaryService) {
        this.pedidoService = pedidoService;
        this.archivoAdjuntoRepository = archivoAdjuntoRepository;
        this.cloudinaryService = cloudinaryService;
    }

    @GetMapping("/fotos")
    public ResponseEntity<PageResponse<PedidoFotoDto>> buscarFotos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,

            @RequestParam(required = false) Integer id,            // id cliente
            @RequestParam(required = false) String tipoPedido,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String grupo,
            @RequestParam(required = false) String pieza,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String ref,

            @RequestParam(required = false) Boolean activo,

            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta
    ) {

        Date fechaDesdeDate = (fechaDesde != null)
                ? Date.from(fechaDesde.atStartOfDay(ZoneId.systemDefault()).toInstant())
                : null;

        Date fechaHastaDate = (fechaHasta != null)
                ? Date.from(fechaHasta.atStartOfDay(ZoneId.systemDefault()).toInstant())
                : null;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "npedido"));

        Page<Pedido> pedidosPage = pedidoService.buscarPedidos(
                id, tipoPedido, estado, grupo, pieza, tipo, ref,
                fechaDesdeDate, fechaHastaDate, activo, pageable
        );

        // IDs de la página
        List<Long> ids = pedidosPage.getContent().stream()
                .map(Pedido::getNpedido)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Traer adjuntos en batch (elige el método correcto según tu repo)
        List<ArchivoAdjunto> adjuntos = !ids.isEmpty() ? archivoAdjuntoRepository.findByPedido_NpedidoIn(ids) : Collections.emptyList();
        // o: archivoAdjuntoRepository.findByPedidoAdjuntoIn(ids);

        // Map: npedido -> primer adjunto (o el que tú decidas)
        Map<Long, ArchivoAdjunto> primerAdjuntoPorPedido = adjuntos.stream()
                .filter(a -> a != null && a.getPedido() != null && a.getPedido().getNpedido() != null)
                .collect(Collectors.toMap(
                        a -> a.getPedido().getNpedido(),
                        Function.identity(),
                        (a1, a2) -> a1 // si hay varios, nos quedamos el primero
                ));

        // Mapear DTO + imagen
        List<PedidoFotoDto> dtoList = pedidosPage.getContent().stream().map(p -> {
            PedidoFotoDto dto = new PedidoFotoDto();
            dto.setNpedido(p.getNpedido());

            if (p.getCliente() != null) {
                dto.setClienteId(Math.toIntExact(p.getCliente().getId()));
                dto.setClienteNombre(p.getCliente().getNombre());
            }

            dto.setDfecha(toLocalDate(p.getDfecha()));
            dto.setFechaEntrega(toLocalDate(p.getFechaEntrega()));

            dto.setTipoPedido(p.getTipoPedido());
            dto.setEstado(p.getEstado());
            dto.setGrupo(p.getGrupo());
            dto.setPieza(p.getPieza());
            dto.setTipo(p.getTipo());
            dto.setRef(p.getRef());
            dto.setActivo(p.isActivo());

            String imagenUrl = "/img/default.jpg";
            try {
                ArchivoAdjunto a = primerAdjuntoPorPedido.get(p.getNpedido());
                if (a != null && a.getUrlCloudinary() != null && !a.getUrlCloudinary().isBlank()) {
                    imagenUrl = cloudinaryService.obtenerImagen(a.getUrlCloudinary());
                }
            } catch (Exception ignored) {
                imagenUrl = "/img/default.jpg";
            }
            dto.setImagenUrl(imagenUrl);

            return dto;
        }).collect(Collectors.toList());

        PageResponse<PedidoFotoDto> response = new PageResponse<>(
                dtoList,
                pedidosPage.getNumber(),
                pedidosPage.getSize(),
                pedidosPage.getTotalElements(),
                pedidosPage.getTotalPages()
        );

        return ResponseEntity.ok(response);
    }

    private LocalDate toLocalDate(Date d) {
        if (d == null) return null;
        if (d instanceof java.sql.Date) {
            return ((java.sql.Date) d).toLocalDate();
        }
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
