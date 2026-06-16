package com.patterns.behavioral.chain;

/**
 * CHAIN OF RESPONSIBILITY — İstek zinciri boyunca ilet
 *
 * Kullanım: Spring Security filter chain, Servlet Filter,
 *           middleware pipeline, logging handler zinciri
 */
public class ChainOfResponsibility {

    // ================================================================
    // HTTP Request Validation Pipeline
    // ================================================================
    record HttpRequest(String token, String ip, String body, int contentLength) {}
    record HttpResponse(int status, String message) {}

    static abstract class RequestHandler {
        private RequestHandler next;

        RequestHandler setNext(RequestHandler next) { this.next = next; return next; }

        HttpResponse handle(HttpRequest request) {
            if (next != null) return next.handle(request);
            return new HttpResponse(200, "OK");
        }
    }

    static class AuthenticationHandler extends RequestHandler {
        @Override
        public HttpResponse handle(HttpRequest request) {
            if (request.token() == null || request.token().isBlank()) {
                return new HttpResponse(401, "Unauthorized: Token eksik");
            }
            System.out.println("[AUTH] Token doğrulandı");
            return super.handle(request);
        }
    }

    static class RateLimitHandler extends RequestHandler {
        private int requestCount = 0;
        private final int limit;

        RateLimitHandler(int limit) { this.limit = limit; }

        @Override
        public HttpResponse handle(HttpRequest request) {
            if (++requestCount > limit) {
                return new HttpResponse(429, "Too Many Requests: Limit " + limit);
            }
            System.out.println("[RATE] İstek " + requestCount + "/" + limit);
            return super.handle(request);
        }
    }

    static class IpBlockHandler extends RequestHandler {
        private final java.util.Set<String> blockedIps = java.util.Set.of("10.0.0.1", "192.168.1.99");

        @Override
        public HttpResponse handle(HttpRequest request) {
            if (blockedIps.contains(request.ip())) {
                return new HttpResponse(403, "Forbidden: IP engellendi " + request.ip());
            }
            System.out.println("[IP] Temiz IP: " + request.ip());
            return super.handle(request);
        }
    }

    static class PayloadSizeHandler extends RequestHandler {
        private final int maxSize;

        PayloadSizeHandler(int maxSize) { this.maxSize = maxSize; }

        @Override
        public HttpResponse handle(HttpRequest request) {
            if (request.contentLength() > maxSize) {
                return new HttpResponse(413, "Payload Too Large: max " + maxSize + " bytes");
            }
            System.out.println("[SIZE] Payload uygun: " + request.contentLength() + " bytes");
            return super.handle(request);
        }
    }

    // Pipeline builder
    static RequestHandler buildPipeline() {
        RequestHandler auth    = new AuthenticationHandler();
        RequestHandler ip      = new IpBlockHandler();
        RequestHandler rate    = new RateLimitHandler(5);
        RequestHandler payload = new PayloadSizeHandler(1024);

        auth.setNext(ip).setNext(rate).setNext(payload);
        return auth;
    }

    public static void main(String[] args) {
        System.out.println("=== CHAIN OF RESPONSIBILITY ===\n");

        RequestHandler pipeline = buildPipeline();

        HttpRequest[] requests = {
            new HttpRequest(null, "192.168.1.1", "{}", 50),          // 401
            new HttpRequest("token-abc", "10.0.0.1", "{}", 50),      // 403
            new HttpRequest("token-abc", "192.168.1.1", "{}", 50),   // 200
            new HttpRequest("token-abc", "192.168.1.1", "{}", 2048), // 413
        };

        for (HttpRequest req : requests) {
            System.out.println("İstek: token=" + req.token() + ", ip=" + req.ip());
            HttpResponse res = pipeline.handle(req);
            System.out.println("Yanıt: " + res.status() + " " + res.message() + "\n");
        }
    }
}
