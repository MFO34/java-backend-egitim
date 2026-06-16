package com.autohub.security;

// JWT kütüphanesi ve Spring importları
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * JWT Token Sağlayıcı - Token oluşturma, doğrulama ve ayrıştırma işlemleri
 *
 * JSON Web Token (JWT) kullanarak stateless kimlik doğrulama yapılır.
 * Kullanıcı bilgileri token içine gömülür, sunucu session tutmak zorunda kalmaz.
 *
 * JWT Yapısı: header.payload.signature
 * - Header: token tipi ve algoritma (HS256)
 * - Payload: kullanıcı bilgileri (claims)
 * - Signature: token bütünlüğünü doğrular
 *
 * OOP Prensibi - KAPSÜLLEME (Encapsulation):
 * JWT işlem detayları bu sınıfta gizlenir, dışarıya sadece soyut metodlar sunulur.
 */
@Component  // Spring bean olarak kayıt edilir
@Slf4j      // Loglama için
public class JwtTokenProvider {

    /**
     * JWT imzalama için gizli anahtar - application.yml'den okunur
     * Minimum 256-bit (32 karakter) olmalı - HS256 algoritması için gereksinim
     */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Access token geçerlilik süresi (milisaniye)
     * Varsayılan: 86400000 ms = 24 saat
     */
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * Refresh token geçerlilik süresi (milisaniye)
     * Varsayılan: 604800000 ms = 7 gün
     */
    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    /**
     * Access token üretir - kullanıcı kimliğini içerir
     *
     * @param authentication Spring Security authentication nesnesi
     * @return JWT access token string'i
     */
    public String generateAccessToken(Authentication authentication) {
        // Kimlik doğrulanmış kullanıcı bilgilerini al
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // Token içine eklenecek ek bilgiler (claims)
        Map<String, Object> extraClaims = new HashMap<>();
        // Kullanıcı rollerini token'a ekle - frontend yetkilendirme için
        Set<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        extraClaims.put("roles", roles);
        extraClaims.put("tokenType", "ACCESS");  // Token tipini belirt

        // Token oluştur
        return buildToken(extraClaims, userDetails.getUsername(), jwtExpiration);
    }

    /**
     * Refresh token üretir - access token yenileme için kullanılır
     *
     * @param username Kullanıcı adı
     * @return JWT refresh token string'i
     */
    public String generateRefreshToken(String username) {
        // Refresh token minimal bilgi içerir (sadece tip)
        Map<String, Object> claims = new HashMap<>();
        claims.put("tokenType", "REFRESH");

        return buildToken(claims, username, refreshExpiration);
    }

    /**
     * Token oluşturma işleminin temel metodu
     * Access ve Refresh token için ortak yapıyı oluşturur
     *
     * @param extraClaims Token'a eklenecek ek bilgiler
     * @param subject Token sahibinin kullanıcı adı (subject)
     * @param expiration Token geçerlilik süresi (ms)
     * @return İmzalanmış JWT token string'i
     */
    private String buildToken(Map<String, Object> extraClaims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(extraClaims)           // Ek bilgileri ekle
                .setSubject(subject)               // Token sahibini belirt (username)
                .setIssuedAt(now)                  // Token oluşturma zamanı
                .setExpiration(expiryDate)         // Token bitiş zamanı
                .setIssuer("autohub-api")          // Token yayıncısı
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)  // HS256 ile imzala
                .compact();                        // Token'ı string'e dönüştür
    }

    /**
     * Token'dan kullanıcı adını çıkarır
     *
     * @param token JWT token string'i
     * @return Kullanıcı adı (subject)
     */
    public String extractUsername(String token) {
        // Subject claim'ini döndür - bu kullanıcı adıdır
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Token geçerli mi kontrol eder
     * Kullanıcı adı eşleşmeli ve token süresi dolmamış olmalı
     *
     * @param token Doğrulanacak JWT token
     * @param userDetails Spring Security kullanıcı detayları
     * @return Token geçerliyse true
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            // Token'dan kullanıcı adını çıkar
            final String username = extractUsername(token);
            // Kullanıcı adı eşleşiyor mu VE token süresi dolmamış mı?
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException ex) {
            // Token ayrıştırma hatası - geçersiz token
            log.error("JWT token doğrulama hatası: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Token'ın süresi dolmuş mu kontrol eder
     *
     * @param token Kontrol edilecek JWT token
     * @return Süresi dolmuşsa true
     */
    private boolean isTokenExpired(String token) {
        // Token'dan bitiş tarihini çıkar ve şimdiki zamanla karşılaştır
        return extractExpiration(token).before(new Date());
    }

    /**
     * Token'dan bitiş tarihini çıkarır
     *
     * @param token JWT token
     * @return Token bitiş tarihi
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Token'dan belirli bir claim'i çıkaran genel metot
     * Function parametresi ile istenen claim türü belirtilir (Generic method)
     *
     * @param token JWT token
     * @param claimsResolver Hangi claim'in alınacağını belirten fonksiyon
     * @param <T> Claim değerinin tipi
     * @return İstenen claim değeri
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        // Tüm claim'leri çıkar ve istenen claim'i fonksiyon ile al
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Token'dan tüm claim'leri çıkarır
     * Token imzasını doğrulayarak payload'ı parse eder
     *
     * @param token JWT token string'i
     * @return Tüm claim'leri içeren Claims nesnesi
     * @throws JwtException Token geçersiz veya süresi dolmuşsa
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())    // İmza doğrulama anahtarı
                .build()
                .parseClaimsJws(token)              // Token'ı parse et ve doğrula
                .getBody();                         // Payload (claims) kısmını al
    }

    /**
     * JWT imzalama için güvenli anahtar oluşturur
     * Base64 encoded secret'dan HMAC-SHA anahtarı üretir
     *
     * @return javax.crypto.SecretKey nesnesi
     */
    private Key getSigningKey() {
        // Secret key'i Base64'ten decode et
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        // HMAC-SHA256 için uygun anahtar oluştur
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Token geçerliliğini doğrular (userDetails olmadan)
     * Sadece token yapısı ve süresi kontrol edilir
     *
     * @param token Doğrulanacak token
     * @return Geçerliyse true
     */
    public boolean validateToken(String token) {
        try {
            // Token'ı parse etmeye çalış - başarılı ise geçerli
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (MalformedJwtException e) {
            // Token formatı hatalı
            log.error("Geçersiz JWT token formatı: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            // Token süresi dolmuş
            log.error("JWT token süresi dolmuş: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            // Desteklenmeyen JWT türü
            log.error("Desteklenmeyen JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            // Token boş veya null
            log.error("JWT token boş: {}", e.getMessage());
        }
        return false;
    }
}
