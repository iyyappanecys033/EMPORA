package com.examly.springapp.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Structured observability filter attaching a correlation ID to every request
 * and emitting latency telemetry for API calls without logging credentials,
 * cookies, tokens, or sensitive payload bodies.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String incoming = request.getHeader(CORRELATION_ID_HEADER);
        String correlationId = sanitizeCorrelationId(incoming);

        MDC.put(MDC_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        long startNanos = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000L;
            String uri = request.getRequestURI();
            if (uri != null && uri.startsWith("/api/")) {
                log.info("api_request correlationId={} method={} path={} status={} durationMs={}",
                        correlationId, request.getMethod(), uri, response.getStatus(), durationMs);
            }
            MDC.remove(MDC_KEY);
        }
    }

    private String sanitizeCorrelationId(String raw) {
        if (raw == null || raw.isBlank()) {
            return UUID.randomUUID().toString();
        }
        String cleaned = raw.replaceAll("[^a-zA-Z0-9\\-_.]", "");
        if (cleaned.isEmpty()) {
            return UUID.randomUUID().toString();
        }
        return cleaned.length() > 64 ? cleaned.substring(0, 64) : cleaned;
    }
}

