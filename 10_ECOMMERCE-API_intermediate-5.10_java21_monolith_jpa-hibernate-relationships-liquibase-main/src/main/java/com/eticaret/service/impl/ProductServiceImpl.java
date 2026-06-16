package com.eticaret.service.impl;

import com.eticaret.dto.request.CreateProductRequest;
import com.eticaret.dto.request.ProductFilterRequest;
import com.eticaret.dto.request.UpdateProductRequest;
import com.eticaret.dto.response.ProductResponse;
import com.eticaret.dto.response.ProductSummaryResponse;
import com.eticaret.entity.Tag;
import com.eticaret.exception.BusinessException;
import com.eticaret.exception.ResourceNotFoundException;
import com.eticaret.mapper.ProductMapper;
import com.eticaret.repository.CategoryRepository;
import com.eticaret.repository.ProductRepository;
import com.eticaret.repository.ReviewRepository;
import com.eticaret.repository.TagRepository;
import com.eticaret.specification.ProductSpecification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ProductServiceImpl {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ReviewRepository reviewRepository;
    private final ProductMapper productMapper;

    public ProductServiceImpl(ProductRepository productRepository,
                               CategoryRepository categoryRepository,
                               TagRepository tagRepository,
                               ReviewRepository reviewRepository,
                               ProductMapper productMapper) {
        this.productRepository  = productRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository      = tagRepository;
        this.reviewRepository   = reviewRepository;
        this.productMapper      = productMapper;
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        if (productRepository.existsBySlug(request.slug())) {
            throw new BusinessException("Bu slug zaten mevcut: " + request.slug(), "DUPLICATE_SLUG");
        }

        var product = productMapper.toEntity(request);

        // Kategori set et
        if (request.categoryId() != null) {
            var category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Kategori", request.categoryId()));
            product.setCategory(category);
        }

        // Etiketleri işle — var olanı bul, yok olanı oluştur
        if (request.tagNames() != null && !request.tagNames().isEmpty()) {
            var tags = processTagNames(request.tagNames());
            product.setTags(tags);
        }

        var saved = productRepository.save(product);
        log.info("Ürün oluşturuldu: id={}, slug={}", saved.getId(), saved.getSlug());
        return buildProductResponse(saved);
    }

    /**
     * Ürünü ID ile getirir.
     *
     * filter(p -> getIsActive()):
     *   DB'de silinmiş ürünler isActive=false tutulur (soft delete).
     *   filter ile yalnızca aktif ürünler döndürülür.
     *   Alternatif: findByIdAndIsActiveTrue() — aynı sonuç, ama filter daha okunabilir.
     *
     * buildProductResponse():
     *   Mapper'ın ürettiği response'a rating ve yorum sayısı ekler.
     *   Neden mapper içinde yapılmıyor? Mapper saf dönüşüm yapar, DB sorgusu atmaz.
     *   Rating/review hesaplaması DB sorgusu gerektirdiği için ayrı metotta.
     */
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        var product = productRepository.findById(id)
            .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
            .orElseThrow(() -> new ResourceNotFoundException("Ürün", id));
        return buildProductResponse(product);
    }

    /**
     * Ürünü slug ile getirir (SEO URL için).
     * /urunler/apple-iphone-15-pro gibi URL'lerden slug ile direkt erişim.
     *
     * findBySlugAndIsActiveTrue(): Tek sorguda slug eşleşmesi + aktiflik kontrolü.
     * Daha verimli: WHERE slug=? AND is_active=true — iki ayrı kontrol yerine tek SQL.
     */
    @Transactional(readOnly = true)
    public ProductResponse getProductBySlug(String slug) {
        var product = productRepository.findBySlugAndIsActiveTrue(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Ürün", "slug", slug));
        return buildProductResponse(product);
    }

    /**
     * Dinamik filtreleme — Specification Pattern.
     *
     * Problem: Çok sayıda opsiyonel filtre var (keyword, fiyat, kategori, stok...).
     *   Her kombinasyon için ayrı repository metodu yazmak ölçeklenmez.
     *   8 filtre → 2^8 = 256 kombinasyon → 256 metod OLMAZ.
     *
     * Specification Pattern çözümü:
     *   Her filtre bir Specification<Product> döner.
     *   Kullanılan filtreler AND ile birleştirilir.
     *   Kullanılmayan filtreler (null) ignore edilir.
     *   Tek findAll(spec, pageable) çağrısı dinamik WHERE üretir.
     *
     * ProductSpecification.buildFilter():
     *   keyword → WHERE (name LIKE '%x%' OR description LIKE '%x%')
     *   categoryId → WHERE category_id = ?
     *   minPrice/maxPrice → WHERE price BETWEEN ? AND ?
     *   tagSlug → JOIN tags WHERE tags.slug = ?
     *   inStockOnly → WHERE stock_quantity > 0
     *   onSaleOnly → WHERE original_price IS NOT NULL AND original_price > price
     */
    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> filterProducts(
            ProductFilterRequest filter, Pageable pageable) {

        var spec = ProductSpecification.buildFilter(
            filter.keyword(),
            filter.categoryId(),
            filter.minPrice(),
            filter.maxPrice(),
            filter.tagSlug(),
            filter.inStockOnly(),
            filter.onSaleOnly()
        );

        // JpaSpecificationExecutor.findAll: Specification'ı SQL WHERE'e çevirir
        return productRepository.findAll(spec, pageable)
            .map(this::buildSummaryResponse);
    }

    /**
     * Tüm aktif ürünleri sayfalı getirir.
     *
     * findAllByIsActiveTrue() + @EntityGraph:
     *   "category" ve "images" alanları JOIN FETCH ile tek sorguda gelir.
     *   Alternatif: findAll() + Lazy loading → her ürün için ayrı category/image SQL → N+1.
     *   100 ürün × 2 ilişki = 201 SQL yerine tek SQL.
     */
    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> getAllProducts(Pageable pageable) {
        return productRepository.findAllByIsActiveTrue(pageable)
            .map(this::buildSummaryResponse);
    }

    /**
     * Ürün günceller — PATCH semantiği (kısmi güncelleme).
     *
     * Neden PUT değil PATCH mantığı?
     *   PUT: Tüm alanları göndermeyi zorunlu kılar.
     *   PATCH: Sadece değiştirilen alanlar gönderilir.
     *   "null ise güncelleme" kontrolü → gönderilmeyen alanlar bozulmaz.
     *
     * JPA dirty checking:
     *   Yüklenen product entity'si değiştirilir.
     *   Transaction commit'te Hibernate değişiklikleri tespit eder ve UPDATE SQL atar.
     *   Sadece değişen alanlar UPDATE'e girer (optimized dirty checking ile).
     *
     * Tag güncelleme:
     *   product.getTags().clear() + processTagNames() → mevcut etiketleri sil, yenilerini ekle.
     *   @ManyToMany orphanRemoval: ilişki tablosundan eski bağlantılar silinir.
     */
    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        var product = productRepository.findById(id)
            .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
            .orElseThrow(() -> new ResourceNotFoundException("Ürün", id));

        // Null kontrolü: sadece gönderilen alanları güncelle (PATCH davranışı)
        if (request.name()          != null) product.setName(request.name());
        if (request.description()   != null) product.setDescription(request.description());
        if (request.price()         != null) product.setPrice(request.price());
        if (request.originalPrice() != null) product.setOriginalPrice(request.originalPrice());
        if (request.stockQuantity() != null) product.setStockQuantity(request.stockQuantity());

        if (request.categoryId() != null) {
            var cat = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Kategori", request.categoryId()));
            product.setCategory(cat);
        }

        if (request.tagNames() != null) {
            // Mevcut etiket ilişkilerini temizle, yenilerini işle
            // @ManyToMany bağlantı tablosundaki eski satırlar silinir, yenileri eklenir
            product.getTags().clear();
            product.setTags(processTagNames(request.tagNames()));
        }

        return buildProductResponse(productRepository.save(product));
    }

    /**
     * Ürünü siler — soft delete.
     *
     * Neden hard delete değil?
     *   OrderItem.product_id FK referansı var: siparişlerde ürün adı, fiyatı görünmeli.
     *   DB'den silmek: FK ihlali ya da cascade → sipariş geçmişi bozulur.
     *   isActive=false → ürün listede görünmez, sipariş geçmişinde hâlâ var.
     *
     * Stok sıfırlama gerekir mi?
     *   Hayır: Silinmiş ürün satışa kapanmış. Sipariş oluşturmada filter(isActive) reddeder.
     */
    @Transactional
    public void deleteProduct(Long id) {
        var product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Ürün", id));
        product.setIsActive(false);
        productRepository.save(product);
        log.info("Ürün silindi (soft): id={}", id);
    }

    /**
     * En çok satan ürünleri getirir.
     *
     * findBestSellingProducts(limit):
     *   SELECT p.* FROM products p
     *   JOIN order_items oi ON p.id = oi.product_id
     *   GROUP BY p.id ORDER BY SUM(oi.quantity) DESC LIMIT ?
     *   Satılan toplam adede göre sıralama.
     */
    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> getBestSelling(int limit) {
        return productRepository.findBestSellingProducts(limit).stream()
            .map(this::buildSummaryResponse)
            .toList();
    }

    // ===== YARDIMCI METODLAR =====

    /**
     * Etiket isimlerini Tag entity'lerine dönüştürür.
     *
     * Find-or-create pattern:
     *   Etiket zaten varsa → mevcut entity'yi kullan (duplicate etiket oluşturma).
     *   Yoksa → yeni oluştur ve kaydet.
     *
     * Slug üretimi: "Elektrikli Araç" → "elektrikli-arac"
     *   toLowerCase: büyük-küçük harf normalleşme
     *   replaceAll("[^a-z0-9]", "-"): Türkçe karakter ve boşlukları tire'ye çevir
     *   replaceAll("-+", "-"): Ardışık tireleri teke indir ("--" → "-")
     *   Bu slug DB'de unique constraint ile korunur.
     */
    private List<Tag> processTagNames(List<String> tagNames) {
        var tags = new ArrayList<Tag>();
        for (var name : tagNames) {
            // Slug: URL dostu, benzersiz tanımlayıcı
            var slug = name.toLowerCase()
                .replaceAll("[^a-z0-9]", "-")
                .replaceAll("-+", "-");

            // Bu slug zaten DB'de var mı? Varsa kullan, yoksa oluştur
            var tag = tagRepository.findBySlug(slug)
                .orElseGet(() -> {
                    var newTag = Tag.builder().name(name).slug(slug).build();
                    return tagRepository.save(newTag);
                });
            tags.add(tag);
        }
        return tags;
    }

    /**
     * Ürün detay response'u oluşturur — rating ve yorum sayısını ekler.
     *
     * Neden mapper bu işi yapmıyor?
     *   MapStruct mapper: entity alanlarını direkt DTO alanlarına kopyalar.
     *   DB sorgusu atmak mapper'ın sorumluluğu değil (single responsibility).
     *   Rating: AVG(review.rating) → aggregate query gerektirir.
     *   Review count: COUNT(review) WHERE is_approved=true → ayrı query.
     *
     * Record immutable: new ProductResponse(...) ile yeni instance oluşturulur.
     *   Setter yok → builder veya canonical constructor kullanmak zorunlu.
     */
    private ProductResponse buildProductResponse(com.eticaret.entity.Product product) {
        var response = productMapper.toResponse(product);
        // Onaylı yorumların ortalaması (AVG SQL aggregate)
        var avgRating = reviewRepository.findAverageRatingByProductId(product.getId());
        // Yalnızca onaylı yorumlar sayılır (is_approved=true)
        var reviewCount = reviewRepository.countByProductIdAndIsApprovedTrue(product.getId());

        // Record immutable → yeni instance
        return new ProductResponse(
            response.id(), response.name(), response.slug(), response.description(),
            response.price(), response.originalPrice(), response.stockQuantity(),
            response.isActive(), response.status(), response.category(),
            response.images(), response.tags(),
            avgRating, reviewCount,
            response.createdAt(), response.updatedAt()
        );
    }

    private ProductSummaryResponse buildSummaryResponse(com.eticaret.entity.Product product) {
        var summary = productMapper.toSummary(product);
        var avgRating = reviewRepository.findAverageRatingByProductId(product.getId());
        var reviewCount = reviewRepository.countByProductIdAndIsApprovedTrue(product.getId());

        return new ProductSummaryResponse(
            summary.id(), summary.name(), summary.slug(),
            summary.price(), summary.originalPrice(), summary.inStock(),
            summary.categoryName(), summary.primaryImageUrl(),
            avgRating, reviewCount
        );
    }
}
