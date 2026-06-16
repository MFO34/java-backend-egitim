package com.auth.config;

import com.auth.security.CustomUserDetailsService;
import com.auth.security.filter.JwtAuthFilter;
import com.auth.security.oauth2.OAuth2FailureHandler;
import com.auth.security.oauth2.OAuth2SuccessHandler;
import com.auth.security.oauth2.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * GÜVENLİK YAPILANDIRMASI
 * ========================
 * Spring Security 6.x'in lambda DSL (domain-specific language) yapılandırması.
 *
 * Bu sınıf 4 temel şeyi yapar:
 *   1. Hangi endpoint'ler herkese açık, hangisi JWT gerektirir → SecurityFilterChain
 *   2. CORS: Hangi origin'den istek kabul ederiz (frontend URL)
 *   3. CSRF: REST API olduğundan devre dışı (JWT stateless koruma sağlar)
 *   4. OAuth2: Google/GitHub girişi ve success/failure handler'ları
 *
 * @EnableMethodSecurity:
 *   Controller metodlarında @PreAuthorize, @PostAuthorize, @Secured kullanmak için gerekli.
 *   prePostEnabled=true (default) → @PreAuthorize/@PostAuthorize
 *   securedEnabled=true → @Secured("ROLE_ADMIN")
 *
 * SecurityFilterChain nedir?
 *   Spring Security, gelen her HTTP isteğini bir filtre zincirinden geçirir.
 *   Bu zinciri burada yapılandırıyoruz.
 *   JwtAuthFilter → UsernamePasswordAuthenticationFilter → ... → DispatcherServlet
 */
@Configuration
@EnableWebSecurity        // Spring Security'yi etkinleştir
@EnableMethodSecurity(    // Metod düzeyinde güvenliği etkinleştir
    prePostEnabled = true,    // @PreAuthorize, @PostAuthorize
    securedEnabled = true     // @Secured("ROLE_ADMIN")
)
@RequiredArgsConstructor
public class SecurityConfig {

    // UserDetails yüklemek için servis
    private final CustomUserDetailsService userDetailsService;

    // JWT doğrulama filtresi (her istekte çalışır)
    private final JwtAuthFilter jwtAuthFilter;

    // OAuth2 kullanıcı servisi (Google/GitHub'dan kullanıcı bilgisi alır)
    private final CustomOAuth2UserService oAuth2UserService;

    // OAuth2 başarı/başarısızlık handler'ları
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;

