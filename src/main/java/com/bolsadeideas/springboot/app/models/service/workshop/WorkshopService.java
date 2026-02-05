package com.bolsadeideas.springboot.app.models.service.workshop;

import com.bolsadeideas.springboot.app.models.entity.Pedido;
import com.bolsadeideas.springboot.app.models.entity.workshop.ChecklistInstance;
import com.bolsadeideas.springboot.app.models.entity.workshop.ChecklistItemInstance;

import java.util.List;
import java.util.Map;

public interface WorkshopService {
    void seedTemplates();
    void instantiateChecklistsForPedido(Pedido pedido);
    List<ChecklistInstance> getChecklistsByPedido(Long npedido);
    ChecklistInstance getChecklistInstanceByPedidoAndCode(Long npedido, String code);
    ChecklistItemInstance updateChecklistItem(Long itemInstanceId, Map<String, Object> updates, String username);
    List<ChecklistItemInstance> updateChecklistItemsBatch(List<Map<String, Object>> batchUpdates, String username);
    void removeDuplicateChecklistsForPedido(Long npedido);
}
