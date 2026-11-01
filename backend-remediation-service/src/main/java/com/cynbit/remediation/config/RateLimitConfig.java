package com.cynbit.remediation.config;

import com.cynbit.remediation.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitConfig implements WebMvcConfigurer {

    private final RateLimiterService rateLimiterService;

    public RateLimitConfig(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RateLimitInterceptor(rateLimiterService))
                .addPathPatterns("/api/v1/inquiries/**", "/api/v1/careers/**");
    }

    public static class RateLimitInterceptor implements HandlerInterceptor {
        private final RateLimiterService rateLimiterService;

        public RateLimitInterceptor(RateLimiterService rateLimiterService) {
            this.rateLimiterService = rateLimiterService;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            String clientIp = rateLimiterService.resolveClientIp(request);
            String uri = request.getRequestURI();

            RateLimiterService.ConsumptionResult result = rateLimiterService.tryConsume(clientIp, uri);
            if (!result.isAllowed()) {
                response.setHeader("Retry-After", String.valueOf(result.getRetryAfterSeconds()));
                throw new RateLimitExceededException(
                        "Rate limit exceeded for endpoint: " + uri + ". Please try again in " + result.getRetryAfterSeconds() + " seconds.",
                        result.getRetryAfterSeconds()
                );
            }
            return true;
        }
    }

    @org.springframework.stereotype.Service
    public static class RateLimiterService {

        private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
        private final int inquiryLimit;
        private final int careerLimit;
        private final Set<String> trustedProxies;

        public RateLimiterService(
                @Value("${app.rate-limiting.inquiry-limit-per-5-minutes:10}") int inquiryLimit,
                @Value("${app.rate-limiting.career-limit-per-15-minutes:5}") int careerLimit,
                @Value("${app.rate-limiting.trusted-proxies:127.0.0.1,0:0:0:0:0:0:0:1,::1}") List<String> trustedProxies) {

            if (inquiryLimit <= 0) {
                throw new IllegalArgumentException("Inquiry rate limit capacity must be strictly positive (> 0)");
            }
            if (careerLimit <= 0) {
                throw new IllegalArgumentException("Career rate limit capacity must be strictly positive (> 0)");
            }

            this.inquiryLimit = inquiryLimit;
            this.careerLimit = careerLimit;
            this.trustedProxies = new HashSet<>(trustedProxies != null ? trustedProxies : List.of());
        }

        /**
         * Resolves the real client IP securely.
         * Only trusts X-Forwarded-For headers if the immediate connection originates from an explicitly trusted proxy.
         */
        public String resolveClientIp(HttpServletRequest request) {
            String remoteAddr = request.getRemoteAddr();
            if (remoteAddr != null && trustedProxies.contains(remoteAddr.trim())) {
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                    return xForwardedFor.split(",")[0].trim();
                }
            }
            return remoteAddr != null ? remoteAddr : "unknown";
        }

        public ConsumptionResult tryConsume(String clientIp, String endpoint) {
            String key = clientIp + ":" + getEndpointCategory(endpoint);
            TokenBucket bucket = buckets.computeIfAbsent(key, k -> createBucketForEndpoint(endpoint));
            return bucket.tryConsume();
        }

        /**
         * Evicts stale token buckets that have been inactive longer than maxIdleMs.
         * In a clustered production environment, this in-memory store should be replaced
         * with a distributed Redis-based rate limiter (e.g. Redisson or Bucket4j-redis).
         */
        public void cleanupStaleBuckets(long maxIdleMs) {
            long now = System.currentTimeMillis();
            buckets.entrySet().removeIf(entry -> entry.getValue().isStale(now, maxIdleMs));
        }

        public int getActiveBucketCount() {
            return buckets.size();
        }

        private String getEndpointCategory(String endpoint) {
            if (endpoint != null && endpoint.contains("career")) {
                return "career";
            }
            return "inquiry";
        }

        private TokenBucket createBucketForEndpoint(String endpoint) {
            if (endpoint != null && endpoint.contains("career")) {
                // 5 requests per 15 minutes = 5 tokens max
                return new TokenBucket(careerLimit, 15 * 60 * 1000L);
            }
            // 10 requests per 5 minutes = 10 tokens max
            return new TokenBucket(inquiryLimit, 5 * 60 * 1000L);
        }

        public static class ConsumptionResult {
            private final boolean allowed;
            private final long retryAfterSeconds;

            public ConsumptionResult(boolean allowed, long retryAfterSeconds) {
                this.allowed = allowed;
                this.retryAfterSeconds = retryAfterSeconds;
            }

            public boolean isAllowed() { return allowed; }
            public long getRetryAfterSeconds() { return retryAfterSeconds; }
        }
    }

    public static class TokenBucket {
        private final int capacity;
        private final long refillIntervalMs;
        private int tokens;
        private long lastRefillTimestamp;
        private long lastAccessTimestamp;

        public TokenBucket(int capacity, long refillWindowMs) {
            if (capacity <= 0) {
                throw new IllegalArgumentException("TokenBucket capacity must be strictly positive (> 0)");
            }
            if (refillWindowMs <= 0) {
                throw new IllegalArgumentException("TokenBucket refillWindowMs must be strictly positive (> 0)");
            }

            this.capacity = capacity;
            this.refillIntervalMs = Math.max(1L, refillWindowMs / capacity);
            this.tokens = capacity;
            long now = System.currentTimeMillis();
            this.lastRefillTimestamp = now;
            this.lastAccessTimestamp = now;
        }

        public synchronized RateLimiterService.ConsumptionResult tryConsume() {
            long now = System.currentTimeMillis();
            lastAccessTimestamp = now;
            refill(now);

            if (tokens > 0) {
                tokens--;
                return new RateLimiterService.ConsumptionResult(true, 0);
            }

            long timeUntilNextRefillMs = Math.max(0, refillIntervalMs - (now - lastRefillTimestamp));
            long retryAfterSeconds = Math.max(1L, (timeUntilNextRefillMs + 999) / 1000);
            return new RateLimiterService.ConsumptionResult(false, retryAfterSeconds);
        }

        private void refill(long now) {
            long elapsed = now - lastRefillTimestamp;
            if (elapsed >= refillIntervalMs) {
                int tokensToAdd = (int) (elapsed / refillIntervalMs);
                if (tokensToAdd > 0) {
                    tokens = Math.min(capacity, tokens + tokensToAdd);
                    lastRefillTimestamp += (long) tokensToAdd * refillIntervalMs;
                }
            }
        }

        public synchronized int getAvailableTokens() {
            refill(System.currentTimeMillis());
            return tokens;
        }

        public synchronized boolean isStale(long now, long maxIdleMs) {
            return (now - lastAccessTimestamp) > maxIdleMs;
        }
    }
}
