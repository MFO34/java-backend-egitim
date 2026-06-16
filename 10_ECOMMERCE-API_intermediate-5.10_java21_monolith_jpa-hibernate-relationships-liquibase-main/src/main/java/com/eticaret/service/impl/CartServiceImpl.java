package com.eticaret.service.impl;

import com.eticaret.dto.request.AddToCartRequest;
import com.eticaret.dto.response.CartResponse;
import com.eticaret.entity.CartItem;
import com.eticaret.exception.BusinessException;
import com.eticaret.exception.OutOfStockException;
import com.eticaret.exception.ResourceNotFoundException;
import com.eticaret.mapper.CartMapper;
import com.eticaret.repository.CartRepository;
import com.eticaret.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SEPET SERVİSİ — İş Mantığı Katmanı
 * =====================================
 *
 * Mimari kararlar:
 *   - Her kullanıcıya kayıt anında bir sepet oluşturulur (UserServiceImpl).
 *     Yani "sepet yok" durumu hata senaryosu, normal akış değil.
 *
 *   - Sepet stok REZERVE ETMEZ — sadece kullanıcının niyetini saklar.
 *     Gerçek stok kontrolü ve azaltma sipariş oluşturulunca (OrderServiceImpl) yapılır.
 *     Bu nedenle "sepete eklendi ama sipariş sırasında stok bitti" senaryosu mümkündür.
 *
 *   - @Transactional: Sepet + kalemler tutarlı olmalı.
 *     addToCart'ta ürün eklenir, save yapılır — ikisi ya hep ya hiç.
 *
 * orphanRemoval=true (@OneToMany(orphanRemoval=true)):
 *   CartItem'ı collection'dan çıkarınca JPA otomatik DELETE atar.
 *   Ayrıca cartItemRepository.delete() çağırmaya gerek yok.
 *   Kapalı tutmak için Cart entity'si dışından CartItem silinemez.
 */
@Service
@Slf4j
public class CartServiceImpl {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final CartMapper cartMapper;

    public CartServiceImpl(CartRepository cartRepository,
                            ProductRepository productRepository,
                            CartMapper cartMapper) {
        this.cartRepository   = cartRepository;
        this.productRepository = productRepository;
        this.cartMapper       = cartMapper;
    }

