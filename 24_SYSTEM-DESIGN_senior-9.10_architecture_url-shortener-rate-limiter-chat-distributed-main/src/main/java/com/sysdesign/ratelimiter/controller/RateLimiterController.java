package com.sysdesign.ratelimiter.controller;

import com.sysdesign.ratelimiter.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rate-limit")
public class RateLimiterController {

    private final RateLimiterService service;

    public RateLimiterController(RateLimiterService service) {
        this.service = service;
    }

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testRateLimit(HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        boolean allowed = service.isAllowedTokenBucket(clientIp);

        if (!allowed) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("X-RateLimit-Limit", "100")
                    .header("Retry-After", "1")
                    .body(Map.of("error", "Rate limit exceeded", "clientIp", clientIp));
        }

        return ResponseEntity.ok()
                .header("X-RateLimit-Remaining", String.valueOf(service.getStatus(clientIp).get("tokenBucketAvailable")))
                .body(Map.of("message", "Request allowed", "clientIp", clientIp));
    }

    @GetMapping("/status/{clientId}")
    public ResponseEntity<Map<String, Object>> status(@PathVariable String clientId) {
        return ResponseEntity.ok(service.getStatus(clientId));
    }

    @GetMapping("/sliding-window")
    public ResponseEntity<Map<String, Object>> testSlidingWindow(HttpServletRequest request) {
        String clientId = request.getRemoteAddr();
        boolean allowed = service.isAllowedSlidingWindow(clientId);

        if (!allowed) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Sliding window limit exceeded"));
        }
        return ResponseEntity.ok(Map.of("allowed", true));
    }
}
