package com.ticketwave.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationIdFilter extends HttpFilter {
    public static final String CORRELATION_ID = "X-Correlation-Id";

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        try {
            String id = req.getHeader(CORRELATION_ID);
            if (id == null || id.isEmpty()) {
                id = UUID.randomUUID().toString();
            }
            MDC.put(CORRELATION_ID, id);
            res.setHeader(CORRELATION_ID, id);
            chain.doFilter(req, res);
        } finally {
            MDC.remove(CORRELATION_ID);
        }
    }
}
