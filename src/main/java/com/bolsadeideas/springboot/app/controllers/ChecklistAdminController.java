package com.bolsadeideas.springboot.app.controllers;

import com.bolsadeideas.springboot.app.models.dao.workshop.ChecklistInstanceDao;
import com.bolsadeideas.springboot.app.models.entity.workshop.ChecklistInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/checklist-admin")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class ChecklistAdminController {

    private static final Logger log = LoggerFactory.getLogger(ChecklistAdminController.class);

    @Autowired
    private ChecklistInstanceDao instanceDao;

    @PostMapping("/cleanup-duplicates")
    @Transactional
    public ResponseEntity<Map<String, Object>> cleanupDuplicates() {
        log.info("Manual cleanup triggered via API endpoint");

        List<ChecklistInstance> allInstances = instanceDao.findAllWithRelations();
        int totalInstances = allInstances.size();

        Map<String, List<ChecklistInstance>> grouped = new HashMap<>();

        for (ChecklistInstance instance : allInstances) {
            String key = instance.getPedido().getNpedido() + "_" + instance.getTemplate().getId();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(instance);
        }

        int duplicatesRemoved = 0;
        List<Long> deletedIds = new ArrayList<>();

        for (Map.Entry<String, List<ChecklistInstance>> entry : grouped.entrySet()) {
            List<ChecklistInstance> duplicates = entry.getValue();
            if (duplicates.size() > 1) {
                duplicates.sort(Comparator.comparing(ChecklistInstance::getId));

                for (int i = 1; i < duplicates.size(); i++) {
                    ChecklistInstance toDelete = duplicates.get(i);
                    deletedIds.add(toDelete.getId());
                    instanceDao.deleteById(toDelete.getId());
                    duplicatesRemoved++;
                }
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("totalInstancesBefore", totalInstances);
        response.put("duplicatesRemoved", duplicatesRemoved);
        response.put("deletedIds", deletedIds);
        response.put("totalInstancesAfter", totalInstances - duplicatesRemoved);

        log.info("Manual cleanup completed. Removed {} duplicates", duplicatesRemoved);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-duplicates")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> verifyDuplicates() {
        List<ChecklistInstance> allInstances = instanceDao.findAllWithRelations();

        Map<String, Integer> duplicateCounts = new HashMap<>();
        Map<String, List<ChecklistInstance>> grouped = new HashMap<>();

        for (ChecklistInstance instance : allInstances) {
            String key = instance.getPedido().getNpedido() + "_" + instance.getTemplate().getId();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(instance);
        }

        int totalDuplicateGroups = 0;
        int totalDuplicateInstances = 0;

        for (Map.Entry<String, List<ChecklistInstance>> entry : grouped.entrySet()) {
            if (entry.getValue().size() > 1) {
                duplicateCounts.put(entry.getKey(), entry.getValue().size());
                totalDuplicateGroups++;
                totalDuplicateInstances += (entry.getValue().size() - 1);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("totalInstances", allInstances.size());
        response.put("hasDuplicates", totalDuplicateGroups > 0);
        response.put("duplicateGroups", totalDuplicateGroups);
        response.put("totalExcessInstances", totalDuplicateInstances);
        response.put("duplicateDetails", duplicateCounts);

        return ResponseEntity.ok(response);
    }
}
