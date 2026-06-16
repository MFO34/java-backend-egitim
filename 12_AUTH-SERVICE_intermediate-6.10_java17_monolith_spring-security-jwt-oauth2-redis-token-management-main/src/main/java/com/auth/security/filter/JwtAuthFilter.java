package com.auth.security.filter;

import com.auth.security.JwtService;
import com.auth.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT KİMLİK DOĞRULAMA FİLTRESİ
 * =================================
 * OncePerRequestFilter:
 *   Her HTTP isteği için TEK SEFERLIK çalışan filtre.
 *   Spring'in doFilter zincirinde otomatik yönetilir.
 *   Neden OncePerRequest? Bazı framework'lerde filtreler iki kez çağrılabilir.
 *   Bu sınıf bunu önler.
 *
 * Filtre ne yapar?
 *   1. Authorization başlığından Bearer token'ı al
 *   2. Token blacklist'te mi? → 401
 *   3. Token geçerli mi? → Kullanıcıyı DB'den yükle
 *   4. SecurityContextHolder'a koy → İstek "authenticated" sayılır
 *
 * SecurityFilterChain'deki yeri:
 *   ... → LogoutFilter → JwtAuthFilter → UsernamePasswordAuthenticationFilter → ...
 *
 * SecurityContext nedir?
 *   Her thread'e ait (ThreadLocal) güvenlik bilgisi konteyneri.
 *   Mevcut kullanıcı kim? → SecurityContextHolder.getContext().getAuthentication()
 */
@Component
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokenService tokenService;

    public JwtAuthFilter(JwtService jwtService,
                         UserDetailsService userDetailsService,
                         TokenService tokenService) {
        this.jwtService       = jwtService;
        this.userDetailsService = userDetailsService;
        this.tokenService     = tokenService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Authorization başlığını al
        var authHeader = request.getHeader("Authorization");

        // Bearer token yoksa — bu filtreden geç, sonraki filtreye devret
        // Public endpoint'ler (login, register) buradan geçer
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. "Bearer " önekini at, salt token değerini al
        var jwt = authHeader.substring(7);

        // 3. Token'dan e-posta adresini çıkar
        String userEmail;
        try {
            userEmail = jwtService.extractEmail(jwt);
        } catch (Exception e) {
            log.debug("JWT e-posta çıkarılamadı: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        // 4. Kullanıcı var ve henüz authenticate edilmemiş mi?
        //    (zaten authenticate edildiyse tekrar yapma)
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 5. TOKEN BLACKLİST KONTROLÜ (Redis)
            //    Kullanıcı logout yaptıysa token blacklist'te olur
            var jti = jwtService.extractJti(jwt);
            if (tokenService.isTokenBlacklisted(jti)) {
                log.debug("Token blacklist'te: jti={}", jti);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Token geçersiz kılınmış. Lütfen tekrar giriş yapın.");
                return;
            }

            // 6. Kullanıcıyı veritabanından yükle
            //    UserDetailsService.loadUserByUsername(email) çağrılır
            var userDetails = userDetailsService.loadUserByUsername(userEmail);

            // 7. Token hâlâ geçerli mi? (imza + süre kontrolü)
            if (jwtService.isTokenValid(jwt, (com.auth.entity.User) userDetails)) {

                /**
                 * 8. Authentication nesnesini oluştur ve SecurityContext'e koy
                 *
                 * UsernamePasswordAuthenticationToken(userDetails, credentials, authorities):
                 *   credentials = null (şifre artık gerekmez — token doğrulandı)
                 *   authorities = kullanıcının rolleri ve izinleri
                 *
                 * Bu token SecurityContextHolder'a konunca:
                 *   @PreAuthorize, hasRole(), hasAuthority() çalışabilir.
                 *   Controller'da: @AuthenticationPrincipal User user → mevcut kullanıcı
                 */
                var authToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,                          // Şifre artık gerekmiyor
                    userDetails.getAuthorities()   // Roller + İzinler
                );

                // İstek detaylarını ekle (IP, session vb.)
                authToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // SecurityContext'e koy — bu istek boyunca geçerli
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("Kullanıcı authenticate edildi: {}", userEmail);
            }
        }

        // 9. Sonraki filtreye devret
        filterChain.doFilter(request, response);
    }
}
