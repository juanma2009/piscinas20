package com.bolsadeideas.springboot.app.models.dao.workshop;

import com.bolsadeideas.springboot.app.models.entity.workshop.ChecklistItemInstance;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ChecklistItemInstanceDao extends CrudRepository<ChecklistItemInstance, Long> {
    List<ChecklistItemInstance> findByItemCode(String itemCode);
}