    /**
     * Sepeti getirir.
     *
     * @Transactional(readOnly=true): Veritabanına yazma yapılmaz.
     *   - Hibernate: flush mode NEVER → dirty checking atlanır, performans artar
     *   - PostgreSQL: read replica'ya yönlenebilir (routing yapılandırılmışsa)
     *   - findWithItemsByUserId: @EntityGraph kullanır → items + product JOIN FETCH
     *     Alternatif: Lazy loading ile her item için ayrı SQL → N+1 problemi
     */
    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        var cart = cartRepository.findWithItemsByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Sepet", "userId", userId));
        return cartMapper.toResponse(cart);
    }

    /**
     * Sepete ürün ekler.
     *
     * Stok kontrolü neden burada yapılır?
     *   Kullanıcı deneyimi: stok yokken "sepete eklendi" demek yanlış izlenim yaratır.
     *   Ancak bu kontrol kesin değil — kalem tutulur ama sipariş anında tekrar kontrol edilir.
     *
     * Neden iki aşamalı stok kontrolü var?
     *   1. addToCart → anlık stok kontrolü (soft check): kullanıcı deneyimi için
     *   2. createOrder → atomik stok azaltma (hard check): gerçek rezervasyon için
     *   İki arasında başka biri sipariş verebilir → "sepete eklendi ama yetmedi" normal durum.
     *
     * Upsert mantığı:
     *   Ürün zaten sepetteyse miktarı artır (yeni satır açma).
     *   Bu hem veritabanı tasarrufu hem kullanıcı deneyimi açısından doğru.
     */
    @Transactional
    public CartResponse addToCart(Long userId, AddToCartRequest request) {
        // Sepeti kalemler + ürünlerle birlikte getir (JOIN FETCH → N+1 yok)
        var cart = cartRepository.findWithItemsByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Sepet", "userId", userId));

        // Silinmiş ürün sepete eklenemez — filter(p -> getIsActive()) bunu sağlar
        var product = productRepository.findById(request.productId())
            .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
            .orElseThrow(() -> new ResourceNotFoundException("Ürün", request.productId()));

        // İstenen miktar stokta yok mu? Kullanıcıya hemen söyle (UX)
        if (product.getStockQuantity() < request.quantity()) {
            throw new OutOfStockException(
                product.getName(), request.quantity(), product.getStockQuantity());
        }

        // Ürün zaten sepette mi? → miktarı artır (upsert)
        var existingItem = cart.findItem(product.getId());

        if (existingItem.isPresent()) {
            var item = existingItem.get();
            int newQty = item.getQuantity() + request.quantity();
            // Toplam miktar (mevcut + eklenen) stoku aşıyor mu?
            if (newQty > product.getStockQuantity()) {
                throw new OutOfStockException(product.getName(), newQty, product.getStockQuantity());
            }
            item.setQuantity(newQty);
            // JPA dirty checking: item değişti → transaction commit'te UPDATE otomatik atılır
        } else {
            // Yeni kalem oluştur — cart referansını ver (çift yönlü ilişki tutarlılığı)
            var newItem = CartItem.builder()
                .cart(cart)          // CartItem → Cart bağlantısı (FK: cart_id)
                .product(product)
                .quantity(request.quantity())
                .subtotal(product.getPrice())  // @PrePersist ile quantity×price hesaplanır
                .build();
            cart.getItems().add(newItem);
            // Cascade.ALL: cart kaydedilince yeni item de kaydedilir (ayrı save() gerekmez)
        }

        var saved = cartRepository.save(cart);
        log.info("Sepete eklendi: userId={}, productId={}", userId, request.productId());
        return cartMapper.toResponse(saved);
    }

    /**
     * Sepetten ürün kaldırır.
     *
     * cart.getItems().remove(item) nasıl çalışır?
     *   Cart entity'si: @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
     *   orphanRemoval = true → collection'dan çıkarılan CartItem için DELETE SQL atılır.
     *   Ayrıca cartItemRepository.delete() veya DELETE sorgusu yazmaya gerek yok.
     *
     * Neden önce cart.findItem() ile kalem bulunur?
     *   Kullanıcının sahip olmadığı bir kalem ID'si verirse hata fırlatmak için.
     *   Yoksa remove() sessizce başarısız olurdu (hata yok, silinme de yok).
     */
    @Transactional
    public CartResponse removeFromCart(Long userId, Long productId) {
        var cart = cartRepository.findWithItemsByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Sepet", "userId", userId));

        // Kullanıcı bu ürünü sepete eklemiş mi kontrol et
        var item = cart.findItem(productId)
            .orElseThrow(() -> new BusinessException("Ürün sepette bulunamadı", "ITEM_NOT_FOUND"));

        // orphanRemoval=true → JPA bu item için DELETE atar (transaction commit'te)
        cart.getItems().remove(item);
        cartRepository.save(cart);

        return cartMapper.toResponse(cart);
    }

    /**
     * Sepeti tamamen boşaltır.
     *
     * cart.clear() → Cart entity'sinin helper metodu: items.clear() çağırır.
     * orphanRemoval = true olduğu için tüm CartItem'lar için DELETE SQL atılır.
     * Bu işlem sipariş oluşturulunca da OrderServiceImpl tarafından çağrılır.
     *
     * Neden ayrı bir clear() metodu var?
     *   Kullanıcı "sepeti temizle" butonuna basabilir (sipariş öncesi vazgeçme).
     *   Ayrıca sipariş sonrası otomatik temizleme için OrderServiceImpl kullanır.
     */
    @Transactional
    public CartResponse clearCart(Long userId) {
        var cart = cartRepository.findWithItemsByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Sepet", "userId", userId));

        // items.clear() → orphanRemoval → tüm kalemler için DELETE
        cart.clear();
        cartRepository.save(cart);
        return cartMapper.toResponse(cart);
    }
}
