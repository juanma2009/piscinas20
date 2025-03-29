package com.bolsadeideas.springboot.app.models.dao;


import com.bolsadeideas.springboot.app.models.entity.Pedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public interface PedidoDao extends PagingAndSortingRepository<Pedido, Long> {





	@Query("select p from Pedido p where p.cliente.nombre =?1 and p.estado = ?2")
	public Iterable<Pedido> findByClienteOrEstadoReport(String cliente, String estado);


	@Query("select p from Pedido p where p.cliente.id = ?1")
	public Page<Pedido> findByClienteId(Long cliente,  Pageable pageable);

	public Pedido findTopByOrderByNpedidoDesc();

	@Query("select  COUNT(*) FROM Pedido ")
	public  long count();

	@Query("select p from Pedido p where p.cliente.id = ?1 and p.estado = 'TERMINADO' and p.facturado=false")
	public Page<Pedido> findPedidosByIdClienteAndTerminado(Long idCliente, Pageable pageable);
}
