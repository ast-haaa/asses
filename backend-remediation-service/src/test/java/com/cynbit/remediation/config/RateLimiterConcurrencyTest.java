package com.cynbit.remediation.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterConcurrencyTest {

    @Test
    @DisplayName("Should handle 50 concurrent requests atomically without race conditions using synchronized TokenBucket")
    void shouldHandleConcurrentRequestsAtomically() throws InterruptedException {
        int capacity = 10;
        long refillWindowMs = 600_000L; // 10 minutes, no refills during test
        RateLimitConfig.TokenBucket bucket = new RateLimitConfig.TokenBucket(capacity, refillWindowMs);

        int totalThreads = 50;
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(totalThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < totalThreads; i++) {
            executor.submit(() -> {
                try {
                    startGate.await(); // Simultaneous burst
                    RateLimitConfig.RateLimiterService.ConsumptionResult result = bucket.tryConsume();
                    if (result.isAllowed()) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                        assertTrue(result.getRetryAfterSeconds() > 0, "Retry-After seconds must be positive");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endGate.countDown();
                }
            });
        }

        // Fire all threads simultaneously
        startGate.countDown();
        boolean finished = endGate.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished, "All threads must complete within 5 seconds");
        assertEquals(10, successCount.get(), "Exactly 10 requests must succeed for capacity 10");
        assertEquals(40, failureCount.get(), "Exactly 40 requests must be rejected with 429 / Retry-After");
        assertEquals(0, bucket.getAvailableTokens(), "Bucket must be exhausted");
    }

    @Test
    @DisplayName("Should refill tokens over elapsed time intervals")
    void shouldRefillTokensOverTime() throws InterruptedException {
        int capacity = 2;
        long refillWindowMs = 200L; // 1 token every 100ms
        RateLimitConfig.TokenBucket bucket = new RateLimitConfig.TokenBucket(capacity, refillWindowMs);

        // Exhaust both tokens
        assertTrue(bucket.tryConsume().isAllowed());
        assertTrue(bucket.tryConsume().isAllowed());
        assertFalse(bucket.tryConsume().isAllowed());
        assertEquals(0, bucket.getAvailableTokens());

        // Wait for 1 token to refill (100ms + buffer)
        Thread.sleep(120);
        assertTrue(bucket.getAvailableTokens() >= 1, "At least 1 token must have refilled");
        assertTrue(bucket.tryConsume().isAllowed(), "Should consume refilled token");

        // Wait for full capacity refill
        Thread.sleep(220);
        assertEquals(2, bucket.getAvailableTokens(), "Tokens should replenish up to capacity");
    }

    @Test
    @DisplayName("Should validate that rate limit capacities and windows are strictly positive")
    void shouldRejectNonPositiveCapacities() {
        assertThrows(IllegalArgumentException.class, () -> new RateLimitConfig.TokenBucket(0, 1000));
        assertThrows(IllegalArgumentException.class, () -> new RateLimitConfig.TokenBucket(-5, 1000));
        assertThrows(IllegalArgumentException.class, () -> new RateLimitConfig.TokenBucket(10, 0));
        assertThrows(IllegalArgumentException.class, () -> new RateLimitConfig.TokenBucket(10, -500));

        assertThrows(IllegalArgumentException.class, () ->
                new RateLimitConfig.RateLimiterService(0, 5, List.of("127.0.0.1")));
        assertThrows(IllegalArgumentException.class, () ->
                new RateLimitConfig.RateLimiterService(10, 0, List.of("127.0.0.1")));
    }

    @Test
    @DisplayName("Should only resolve X-Forwarded-For when connection comes from trusted proxy")
    void shouldOnlyTrustForwardedForFromTrustedProxies() {
        RateLimitConfig.RateLimiterService service = new RateLimitConfig.RateLimiterService(10, 5, List.of("127.0.0.1", "10.0.0.1"));

        // Case 1: Direct client (untrusted remoteAddr) spoofing X-Forwarded-For
        MockHttpServletRequest untrustedRequest = new MockHttpServletRequest();
        untrustedRequest.setRemoteAddr("198.51.100.45"); // untrusted public IP
        untrustedRequest.addHeader("X-Forwarded-For", "203.0.113.195, 198.51.100.45");

        String resolvedUntrusted = service.resolveClientIp(untrustedRequest);
        assertEquals("198.51.100.45", resolvedUntrusted, "Must ignore X-Forwarded-For from untrusted remote address");

        // Case 2: Trusted reverse proxy (e.g. Vercel / Nginx on 127.0.0.1)
        MockHttpServletRequest trustedRequest = new MockHttpServletRequest();
        trustedRequest.setRemoteAddr("127.0.0.1"); // trusted proxy
        trustedRequest.addHeader("X-Forwarded-For", "203.0.113.195, 10.0.0.1");

        String resolvedTrusted = service.resolveClientIp(trustedRequest);
        assertEquals("203.0.113.195", resolvedTrusted, "Must resolve first client IP in X-Forwarded-For from trusted proxy");
    }

    @Test
    @DisplayName("Should evict stale buckets during cleanup")
    void shouldEvictStaleBuckets() {
        RateLimitConfig.RateLimiterService service = new RateLimitConfig.RateLimiterService(10, 5, List.of("127.0.0.1"));

        service.tryConsume("10.0.0.1", "/api/v1/inquiries");
        service.tryConsume("10.0.0.2", "/api/v1/inquiries");
        assertEquals(2, service.getActiveBucketCount());

        // Cleanup with maxIdleMs = -1 (evicts everything accessed in past)
        service.cleanupStaleBuckets(-1);
        assertEquals(0, service.getActiveBucketCount(), "All stale buckets must be evicted");
    }
}
