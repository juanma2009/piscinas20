package com.bolsadeideas.springboot.app.models.dao.workshop;

import com.bolsadeideas.springboot.app.models.entity.workshop.ChecklistTemplate;
import org.springframework.data.repository.CrudRepository;
import java.util.Optional;

public interface ChecklistTemplateDao extends CrudRepository<ChecklistTemplate, Long> {
    Optional<ChecklistTemplate> findByCode(String code);
}
