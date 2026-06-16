package com.autohub.controller;

// Proje ve Spring importları
import com.autohub.dto.request.LoginRequest;
import com.autohub.dto.request.RegisterRequest;
import com.autohub.dto.response.AuthResponse;
import com.autohub.entity.Role;
import com.autohub.entity.User;
import com.autohub.exception.DuplicateResourceException;
import com.autohub.repository.RoleRepository;
import com.autohub.repository.UserRepository;
import com.autohub.security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Kimlik Doğrulama Controller - Login, Register ve Token yönetimi
 *
 * OOP Prensibi - SORUMLULUK AYIRIMI:
 * Sadece kimlik doğrulama işlemlerinden sorumludur.
 *
 * Güvenlik Notu:
 * Bu endpoint'ler SecurityConfig'de "permitAll()" ile herkese açıktır.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Kullanıcı girişi
     * POST /api/auth/login
     *
     * @param request Kullanıcı adı ve şifre
     * @return JWT access + refresh token çifti
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login isteği: username={}", request.getUsername());

        try {
            // Spring Security ile kimlik doğrulama
            // Bu satır UserDetailsService.loadUserByUsername() ve PasswordEncoder'ı tetikler
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getPassword()
                )
            );

            // Kimlik doğrulandı - JWT token'ları oluştur
            User user = (User) authentication.getPrincipal();

            // Access token oluştur - kısa ömürlü
            String accessToken = jwtTokenProvider.generateAccessToken(authentication);
            // Refresh token oluştur - uzun ömürlü
            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

            // Kullanıcının rollerini çıkar
            Set<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

            // Başarılı giriş yanıtı oluştur
            AuthResponse response = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400L)  // 24 saat (saniye)
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(roles)
                .build();

            log.info("Başarılı giriş: username={}", user.getUsername());
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            // Hatalı şifre veya kullanıcı adı
            log.warn("Başarısız giriş denemesi: username={}", request.getUsername());
            // Güvenlik: Detaylı hata mesajı verme (username mı yoksa şifre mi yanlış belirsiz bırak)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * Yeni kullanıcı kaydı
     * POST /api/auth/register
     *
     * @param request Kayıt bilgileri
     * @return 201 Created
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Kayıt isteği: username={}", request.getUsername());

        // Şifre ve şifre tekrarı eşleşiyor mu?
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest().build();
        }

        // Kullanıcı adı duplicate kontrolü
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Bu kullanıcı adı zaten kullanılıyor: " + request.getUsername());
        }

        // E-posta duplicate kontrolü
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Bu e-posta adresi zaten kayıtlı: " + request.getEmail());
        }

        // Varsayılan rol ata - yeni kullanıcılar CUSTOMER rolüyle başlar
        Set<Role> roles = new HashSet<>();
        roleRepository.findByName(Role.CUSTOMER).ifPresent(roles::add);

        // Yeni kullanıcı oluştur
        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            // Şifreyi BCrypt ile hash'le - ASLA düz metin saklanmaz
            .password(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .roles(roles)
            .build();

        // Kullanıcıyı kaydet
        User savedUser = userRepository.save(user);

        // Otomatik login - kayıt sonrası token üret
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(savedUser.getUsername());

        AuthResponse response = AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .username(savedUser.getUsername())
            .email(savedUser.getEmail())
            .fullName(savedUser.getFullName())
            .roles(roles.stream().map(Role::getName).collect(Collectors.toSet()))
            .build();

        log.info("Yeni kullanıcı kaydedildi: username={}", savedUser.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Access token yenileme
     * POST /api/auth/refresh
     *
     * @param refreshToken Geçerli refresh token (Header'dan)
     * @return Yeni access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @RequestHeader("X-Refresh-Token") String refreshToken) {

        // Refresh token geçerli mi?
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Token'dan kullanıcı adını çıkar
        String username = jwtTokenProvider.extractUsername(refreshToken);

        // Kullanıcıyı veritabanından yükle
        User user = userRepository.findByUsernameAndIsDeletedFalse(username)
            .orElse(null);

        if (user == null || !user.isEnabled()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Yeni access token üret
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            user, null, user.getAuthorities());
        String newAccessToken = jwtTokenProvider.generateAccessToken(authentication);

        AuthResponse response = AuthResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(refreshToken)  // Refresh token değişmez
            .username(user.getUsername())
            .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Çıkış işlemi
     * POST /api/auth/logout
     *
     * JWT stateless olduğu için sunucu taraflı session yoktur.
     * Client refresh token'ı siler. Blacklist mekanizması Redis'te tutulabilir.
     *
     * @return 200 OK
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // JWT stateless - sunucuda özel işlem yok
        // Client tarafında token'lar silinmeli
        // İleride: Refresh token Redis blacklist'ine eklenebilir
        log.info("Çıkış yapıldı");
        return ResponseEntity.ok().build();
    }
}
