package com.schoolmanagement.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** A bounded, per-instance limit. Use a shared gateway limiter for multiple API replicas. */
@Component
public class AuthenticationRateLimit extends OncePerRequestFilter {

    private record Window(long started, int requests) {}

    private final Map<String, Window> windows = new HashMap<>();
    private static final Set<String> ROUTES = Set.of(
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/forgot-password",
        "/api/auth/resend-verification",
        "/api/auth/verify-email",
        "/api/auth/reset-password"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !ROUTES.contains(request.getRequestURI());
    }

    private synchronized boolean allow(String address) {
        long now = System.currentTimeMillis();
        windows.entrySet().removeIf(entry -> now - entry.getValue().started() >= 60_000);
        Window previous = windows.get(address);
        if (previous == null && windows.size() >= 10_000) return false;
        Window next =
            previous == null ? new Window(now, 1) : new Window(previous.started(), previous.requests() + 1);
        windows.put(address, next);
        return next.requests() <= 60;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain
    ) throws ServletException, IOException {
        if (!allow(request.getRemoteAddr())) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType("application/problem+json");
            response
                .getWriter()
                .write(
                    "{\"status\":429,\"title\":\"Too many requests\",\"detail\":\"Wait a minute before trying again.\"}"
                );
            return;
        }
        chain.doFilter(request, response);
    }
}
