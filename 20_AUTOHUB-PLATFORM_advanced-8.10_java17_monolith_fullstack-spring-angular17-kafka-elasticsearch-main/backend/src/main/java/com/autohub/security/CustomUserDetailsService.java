package com.autohub.security;

// Spring Security ve proje importları
import com.autohub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Özel Kullanıcı Detayları Servisi
 *
 * Spring Security'nin UserDetailsService arayüzünü implemente eder.
 * Kimlik doğrulama sırasında kullanıcı bilgilerini veritabanından yükler.
 *
 * OOP Prensibi - POLİMORFİZM (Polymorphism):
 * UserDetailsService arayüzünü implemente ederek Spring Security ile uyumlu çalışır.
 *
 * OOP Prensibi - SOYUTLAMA (Abstraction):
 * loadUserByUsername() metodu, veritabanı detaylarını Spring Security'den gizler.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    // Kullanıcı veritabanı işlemleri için repository
    private final UserRepository userRepository;

    /**
     * Kullanıcı adına göre kullanıcı detaylarını yükler
     * Spring Security kimlik doğrulama sırasında bu metodu çağırır
     *
     * @param username Giriş yapan kullanıcının adı
     * @return UserDetails nesnesi (User entity'si UserDetails'ı implemente ediyor)
     * @throws UsernameNotFoundException Kullanıcı bulunamazsa
     */
    @Override
    @Transactional(readOnly = true)  // Sadece okuma - veritabanı optimizasyonu
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Kullanıcı bilgileri yükleniyor: username={}", username);

        // Kullanıcıyı veritabanından yükle
        // User entity'si UserDetails'ı implemente ettiği için doğrudan döndürülebilir
        return userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> {
                    log.error("Kullanıcı bulunamadı: username={}", username);
                    // Spring Security bu hatayı yakalayarak "Bad Credentials" mesajı üretir
                    return new UsernameNotFoundException("Kullanıcı bulunamadı: " + username);
                });
    }
}
