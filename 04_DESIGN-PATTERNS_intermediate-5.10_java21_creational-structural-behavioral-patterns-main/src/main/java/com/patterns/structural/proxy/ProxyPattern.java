package com.patterns.structural.proxy;

import java.lang.reflect.*;
import java.util.*;

/**
 * PROXY PATTERN — Erişim kontrolü ve ek davranış
 *
 * Türleri:
 *   Virtual Proxy  → lazy initialization (pahalı nesneyi gerektiğinde oluştur)
 *   Protection Proxy → erişim kontrolü
 *   Caching Proxy  → sonuç önbellekleme
 *   Logging Proxy  → operasyon loglama
 *
 * Kullanım: Spring AOP (@Transactional, @Cacheable, @Async tümü proxy kullanır)
 *           Hibernate lazy loading, JDK Dynamic Proxy
 */
public class ProxyPattern {

    // ================================================================
    // 1. Virtual Proxy — Lazy Initialization
    // ================================================================
    interface Image {
        void display();
        int getWidth();
        int getHeight();
    }

    static class RealImage implements Image {
        private final String filename;
        private final int width, height;

        RealImage(String filename) {
            this.filename = filename;
            System.out.println("[DISK] Yükleniyor: " + filename); // pahalı işlem
            this.width  = 1920;
            this.height = 1080;
        }

        @Override public void display()    { System.out.println("[DISPLAY] " + filename); }
        @Override public int getWidth()    { return width; }
        @Override public int getHeight()   { return height; }
    }

    static class LazyImageProxy implements Image {
        private final String filename;
        private RealImage realImage; // gerekene kadar null

        LazyImageProxy(String filename) { this.filename = filename; }

        private RealImage getRealImage() {
            if (realImage == null) realImage = new RealImage(filename); // lazy
            return realImage;
        }

        @Override public void display()  { getRealImage().display(); }
        @Override public int getWidth()  { return getRealImage().getWidth(); }
        @Override public int getHeight() { return getRealImage().getHeight(); }
    }

    // ================================================================
    // 2. Protection Proxy — Yetki kontrolü
    // ================================================================
    interface UserService {
        void createUser(String name);
        void deleteUser(String name);
        List<String> getUsers();
    }

    static class UserServiceImpl implements UserService {
        private final List<String> users = new ArrayList<>(List.of("admin", "alice", "bob"));

        @Override public void createUser(String name) { users.add(name); System.out.println("Kullanıcı oluşturuldu: " + name); }
        @Override public void deleteUser(String name) { users.remove(name); System.out.println("Kullanıcı silindi: " + name); }
        @Override public List<String> getUsers()      { return Collections.unmodifiableList(users); }
    }

    static class SecureUserServiceProxy implements UserService {
        private final UserService target;
        private final String currentRole;

        SecureUserServiceProxy(UserService target, String role) {
            this.target = target;
            this.currentRole = role;
        }

        @Override
        public void createUser(String name) {
            checkRole("ADMIN");
            target.createUser(name);
        }

        @Override
        public void deleteUser(String name) {
            checkRole("ADMIN");
            target.deleteUser(name);
        }

        @Override
        public List<String> getUsers() { return target.getUsers(); }

        private void checkRole(String required) {
            if (!currentRole.equals(required))
                throw new SecurityException("Yetki hatası: %s gerekli, mevcut: %s".formatted(required, currentRole));
        }
    }

    // ================================================================
    // 3. Caching Proxy
    // ================================================================
    interface ProductRepository {
        String findById(int id);
    }

    static class DatabaseProductRepository implements ProductRepository {
        @Override
        public String findById(int id) {
            System.out.println("[DB] Sorgu çalışıyor: id=" + id);
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return "Product-" + id;
        }
    }

    static class CachingProductProxy implements ProductRepository {
        private final ProductRepository target;
        private final Map<Integer, String> cache = new HashMap<>();

        CachingProductProxy(ProductRepository target) { this.target = target; }

        @Override
        public String findById(int id) {
            return cache.computeIfAbsent(id, key -> {
                System.out.println("[CACHE] Miss, DB'den alınıyor");
                return target.findById(key);
            });
        }
    }

    // ================================================================
    // 4. JDK Dynamic Proxy — Spring AOP'un temeli
    // ================================================================
    static class LoggingInvocationHandler implements InvocationHandler {
        private final Object target;

        LoggingInvocationHandler(Object target) { this.target = target; }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            System.out.printf("[AOP] %s() çağrıldı, args=%s%n", method.getName(), Arrays.toString(args));
            long start = System.currentTimeMillis();
            Object result = method.invoke(target, args);
            System.out.printf("[AOP] %s() tamamlandı (%dms)%n",
                    method.getName(), System.currentTimeMillis() - start);
            return result;
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T createLoggingProxy(T target, Class<T> iface) {
        return (T) Proxy.newProxyInstance(
                iface.getClassLoader(),
                new Class[]{iface},
                new LoggingInvocationHandler(target));
    }

    public static void main(String[] args) {
        System.out.println("=== PROXY PATTERN ===\n");

        // Virtual Proxy
        System.out.println("--- Virtual Proxy ---");
        Image image = new LazyImageProxy("photo.jpg");
        System.out.println("Proxy oluşturuldu (disk yok)");
        image.display(); // şimdi yüklendi

        // Protection Proxy
        System.out.println("\n--- Protection Proxy ---");
        UserService service = new SecureUserServiceProxy(new UserServiceImpl(), "USER");
        System.out.println("Kullanıcılar: " + service.getUsers());
        try { service.deleteUser("alice"); }
        catch (SecurityException e) { System.out.println("Yakalandı: " + e.getMessage()); }

        UserService adminService = new SecureUserServiceProxy(new UserServiceImpl(), "ADMIN");
        adminService.createUser("dave");

        // Caching Proxy
        System.out.println("\n--- Caching Proxy ---");
        ProductRepository repo = new CachingProductProxy(new DatabaseProductRepository());
        System.out.println(repo.findById(1));
        System.out.println(repo.findById(1)); // cache hit
        System.out.println(repo.findById(2));

        // Dynamic Proxy
        System.out.println("\n--- JDK Dynamic Proxy ---");
        UserService proxied = createLoggingProxy(new UserServiceImpl(), UserService.class);
        proxied.getUsers();
        proxied.createUser("proxy-user");
    }
}
