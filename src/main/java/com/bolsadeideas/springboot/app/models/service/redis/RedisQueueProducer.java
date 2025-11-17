package com.bolsadeideas.springboot.app.models.service.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisQueueProducer {

        private final StringRedisTemplate redisTemplate;

        public RedisQueueProducer(StringRedisTemplate redisTemplate) {
            this.redisTemplate = redisTemplate;
        }

        // Método para enviar un mensaje a la cola de Redis
        public void sendMessage(String mensaje) {
            redisTemplate.opsForList().leftPush("pedido-queue", mensaje);
            System.out.println("Mensaje enviado a Redis: " + mensaje);
        }

}
