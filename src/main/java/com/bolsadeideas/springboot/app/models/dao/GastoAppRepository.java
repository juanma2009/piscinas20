package com.bolsadeideas.springboot.app.models.dao;

import com.bolsadeideas.springboot.app.models.entity.GastoApp;
import org.springframework.data.repository.CrudRepository;

public interface GastoAppRepository extends CrudRepository<GastoApp, Long> {
}
