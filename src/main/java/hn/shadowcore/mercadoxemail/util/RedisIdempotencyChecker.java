package hn.shadowcore.mercadoxemail.util;


import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RedisIdempotencyChecker {

    private final StringRedisTemplate redisTemplate;
    private static final String PREFIX = "eventId:";

    public boolean isDuplicate(UUID eventId) {
        Boolean alreadyExists = redisTemplate.hasKey(PREFIX + eventId);
        if(Boolean.TRUE.equals(alreadyExists)) {
            return true;
        }
        redisTemplate.opsForValue().set(getKey(eventId), eventId.toString(), Duration.ofHours(24));
        return false;
    }

    private String getKey(UUID uuid) {
        return PREFIX + uuid;
    }

}
