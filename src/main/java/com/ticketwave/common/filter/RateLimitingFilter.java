package com.ticketwave.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory rate limiter that allows a fixed number of requests per IP per hour.
 *
 * This avoids the need for an external library like Bucket4j so that the project
 * can build without pulling additional dependencies.
 */
@Component
public class RateLimitingFilter extends HttpFilter {
    private static class State {
        int count;
        Instant windowStart;
    }

    private final Map<String, State> states = new ConcurrentHashMap<>();
    private static final int MAX_PER_HOUR = 100;
    private static final long WINDOW_MILLIS = 60 * 60 * 1000;

    private boolean allow(String key) {
        State s = states.computeIfAbsent(key, k -> {
            State ns = new State();
            ns.count = 0;
            ns.windowStart = Instant.now();
            return ns;
        });

        Instant now = Instant.now();
        if (now.toEpochMilli() - s.windowStart.toEpochMilli() > WINDOW_MILLIS) {
            s.count = 0;
            s.windowStart = now;
        }

        if (s.count < MAX_PER_HOUR) {
            s.count++;
            return true;
        }
        return false;
    }

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String ip = req.getRemoteAddr();
        if (allow(ip)) {
            chain.doFilter(req, res);
        } else {
            // servlet API variant in use doesn't expose the SC_TOO_MANY_REQUESTS constant;
            // use numeric value directly (429)
            res.setStatus(429);
            res.getWriter().write("Too many requests");
        }
    }
}
