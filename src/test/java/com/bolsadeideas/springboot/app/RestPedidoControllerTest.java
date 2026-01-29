package com.bolsadeideas.springboot.app;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.bolsadeideas.springboot.app.models.entity.Cliente;
import com.bolsadeideas.springboot.app.models.entity.Pedido;
import com.bolsadeideas.springboot.app.models.service.PedidoServiceImpl;

@SpringBootTest
@AutoConfigureMockMvc
public class RestPedidoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PedidoServiceImpl pedidoService;

    @Test
    @WithMockUser
    public void testListarPedidos() throws Exception {
        // Arrange
        List<Pedido> pedidos = new ArrayList<>();
        Pedido p = new Pedido();
        p.setNpedido(1L);
        p.setRef("REF123");
        p.setTipoPedido("Reparacion");
        p.setEstado("PENDIENTE");
        
        Cliente c = new Cliente();
        c.setNombre("Juan");
        p.setCliente(c);
        
        pedidos.add(p);
        
        PageImpl<Pedido> page = new PageImpl<>(pedidos);
        
        when(pedidoService.findAllPedidos(any(Pageable.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/pedido/listarPedidos")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].npedido").value(1))
                .andExpect(jsonPath("$.data[0].cliente").value("Juan"))
                .andExpect(jsonPath("$.recordsTotal").value(1));
    }

    @Test
    @WithMockUser
    public void testListarPedidosError() throws Exception {
        // Simular un error en el servicio
        when(pedidoService.findAllPedidos(any(Pageable.class))).thenThrow(new RuntimeException("Error de base de datos"));

        mockMvc.perform(get("/api/pedido/listarPedidos")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value("Error de base de datos"))
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
