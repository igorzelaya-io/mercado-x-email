package hn.shadowcore.mercadox.email.service.whatsapp;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Guards the HTTP ingestion boundary against Meta's transport-level webhook retries.
 *
 * Meta retries delivery on any non-2xx or timeout. The wamid (WhatsApp's own
 * message ID) is assigned by Meta before the request reaches us, making it stable
 * across retries. We claim it atomically with SETNX before publishing to Kafka so
 * the same message never enters our internal pipeline twice.
 *
 * TTL of 24 hours covers Meta's standard retry window. This is distinct from
 * @KafkaIdempotent (which dedupes on our own eventId at the Kafka consumer boundary).
 */
@Service
@RequiredArgsConstructor
public class WhatsAppWamidDedupeService {

    private static final String PREFIX = "wamid:";
    private static final Duration TTL   = Duration.ofHours(24);

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Atomically claims the wamid for processing.
     *
     * @return true if this is the first time this wamid has been seen (proceed)
     *         false if it was already processed (Meta retry — skip safely)
     */
    public boolean claim(String wamid) {
        Boolean wasAbsent = stringRedisTemplate.opsForValue()
                .setIfAbsent(PREFIX + wamid, "1", TTL);
        return Boolean.TRUE.equals(wasAbsent);
    }
}
