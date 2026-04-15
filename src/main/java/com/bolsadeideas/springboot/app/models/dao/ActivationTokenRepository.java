package com.bolsadeideas.springboot.app.models.dao;

import com.bolsadeideas.springboot.app.models.entity.ActivationToken;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ActivationTokenRepository extends CrudRepository<ActivationToken, Long> {
    Optional<ActivationToken> findByTokenHash(String tokenHash);
}
