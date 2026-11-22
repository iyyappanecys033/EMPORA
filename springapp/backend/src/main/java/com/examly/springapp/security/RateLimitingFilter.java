package com.examly.springapp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * [A06: Insecure Design]
 * Preventive rate-limiting and brute-force protection filter using an in-memory
 * sliding token window.
 * 
 * Guards sensitive authentication vectors:
 * - /api/auth/login
 * - /api/auth/register
 * - /api/auth/refresh
 * 
 * Enforces a strict threshold of max 15 requests per 60-second window per remote IP.
 * Violations immediately return HTTP 429 Too Many Requests with RFC 7807 payload
 * and a standard Retry-After header.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private static final int MAX_REQUESTS_PER_WINDOW = 15;
    private static final long WINDOW_DURATION_SECONDS = 60L;

    private static final class ClientBucket {
        private final AtomicInteger requestCount = new AtomicInteger(0);
        private volatile long windowStartTimeEpoch = Instant.now().getEpochSecond();

        public boolean tryConsume() {
            long currentEpoch = Instant.now().getEpochSecond();
            if (currentEpoch - windowStartTimeEpoch >= WINDOW_DURATION_SECONDS) {
                synchronized (this) {
                    if (currentEpoch - windowStartTimeEpoch >= WINDOW_DURATION_SECONDS) {
                        windowStartTimeEpoch = currentEpoch;
                        requestCount.set(0);
                    }
                }
            }
            return requestCount.incrementAndGet() <= MAX_REQUESTS_PER_WINDOW;
        }

        public long getRetryAfterSeconds() {
            long currentEpoch = Instant.now().getEpochSecond();
            long elapsed = currentEpoch - windowStartTimeEpoch;
            return Math.max(1, WINDOW_DURATION_SECONDS - elapsed);
        }
    }

    private final Map<String, ClientBucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Rate limit strictly authentication mutation endpoints
        if (request.getMethod().equalsIgnoreCase("POST")) {
            return !(path.equals("/api/auth/login") ||
                     path.equals("/api/auth/register") ||
                     path.equals("/api/auth/refresh"));
        }
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientIp = extractClientIp(request);
        ClientBucket bucket = buckets.computeIfAbsent(clientIp, k -> new ClientBucket());

        if (!bucket.tryConsume()) {
            long retryAfter = bucket.getRetryAfterSeconds();
            log.warn("[A06: Insecure Design] Rate limit exceeded for IP: {} on URI: {}. Rejected with 429.",
                    LogSanitizer.sanitize(clientIp), LogSanitizer.sanitize(request.getRequestURI()));

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.getWriter().write(String.format(
                    "{\"type\":\"https://empora.internal/errors/rate-limit-exceeded\"," +
                    "\"title\":\"Too Many Requests\"," +
                    "\"status\":429," +
                    "\"detail\":\"Request rate limit exceeded. Please wait %d seconds before retrying.\"," +
                    "\"retryAfter\":%d}",
                    retryAfter, retryAfter
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.trim().isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
