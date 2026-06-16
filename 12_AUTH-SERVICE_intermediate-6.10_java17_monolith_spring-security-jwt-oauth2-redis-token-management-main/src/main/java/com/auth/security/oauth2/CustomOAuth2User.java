package com.auth.security.oauth2;

import com.auth.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

/**
 * ÖZEL OAUTH2 KULLANICI
 * =======================
 * OAuth2User arayüzünü sarar ve içinde User entity'sini taşır.
 * OAuth2SuccessHandler bu sınıftan User'ı alır → JWT üretir.
 *
 * OAuth2User: Google/GitHub'dan gelen kullanıcı bilgisi
 * User: Bizim veritabanımızdaki kullanıcı kaydı
 * CustomOAuth2User: İkisini birleştirir
 */
public class CustomOAuth2User implements OAuth2User {

    // Veritabanımızdaki kullanıcı kaydı
    private final User user;

    // Google/GitHub'dan gelen ham attribute'lar
    private final Map<String, Object> attributes;

    public CustomOAuth2User(User user, Map<String, Object> attributes) {
        this.user       = user;
        this.attributes = attributes;
    }

    // User entity'sinin Spring Security rolleri + izinleri
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getAuthorities();
    }

    // Google/GitHub attribute map'i
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    // OAuth2User name = e-posta (veya Google'ın name alanı)
    @Override
    public String getName() {
        return user.getEmail();
    }

    // Bizim User entity'miz
    public User getUser() {
        return user;
    }
}
