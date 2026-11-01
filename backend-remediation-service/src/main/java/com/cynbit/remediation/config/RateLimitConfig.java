package com.cynbit.remediation.config;

import com.cynbit.remediation.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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
            String clientIp = resolveClientIp(request);
            String uri = request.getRequestURI();

            if (!rateLimiterService.tryConsume(clientIp, uri)) {
                throw new RateLimitExceededException("Rate limit exceeded for endpoint: " + uri + ". Please try again later.");
            }
            return true;
        }

        private String resolveClientIp(HttpServletRequest request) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                return xForwardedFor.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
    }

    @org.springframework.stereotype.Service
    public static class RateLimiterService {

        private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
        private final int inquiryLimit;
        private final int careerLimit;

        public RateLimiterService(
                @Value("${app.rate-limiting.inquiry-limit-per-5-minutes:10}") int inquiryLimit,
                @Value("${app.rate-limiting.career-limit-per-15-minutes:5}") int careerLimit) {
            this.inquiryLimit = inquiryLimit;
            this.careerLimit = careerLimit;
        }

        public boolean tryConsume(String clientIp, String endpoint) {
            String key = clientIp + ":" + getEndpointCategory(endpoint);
            TokenBucket bucket = buckets.computeIfAbsent(key, k -> createBucketForEndpoint(endpoint));
            return bucket.tryConsume();
        }

        private String getEndpointCategory(String endpoint) {
            if (endpoint.contains("career")) {
                return "career";
            }
            return "inquiry";
        }

        private TokenBucket createBucketForEndpoint(String endpoint) {
            if (endpoint.contains("career")) {
                // 5 requests per 15 minutes = 5 tokens max, refill 1 token every 180 seconds
                return new TokenBucket(careerLimit, 15 * 60 * 1000L);
            }
            // 10 requests per 5 minutes = 10 tokens max, refill 1 token every 30 seconds
            return new TokenBucket(inquiryLimit, 5 * 60 * 1000L);
        }
    }

    public static class TokenBucket {
        private final int capacity;
        private final long refillIntervalMs;
        private final AtomicInteger tokens;
        private final AtomicLong lastRefillTimestamp;

        public TokenBucket(int capacity, long refillWindowMs) {
            this.capacity = capacity;
            this.refillIntervalMs = refillWindowMs / capacity;
            this.tokens = new AtomicInteger(capacity);
            this.lastRefillTimestamp = new AtomicLong(System.currentTimeMillis());
        }

        public synchronized boolean tryConsume() {
            refill();
            if (tokens.get() > 0) {
                tokens.decrementAndGet();
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long last = lastRefillTimestamp.get();
            long elapsed = now - last;

            if (elapsed > refillIntervalMs) {
                int tokensToAdd = (int) (elapsed / refillIntervalMs);
                if (tokensToAdd > 0) {
                    tokens.updateAndGet(current -> Math.min(capacity, current + tokensToAdd));
                    lastRefillTimestamp.set(now);
                }
            }
        }

        public int getAvailableTokens() {
            refill();
            return tokens.get();
        }
    }
}
