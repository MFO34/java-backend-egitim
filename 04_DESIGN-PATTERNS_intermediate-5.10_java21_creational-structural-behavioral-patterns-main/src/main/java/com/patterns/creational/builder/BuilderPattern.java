package com.patterns.creational.builder;

import java.util.ArrayList;
import java.util.List;

/**
 * BUILDER PATTERN — Karmaşık nesne adım adım oluşturma
 *
 * Kullanım: StringBuilder, Stream.Builder, Lombok @Builder,
 *           HttpRequest.newBuilder(), Spring MockMvcRequestBuilders
 *
 * Ne zaman kullanılır?
 *   - Constructor'da 4+ parametre varsa
 *   - Bazı alanlar opsiyonelsa
 *   - Nesne oluşturma adımları önemliyse
 */
public class BuilderPattern {

    // ================================================================
    // Klasik Builder
    // ================================================================
    static class HttpRequest {
        private final String url;
        private final String method;
        private final String body;
        private final List<String> headers;
        private final int timeoutSeconds;
        private final boolean followRedirects;

        private HttpRequest(Builder builder) {
            this.url             = builder.url;
            this.method          = builder.method;
            this.body            = builder.body;
            this.headers         = List.copyOf(builder.headers);
            this.timeoutSeconds  = builder.timeoutSeconds;
            this.followRedirects = builder.followRedirects;
        }

        @Override
        public String toString() {
            return "HttpRequest{method='%s', url='%s', headers=%s, timeout=%ds, body='%s'}"
                    .formatted(method, url, headers, timeoutSeconds, body);
        }

        static class Builder {
            private final String url;           // zorunlu
            private String method = "GET";      // varsayılan
            private String body;
            private final List<String> headers = new ArrayList<>();
            private int timeoutSeconds = 30;
            private boolean followRedirects = true;

            Builder(String url) { this.url = url; }

            Builder method(String method)       { this.method = method; return this; }
            Builder body(String body)           { this.body = body; return this; }
            Builder header(String header)       { this.headers.add(header); return this; }
            Builder timeout(int seconds)        { this.timeoutSeconds = seconds; return this; }
            Builder followRedirects(boolean fr) { this.followRedirects = fr; return this; }

            HttpRequest build() {
                if (url == null || url.isBlank()) throw new IllegalStateException("URL zorunlu");
                return new HttpRequest(this);
            }
        }
    }

    // ================================================================
    // Gerçek Dünya: QueryBuilder
    // ================================================================
    static class SqlQueryBuilder {
        private String table;
        private final List<String> conditions = new ArrayList<>();
        private final List<String> columns = new ArrayList<>();
        private String orderBy;
        private int limit = -1;
        private int offset = -1;

        static SqlQueryBuilder from(String table) {
            SqlQueryBuilder qb = new SqlQueryBuilder();
            qb.table = table;
            return qb;
        }

        SqlQueryBuilder select(String... cols) { columns.addAll(List.of(cols)); return this; }
        SqlQueryBuilder where(String condition) { conditions.add(condition); return this; }
        SqlQueryBuilder orderBy(String col)    { this.orderBy = col; return this; }
        SqlQueryBuilder limit(int n)           { this.limit = n; return this; }
        SqlQueryBuilder offset(int n)          { this.offset = n; return this; }

        String build() {
            StringBuilder sb = new StringBuilder("SELECT ");
            sb.append(columns.isEmpty() ? "*" : String.join(", ", columns));
            sb.append(" FROM ").append(table);
            if (!conditions.isEmpty()) sb.append(" WHERE ").append(String.join(" AND ", conditions));
            if (orderBy != null)       sb.append(" ORDER BY ").append(orderBy);
            if (limit > 0)             sb.append(" LIMIT ").append(limit);
            if (offset > 0)            sb.append(" OFFSET ").append(offset);
            return sb.toString();
        }
    }

    // ================================================================
    // Java 16+ Record ile Builder
    // ================================================================
    record User(String name, String email, String role, boolean active) {
        static Builder builder() { return new Builder(); }

        static class Builder {
            private String name, email, role = "USER";
            private boolean active = true;

            Builder name(String name)   { this.name = name; return this; }
            Builder email(String email) { this.email = email; return this; }
            Builder role(String role)   { this.role = role; return this; }
            Builder active(boolean a)   { this.active = a; return this; }

            User build() {
                if (name == null || email == null) throw new IllegalStateException("name ve email zorunlu");
                return new User(name, email, role, active);
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("=== BUILDER PATTERN ===\n");

        // HttpRequest Builder
        HttpRequest request = new HttpRequest.Builder("https://api.example.com/users")
                .method("POST")
                .header("Content-Type: application/json")
                .header("Authorization: Bearer token123")
                .body("{\"name\": \"Ahmet\"}")
                .timeout(10)
                .build();
        System.out.println(request);

        // SQL Query Builder
        String query = SqlQueryBuilder.from("orders")
                .select("id", "user_id", "total_price", "status")
                .where("status = 'PENDING'")
                .where("total_price > 100")
                .orderBy("created_at DESC")
                .limit(20)
                .offset(40)
                .build();
        System.out.println("\nSQL: " + query);

        // User Record Builder
        User user = User.builder()
                .name("Muhammed")
                .email("m@example.com")
                .role("ADMIN")
                .build();
        System.out.println("\nUser: " + user);
    }
}
