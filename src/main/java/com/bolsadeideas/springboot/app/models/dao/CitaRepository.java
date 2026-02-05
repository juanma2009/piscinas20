package com.bolsadeideas.springboot.app.models.dao;

import com.bolsadeideas.springboot.app.models.entity.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    @Query("select c from Cita c left join fetch c.cliente left join fetch c.pedido order by c.id desc")
    List<Cita> findAllWithEntities();

    @Query("select c from Cita c left join fetch c.cliente left join fetch c.pedido where c.id = ?1")
    Cita findByIdWithEntities(Long id);

    @Query("select c from Cita c left join fetch c.cliente left join fetch c.pedido where c.cliente.id = ?1 order by c.fechaCita desc")
    List<Cita> findByClienteId(Long clienteId);

    @Query("select c from Cita c where c.pedido.npedido = ?1")
    List<Cita> findByPedidoId(Long pedidoId);

    @Query("select c from Cita c where c.fechaCita between ?1 and ?2 order by c.fechaCita asc")
    List<Cita> findByRangoFechas(LocalDateTime inicio, LocalDateTime fin);
}
