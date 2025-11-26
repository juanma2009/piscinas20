package com.bolsadeideas.springboot.app.models.service.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisTestService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    public void testConnection() {
        // Verificamos si la conexión a Redis funciona correctamente
        String value = redisTemplate.opsForValue().get("testKey");
        System.out.println("Valor de testKey en Redis: " + value);
    }
}
