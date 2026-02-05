package com.bolsadeideas.springboot.app.models.dao.workshop;

import com.bolsadeideas.springboot.app.models.entity.workshop.ChecklistTemplateItem;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ChecklistTemplateItemDao extends CrudRepository<ChecklistTemplateItem, Long> {
    List<ChecklistTemplateItem> findByItemCode(String itemCode);
}
