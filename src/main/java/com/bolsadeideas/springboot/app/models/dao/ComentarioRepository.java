package com.bolsadeideas.springboot.app.models.dao;

import com.bolsadeideas.springboot.app.models.entity.Comentario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComentarioRepository extends JpaRepository<Comentario, Long> {
    @Query("SELECT c FROM Comentario c WHERE c.pedido.npedido = :pedidoId")
    List<Comentario> findByPedidoId(@Param("pedidoId") Long pedidoId);

}
