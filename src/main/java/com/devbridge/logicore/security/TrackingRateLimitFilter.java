package com.devbridge.logicore.security;

import com.devbridge.logicore.enums.ErrorCode;
import com.devbridge.logicore.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limits how often one IP address can call the public tracking endpoint.
 * Fixed window, kept in memory (resets on restart, counted per server instance).
 * Not a @Component on purpose: SecurityConfig creates it, so it runs only once.
 */
public class TrackingRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TrackingRateLimitFilter.class);
    private static final int CLEANUP_THRESHOLD = 10000;

    private static class Window {
        private final long startMillis;
        private final int count;

        private Window(long startMillis, int count) {
            this.startMillis = startMillis;
            this.count = count;
        }
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<String, Window>();
    private final int maxRequests;
    private final long windowMillis;
    private final HandlerExceptionResolver exceptionResolver;

    public TrackingRateLimitFilter(int maxRequests, int windowSeconds, HandlerExceptionResolver exceptionResolver) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowSeconds * 1000L;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        boolean isTrackingGet = "GET".equals(request.getMethod())
                && request.getRequestURI().startsWith("/api/tracking/");
        return !isTrackingGet;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // getRemoteAddr is the direct caller. Behind a proxy or load balancer, configure the
        // server to use the forwarded address instead of trusting the header here.
        final String ip = request.getRemoteAddr();
        final long now = System.currentTimeMillis();

        Window window = windows.compute(ip, (key, old) -> {
            if (old == null || now - old.startMillis >= windowMillis) {
                return new Window(now, 1);
            }
            return new Window(old.startMillis, old.count + 1);
        });

        if (windows.size() > CLEANUP_THRESHOLD) {
            windows.entrySet().removeIf(e -> now - e.getValue().startMillis >= windowMillis);
        }

        if (window.count > maxRequests) {
            long retryAfterSeconds = Math.max(1L, (window.startMillis + windowMillis - now + 999L) / 1000L);
            log.warn("Tracking rate limit reached: retryAfterSeconds={}", retryAfterSeconds);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            // Reuses GlobalExceptionHandler so the 429 has the same JSON as every other error.
            exceptionResolver.resolveException(request, response, null,
                    new BusinessException(ErrorCode.TOO_MANY_REQUESTS));
            return;
        }

        chain.doFilter(request, response);
    }
}