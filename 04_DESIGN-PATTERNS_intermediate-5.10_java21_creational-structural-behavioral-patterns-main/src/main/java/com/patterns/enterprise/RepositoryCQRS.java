package com.patterns.enterprise;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ENTERPRISE PATTERNS — Repository, CQRS, Unit of Work
 *
 * Repository  → Veri erişimini soyutlar, domain'i persistence'dan ayırır
 * CQRS        → Command (yazma) ve Query (okuma) ayrımı
 * Unit of Work→ İlgili operasyonları atomik hale getirir (Hibernate Session gibi)
 */
public class RepositoryCQRS {

    // ================================================================
    // Domain Model
    // ================================================================
    record ProductId(String value) {}

    static class Product {
        private final ProductId id;
        private String name;
        private double price;
        private int stock;

        Product(ProductId id, String name, double price, int stock) {
            this.id = id; this.name = name; this.price = price; this.stock = stock;
        }

        void updatePrice(double newPrice)    { this.price = newPrice; }
        void decreaseStock(int qty)          { this.stock -= qty; }
        void increaseStock(int qty)          { this.stock += qty; }

        ProductId id()    { return id; }
        String name()     { return name; }
        double price()    { return price; }
        int stock()       { return stock; }

        @Override public String toString() {
            return "Product{id=%s, name=%s, price=%.2f, stock=%d}".formatted(id.value(), name, price, stock);
        }
    }

    // ================================================================
    // 1. REPOSITORY PATTERN
    // ================================================================
    interface ProductRepository {
        void save(Product product);
        Optional<Product> findById(ProductId id);
        List<Product> findAll();
        List<Product> findByPriceLessThan(double maxPrice);
        void delete(ProductId id);
    }

    static class InMemoryProductRepository implements ProductRepository {
        private final Map<String, Product> store = new ConcurrentHashMap<>();

        @Override
        public void save(Product product) {
            store.put(product.id().value(), product);
            System.out.println("[REPO] Kaydedildi: " + product.name());
        }

        @Override
        public Optional<Product> findById(ProductId id) {
            return Optional.ofNullable(store.get(id.value()));
        }

        @Override
        public List<Product> findAll() { return List.copyOf(store.values()); }

        @Override
        public List<Product> findByPriceLessThan(double maxPrice) {
            return store.values().stream()
                    .filter(p -> p.price() < maxPrice)
                    .toList();
        }

        @Override
        public void delete(ProductId id) {
            store.remove(id.value());
            System.out.println("[REPO] Silindi: " + id.value());
        }
    }

    // ================================================================
    // 2. CQRS — Command ve Query ayrımı
    // ================================================================

    // Commands (Write side)
    sealed interface ProductCommand permits
            ProductCommand.CreateProduct,
            ProductCommand.UpdatePrice,
            ProductCommand.AdjustStock {

        record CreateProduct(String id, String name, double price, int stock) implements ProductCommand {}
        record UpdatePrice(String id, double newPrice) implements ProductCommand {}
        record AdjustStock(String id, int delta) implements ProductCommand {}
    }

    // Queries (Read side)
    sealed interface ProductQuery permits
            ProductQuery.GetById,
            ProductQuery.GetAll,
            ProductQuery.GetAffordable {

        record GetById(String id) implements ProductQuery {}
        record GetAll() implements ProductQuery {}
        record GetAffordable(double maxPrice) implements ProductQuery {}
    }

    // Command Handler
    static class ProductCommandHandler {
        private final ProductRepository repository;

        ProductCommandHandler(ProductRepository repo) { this.repository = repo; }

        void handle(ProductCommand command) {
            switch (command) {
                case ProductCommand.CreateProduct cmd -> {
                    var product = new Product(new ProductId(cmd.id()), cmd.name(), cmd.price(), cmd.stock());
                    repository.save(product);
                }
                case ProductCommand.UpdatePrice cmd -> {
                    repository.findById(new ProductId(cmd.id())).ifPresent(p -> {
                        p.updatePrice(cmd.newPrice());
                        repository.save(p);
                        System.out.println("[CMD] Fiyat güncellendi: " + p.name() + " → " + cmd.newPrice());
                    });
                }
                case ProductCommand.AdjustStock cmd -> {
                    repository.findById(new ProductId(cmd.id())).ifPresent(p -> {
                        if (cmd.delta() > 0) p.increaseStock(cmd.delta());
                        else p.decreaseStock(Math.abs(cmd.delta()));
                        repository.save(p);
                        System.out.println("[CMD] Stok güncellendi: " + p.name() + " → " + p.stock());
                    });
                }
            }
        }
    }

    // Query Handler
    static class ProductQueryHandler {
        private final ProductRepository repository;

        ProductQueryHandler(ProductRepository repo) { this.repository = repo; }

        Object handle(ProductQuery query) {
            return switch (query) {
                case ProductQuery.GetById q   -> repository.findById(new ProductId(q.id()));
                case ProductQuery.GetAll q    -> repository.findAll();
                case ProductQuery.GetAffordable q -> repository.findByPriceLessThan(q.maxPrice());
            };
        }
    }

    // ================================================================
    // 3. UNIT OF WORK — Atomik operasyonlar
    // ================================================================
    static class UnitOfWork {
        private final List<Runnable> operations = new ArrayList<>();
        private final List<String> log = new ArrayList<>();

        void registerOperation(String description, Runnable op) {
            operations.add(op);
            log.add(description);
        }

        void commit() {
            System.out.println("[UoW] Commit başlıyor...");
            try {
                for (int i = 0; i < operations.size(); i++) {
                    operations.get(i).run();
                    System.out.println("[UoW] ✓ " + log.get(i));
                }
                System.out.println("[UoW] Commit başarılı");
            } catch (Exception e) {
                System.out.println("[UoW] ✗ Rollback: " + e.getMessage());
                throw e;
            } finally {
                operations.clear();
                log.clear();
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("=== ENTERPRISE PATTERNS ===\n");

        ProductRepository repo = new InMemoryProductRepository();
        ProductCommandHandler cmdHandler = new ProductCommandHandler(repo);
        ProductQueryHandler qryHandler = new ProductQueryHandler(repo);

        // Commands
        System.out.println("--- Commands ---");
        cmdHandler.handle(new ProductCommand.CreateProduct("P1", "Laptop", 15000, 50));
        cmdHandler.handle(new ProductCommand.CreateProduct("P2", "Phone", 8000, 120));
        cmdHandler.handle(new ProductCommand.CreateProduct("P3", "Tablet", 5000, 30));

        cmdHandler.handle(new ProductCommand.UpdatePrice("P1", 14000));
        cmdHandler.handle(new ProductCommand.AdjustStock("P2", -10));

        // Queries
        System.out.println("\n--- Queries ---");
        System.out.println("GetById P1: " + qryHandler.handle(new ProductQuery.GetById("P1")));
        System.out.println("GetAffordable < 10000: " + qryHandler.handle(new ProductQuery.GetAffordable(10000)));
        System.out.println("GetAll: " + qryHandler.handle(new ProductQuery.GetAll()));

        // Unit of Work
        System.out.println("\n--- Unit of Work ---");
        UnitOfWork uow = new UnitOfWork();
        uow.registerOperation("Sipariş oluştur", () -> System.out.println("Sipariş DB'ye yazıldı"));
        uow.registerOperation("Stok düşür", () -> cmdHandler.handle(new ProductCommand.AdjustStock("P1", -1)));
        uow.registerOperation("Ödeme kaydet", () -> System.out.println("Ödeme DB'ye yazıldı"));
        uow.commit();
    }
}
