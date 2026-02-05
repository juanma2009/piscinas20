package com.bolsadeideas.springboot.app.models.service.workshop;

import com.bolsadeideas.springboot.app.models.dao.workshop.ChecklistInstanceDao;
import com.bolsadeideas.springboot.app.models.entity.workshop.ChecklistInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
public class ChecklistDataCleanup {

    private static final Logger log = LoggerFactory.getLogger(ChecklistDataCleanup.class);

    @Autowired
    private ChecklistInstanceDao instanceDao;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void cleanupDuplicates() {
        log.info("=== Starting checklist duplicate cleanup ===");

        List<ChecklistInstance> allInstances = instanceDao.findAllWithRelations();
        log.info("Total checklist instances found: {}", allInstances.size());

        Map<String, List<ChecklistInstance>> grouped = new HashMap<>();

        for (ChecklistInstance instance : allInstances) {
            String key = instance.getPedido().getNpedido() + "_" + instance.getTemplate().getId();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(instance);
        }

        int duplicatesRemoved = 0;
        List<Long> idsToDelete = new ArrayList<>();
        
        for (Map.Entry<String, List<ChecklistInstance>> entry : grouped.entrySet()) {
            List<ChecklistInstance> duplicates = entry.getValue();
            if (duplicates.size() > 1) {
                log.warn("Found {} duplicates for pedido-template: {}", duplicates.size(), entry.getKey());
                duplicates.sort(Comparator.comparing(ChecklistInstance::getId));
                
                for (int i = 1; i < duplicates.size(); i++) {
                    ChecklistInstance toDelete = duplicates.get(i);
                    log.info("Marking for deletion: instance ID {} (keeping ID {})", 
                            toDelete.getId(), duplicates.get(0).getId());
                    idsToDelete.add(toDelete.getId());
                    duplicatesRemoved++;
                }
            }
        }

        // Delete all marked instances
        for (Long id : idsToDelete) {
            instanceDao.deleteById(id);
            log.debug("Deleted instance ID: {}", id);
        }

        log.info("=== Cleanup completed. Removed {} duplicate instances ===", duplicatesRemoved);
    }
}
