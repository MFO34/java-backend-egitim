package com.eticaret.service.impl;

import com.eticaret.dto.request.CreateUserRequest;
import com.eticaret.dto.response.UserResponse;
import com.eticaret.entity.Cart;
import com.eticaret.entity.User;
import com.eticaret.exception.BusinessException;
import com.eticaret.exception.ResourceNotFoundException;
import com.eticaret.mapper.UserMapper;
import com.eticaret.repository.CartRepository;
import com.eticaret.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * KULLANICI SERVİSİ — İş Mantığı Katmanı
 * ==========================================
 *
 * Kullanıcı yaşam döngüsü:
 *   Kayıt → aktif (isActive=true) → pasif (isActive=false, soft delete)
 *   Pasif kullanıcı giriş yapamaz, sipariş veremez ama geçmiş siparişleri saklanır.
 *
 * Neden hard delete değil soft delete?
 *   Sipariş geçmişinde kullanıcıya FK referansı vardır.
 *   DB'den silmek FK ihlali veya cascade silme yaratır.
 *   Soft delete (isActive=false) ile geçmiş veriler korunur, kullanıcı devre dışı bırakılır.
 *   KVKK/GDPR silme talebi: Ayrı bir anonymize() metodu ile kişisel veriler anonimleştirilir.
 *
 * Neden kullanıcı oluşturulunca sepet de oluşturulur?
 *   Her kullanıcının tam olarak bir sepeti olur (1:1 ilişki).
 *   Sepet ayrı bir endpoint'te "oluşturulma" yerine kayıt anında hazır olur.
 *   CartServiceImpl.getCart() bunu varsayar — yoksa ResourceNotFoundException fırlatır.
 *
 * Not: Gerçek uygulamada şifre hashleme (BCryptPasswordEncoder) burada yapılır.
 *   Bu projede auth ayrı modülde (12_AUTH) gösterildiği için burada basit tutuldu.
 */
@Service
@Slf4j
public class UserServiceImpl {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final UserMapper userMapper;

    public UserServiceImpl(UserRepository userRepository,
                           CartRepository cartRepository,
                           UserMapper userMapper) {
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.userMapper     = userMapper;
    }

    /**
     * Yeni kullanıcı oluşturur ve otomatik sepet atar.
     *
     * @Transactional: Kullanıcı kaydı + sepet oluşturma atomik olmalı.
     *   Sepet kaydı başarısız olursa kullanıcı kaydı da geri alınır (ROLLBACK).
     *   Aksi halde "kullanıcı var ama sepeti yok" tutarsız duruma düşülür.
     *
     * E-posta benzersizlik kontrolü:
     *   DB'de unique constraint var — save() sırasında da yakalanır.
     *   Ama önceden kontrol edip BusinessException fırlatmak daha okunabilir hata mesajı sağlar.
     *   Constraint ihlali: DataIntegrityViolationException → kullanıcıya anlamsız teknik hata.
     *   Önceden kontrol: BusinessException → "Bu e-posta zaten kayıtlı" mesajı.
     */
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        // E-posta benzersizlik kontrolü — erken kontrol, daha iyi hata mesajı
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(
                "Bu e-posta adresi zaten kayıtlı: " + request.email(),
                "DUPLICATE_EMAIL");
        }

        // Mapper ile DTO → entity dönüşümü
        var user = userMapper.toEntity(request);
        // Gerçek uygulamada: user.setPassword(passwordEncoder.encode(request.password()));
        // BCrypt hash olmadan şifre düz metin — production'da YASAK

        var saved = userRepository.save(user);

        // Kullanıcı oluşturulunca sepetini de oluştur (@OneToOne ilişki)
        // Neden Cascade kullanılmıyor da ayrı save()?
        //   Cascade.PERSIST: User entity'sine Cart eklenip kaydedilince cascade eder.
        //   Ama burada user.setCart(cart) yapmak iki yönlü bağlantı yönetimi gerektirir.
        //   Daha sade yol: cartRepository.save() doğrudan çağırmak.
        var cart = Cart.builder().user(saved).build();
        cartRepository.save(cart);

        log.info("Kullanıcı oluşturuldu: id={}, email={}", saved.getId(), saved.getEmail());
        return userMapper.toResponse(saved);
    }

    /**
     * Kullanıcıyı ID ile getirir.
     *
     * @Transactional(readOnly=true): Okuma işlemi.
     *   Hibernate'e "bu transaction'da yazma yok" bilgisi verilir.
     *   Sonuç: dirty checking atlanır, L1 cache optimizasyonu sağlanır.
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        var user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", id));
        return userMapper.toResponse(user);
    }

    /**
     * Tüm kullanıcıları getirir (admin paneli için).
     *
     * @Transactional(readOnly=true): Okuma işlemi.
     *
     * Büyük kullanıcı tabanında Pageable kullanılmalı.
     *   findAll() → tüm kullanıcıları belleğe çeker → OutOfMemory riski.
     *   Bu projede basit tutuldu; production'da Pageable zorunlu.
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
            .map(userMapper::toResponse)
            .toList();
    }

    /**
     * Kullanıcıyı pasif yapar (soft delete).
     *
     * Neden hard delete (repository.delete()) değil?
     *   Order, Review gibi tablolarda users.id'ye FK referansı var.
     *   DELETE yapılırsa ya FK ihlali hatası alınır ya da cascade ile sipariş geçmişi de silinir.
     *   isActive=false → kullanıcı giriş yapamaz ama geçmiş veriler bozulmaz.
     *
     * KVKK/GDPR sil talebi:
     *   isActive=false yeterli değil — kişisel veriler de silinmeli.
     *   Gerçek uygulamada: user.setEmail("deleted_"+id+"@anon.com"), isim sil, vb.
     *   Bu projede sadece pasif alma gösterildi.
     */
    @Transactional
    public void deactivateUser(Long id) {
        var user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", id));

        // Soft delete: isActive=false → giriş engellenir, veriler korunur
        user.setIsActive(false);
        userRepository.save(user);
        log.info("Kullanıcı pasife alındı: id={}", id);
    }
}
