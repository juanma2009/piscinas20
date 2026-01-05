package com.bolsadeideas.springboot.app.models.dao;

import com.bolsadeideas.springboot.app.models.entity.Pedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoDao extends PagingAndSortingRepository<Pedido, Long> {

	@Query("select p from Pedido p where p.cliente.nombre =?1 and p.estado = ?2")
	Iterable<Pedido> findByClienteOrEstadoReport(String cliente, String estado);

	/**
	 *
	 * @param id
	 * @param pageable
	 * @return
	 */
	@Query("SELECT p FROM Pedido p WHERE p.npedido = :id")
	Page<Pedido> findPedidoById(Long id, Pageable pageable);

	@Query("SELECT COUNT(p) FROM Pedido p WHERE p.npedido = :id")
	long countPedidoById(Long id);



	@Query("select p from Pedido p where p.cliente.id = ?1")
	Page<Pedido> findByClienteId(Long cliente, Pageable pageable);

	@Query("select p from Pedido p where p.cliente.id = ?1")
	List<Pedido> findByClienteIds(Long cliente);

	Pedido findTopByOrderByNpedidoDesc();

	@Query("select COUNT(p) FROM Pedido p")
	long count();

	@Query("select p from Pedido p where p.cliente.id = ?1 and p.estado = 'TERMINADO' and p.facturado=false")
	Page<Pedido> findPedidosByIdClienteAndTerminado(Long idCliente, Pageable pageable);

	@Query(
			value = "SELECT p FROM Pedido p JOIN FETCH p.cliente where p.activo=true",  // Carga cliente junto con pedido
			countQuery = "SELECT COUNT(p) FROM Pedido p"
	)
	Page<Pedido> findAllWithCliente(Pageable pageable);

	@Query("SELECT p FROM Pedido p JOIN FETCH p.cliente WHERE p.npedido = :id")
	Pedido findByIdWithCliente(Long id);

}
