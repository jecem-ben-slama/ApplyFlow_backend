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

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String path = request.getRequestURI();
        String group = bucketGroup(path);
        String key = resolveClientKey(request) + ":" + group;
        Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket(group));

        if (bucket.tryConsume(1)) {
            return true;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setHeader("Retry-After", retryAfterSeconds(group));
        response.getWriter().write("{\"error\":\"Rate limit exceeded. Please slow down and try again shortly.\"}");
        return false;
    }

    private String bucketGroup(String path) {
        if (path.startsWith("/api/auth"))
            return "auth";
        if (path.startsWith("/api/emails"))
            return "emails";
        return "default";
    }

    private Bucket newBucket(String group) {
        return switch (group) {
            // Allows human multi-tab refreshes or retry loops comfortably;
            // also fronted by a live Google token check, so this isn't the only brake here
            case "auth" -> Bucket.builder()
                    .addLimit(Bandwidth.builder().capacity(20).refillGreedy(20, Duration.ofMinutes(1)).build())
                    .build();
            // Calibrated to align with personal Gmail SMTP rate constraints
            case "emails" -> Bucket.builder()
                    .addLimit(Bandwidth.builder().capacity(2).refillGreedy(2, Duration.ofSeconds(1)).build()) // Burst
                                                                                                              // limit
                    .addLimit(Bandwidth.builder().capacity(15).refillGreedy(15, Duration.ofHours(1)).build()) // Sustained
                                                                                                              // safety
                                                                                                              // wall
                    .build();
            // Default app layout data / navigation traffic
            default -> Bucket.builder()
                    .addLimit(Bandwidth.builder().capacity(100).refillGreedy(100, Duration.ofMinutes(1)).build())
                    .build();
        };
    }

    // Reports the wait time for whichever limit actually bites for this group.
    // "emails" is gated by an hourly sustained cap once the 2/sec burst is spent,
    // so telling the client "retry in 60s" there would just get them 429'd again.
    private String retryAfterSeconds(String group) {
        return switch (group) {
            case "auth" -> "60";
            case "emails" -> "3600";
            default -> "60";
        };
    }

    private String resolveClientKey(HttpServletRequest request) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return "user:" + auth.getName();
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return "ip:" + forwardedFor.split(",")[0].trim();
        }
        return "ip:" + request.getRemoteAddr();
    }
}