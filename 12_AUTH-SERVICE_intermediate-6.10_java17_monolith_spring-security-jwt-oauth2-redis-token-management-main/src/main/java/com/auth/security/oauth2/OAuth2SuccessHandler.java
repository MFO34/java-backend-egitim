package com.auth.security.oauth2;

import com.auth.entity.User;
import com.auth.security.JwtService;
import com.auth.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * OAUTH2 BAŞARI HANDLER'I
 * ========================
 * OAuth2 girişi başarılı olunca Spring Security bu sınıfı çağırır.
 * SimpleUrlAuthenticationSuccessHandler'ı extend ederek redirect davranışını özelleştiririz.
 *
 * Akış:
 *   OAuth2 girişi başarılı
 *     → CustomOAuth2UserService.loadUser() çağrıldı (kullanıcı DB'de oluşturuldu/güncellendi)
 *     → OAuth2SuccessHandler.onAuthenticationSuccess() çağrılır
 *     → JWT token üretilir
 *     → Frontend'e redirect: http://localhost:3000/oauth2/callback?token=xxx&refreshToken=yyy
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    // JWT işlemleri için servis
    private final JwtService jwtService;

    // Token kaydetme/blacklist için servis
    private final TokenService tokenService;

    // application.yml'den: app.oauth2.redirect-success-uri
    @Value("${app.oauth2.redirect-success-uri}")
    private String redirectSuccessUri;

    /**
     * OAuth2 giriş başarılı olduğunda çağrılır.
     * JWT üretip frontend'e redirect eder.
     *
     * @param request        HTTP isteği (IP, User-Agent için)
     * @param response       HTTP yanıtı (redirect için)
     * @param authentication Kimlik doğrulama nesnesi (CustomOAuth2User içeriyor)
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        // 1. Authentication'dan CustomOAuth2User'ı al
        var oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        // 2. CustomOAuth2User içinden User entity'yi al
        var user = oAuth2User.getUser();

        log.info("OAuth2 başarılı giriş: userId={}, email={}", user.getId(), user.getEmail());

        // 3. JWT access token üret
        var accessToken = jwtService.generateAccessToken(user);

        // 4. Refresh token üret ve DB'ye kaydet
        var refreshTokenValue = jwtService.generateRefreshToken(user);

        // 5. IP ve User-Agent bilgisini al (refresh token için)
        var ipAddress  = getClientIp(request);
        var userAgent  = request.getHeader("User-Agent");

        // 6. Refresh token'ı veritabanına kaydet
        tokenService.saveRefreshToken(user, refreshTokenValue, ipAddress, userAgent);

        // 7. Redirect URL'ini oluştur: ?token=xxx&refreshToken=yyy
        var redirectUrl = UriComponentsBuilder
            .fromUriString(redirectSuccessUri)         // http://localhost:3000/oauth2/callback
            .queryParam("token", accessToken)          // access token URL'de (kısa ömürlü, ok)
            .queryParam("refreshToken", refreshTokenValue) // refresh token
            .build()
            .toUriString();

        log.debug("OAuth2 redirect URL: {}", redirectSuccessUri); // Güvenlik: token'ı loglama

        // 8. Frontend'e yönlendir
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    /**
     * İstemcinin gerçek IP adresini al.
     * Proxy arkasındaysa X-Forwarded-For header'ına bak.
     */
    private String getClientIp(HttpServletRequest request) {
        // Proxy/load balancer arkasında gerçek IP bu header'da olur
        var xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // "ip1, ip2, ip3" formatında — ilki gerçek istemci IP'si
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
