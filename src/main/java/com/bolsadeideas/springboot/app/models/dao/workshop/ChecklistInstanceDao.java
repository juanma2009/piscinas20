package com.bolsadeideas.springboot.app.models.dao.workshop;

import com.bolsadeideas.springboot.app.models.entity.workshop.ChecklistInstance;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import java.util.List;
import java.util.Optional;

public interface ChecklistInstanceDao extends CrudRepository<ChecklistInstance, Long> {
    @Query("SELECT DISTINCT ci FROM ChecklistInstance ci JOIN FETCH ci.template LEFT JOIN FETCH ci.items WHERE ci.pedido.npedido = ?1")
    List<ChecklistInstance> findByPedidoNpedido(Long npedido);

    @Query("SELECT ci FROM ChecklistInstance ci WHERE ci.pedido.npedido = ?1 AND ci.template.id = ?2")
    Optional<ChecklistInstance> findByPedidoAndTemplate(Long pedidoNpedido, Long templateId);

    @Query("SELECT ci FROM ChecklistInstance ci JOIN FETCH ci.pedido JOIN FETCH ci.template")
    List<ChecklistInstance> findAllWithRelations();
}
