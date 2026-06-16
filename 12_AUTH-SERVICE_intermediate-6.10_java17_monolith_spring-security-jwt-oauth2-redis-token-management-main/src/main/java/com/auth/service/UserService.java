package com.auth.service;

import com.auth.dto.request.UpdateProfileRequest;
import com.auth.dto.response.UserResponse;
import com.auth.entity.User;
import com.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * KULLANICI SERVİSİ
 * =================
 * Kullanıcı profil görüntüleme ve güncelleme işlemleri.
 *
 * Güvenlik notu:
 *   Bu servisteki metodlar SecurityContext'ten gelen authenticated kullanıcıya ait.
 *   Controller'da @PreAuthorize ile "sadece kendi profilini görebilir" kısıtlaması var.
 *   Admin metodları ise @Secured("ROLE_ADMIN") ile korunur.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /**
     * Giriş yapmış kullanıcının profilini getir.
     *
     * @param email SecurityContext'ten gelen e-posta (JWT'den çözülür)
     * @return Kullanıcı bilgileri (şifre hariç)
     */
    public UserResponse getMyProfile(String email) {
        var user = findByEmailOrThrow(email);
        return mapToUserResponse(user);
    }

    /**
     * Kullanıcı profilini güncelle.
     * Sadece ad, soyad ve profil resmi güncellenebilir.
     * E-posta ve şifre değiştirme ayrı endpoint'lerdir.
     */
    @Transactional
    public UserResponse updateMyProfile(String email, UpdateProfileRequest request) {
        var user = findByEmailOrThrow(email);

        // Null değilse güncelle (partial update)
        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.profilePictureUrl() != null) {
            user.setProfilePictureUrl(request.profilePictureUrl());
        }

        var updatedUser = userRepository.save(user);
        log.info("Profil güncellendi: userId={}", updatedUser.getId());

        return mapToUserResponse(updatedUser);
    }

    /**
     * TÜM KULLANICILARI LİSTELE (Sadece Admin)
     * Sayfalama eklenebilir — şimdilik basit liste.
     */
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
            .stream()
            .map(this::mapToUserResponse)
            .toList();
    }

    /**
     * ID ile kullanıcı getir (Admin için).
     */
    public UserResponse getUserById(Long id) {
        var user = userRepository.findById(id)
            .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: id=" + id));
        return mapToUserResponse(user);
    }

    /**
     * Kullanıcıyı devre dışı bırak/aktif et (Admin için).
     * Banka: hesap silme yerine disable tercih edilir.
     */
    @Transactional
    public UserResponse toggleUserEnabled(Long id) {
        var user = userRepository.findById(id)
            .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: id=" + id));

        user.setEnabled(!user.isEnabled()); // Toggle
        var updated = userRepository.save(user);

        log.info("Kullanıcı durumu değiştirildi: userId={}, enabled={}", id, updated.isEnabled());
        return mapToUserResponse(updated);
    }

    /**
     * Kilitli hesabı aç (Admin için).
     * Çok fazla başarısız giriş denemesinden kilit açılır.
     */
    @Transactional
    public UserResponse unlockUser(Long id) {
        var user = userRepository.findById(id)
            .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: id=" + id));

        user.setAccountLocked(false);
        user.setFailedLoginAttempts(0);
        var updated = userRepository.save(user);

        log.info("Hesap kilidi açıldı: userId={}", id);
        return mapToUserResponse(updated);
    }

    // ========== YARDIMCI METODLAR ==========

    private User findByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: " + email));
    }

    /**
     * User entity → UserResponse DTO dönüşümü.
     * MapStruct kullanmak yerine burada manuel map ediyoruz (basit olduğu için).
     * Şifre, hassas alanlar DTO'ya dahil edilmez!
     */
    private UserResponse mapToUserResponse(User user) {
        // Rol isimlerini Set<String>'e çevir
        var roles = user.getRoles().stream()
            .map(role -> role.getName())
            .toList();

        return new UserResponse(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getEmail(),
            user.getProvider().name(),
            user.isEmailVerified(),
            user.isEnabled(),
            user.getProfilePictureUrl(),
            roles,
            user.getCreatedAt()
        );
    }
}
