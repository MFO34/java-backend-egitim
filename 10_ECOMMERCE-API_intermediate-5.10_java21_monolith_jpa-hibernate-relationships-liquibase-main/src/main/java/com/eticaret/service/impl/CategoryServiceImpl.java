package com.eticaret.service.impl;

import com.eticaret.dto.request.CreateCategoryRequest;
import com.eticaret.dto.response.CategoryResponse;
import com.eticaret.exception.BusinessException;
import com.eticaret.exception.ResourceNotFoundException;
import com.eticaret.mapper.CategoryMapper;
import com.eticaret.repository.CategoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * KATEGORİ SERVİSİ — İş Mantığı Katmanı
 * ==========================================
 *
 * Kategori ağaç yapısı:
 *   Elektronik (root)
 *   └── Telefon (child)
 *       └── Akıllı Telefon (grandchild)
 *   Giyim (root)
 *   └── Erkek (child)
 *
 *   @ManyToOne parent: Alt kategorinin üst kategoriye FK referansı.
 *   @OneToMany children: Üst kategorinin alt kategorilerini lazy yükler.
 *
 * Slug nedir?
 *   URL dostu benzersiz tanımlayıcı: "elektronik", "cep-telefonu".
 *   SEO için ID yerine slug kullanılır: /kategori/elektronik
 *   Benzersiz olmalı (DB unique constraint + önceden kontrol).
 *
 * N+1 Problemi ve çözümü:
 *   getAllCategories(): Alt kategoriler + ürün sayısı gerekiyor.
 *   Naif yaklaşım: Her kategori için children ve products ayrı SQL → N+1 sorgu.
 *   Çözüm: findRootCategoriesWithChildren() → @EntityGraph ile JOIN FETCH.
 *   Tek SQL: root kategoriler + alt kategoriler + ürün sayısı birlikte gelir.
 */
@Service
@Slf4j
public class CategoryServiceImpl {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryRepository categoryRepository,
                                CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper     = categoryMapper;
    }

    /**
     * Yeni kategori oluşturur.
     *
     * Slug benzersizlik kontrolü:
     *   DB'de unique constraint var ama önceden kontrol daha açıklayıcı hata verir.
     *   Constraint ihlali: DataIntegrityViolationException (teknik, belirsiz).
     *   Önceden kontrol: BusinessException("Bu slug zaten mevcut") (kullanıcı dostu).
     *
     * Parent kategori neden opsiyonel?
     *   Root kategori (Elektronik, Giyim) parent'sız oluşturulur.
     *   Alt kategori (Telefon) parentId ile oluşturulur.
     *   parentId null → root kategori.
     */
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        // Slug zaten kullanılıyor mu? (SEO URL benzersizliği)
        if (categoryRepository.existsBySlug(request.slug())) {
            throw new BusinessException("Bu slug zaten mevcut: " + request.slug(), "DUPLICATE_SLUG");
        }

        // Mapper: DTO → entity (id, parent, children hariç)
        var category = categoryMapper.toEntity(request);

        // Alt kategori ise parent'ı bağla
        if (request.parentId() != null) {
            var parent = categoryRepository.findById(request.parentId())
                .orElseThrow(() -> new ResourceNotFoundException("Kategori", request.parentId()));
            category.setParent(parent);
            // Çift yönlü ilişki: parent.getChildren().add(category) gerekmez
            // Çünkü @ManyToOne parent FK'yı sahiplenir — save() ile yazılır
        }

        var saved = categoryRepository.save(category);
        log.info("Kategori oluşturuldu: id={}, name={}", saved.getId(), saved.getName());
        return categoryMapper.toResponse(saved);
    }

    /**
     * Tüm kök kategorileri alt kategorileriyle birlikte getirir.
     *
     * findRootCategoriesWithChildren():
     *   "WHERE parent_id IS NULL" → kök kategoriler
     *   @EntityGraph(attributePaths = {"children", "products"}) → JOIN FETCH
     *   Bu sayede her kategori için ayrı children/products sorgusu atılmaz.
     *
     * Neden Record immutable olduğu için yeni instance oluşturulur?
     *   categoryMapper.toResponse(c) → children alanı boş gelir (mapper bilmiyor).
     *   Alt kategorileri ve ürün sayısını eklemek için yeni CategoryResponse oluşturmak şart.
     *   Java record'lar immutable: alanlar constructor'da set edilir, setter yok.
     *
     * Alternatif: categoryMapper'ı genişletmek (MapStruct custom mapping).
     *   Bu projede açıklık için manuel yapıldı.
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        // JOIN FETCH: root kategoriler + children + products tek sorguda
        return categoryRepository.findRootCategoriesWithChildren().stream()
            .map(c -> {
                var response = categoryMapper.toResponse(c);

                // Alt kategorileri dönüştür
                var children = c.getChildren().stream()
                    .map(categoryMapper::toResponse)
                    .toList();

                // Record immutable → yeni instance: tüm alanlar + children + ürün sayısı
                return new CategoryResponse(
                    response.id(), response.name(), response.slug(),
                    response.description(), response.parentId(), response.parentName(),
                    children,
                    c.getProducts().size()  // Bu kategoriye ait ürün sayısı
                );
            })
            .toList();
    }

    /**
     * ID ile kategori getirir.
     *
     * @Transactional(readOnly=true): Yazma yok, Hibernate optimize eder.
     */
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        var cat = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Kategori", id));
        return categoryMapper.toResponse(cat);
    }

    /**
     * Kategoriyi pasif yapar (soft delete).
     *
     * Neden hard delete değil?
     *   Ürünlerin category_id FK referansı var.
     *   Kategori silinirse ürünlerin kategori bilgisi bozulur veya cascade silinir.
     *   isActive=false → kategori menüde gösterilmez, ürünler etkilenmez.
     *
     * Alt kategoriler ne olur?
     *   Bu implementasyonda alt kategoriler ayrıca pasife alınmıyor.
     *   Production'da recursive soft delete veya "alt kategoriler var mı?" kontrolü eklenmeli.
     */
    @Transactional
    public void deleteCategory(Long id) {
        var cat = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Kategori", id));
        // Soft delete: menüde görünmez, ürün referansları bozulmaz
        cat.setIsActive(false);
        categoryRepository.save(cat);
    }
}
