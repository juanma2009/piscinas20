package com.bolsadeideas.springboot.app.models.dao.workshop;

import com.bolsadeideas.springboot.app.models.entity.workshop.OrderStone;
import org.springframework.data.repository.CrudRepository;

public interface OrderStoneDao extends CrudRepository<OrderStone, Long> {
}
