package com.patterns.creational.factory;

/**
 * FACTORY METHOD & ABSTRACT FACTORY
 *
 * Factory Method  → Hangi sınıfı oluşturacağını alt sınıf belirler
 * Abstract Factory → İlgili nesneler ailesini üretir
 *
 * Kullanım: Spring BeanFactory, JDBC DriverManager, LoggerFactory
 */
public class FactoryPatterns {

    // ================================================================
    // FACTORY METHOD
    // ================================================================

    interface Notification {
        void send(String message, String recipient);
    }

    static class EmailNotification implements Notification {
        @Override
        public void send(String message, String recipient) {
            System.out.printf("[EMAIL] → %s: %s%n", recipient, message);
        }
    }

    static class SmsNotification implements Notification {
        @Override
        public void send(String message, String recipient) {
            System.out.printf("[SMS] → %s: %s%n", recipient, message);
        }
    }

    static class PushNotification implements Notification {
        @Override
        public void send(String message, String recipient) {
            System.out.printf("[PUSH] → %s: %s%n", recipient, message);
        }
    }

    // Factory — nesne oluşturmayı merkezileştirir
    static class NotificationFactory {
        public static Notification create(String type) {
            return switch (type.toUpperCase()) {
                case "EMAIL" -> new EmailNotification();
                case "SMS"   -> new SmsNotification();
                case "PUSH"  -> new PushNotification();
                default -> throw new IllegalArgumentException("Bilinmeyen tip: " + type);
            };
        }
    }

    // ================================================================
    // ABSTRACT FACTORY — İlgili nesneler ailesi
    // Örnek: farklı DB'ler için connection + query builder çifti
    // ================================================================

    interface DatabaseConnection {
        void connect();
        void disconnect();
    }

    interface QueryBuilder {
        String buildSelectQuery(String table);
        String buildInsertQuery(String table);
    }

    // PostgreSQL ailesi
    static class PostgreSQLConnection implements DatabaseConnection {
        @Override public void connect()    { System.out.println("PostgreSQL bağlandı"); }
        @Override public void disconnect() { System.out.println("PostgreSQL bağlantısı kesildi"); }
    }

    static class PostgreSQLQueryBuilder implements QueryBuilder {
        @Override public String buildSelectQuery(String t) { return "SELECT * FROM " + t + " LIMIT 100"; }
        @Override public String buildInsertQuery(String t) { return "INSERT INTO " + t + " VALUES (...)"; }
    }

    // Oracle ailesi
    static class OracleConnection implements DatabaseConnection {
        @Override public void connect()    { System.out.println("Oracle bağlandı"); }
        @Override public void disconnect() { System.out.println("Oracle bağlantısı kesildi"); }
    }

    static class OracleQueryBuilder implements QueryBuilder {
        @Override public String buildSelectQuery(String t) { return "SELECT * FROM " + t + " WHERE ROWNUM <= 100"; }
        @Override public String buildInsertQuery(String t) { return "INSERT INTO " + t + " VALUES (...)"; }
    }

    // Abstract Factory
    interface DatabaseFactory {
        DatabaseConnection createConnection();
        QueryBuilder createQueryBuilder();
    }

    static class PostgreSQLFactory implements DatabaseFactory {
        @Override public DatabaseConnection createConnection() { return new PostgreSQLConnection(); }
        @Override public QueryBuilder createQueryBuilder() { return new PostgreSQLQueryBuilder(); }
    }

    static class OracleFactory implements DatabaseFactory {
        @Override public DatabaseConnection createConnection() { return new OracleConnection(); }
        @Override public QueryBuilder createQueryBuilder() { return new OracleQueryBuilder(); }
    }

    static DatabaseFactory getDatabaseFactory(String dbType) {
        return switch (dbType.toUpperCase()) {
            case "POSTGRESQL" -> new PostgreSQLFactory();
            case "ORACLE"     -> new OracleFactory();
            default -> throw new IllegalArgumentException("Desteklenmeyen DB: " + dbType);
        };
    }

    public static void main(String[] args) {
        System.out.println("=== FACTORY PATTERNS ===\n");

        // Factory Method
        System.out.println("--- Factory Method ---");
        Notification email = NotificationFactory.create("EMAIL");
        Notification sms   = NotificationFactory.create("SMS");
        Notification push  = NotificationFactory.create("PUSH");
        email.send("Sipariş onaylandı", "user@example.com");
        sms.send("OTP: 123456", "+905551234567");
        push.send("Kampanya başladı!", "device-token-xyz");

        // Abstract Factory
        System.out.println("\n--- Abstract Factory ---");
        DatabaseFactory pgFactory = getDatabaseFactory("POSTGRESQL");
        DatabaseConnection pgConn = pgFactory.createConnection();
        QueryBuilder pgQB = pgFactory.createQueryBuilder();
        pgConn.connect();
        System.out.println(pgQB.buildSelectQuery("users"));
        pgConn.disconnect();

        DatabaseFactory oraFactory = getDatabaseFactory("ORACLE");
        DatabaseConnection oraConn = oraFactory.createConnection();
        QueryBuilder oraQB = oraFactory.createQueryBuilder();
        oraConn.connect();
        System.out.println(oraQB.buildSelectQuery("employees"));
        oraConn.disconnect();
    }
}
