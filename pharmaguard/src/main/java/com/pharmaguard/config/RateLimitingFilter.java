package com.pharmaguard.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(2)
public class RateLimitingFilter implements Filter {

    private static final int MAX_AUTH_REQUESTS_PER_MINUTE = 20;
    private static final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();

    private static class RequestCounter {
        final long resetTime;
        final AtomicInteger count;

        RequestCounter(long resetTime) {
            this.resetTime = resetTime;
            this.count = new AtomicInteger(1);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String uri = httpRequest.getRequestURI();

        // Rate limit sensitive endpoints (/login POST, /signup POST, /api/ocr)
        if ("POST".equalsIgnoreCase(httpRequest.getMethod()) && (uri.contains("/login") || uri.contains("/signup") || uri.contains("/ocr"))) {
            String clientIp = getClientIP(httpRequest);
            long currentTime = System.currentTimeMillis();

            RequestCounter counter = requestCounts.compute(clientIp, (ip, existing) -> {
                if (existing == null || currentTime > existing.resetTime) {
                    return new RequestCounter(currentTime + 60000); // 1 minute window
                }
                existing.count.incrementAndGet();
                return existing;
            });

            if (counter.count.get() > MAX_AUTH_REQUESTS_PER_MINUTE) {
                httpResponse.setStatus(429); // 429 Too Many Requests
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write("{\"success\":false, \"error\": {\"code\":\"RATE_LIMIT_EXCEEDED\", \"message\":\"Too many requests. Please try again in 1 minute.\"}}");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
