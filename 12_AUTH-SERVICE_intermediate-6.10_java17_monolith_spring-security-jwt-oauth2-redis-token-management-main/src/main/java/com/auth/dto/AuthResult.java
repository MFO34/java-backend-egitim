package com.auth.dto;

import com.auth.dto.response.TokenResponse;
import com.auth.dto.response.UserResponse;

/**
 * AUTH RESULT — JAVA 21 SEALED INTERFACE
 * =========================================
 * Kimlik doğrulama sonucunu temsil eder.
 * Sealed sayesinde tüm olası sonuçlar derleme zamanında bilinir.
 *
 * Geleneksel yaklaşım:
 *   try { ... return token; }
 *   catch (BadCredentialsException e) { ... }
 *   → Exception ile kontrol akışı — kötü pratik
 *
 * Sealed Interface + Pattern Matching:
 *   AuthResult result = authService.login(request);
 *   return switch (result) {                         // Exhaustive!
 *       case Success s   -> ResponseEntity.ok(s.tokenResponse());
 *       case Failure f   -> ResponseEntity.status(f.httpStatus()).body(f);
 *       case Pending p   -> ResponseEntity.status(202).body(p);
 *   };
 *
 * Avantajlar:
 *   1. Exception ile kontrol akışı yok — temiz kod
 *   2. Compiler tüm durumların ele alındığını garantiler (exhaustive switch)
 *   3. Her durum kendi verisini taşır
 *   4. Pattern matching ile tip güvenli erişim
 */
public sealed interface AuthResult
        permits AuthResult.Success,
                AuthResult.Failure,
                AuthResult.EmailNotVerified {

    // ===== BAŞARILI GİRİŞ =====
    /**
     * Giriş başarılı — access + refresh token döner.
     */
    record Success(TokenResponse tokenResponse) implements AuthResult {}

    // ===== BAŞARISIZ GİRİŞ =====
    /**
     * Giriş başarısız — hata kodu ve mesaj içerir.
     * errorCode: Frontend'in hangi hatayı göstereceğini belirler.
     */
    record Failure(
        String errorCode,    // "INVALID_CREDENTIALS", "ACCOUNT_LOCKED" vb.
        String message,      // Kullanıcıya gösterilecek mesaj
        int httpStatus       // 401, 403, 423 (locked) vb.
    ) implements AuthResult {}

    // ===== E-POSTA DOĞRULANMADI =====
    /**
     * Hesap var ama e-posta doğrulanmamış.
     * Frontend doğrulama e-postası yeniden gönderme seçeneği sunabilir.
     */
    record EmailNotVerified(String email) implements AuthResult {}

    // ===== FACTORY METODLAR =====

    static AuthResult success(TokenResponse token) {
        return new Success(token);
    }

    static AuthResult failure(String errorCode, String message, int httpStatus) {
        return new Failure(errorCode, message, httpStatus);
    }

    static AuthResult emailNotVerified(String email) {
        return new EmailNotVerified(email);
    }

    // Yaygın hata sabitleri
    static AuthResult invalidCredentials() {
        return failure("INVALID_CREDENTIALS", "E-posta veya şifre hatalı.", 401);
    }

    static AuthResult accountLocked() {
        return failure("ACCOUNT_LOCKED",
            "Hesabınız çok fazla başarısız giriş denemesi nedeniyle kilitlendi.", 423);
    }

    static AuthResult accountDisabled() {
        return failure("ACCOUNT_DISABLED", "Hesabınız devre dışı bırakılmıştır.", 403);
    }
}
