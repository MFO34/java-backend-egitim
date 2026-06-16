package com.autohub.security;

// Spring Security importları
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security Konfigürasyonu
 *
 * JWT tabanlı stateless kimlik doğrulama ve yetkilendirme kurallarını tanımlar.
 *
 * OOP Prensibi - KAPSÜLLEME (Encapsulation):
 * Güvenlik kuralları bu sınıfta kapsüllenerek merkezi yönetim sağlanır.
 *
 * @Configuration: Bean tanımları içerdiğini belirtir
 * @EnableWebSecurity: Spring Security web güvenliğini aktif eder
 * @EnableMethodSecurity: Metot seviyesinde güvenlik anotasyonlarını aktif eder
 *   (örn: @PreAuthorize("hasRole('ADMIN')"))
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)  // @PreAuthorize, @PostAuthorize aktif
@RequiredArgsConstructor
public class SecurityConfig {

    // JWT filter - her istekte çalışır
    private final JwtAuthenticationFilter jwtAuthFilter;
    // Kullanıcı detayları servisi - kimlik doğrulama için
    private final UserDetailsService userDetailsService;

    /**
     * Ana güvenlik filter zincirini konfigüre eder
     * Hangi endpoint'lerin korumalı olduğu burada belirlenir
     *
     * @param http HttpSecurity builder nesnesi
     * @return Yapılandırılmış SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF korumasını devre dışı bırak - JWT ile stateless API için gerekli
            // CSRF sadece cookie-based session'larda gereklidir
            .csrf(AbstractHttpConfigurer::disable)

            // CORS konfigürasyonu - Angular frontend'e izin ver
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // URL bazlı yetkilendirme kuralları
            .authorizeHttpRequests(auth -> auth
                // Public endpoint'ler - kimlik doğrulama gerektirmez
                .requestMatchers(
                    "/api/auth/**",           // Login, register, token yenileme
                    "/api/public/**",          // Genel araç listesi
                    "/actuator/health",        // Sağlık kontrolü
                    "/swagger-ui/**",          // API dokümantasyonu
                    "/v3/api-docs/**"          // OpenAPI spesifikasyonu
                ).permitAll()

                // Admin endpoint'leri - sadece ADMIN rolü erişebilir
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // Yönetici işlemleri - ADMIN veya MANAGER rolü
                .requestMatchers("/api/vehicles/create",
                                 "/api/vehicles/update/**",
                                 "/api/vehicles/delete/**").hasAnyRole("ADMIN", "MANAGER")

                // Müşteri endpoint'leri - ADMIN, MANAGER veya EMPLOYEE erişebilir
                .requestMatchers("/api/customers/**").hasAnyRole("ADMIN", "MANAGER", "EMPLOYEE")

                // Diğer tüm istekler kimlik doğrulama gerektirir
                .anyRequest().authenticated()
            )

            // Session yönetimi: STATELESS - JWT ile session tutulmaz
            // Her istek kendi token'ı ile authenticate edilir
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Authentication provider - kimlik doğrulama mekanizması
            .authenticationProvider(authenticationProvider())

            // JWT filter'ı UsernamePasswordAuthenticationFilter'dan önce ekle
            // Bu sayede her istek önce JWT ile doğrulanır
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Kimlik doğrulama sağlayıcısı
     * Kullanıcı adı/şifre ile doğrulama için DaoAuthenticationProvider kullanır
     *
     * @return Yapılandırılmış AuthenticationProvider
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        // Kullanıcıları veritabanından yükleyen servis
        authProvider.setUserDetailsService(userDetailsService);
        // BCrypt ile hash'lenmiş şifreleri doğrular
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Authentication Manager - programatik kimlik doğrulama için
     * AuthController'da kullanılır
     *
     * @param config Spring'in authentication konfigürasyonu
     * @return AuthenticationManager instance'ı
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Şifre encoder - BCrypt kullanır
     * BCrypt: Güvenli, adaptif hash fonksiyonu
     * strength=12: Hash hesaplama güçlüğü (2^12 iterasyon) - brute force'u zorlaştırır
     *
     * @return BCryptPasswordEncoder instance'ı
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt strength parametresi: 4-31 arası, yükseldikçe daha güvenli ama yavaş
        // 12 değeri güvenlik/performans dengesi açısından idealdir
        return new BCryptPasswordEncoder(12);
    }

    /**
     * CORS (Cross-Origin Resource Sharing) konfigürasyonu
     * Angular frontend (localhost:4200) arka ucumuza istek yapabilsin diye gerekli
     *
     * @return CORS konfigürasyon kaynağı
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // İzin verilen origin'ler - Angular geliştirme sunucusu
        configuration.setAllowedOrigins(List.of(
            "http://localhost:4200",    // Angular dev server
            "http://localhost:80",      // Production nginx
            "https://autohub.com"       // Production domain
        ));

        // İzin verilen HTTP metotları
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // İzin verilen request header'ları
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",    // JWT token header'ı
            "Content-Type",     // İstek içerik tipi
            "Accept",           // Kabul edilen yanıt tipi
            "X-Requested-With", // Ajax istek header'ı
            "Origin"            // Kaynak origin
        ));

        // Cookie ve Authorization header'ların gönderilmesine izin ver
        configuration.setAllowCredentials(true);

        // Preflight isteği cache süresi (saniye) - 1 saat
        configuration.setMaxAge(3600L);

        // Tüm path'lere uygula
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
