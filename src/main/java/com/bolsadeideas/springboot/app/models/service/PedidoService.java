package com.bolsadeideas.springboot.app.models.service;


import com.bolsadeideas.springboot.app.models.entity.Pedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;


public interface PedidoService {

	public Iterable<Pedido> findAll();

	Map<String, Double> totalFacturadoPorMesAnios(Long clienteId, int anio);

	Map<String, Double> totalFacturadoPorMes(Long clienteId);

	public Page<Pedido> findAll(Pageable pageable);

	public Pedido save(Pedido pedido);

	public Pedido findOne(Long id);

	public void delete(Long id);
	
	public long count();
	
	public Page<Pedido> findByCliente(String cliente,String estado, Pageable pageable);


    public Pedido obtenerUltimoNumeroPedido();

	public List<Pedido> findAllPedidos();

    @Transactional(readOnly = true)
    Page<Pedido> findAllPedidos(Pageable pageable);

    public Page<Pedido> findPedidoByIdClienteAndFinalizados(Long idCliente, Pageable pageable);

	public Page<Pedido>  findAllByCliente(Long idcliente, Pageable pageable);

	public Iterable<Pedido> findAllByClienteAndEstado(String idcliente,String estado);

	public Map<String, Integer> contarPedidosPorMes(Long clienteId) ;

	List<Integer> obtenerAniosConPedidos(Long clienteId);

	Map<String, Integer> contarPedidosPorMesYAnio(Long clienteId, int anio);

	List<Integer> obtenerAniosConFacturacion(Long clienteId);
	Map<String, Double> totalFacturadoPorMesAnio(Long clienteId, int anio);

	//obtenEr pedido por id
	@Query("SELECT p FROM Pedido p JOIN FETCH p.cliente WHERE p.npedido = :id")
	public Pedido findPedidoById(Long id);

}
