package com.auth.security;

import com.auth.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ÖZEL KULLANICI DETAYLARI SERVİSİ
 * ==================================
 * UserDetailsService: Spring Security'nin kullanıcı yükleme arayüzü.
 * loadUserByUsername(String username): Kullanıcıyı DB'den yükler.
 *
 * Bu sınıf nerede devreye girer?
 *   1. JwtAuthFilter: Token'dan e-posta alır → bu servis DB'den user yükler
 *   2. AuthenticationManager: login sırasında çağrılır
 *   3. Spring Security, dönen UserDetails nesnesini authenticate eder
 *
 * @Transactional(readOnly = true):
 *   User.roles lazily yükleniyor → transaction açık olmalı.
 *   FetchType.EAGER olduğundan aslında gerekmez ama best practice.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * E-posta ile kullanıcı yükle.
     * username parametresi burada e-posta adresidir.
     * Bulunamazsa UsernameNotFoundException → Spring Security 401 döndürür.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
            .orElseThrow(() ->
                new UsernameNotFoundException("Kullanıcı bulunamadı: " + email)
            );
    }
}
