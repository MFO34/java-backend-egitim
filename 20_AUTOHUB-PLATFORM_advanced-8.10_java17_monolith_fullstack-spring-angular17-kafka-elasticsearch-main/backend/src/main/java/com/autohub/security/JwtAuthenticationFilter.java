package com.autohub.security;

// Spring Security ve Servlet importları
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Kimlik Doğrulama Filtresi
 *
 * Her HTTP isteğinde çalışır (OncePerRequestFilter) ve:
 * 1. Authorization header'dan JWT token'ı çıkarır
 * 2. Token'ı doğrular
 * 3. Kullanıcı bilgilerini SecurityContext'e yükler
 *
 * OOP Prensibi - KALITIM (Inheritance):
 * OncePerRequestFilter'ı extend eder - her istekte bir kez çalışan filter pattern
 *
 * OOP Prensibi - POLİMORFİZM (Polymorphism):
 * doFilterInternal metodu override edilir - polimorfik davranış
 */
@Component
@RequiredArgsConstructor  // Constructor injection
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // JWT token işlemleri için sağlayıcı
    private final JwtTokenProvider jwtTokenProvider;

    // Kullanıcı detaylarını veritabanından yüklemek için
    private final UserDetailsService userDetailsService;

    /**
     * Her HTTP isteğinde çalışan filter metodu
     * Önce token kontrol edilir, geçerliyse kullanıcı authenticate edilir
     *
     * @param request HTTP isteği
     * @param response HTTP yanıtı
     * @param filterChain Sonraki filter zinciri
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Authorization header'dan token'ı çıkar
        String token = extractTokenFromRequest(request);

        // Token varsa işle, yoksa zinciri devam ettir (public endpoint'ler için)
        if (StringUtils.hasText(token)) {
            try {
                // Token yapısal olarak geçerli mi?
                if (jwtTokenProvider.validateToken(token)) {
                    // Token'dan kullanıcı adını çıkar
                    String username = jwtTokenProvider.extractUsername(token);

                    // Güvenlik bağlamında zaten kimlik doğrulanmış kullanıcı var mı?
                    // Varsa tekrar işleme gerek yok (performans optimizasyonu)
                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                        // Kullanıcı bilgilerini veritabanından yükle
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                        // Token kullanıcı detayları ile geçerli mi?
                        if (jwtTokenProvider.isTokenValid(token, userDetails)) {
                            // Kimlik doğrulama nesnesi oluştur
                            UsernamePasswordAuthenticationToken authToken =
                                    new UsernamePasswordAuthenticationToken(
                                            userDetails,          // Principal (kimlik)
                                            null,                 // Credentials (şifre - null çünkü JWT ile doğrulandı)
                                            userDetails.getAuthorities()  // Yetkiler (roller)
                                    );

                            // İstek detaylarını ekle (IP adresi vs.)
                            authToken.setDetails(
                                    new WebAuthenticationDetailsSource().buildDetails(request));

                            // Güvenlik bağlamına kimlik doğrulama bilgilerini ekle
                            // Bu sayede bu istek boyunca kullanıcı "authenticated" kabul edilir
                            SecurityContextHolder.getContext().setAuthentication(authToken);

                            log.debug("Kullanıcı kimliği doğrulandı: username={}", username);
                        }
                    }
                }
            } catch (Exception e) {
                // Token işleme hatası - log yaz ama isteği engelleme (sonraki filter karar verir)
                log.error("JWT token işleme hatası: {}", e.getMessage());
                // SecurityContext'i temizle - güvenlik için
                SecurityContextHolder.clearContext();
            }
        }

        // Zincirdeki bir sonraki filter'a geç
        filterChain.doFilter(request, response);
    }

    /**
     * HTTP isteğinden Bearer token'ı çıkarır
     *
     * Authorization header formatı: "Bearer <token>"
     *
     * @param request HTTP isteği
     * @return JWT token string'i veya null (token yoksa)
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        // Authorization header'ı oku
        String authHeader = request.getHeader("Authorization");

        // Header var mı ve "Bearer " ile başlıyor mu?
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            // "Bearer " (7 karakter) sonrasını al - bu JWT token'ıdır
            return authHeader.substring(7);
        }

        // Header yoksa veya format yanlışsa null döndür
        return null;
    }

    /**
     * Hangi endpoint'lerde bu filter'ın atlanacağını belirler
     * Public endpoint'ler (login, register) için filter atlanır
     *
     * @param request HTTP isteği
     * @return true ise filter atlanır
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // Auth endpoint'leri public - token gerekmez
        return path.startsWith("/api/auth/") ||
               path.startsWith("/api/public/") ||
               path.equals("/actuator/health") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs");
    }
}
