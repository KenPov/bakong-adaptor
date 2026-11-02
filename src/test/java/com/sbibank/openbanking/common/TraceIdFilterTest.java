package com.sbibank.openbanking.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.sbibank.openbanking.common.web.TraceIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @Test
    void generatesTraceIdWhenHeaderAbsent() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = (req, res) -> {};

        filter.doFilter(request, response, filterChain);

        String traceId = response.getHeader(TraceIdFilter.TRACE_ID_HEADER);
        assertNotNull(traceId);
        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
        assertEquals("DENY", response.getHeader("X-Frame-Options"));
    }

    @Test
    void preservesExistingTraceIdWhenHeaderPresent() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "custom-trace-12345");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = (req, res) -> {};

        filter.doFilter(request, response, filterChain);

        assertEquals("custom-trace-12345", response.getHeader(TraceIdFilter.TRACE_ID_HEADER));
    }
}
