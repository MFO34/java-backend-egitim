package com.auth.service;

import com.auth.dto.AuthResult;
import com.auth.dto.request.*;
import com.auth.dto.response.MessageResponse;
import com.auth.dto.response.TokenResponse;
import com.auth.dto.response.UserResponse;
import com.auth.entity.*;
import com.auth.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * KİMLİK DOĞRULAMA SERVİSİ
 * =========================
 * Kullanıcı kayıt, giriş, çıkış ve token yönetimi işlemleri.
 *
 * AuthResult sealed interface ile dönüş değerleri:
 *   login() → Success(tokenResponse) | Failure(errorCode) | EmailNotVerified(email)
 *
 * Önemli güvenlik mekanizmaları:
 *   1. Başarısız giriş sayacı: 5 başarısız denemede hesap kilitlenir
 *   2. E-posta doğrulaması: LOCAL provider için zorunlu
 *   3. Token rotation: Her refresh'te yeni token çifti
 *   4. Reuse detection: Kullanılmış refresh token gelince tüm session'lar kapatılır
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    // Spring Security authentication manager (login için)
    private final AuthenticationManager authenticationManager;

    // Kullanıcı repository'leri
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    // Servisler
    private final TokenService tokenService;
    private final EmailService emailService;

    // Şifre hashleme
    private final PasswordEncoder passwordEncoder;

    // JWT üretme (TokenService içinde de kullanılıyor, doğrudan da lazım)
    private final com.auth.security.JwtService jwtService;

    // Başarısız giriş için maksimum deneme sayısı
    private static final int MAX_FAILED_ATTEMPTS = 5;

    // ========== KAYIT ==========

    /**
     * Yeni kullanıcı kaydı.
     *
     * Adımlar:
     *   1. E-posta benzersizlik kontrolü
     *   2. Şifreyi BCrypt ile hashle
     *   3. USER rolünü ata
     *   4. Kullanıcıyı kaydet (enabled=true, emailVerified=false)
     *   5. Doğrulama e-postası gönder (async)
     */
    @Transactional
    public MessageResponse register(RegisterRequest request) {
        // E-posta kullanılıyor mu?
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Bu e-posta adresi zaten kullanılıyor: " + request.email());
        }

        // Varsayılan USER rolünü al (yoksa oluştur)
        var userRole = roleRepository.findByName("USER")
            .orElseGet(() -> roleRepository.save(new Role("USER")));

        // Yeni kullanıcı oluştur
        var user = User.builder()
            .firstName(request.firstName())
            .lastName(request.lastName())
            .email(request.email())
            .password(passwordEncoder.encode(request.password())) // Şifreyi hashle!
            .provider(AuthProvider.LOCAL)                          // Normal kayıt
            .emailVerified(false)                                  // Henüz doğrulanmadı
            .enabled(true)
            .roles(Set.of(userRole))
            .build();

        var savedUser = userRepository.save(user);

        // Doğrulama token'ı oluştur ve kaydet
        var verificationToken = createEmailVerificationToken(savedUser);

        // E-postayı async gönder (kullanıcıyı bekletmez)
        emailService.sendVerificationEmail(
            savedUser.getEmail(),
            savedUser.getFirstName(),
            verificationToken
        );

        log.info("Yeni kullanıcı kaydedildi: {}", savedUser.getEmail());
        return new MessageResponse("Kayıt başarılı! Lütfen e-postanızı kontrol edin.");
    }

    // ========== GİRİŞ ==========

    /**
     * Kullanıcı girişi.
     * AuthResult sealed interface döner — controller Pattern Matching ile işler.
     *
     * @return Success | Failure | EmailNotVerified
     */
    @Transactional
    public AuthResult login(LoginRequest request, String ipAddress, String userAgent) {
        try {
            // Spring Security ile kimlik doğrulama
            // Bu satır: userDetailsService.loadUserByUsername(email) çağırır
            //           sonra passwordEncoder.matches(request.password, storedHash) yapar
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.email(),
                    request.password()
                )
            );

        } catch (BadCredentialsException e) {
            // Şifre yanlış → başarısız deneme sayacını artır
            handleFailedLogin(request.email());
            return AuthResult.invalidCredentials();

        } catch (DisabledException e) {
            // Hesap disabled (enabled=false)
            return AuthResult.failure("ACCOUNT_DISABLED", "Hesabınız devre dışı.", 403);

        } catch (LockedException e) {
            // Hesap kilitli (çok fazla başarısız deneme)
            return AuthResult.accountLocked();
        }

        // Kimlik doğrulama başarılı → kullanıcıyı DB'den al
        var user = userRepository.findByEmail(request.email())
            .orElseThrow(); // authenticate geçtiyse kullanıcı kesinlikle vardır

        // E-posta doğrulandı mı? (sadece LOCAL provider için zorunlu)
        if (!user.isEmailVerified() && user.getProvider() == AuthProvider.LOCAL) {
            log.warn("E-posta doğrulanmamış giriş denemesi: {}", user.getEmail());
            return new AuthResult.EmailNotVerified(user.getEmail());
        }

        // Başarılı giriş → başarısız deneme sayacını sıfırla
        if (user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
        }

        // Token çifti üret
        var accessToken  = jwtService.generateAccessToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

        // Refresh token'ı DB'ye kaydet
        tokenService.saveRefreshToken(user, refreshToken, ipAddress, userAgent);

        log.info("Başarılı giriş: userId={}", user.getId());

        return new AuthResult.Success(
            new TokenResponse(accessToken, refreshToken, "Bearer",
                jwtService.getAccessExpirationMs())
        );
    }

    // ========== ÇIKIŞ ==========

    /**
     * Kullanıcı çıkışı.
     *
     * Adımlar:
     *   1. Access token'ı Redis blacklist'e ekle
     *   2. Kullanıcının tüm refresh token'larını DB'de iptal et
     */
    public MessageResponse logout(String accessToken, Long userId) {
        // Access token'ı geçersiz kıl (Redis'e ekle, TTL = token kalan süresi)
        tokenService.blacklistAccessToken(accessToken);

        // Tüm refresh token'ları iptal et (tüm cihazlardan çıkış)
        tokenService.revokeAllUserRefreshTokens(userId);

        log.info("Kullanıcı çıkış yaptı: userId={}", userId);
        return new MessageResponse("Başarıyla çıkış yapıldı.");
    }

    // ========== TOKEN YENİLEME ==========

    /**
     * Refresh token ile yeni access + refresh token al.
     * Token rotation uygulanır (eski refresh token geçersiz olur).
     */
    @Transactional
    public AuthResult refreshToken(RefreshTokenRequest request,
                                    String ipAddress, String userAgent) {
        var result = tokenService.rotateRefreshToken(
            request.refreshToken(), ipAddress, userAgent
        );

        if (result.isEmpty()) {
            return AuthResult.failure("INVALID_REFRESH_TOKEN",
                "Geçersiz veya süresi dolmuş refresh token.", 401);
        }

        var pair = result.get();

        // Yeni access token'dan expiration bilgisini al
        return new AuthResult.Success(
            new TokenResponse(pair.accessToken(), pair.refreshToken(), "Bearer",
                jwtService.getAccessExpirationMs())
        );
    }

    // ========== E-POSTA DOĞRULAMA ==========

    /**
     * E-posta doğrulama token'ını kontrol et ve kullanıcıyı aktif et.
     */
    @Transactional
    public MessageResponse verifyEmail(String token) {
        // Token DB'de var mı?
        var verToken = emailVerificationTokenRepository.findByToken(token)
            .orElseThrow(() -> new IllegalArgumentException("Geçersiz doğrulama token'ı."));

        // Süresi dolmuş mu?
        if (verToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Doğrulama token'ının süresi dolmuş. Yeni token isteyin.");
        }

        // Zaten doğrulanmış mı?
        if (verToken.isUsed()) {
            return new MessageResponse("E-posta adresi zaten doğrulanmış.");
        }

        // Kullanıcıyı doğrulandı olarak işaretle
        var user = verToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        // Token'ı kullanıldı olarak işaretle
        verToken.setUsed(true);
        emailVerificationTokenRepository.save(verToken);

        log.info("E-posta doğrulandı: userId={}", user.getId());
        return new MessageResponse("E-posta adresiniz başarıyla doğrulandı! Artık giriş yapabilirsiniz.");
    }

    /**
     * Yeni doğrulama e-postası gönder (token süresi dolmuşsa).
     */
    @Transactional
    public MessageResponse resendVerificationEmail(String email) {
        var user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Bu e-posta ile kayıtlı kullanıcı yok."));

        if (user.isEmailVerified()) {
            return new MessageResponse("E-posta adresiniz zaten doğrulanmış.");
        }

        // Yeni token oluştur
        var token = createEmailVerificationToken(user);
        emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), token);

        return new MessageResponse("Doğrulama e-postası gönderildi.");
    }

    // ========== ŞİFRE SIFIRLAMA ==========

    /**
     * Şifre sıfırlama e-postası gönder.
     *
     * Güvenlik notu: Kullanıcı bulunamazsa bile aynı mesajı dön.
     * (Enumeration attack: "Bu e-posta kayıtlı değil" demek bilgi sızdırır)
     */
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            // Sadece LOCAL provider kullanıcıları şifre sıfırlayabilir
            if (user.getProvider() != AuthProvider.LOCAL) {
                log.warn("OAuth2 kullanıcısı şifre sıfırlamaya çalıştı: {}", user.getEmail());
                return; // OAuth2 kullanıcısı → sessizce geç
            }

            // Sıfırlama token'ı oluştur
            var resetToken = new PasswordResetToken();
            resetToken.setToken(UUID.randomUUID().toString());
            resetToken.setUser(user);
            resetToken.setExpiresAt(LocalDateTime.now().plusHours(1)); // 1 saat
            passwordResetTokenRepository.save(resetToken);

            // E-postayı async gönder
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), resetToken.getToken());
        });

        // Her durumda aynı mesaj (güvenlik: e-posta varlığını sızdırma)
        return new MessageResponse("Şifre sıfırlama talimatları e-posta adresinize gönderildi.");
    }

    /**
     * Şifreyi sıfırla (token + yeni şifre ile).
     */
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        // Token geçerli mi?
        var resetToken = passwordResetTokenRepository.findByToken(request.token())
            .orElseThrow(() -> new IllegalArgumentException("Geçersiz şifre sıfırlama token'ı."));

        // Süresi dolmuş mu?
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token süresi dolmuş. Yeniden 'Şifremi Unuttum' yapın.");
        }

        // Kullanılmış mı?
        if (resetToken.isUsed()) {
            throw new IllegalArgumentException("Bu token daha önce kullanılmış.");
        }

        // Şifreyi güncelle
        var user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Token'ı kullanıldı işaretle
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // Tüm refresh token'ları iptal et (şifre değişti → tüm oturumları kapat)
        tokenService.revokeAllUserRefreshTokens(user.getId());

        log.info("Şifre sıfırlandı: userId={}", user.getId());
        return new MessageResponse("Şifreniz başarıyla sıfırlandı. Yeni şifrenizle giriş yapabilirsiniz.");
    }

    // ========== YARDIMCI METODLAR ==========

    /**
     * E-posta doğrulama token'ı oluştur ve DB'ye kaydet.
     * Token string'ini döner (e-posta servisine geçilir).
     */
    private String createEmailVerificationToken(User user) {
        var tokenValue = UUID.randomUUID().toString();

        var token = new EmailVerificationToken();
        token.setToken(tokenValue);
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now().plusHours(24)); // 24 saat
        emailVerificationTokenRepository.save(token);

        return tokenValue;
    }

    /**
     * Başarısız giriş denemesini işle.
     * 5. başarısız denemede hesabı kilitle.
     */
    private void handleFailedLogin(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            var attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            if (attempts >= MAX_FAILED_ATTEMPTS) {
                // Hesabı kilitle
                user.setAccountLocked(true);
                log.warn("Hesap kilitlendi (çok fazla başarısız deneme): userId={}", user.getId());
            }

            userRepository.save(user);
        });
    }
}
