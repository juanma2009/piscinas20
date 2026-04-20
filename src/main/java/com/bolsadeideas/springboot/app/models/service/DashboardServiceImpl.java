package com.bolsadeideas.springboot.app.models.service;

import com.bolsadeideas.springboot.app.models.dao.*;
import com.bolsadeideas.springboot.app.models.entity.Cita;
import com.bolsadeideas.springboot.app.models.entity.Cliente;
import com.bolsadeideas.springboot.app.models.entity.OrdenProduccion;
import com.bolsadeideas.springboot.app.models.entity.Pedido;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements IDashboardService {

    @Autowired private IClienteDao clienteDao;
    @Autowired private PedidoDao pedidoDao;
    @Autowired private CompraInventarioRepository compraRepo;
    @Autowired private IFacturaDao facturaDao;
    @Autowired private OrdenProduccionRepository ordenRepo;
    @Autowired private CitaRepository citaRepo;

    @Override
    @Transactional(readOnly = true)
    public Double getFacturacionTotal() {
        Double total = facturaDao.findTotalFacturas();
        return total != null ? total : 0.0;
    }

    @Override
    @Transactional(readOnly = true)
    public long getPedidosActivosCount() {
        return pedidoDao.countActivePedidos();
    }

    @Override
    @Transactional(readOnly = true)
    public long getClientesNuevosMesCount(Date inicio, Date fin) {
        return clienteDao.countNewClientsInPeriod(inicio, fin);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getComprasMesTotal(LocalDateTime inicio, LocalDateTime fin) {
        Double total = compraRepo.totalGastadoEnPeriodo(inicio, fin);
        return total != null ? total : 0.0;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdenProduccion> getOrdenesRecientes() {
        return ordenRepo.findTop20ByOrderByFechaInicioDesc()
                .stream().limit(5).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> getPedidosRecientes() {
        return pedidoDao.findTop5RecentOrderByNpedidoDesc(PageRequest.of(0, 5));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cliente> getClientesRecientes() {
        return clienteDao.findTop5ByOrderByCreateAtDesc(PageRequest.of(0, 5));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cita> getCitasProximas() {
        return citaRepo.findUpcomingAppointments(LocalDateTime.now(), PageRequest.of(0, 5));
    }

    @Override
    @Transactional(readOnly = true)
    public Double getIngresosDia(LocalDateTime inicio, LocalDateTime fin) {
        Double total = compraRepo.totalGastadoEnPeriodo(inicio, fin);
        return total != null ? total : 0.0;
    }

    @Override
    @Transactional(readOnly = true)
    public Double getFacturacionDia(Date inicio, Date fin) {
        Double total = facturaDao.findTotalFacturasEnPeriodo(inicio, fin);
        return total != null ? total : 0.0;
    }

    @Override
    @Transactional(readOnly = true)
    public  Map<String, Long> getPedidosPorStatus() {
        return pedidoDao.countOrdersByStatus().stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1],
                        (existing, replacement) -> existing
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Double getFacturacionMesTotal(Date inicio, Date fin) {
        Double total = facturaDao.findTotalFacturasEnPeriodo(inicio, fin);
        return total != null ? total : 0.0;
    }
}