    /**
     * ANA GÜVENLİK FİLTRE ZİNCİRİ
     * ============================
     * Tüm HTTP istekleri bu zincirden geçer.
     * Lambda DSL: her konfigürasyonu .configure(c -> ...) formatında yazar.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            // 1. CSRF (Cross-Site Request Forgery) KORUMASI — DEVRE DIŞI
            // REST API'larda CSRF token gerekmez çünkü:
            //   - Tarayıcı cookie kullanmıyoruz (JWT Header'da taşınır)
            //   - Her istek kendi JWT'sini getiriyor
            //   - CSRF saldırısı ancak cookie-based auth'da mümkün
            .csrf(AbstractHttpConfigurer::disable)

            // 2. CORS (Cross-Origin Resource Sharing) YAPILANDIRMASI
            // Frontend (React: localhost:3000) → Backend (localhost:8080) farklı origin
            // Tarayıcı CORS kontrolü yapar, bizim izin vermemiz gerekiyor
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 3. SESSION YÖNETİMİ — STATELESS
            // JWT kullandığımız için sunucu tarafında session saklamıyoruz.
            // Her istek bağımsız, JWT ile kimliğini kanıtlar.
            // STATELESS → HttpSession oluşturulmaz, SecurityContext saklanmaz
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 4. ENDPOINT YETKİLENDİRME KURALLARI
            // Sıralama önemli: ilk eşleşen kural geçerli
            .authorizeHttpRequests(auth -> auth
                // Herkese açık endpoint'ler (token gerekmez)
                .requestMatchers(
                    "/api/v1/auth/**",           // register, login, refresh, verify-email, vb.
                    "/oauth2/**",                // OAuth2 callback URL'leri
                    "/login/oauth2/**",          // Spring OAuth2 callback
                    "/swagger-ui/**",            // Swagger UI
                    "/swagger-ui.html",
                    "/v3/api-docs/**",           // OpenAPI JSON
                    "/actuator/health"           // Health check
                ).permitAll()

                // Sadece ADMIN rolü
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                // Geri kalan her şey → kimlik doğrulama gerekir
                .anyRequest().authenticated()
            )

            // 5. AUTHENTICATION PROVIDER — DaoAuthenticationProvider
            // Spring Security'ye nasıl kullanıcı doğrulayacağını söyler.
            // UserDetailsService + PasswordEncoder kullanır.
            .authenticationProvider(authenticationProvider())

            // 6. JWT FİLTRESİ — UsernamePasswordAuthenticationFilter'dan ÖNCE
            // Her istekte JWT'yi kontrol eder ve SecurityContext'e yükler.
            // Önce koymazsan, JWT kontrolü yapılmadan authentication denenebilir.
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

            // 7. OAUTH2 GİRİŞ YAPILANDIRMASI
            .oauth2Login(oauth2 -> oauth2
                // Kullanıcı bilgisini Google/GitHub'dan çeken servis
                .userInfoEndpoint(userInfo ->
                    userInfo.userService(oAuth2UserService))
                // Başarılı girişte çağrılacak handler
                .successHandler(oAuth2SuccessHandler)
                // Başarısız girişte çağrılacak handler
                .failureHandler(oAuth2FailureHandler)
            );

        return http.build();
    }

    /**
     * CORS YAPILANDIRMASI
     * ===================
     * Cross-Origin Resource Sharing — farklı origin'den gelen isteklere izin ver.
     *
     * Neden gerekli?
     *   Tarayıcı, frontend (localhost:3000) → backend (localhost:8080) isteğini
     *   güvenlik nedeniyle varsayılan olarak engeller. Bu yapılandırma ile izin veririz.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();

        // İzin verilen origin'ler (frontend URL'leri)
        config.setAllowedOriginPatterns(List.of(
            "http://localhost:3000",    // React dev server
            "http://localhost:5173",    // Vite dev server
            "https://*.yourdomain.com"  // Production domain (değiştir)
        ));

        // İzin verilen HTTP metodları
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // İzin verilen header'lar
        config.setAllowedHeaders(List.of(
            "Authorization",    // JWT token
            "Content-Type",     // JSON body
            "X-Requested-With"  // AJAX istek marker'ı
        ));

        // Cookie/credential gönderimine izin ver (OAuth2 için)
        config.setAllowCredentials(true);

        // Preflight cache süresi (OPTIONS isteği tekrar yapılmasın)
        config.setMaxAge(3600L);

        // Tüm endpoint'lere bu CORS kuralını uygula
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * AUTHENTICATION PROVIDER
     * =======================
     * Spring Security'ye kullanıcı nasıl doğrulanacağını söyler.
     * DaoAuthenticationProvider:
     *   1. UserDetailsService ile kullanıcıyı DB'den yükle
     *   2. PasswordEncoder ile gelen şifreyi hash'lenmiş ile karşılaştır
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        var provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService); // DB'den kullanıcı yükle
        provider.setPasswordEncoder(passwordEncoder());     // BCrypt karşılaştırması
        return provider;
    }

    /**
     * AUTHENTICATION MANAGER
     * ======================
     * AuthController'da programatik authentication için gerekli.
     * login endpoint'inde: authenticationManager.authenticate(token)
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * ŞIFRE ENCODER — BCrypt
     * ======================
     * BCrypt: password hashing algoritması.
     *   - Her seferinde farklı salt (tuz) üretir → aynı şifre → farklı hash
     *   - Brute force'a karşı kasıtlı yavaş (work factor: 10)
     *   - "123456" → "$2a$10$randomSalt...hashedValue"
     *
     * ASLA şifreleri plain-text saklamayın!
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // work factor: 10 (default)
    }
}
