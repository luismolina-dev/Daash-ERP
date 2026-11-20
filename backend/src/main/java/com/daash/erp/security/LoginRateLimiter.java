package com.daash.erp.security;

import com.daash.erp.web.BusinessException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Limitador en memoria, seguro para concurrencia: 5 fallos / 15 min por IP y por cuenta. */
@Component
public class LoginRateLimiter {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final ConcurrentHashMap<String, Bucket> ipBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> accountBuckets = new ConcurrentHashMap<>();

    public void check(String ip, String email) {
        if (isBlocked(ipBuckets, key(ip)) || isBlocked(accountBuckets, key(email))) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos fallidos. Intente nuevamente en 15 minutos.");
        }
    }

    public void recordFailure(String ip, String email) {
        ipBuckets.compute(key(ip), (k, bucket) -> bump(bucket));
        accountBuckets.compute(key(email), (k, bucket) -> bump(bucket));
    }

    public void clear(String ip, String email) {
        ipBuckets.remove(key(ip));
        accountBuckets.remove(key(email));
    }

    public void reset() {
        ipBuckets.clear();
        accountBuckets.clear();
    }

    private boolean isBlocked(ConcurrentHashMap<String, Bucket> buckets, String key) {
        Bucket bucket = buckets.get(key);
        return bucket != null && bucket.isBlocked(Instant.now());
    }

    private Bucket bump(Bucket bucket) {
        Bucket target = bucket == null ? new Bucket() : bucket;
        target.record(Instant.now());
        return target;
    }

    private String key(String value) {
        return value == null ? "unknown" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static final class Bucket {
        private int count;
        private Instant firstAttempt;

        synchronized void record(Instant now) {
            if (firstAttempt == null || !firstAttempt.plus(WINDOW).isAfter(now)) {
                firstAttempt = now;
                count = 0;
            }
            count++;
        }

        synchronized boolean isBlocked(Instant now) {
            return count >= MAX_FAILED_ATTEMPTS && firstAttempt != null && firstAttempt.plus(WINDOW).isAfter(now);
        }
    }
}
