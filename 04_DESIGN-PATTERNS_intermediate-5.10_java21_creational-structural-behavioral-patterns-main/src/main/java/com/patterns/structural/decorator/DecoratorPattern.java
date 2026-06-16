package com.patterns.structural.decorator;

/**
 * DECORATOR PATTERN — Davranışı dinamik ekleme
 *
 * Kullanım: Java I/O (BufferedInputStream, GZIPOutputStream),
 *           Spring Security filter chain, HTTP middleware
 *
 * Kalıtım yerine composition: "has-a" > "is-a"
 */
public class DecoratorPattern {

    // Temel arayüz
    interface DataProcessor {
        String process(String data);
    }

    // Temel implementasyon
    static class BaseProcessor implements DataProcessor {
        @Override
        public String process(String data) { return data; }
    }

    // Soyut Decorator
    static abstract class ProcessorDecorator implements DataProcessor {
        protected final DataProcessor wrapped;
        ProcessorDecorator(DataProcessor wrapped) { this.wrapped = wrapped; }
    }

    // Somut Decorator'lar
    static class EncryptionDecorator extends ProcessorDecorator {
        EncryptionDecorator(DataProcessor wrapped) { super(wrapped); }

        @Override
        public String process(String data) {
            String processed = wrapped.process(data);
            return "[ENCRYPTED:" + processed.chars()
                    .map(c -> c + 1)
                    .collect(StringBuilder::new, (sb, c) -> sb.append((char) c), StringBuilder::append) + "]";
        }
    }

    static class CompressionDecorator extends ProcessorDecorator {
        CompressionDecorator(DataProcessor wrapped) { super(wrapped); }

        @Override
        public String process(String data) {
            String processed = wrapped.process(data);
            return "[COMPRESSED:" + processed.length() + "→" + (processed.length() / 2) + "bytes:" + processed + "]";
        }
    }

    static class LoggingDecorator extends ProcessorDecorator {
        LoggingDecorator(DataProcessor wrapped) { super(wrapped); }

        @Override
        public String process(String data) {
            System.out.println("[LOG] İşleme başlıyor: " + data.length() + " karakter");
            String result = wrapped.process(data);
            System.out.println("[LOG] İşlem tamamlandı");
            return result;
        }
    }

    static class CachingDecorator extends ProcessorDecorator {
        private final java.util.Map<String, String> cache = new java.util.HashMap<>();

        CachingDecorator(DataProcessor wrapped) { super(wrapped); }

        @Override
        public String process(String data) {
            return cache.computeIfAbsent(data, key -> {
                System.out.println("[CACHE] Miss — işleniyor");
                return wrapped.process(key);
            });
        }
    }

    // ================================================================
    // Gerçek Dünya: HTTP Request Pipeline (Spring filter chain gibi)
    // ================================================================
    interface HttpHandler {
        String handle(String request);
    }

    static class CoreHandler implements HttpHandler {
        @Override
        public String handle(String request) {
            return "Response for: " + request;
        }
    }

    static class AuthMiddleware implements HttpHandler {
        private final HttpHandler next;
        AuthMiddleware(HttpHandler next) { this.next = next; }

        @Override
        public String handle(String request) {
            if (!request.contains("token")) throw new RuntimeException("401 Unauthorized");
            System.out.println("[AUTH] Token doğrulandı");
            return next.handle(request);
        }
    }

    static class RateLimitMiddleware implements HttpHandler {
        private final HttpHandler next;
        private int requestCount = 0;
        private final int maxRequests;

        RateLimitMiddleware(HttpHandler next, int maxRequests) {
            this.next = next;
            this.maxRequests = maxRequests;
        }

        @Override
        public String handle(String request) {
            if (++requestCount > maxRequests) throw new RuntimeException("429 Too Many Requests");
            System.out.println("[RATE LIMIT] İstek " + requestCount + "/" + maxRequests);
            return next.handle(request);
        }
    }

    public static void main(String[] args) {
        System.out.println("=== DECORATOR PATTERN ===\n");

        // Zincirleme dekorasyon
        DataProcessor processor = new LoggingDecorator(
                new EncryptionDecorator(
                        new CompressionDecorator(
                                new BaseProcessor())));

        System.out.println("Sonuç: " + processor.process("Merhaba Dünya") + "\n");

        // Caching
        DataProcessor cached = new CachingDecorator(new BaseProcessor());
        cached.process("test-data");
        cached.process("test-data"); // cache hit

        // HTTP Pipeline
        System.out.println("\n--- HTTP Pipeline ---");
        HttpHandler pipeline = new RateLimitMiddleware(
                new AuthMiddleware(new CoreHandler()), 3);

        System.out.println(pipeline.handle("GET /api/users?token=abc"));
    }
}
