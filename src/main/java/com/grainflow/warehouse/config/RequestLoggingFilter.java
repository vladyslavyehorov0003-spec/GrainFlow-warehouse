package com.grainflow.warehouse.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

// Dev-only filter that prints incoming request + outgoing status.
// Runs BEFORE JwtAuthFilter so you can see exactly what the gateway forwarded.
@Slf4j
@Component
@Profile("dev")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String method = request.getMethod();
        String path   = request.getRequestURI();
        String query  = request.getQueryString();
        String fullPath = path + (query != null ? "?" + query : "");

        log.info("──────────────────────────────────────────────────");
        log.info("→ {} {}", method, fullPath);

        // Headers — shown one per line for readability
        Collections.list(request.getHeaderNames()).forEach(name ->
                log.info("    {}: {}", name, request.getHeader(name))
        );

        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            log.info("← {} {} [{}ms]", response.getStatus(), fullPath, elapsed);
            log.info("──────────────────────────────────────────────────");
        }
    }
}
