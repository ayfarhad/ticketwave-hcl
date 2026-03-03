package com.ticketwave.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class IdempotencyFilter extends HttpFilter {
    private final ConcurrentHashMap<String, Long> store = new ConcurrentHashMap<>();
    private static final long TTL = TimeUnit.MINUTES.toMillis(5);

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String key = req.getHeader("Idempotency-Key");
        if (key != null) {
            Long existing = store.get(key);
            long now = System.currentTimeMillis();
            if (existing != null && now - existing < TTL) {
                res.setStatus(HttpServletResponse.SC_CONFLICT);
                res.getWriter().write("Duplicate request");
                return;
            }
            store.put(key, now);
        }
        chain.doFilter(req, res);
    }
}
