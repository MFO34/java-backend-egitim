package com.auth.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * OAUTH2 BAŞARISIZLIK HANDLER'I
 * ==============================
 * OAuth2 girişi başarısız olunca Spring Security bu sınıfı çağırır.
 *
 * Neden başarısız olabilir?
 *   - Kullanıcı Google/GitHub'da "İzin Verme" dedi
 *   - Provider yanıt vermedi (timeout)
 *   - state parametresi eşleşmedi (CSRF koruması)
 *   - Provider'dan hata kodu döndü
 */
@Component
@Slf4j
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    // application.yml'den: app.oauth2.redirect-failure-uri
    @Value("${app.oauth2.redirect-failure-uri}")
    private String redirectFailureUri;

    /**
     * OAuth2 giriş başarısız olduğunda çağrılır.
     * Hata mesajıyla birlikte frontend'e redirect eder.
     */
    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                         HttpServletResponse response,
                                         AuthenticationException exception) throws IOException {

        // Hata mesajını logla (detaylı, çünkü bu sunucu tarafı log)
        log.warn("OAuth2 giriş başarısız: {}", exception.getMessage());

        // URL-encode edilmiş hata mesajı (özel karakterler URL'de bozulmasın)
        var errorMessage = URLEncoder.encode(
            "OAuth2 girişi başarısız: " + exception.getMessage(),
            StandardCharsets.UTF_8
        );

        // Frontend'e yönlendir: /login?error=OAuth2+girisi+basarisiz
        var redirectUrl = UriComponentsBuilder
            .fromUriString(redirectFailureUri)    // http://localhost:3000/login
            .queryParam("error", errorMessage)    // hata mesajı
            .build()
            .toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
