package com.bolsadeideas.springboot.app.models.service;

import com.bolsadeideas.springboot.app.models.entity.Cita;
import com.bolsadeideas.springboot.app.models.entity.Cliente;
import com.bolsadeideas.springboot.app.models.entity.OrdenProduccion;
import com.bolsadeideas.springboot.app.models.entity.Pedido;

import java.util.Date;
import java.util.List;

public interface IDashboardService {
    Double getFacturacionTotal();
    long getPedidosActivosCount();
    long getClientesNuevosMesCount(Date inicio, Date fin);
    Double getComprasMesTotal(java.time.LocalDateTime inicio, java.time.LocalDateTime fin);
    List<OrdenProduccion> getOrdenesRecientes();
    List<Pedido> getPedidosRecientes();
    List<Cliente> getClientesRecientes();
    List<Cita> getCitasProximas();
    Double getIngresosDia(java.time.LocalDateTime inicio, java.time.LocalDateTime fin);
    Double getFacturacionDia(java.util.Date inicio, java.util.Date fin);
    java.util.Map<String, Long> getPedidosPorStatus();
    Double getFacturacionMesTotal(java.util.Date inicio, java.util.Date fin);
    String getExternalMetalsData(String currency, String symbol);
}
