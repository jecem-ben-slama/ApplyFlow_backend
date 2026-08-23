package com.applyflow.tracker_api.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    // One bucket per client key. Fine for a single instance; if you ever
    // scale to multiple backend instances behind Nginx, swap this map for
    // a Redis-backed ProxyManager so all instances share the same counts.
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws IOException {

        String path = request.getRequestURI();
        String key = resolveClientKey(request) + ":" + bucketGroup(path);

        Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket(path));

        if (bucket.tryConsume(1)) {
            return true;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setHeader("Retry-After", "60");
        response.getWriter().write(
                "{\"error\":\"Rate limit exceeded. Please slow down and try again shortly.\"}");
        return false;
    }

    /**
     * Different endpoints get different buckets/limits — grouped so e.g. all
     * /api/emails/** calls share one limit, distinct from general reads.
     */
    private String bucketGroup(String path) {
        if (path.startsWith("/api/auth"))
            return "auth";
        if (path.startsWith("/api/emails"))
            return "emails";
        return "default";
    }

    private Bucket newBucket(String path) {
        String group = bucketGroup(path);
        Bandwidth limit = switch (group) {
            // Tight limit on auth endpoints — brute-force protection.
            case "auth" -> Bandwidth.builder()
                    .capacity(5)
                    .refillGreedy(5, Duration.ofMinutes(1))
                    .build();
            // Email sending likely hits a paid provider — keep this tight too.
            case "emails" -> Bandwidth.builder()
                    .capacity(10)
                    .refillGreedy(10, Duration.ofMinutes(1))
                    .build();
            // Everything else — general CRUD/read traffic.
            default -> Bandwidth.builder()
                    .capacity(60)
                    .refillGreedy(60, Duration.ofMinutes(1))
                    .build();
        };
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Prefer the authenticated user's identity over IP, since IP is shared
     * behind NAT/corporate proxies and easy to spoof via headers if trusted
     * blindly. Falls back to IP for unauthenticated endpoints.
     */
    private String resolveClientKey(HttpServletRequest request) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return "user:" + auth.getName();
        }

        // Nginx should already be setting X-Forwarded-For / X-Real-IP for the
        // real client IP, since request.getRemoteAddr() will otherwise just
        // return Nginx's own address. Make sure your Nginx config passes:
        // proxy_set_header X-Real-IP $remote_addr;
        // proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return "ip:" + forwardedFor.split(",")[0].trim();
        }
        return "ip:" + request.getRemoteAddr();
    }
}