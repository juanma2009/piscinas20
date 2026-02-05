package com.bolsadeideas.springboot.app.controllers;

import com.bolsadeideas.springboot.app.models.entity.workshop.ChecklistInstance;
import com.bolsadeideas.springboot.app.models.entity.workshop.ChecklistItemInstance;
import com.bolsadeideas.springboot.app.models.service.workshop.WorkshopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workshop")
public class RestWorkshopController {

    @Autowired
    private WorkshopService workshopService;

    @GetMapping("/orders/{id}/checklists")
    public List<ChecklistInstance> getChecklists(@PathVariable Long id) {
        return workshopService.getChecklistsByPedido(id);
    }

    @PatchMapping("/checklist-items/{itemInstanceId}")
    public ChecklistItemInstance updateChecklistItem(
            @PathVariable Long itemInstanceId,
            @RequestBody Map<String, Object> updates,
            Principal principal) {
        String username = (principal != null) ? principal.getName() : "anonymous";
        return workshopService.updateChecklistItem(itemInstanceId, updates, username);
    }

    @PatchMapping("/checklist-items/batch")
    public Map<String, Object> updateChecklistItemsBatch(
            @RequestBody List<Map<String, Object>> batchUpdates,
            Principal principal) {
        String username = (principal != null) ? principal.getName() : "anonymous";
        List<ChecklistItemInstance> updatedItems = workshopService.updateChecklistItemsBatch(batchUpdates, username);
        
        return Map.of(
            "success", true,
            "totalRequested", batchUpdates.size(),
            "totalUpdated", updatedItems.size(),
            "items", updatedItems
        );
    }

    @GetMapping("/debug/orders/{id}/checklists-info")
    public Map<String, Object> getChecklistsDebug(@PathVariable Long id) {
        List<ChecklistInstance> instances = workshopService.getChecklistsByPedido(id);
        return Map.of(
            "pedidoId", id,
            "totalChecklists", instances.size(),
            "checklists", instances.stream().map(ci -> Map.of(
                "id", ci.getId(),
                "templateCode", ci.getTemplate().getCode(),
                "templateName", ci.getTemplate().getName(),
                "totalItems", ci.getItems().size(),
                "itemsWithValue", ci.getItems().stream().filter(item -> item.getValue() != null && !item.getValue().isEmpty()).count()
            )).toList()
        );
    }
}
