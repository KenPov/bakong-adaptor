package com.sbibank.openbanking.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Ensures every incoming HTTP request is tagged with a trace identifier in SLF4J MDC,
 * aligning with the log patterns defined in application-uat.yml and application-prod.yml.
 *
 * <p>Also propagates standard OWASP defense headers on all responses.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_MDC_KEY = "traceId";
    public static final String SPAN_ID_MDC_KEY = "spanId";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String traceId = resolveTraceId(request);

        MDC.put(TRACE_ID_MDC_KEY, traceId);
        MDC.put(SPAN_ID_MDC_KEY, traceId.substring(0, Math.min(traceId.length(), 16)));

        response.setHeader(TRACE_ID_HEADER, traceId);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID_MDC_KEY);
            MDC.remove(SPAN_ID_MDC_KEY);
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String header = request.getHeader(TRACE_ID_HEADER);
        if (!StringUtils.hasText(header)) {
            header = request.getHeader(CORRELATION_ID_HEADER);
        }
        if (!StringUtils.hasText(header)) {
            header = request.getHeader(REQUEST_ID_HEADER);
        }
        return StringUtils.hasText(header) ? header.trim() : UUID.randomUUID().toString();
    }
}
