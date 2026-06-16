package com.auth.security;

import com.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * JWT SERVİSİ — Token üretme, doğrulama ve parse etme
 * ======================================================
 *
 * JWT YAPISI: Header.Payload.Signature
 * ─────────────────────────────────────
 * Header  (Base64): {"alg":"HS256","typ":"JWT"}
 * Payload (Base64): {"sub":"user@email.com","roles":["USER"],"jti":"uuid","exp":...}
 * Signature       : HMAC-SHA256(base64(header) + "." + base64(payload), secretKey)
 *
 * JJWT 0.12.x API Değişiklikleri:
 *   Eski: Jwts.parserBuilder().setSigningKey(key).build()
 *   Yeni: Jwts.parser().verifyWith(key).build()
 *   Eski: .setSubject() / .setExpiration()
 *   Yeni: .subject() / .expiration()
 */
@Component
@Slf4j
public class JwtService {

    // application.yml'den gelen Base64 kodlu gizli anahtar
    @Value("${jwt.secret}")
    private String jwtSecret;

    // Access token süresi (ms) — varsayılan 15 dakika
    @Value("${jwt.access-expiration-ms}")
    private long accessExpirationMs;

    // Refresh token süresi (ms) — varsayılan 7 gün
    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    /**
     * ACCESS TOKEN OLUŞTUR
     * =====================
     * Payload'a eklenen claims (iddialar):
     *   sub  → kullanıcı e-postası (subject)
     *   jti  → benzersiz token ID (blacklist için gerekli)
     *   uid  → kullanıcı ID
     *   roles → kullanıcı rolleri
     *   type → "access"
     *   iat  → oluşturulma zamanı
     *   exp  → sona erme zamanı
     */
    public String generateAccessToken(User user) {
        var extraClaims = new HashMap<String, Object>();

        // Kullanıcı ID'si — frontend'de kullanılabilir
        extraClaims.put("uid", user.getId());

        // Roller listesi: ["ROLE_USER", "ROLE_ADMIN"]
        var roles = user.getRoles().stream()
            .map(r -> "ROLE_" + r.getName())
            .collect(Collectors.toList());
        extraClaims.put("roles", roles);

        // İzinler: ["user:read", "admin:write"]
        var permissions = user.getAuthorities().stream()
            .map(a -> a.getAuthority())
            .filter(a -> !a.startsWith("ROLE_"))  // Sadece izinler
            .collect(Collectors.toList());
        extraClaims.put("permissions", permissions);

        // Token tipi
        extraClaims.put("type", "access");

        return buildToken(extraClaims, user, accessExpirationMs);
    }

    /**
     * REFRESH TOKEN OLUŞTUR
     * ======================
     * Refresh token sadece yeni access token almak için kullanılır.
     * Payload minimal tutulur — sadece subject ve jti yeterli.
     */
    public String generateRefreshToken(User user) {
        var extraClaims = new HashMap<String, Object>();
        extraClaims.put("type", "refresh");
        return buildToken(extraClaims, user, refreshExpirationMs);
    }

    /**
     * TOKEN OLUŞTURMA — Ortak yapı
     * Jwts.builder() ile imzalanmış JWT oluşturur.
     */
    private String buildToken(Map<String, Object> extraClaims, User user, long expiration) {
        var now = new Date();
        var expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
            // Tüm özel claim'ler (uid, roles, type vb.)
            .claims(extraClaims)
            // Subject: kullanıcı e-postası (UserDetailsService'de findByEmail için kullanılır)
            .subject(user.getEmail())
            // JTI: benzersiz token ID — blacklist için gerekli
            .id(UUID.randomUUID().toString())
            // Oluşturulma zamanı
            .issuedAt(now)
            // Son kullanma zamanı
            .expiration(expiryDate)
            // İmzalama: HMAC-SHA256
            .signWith(getSignKey())
            .compact();
    }

    /**
     * İMZALAMA ANAHTARI OLUŞTUR
     * ==========================
     * application.yml'deki Base64 kodlu secret'ı decode edip
     * HMAC-SHA256 için uygun SecretKey nesnesine çevirir.
     *
     * Önemli: Anahtar en az 256-bit (32 byte) olmalı!
     */
    private SecretKey getSignKey() {
        var keyBytes = Base64.getDecoder().decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * TOKEN'DAN E-POSTA ÇIKAR (Subject)
     * E-posta → UserDetailsService.loadUserByUsername() çağrılır.
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * TOKEN'DAN JTI ÇIKAR
     * Blacklist kontrolü için token'ın benzersiz kimliği.
     */
    public String extractJti(String token) {
        return extractClaim(token, Claims::getId);
    }

    /**
     * TOKEN'DAN SON KULLANMA TARİHİ ÇIKAR
     * Blacklist'e TTL hesaplaması için kullanılır.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * TOKEN'DAN ÖZEL CLAIM ÇIKAR
     * Generics + Function ile tip güvenli claim çıkarma.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        var claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * TÜM CLAIM'LERİ ÇIKAR
     * Token'ı parse eder ve imzayı doğrular.
     * JJWT 0.12.x: .parser().verifyWith(key).build()
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSignKey())    // İmza doğrulama anahtarı
            .build()
            .parseSignedClaims(token)   // Token'ı parse et + imzayı doğrula
            .getPayload();              // Payload (Claims) nesnesini al
    }

    /**
     * TOKEN GEÇERLİ Mİ?
     * 1. İmza doğru mu?
     * 2. Süresi dolmadı mı?
     * 3. Subject kullanıcı ile eşleşiyor mu?
     */
    public boolean isTokenValid(String token, User user) {
        try {
            var email = extractEmail(token);
            return email.equals(user.getEmail()) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * TOKEN SÜRESİ DOLDU MU?
     */
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * TOKEN'I DOĞRULA — Exception fırlatır (filter'da kullanılır)
     * ExpiredJwtException, SignatureException, MalformedJwtException
     * bunları JwtAuthFilter yakalar.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT süresi doldu: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("Geçersiz JWT imzası: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Hatalı JWT formatı: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Desteklenmeyen JWT türü: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claim'leri boş: {}", e.getMessage());
        }
        return false;
    }

    /**
     * TOKEN'IN KALAN SÜRESİ (milisaniye)
     * Blacklist TTL hesaplaması için kullanılır.
     */
    public long getRemainingValidity(String token) {
        var expiration = extractExpiration(token);
        return Math.max(0, expiration.getTime() - System.currentTimeMillis());
    }

    public long getAccessExpirationMs()  { return accessExpirationMs; }
    public long getRefreshExpirationMs() { return refreshExpirationMs; }
}
