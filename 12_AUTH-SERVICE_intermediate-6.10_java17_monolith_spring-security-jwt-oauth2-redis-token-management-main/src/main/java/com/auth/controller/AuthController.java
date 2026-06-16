package com.auth.controller;

import com.auth.dto.AuthResult;
import com.auth.dto.request.*;
import com.auth.dto.response.MessageResponse;
import com.auth.dto.response.TokenResponse;
import com.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * KİMLİK DOĞRULAMA CONTROLLER'I
 * ==============================
 * Authentication (kim olduğunu doğrulama) ile ilgili tüm endpoint'ler.
 *
 * Önemli: AuthResult sealed interface ile Pattern Matching for switch
 *
 * login() metodu AuthService'den AuthResult döner:
 *   case AuthResult.Success s        → 200 OK + token
 *   case AuthResult.Failure f        → f.httpStatus() + error message
 *   case AuthResult.EmailNotVerified → 403 + "E-postanızı doğrulayın"
 *
 * Bu pattern:
 *   - Service katmanı HTTP'den bağımsız kalır (AuthResult döner, ResponseEntity değil)
 *   - Controller HTTP kararını verir (status code, header)
 *   - Tüm durumlar derleme zamanında kontrol edilir (sealed = exhaustive switch)
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Kayıt, giriş, çıkış ve token yönetimi")
public class AuthController {

    private final AuthService authService;

    // ========== KAYIT ==========

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Yeni kullanıcı kaydı", description = "E-posta doğrulama linki gönderilir")
    public MessageResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    // ========== GİRİŞ ==========

    /**
     * GİRİŞ ENDPOINT'İ — AuthResult Pattern Matching
     * ================================================
     * AuthService.login() → AuthResult (sealed) döner
     * Bu metodda switch expression ile tüm durumları ele alırız.
     *
     * Java 21 Pattern Matching for switch:
     *   case AuthResult.Success s        → s.tokenResponse()'a erişebiliriz
     *   case AuthResult.Failure f        → f.httpStatus(), f.message()'a erişebiliriz
     *   case AuthResult.EmailNotVerified → e.email()'e erişebiliriz
     */
    @PostMapping("/login")
    @Operation(summary = "Kullanıcı girişi", description = "JWT access + refresh token döner")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        // IP ve User-Agent bilgisini al (refresh token'a kayıt için)
        var ipAddress = getClientIp(httpRequest);
        var userAgent = httpRequest.getHeader("User-Agent");

        // AuthService'den AuthResult al
        var result = authService.login(request, ipAddress, userAgent);

        // Java 21 Pattern Matching for switch — sealed interface exhaustive
        return switch (result) {
            // Başarılı giriş → 200 OK + token yanıtı
            case AuthResult.Success s ->
                ResponseEntity.ok(s.tokenResponse());

            // E-posta doğrulanmamış → 403 Forbidden
            case AuthResult.EmailNotVerified e ->
                ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse(
                        "E-posta adresinizi doğrulamamışsınız. Lütfen e-postanızı kontrol edin: " + e.email()
                    ));

            // Diğer hatalar → HttpStatus'e göre
            case AuthResult.Failure f ->
                ResponseEntity.status(f.httpStatus())
                    .body(new MessageResponse(f.message()));
        };
    }

    // ========== ÇIKIŞ ==========

    /**
     * Çıkış endpoint'i.
     *
     * @AuthenticationPrincipal: SecurityContext'teki aktif kullanıcıyı inject eder.
     * Authorization header'daki JWT'den çözülür.
     */
    @PostMapping("/logout")
    @Operation(summary = "Çıkış yap", description = "Access token blacklist'e eklenir, refresh token'lar iptal edilir")
    public ResponseEntity<MessageResponse> logout(
            HttpServletRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Authorization: Bearer <token> → token'ı çıkar
        var authHeader = request.getHeader("Authorization");
        var accessToken = authHeader.substring(7); // "Bearer " prefix'ini kaldır

        // Kullanıcı ID'sini al (User entity UserDetails'i implement ediyor)
        var user = (com.auth.entity.User) userDetails;

        var response = authService.logout(accessToken, user.getId());
        return ResponseEntity.ok(response);
    }

    // ========== TOKEN YENİLEME ==========

    @PostMapping("/refresh")
    @Operation(summary = "Token yenile", description = "Refresh token ile yeni access + refresh token al")
    public ResponseEntity<?> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {

        var ipAddress = getClientIp(httpRequest);
        var userAgent = httpRequest.getHeader("User-Agent");

        var result = authService.refreshToken(request, ipAddress, userAgent);

        // AuthResult Pattern Matching (refresh sadece Success veya Failure döner)
        return switch (result) {
            case AuthResult.Success s ->
                ResponseEntity.ok(s.tokenResponse());
            case AuthResult.Failure f ->
                ResponseEntity.status(f.httpStatus())
                    .body(new MessageResponse(f.message()));
            // EmailNotVerified bu endpoint'te olmaz ama sealed = tüm case'ler gerekli
            case AuthResult.EmailNotVerified e ->
                ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Yetkisiz erişim."));
        };
    }

    // ========== E-POSTA DOĞRULAMA ==========

    @GetMapping("/verify-email")
    @Operation(summary = "E-posta doğrula", description = "E-posta linkindeki token ile hesabı aktif et")
    public ResponseEntity<MessageResponse> verifyEmail(@RequestParam String token) {
        var response = authService.verifyEmail(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Doğrulama e-postasını yeniden gönder")
    public ResponseEntity<MessageResponse> resendVerification(@RequestParam String email) {
        var response = authService.resendVerificationEmail(email);
        return ResponseEntity.ok(response);
    }

    // ========== ŞİFRE SIFIRLAMA ==========

    @PostMapping("/forgot-password")
    @Operation(summary = "Şifremi unuttum", description = "Şifre sıfırlama linki gönderilir")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        var response = authService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Şifre sıfırla", description = "Token + yeni şifre ile şifreyi güncelle")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        var response = authService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    // ========== YARDIMCI ==========

    private String getClientIp(HttpServletRequest request) {
        var xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
