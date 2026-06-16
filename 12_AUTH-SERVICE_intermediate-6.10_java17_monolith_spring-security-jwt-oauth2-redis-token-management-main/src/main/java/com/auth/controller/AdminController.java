package com.auth.controller;

import com.auth.dto.response.UserResponse;
import com.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ADMİN CONTROLLER'I
 * ==================
 * Sadece ADMIN rolüne sahip kullanıcıların erişebildiği endpoint'ler.
 *
 * @Secured("ROLE_ADMIN"):
 *   Method-level security — @EnableMethodSecurity(securedEnabled=true) ile etkinleştirdik.
 *   @PreAuthorize("hasRole('ADMIN')") ile eşdeğer ama daha basit syntax.
 *
 *   Dikkat: @Secured'da "ROLE_" prefix yazılır: @Secured("ROLE_ADMIN")
 *           @PreAuthorize'da yazılmaz: hasRole("ADMIN") (Spring otomatik ekler)
 *
 * SecurityConfig'de:
 *   .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
 *   Bu çift koruma — hem URL level hem method level.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Yönetici işlemleri — sadece ADMIN rolü")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final UserService userService;

    /**
     * TÜM KULLANICILARI LİSTELE
     * ==========================
     * GET /api/v1/admin/users
     */
    @GetMapping("/users")
    @Secured("ROLE_ADMIN")
    @Operation(summary = "Tüm kullanıcıları listele")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * ID İLE KULLANICI GETİR
     * ======================
     * GET /api/v1/admin/users/{id}
     */
    @GetMapping("/users/{id}")
    @Secured("ROLE_ADMIN")
    @Operation(summary = "ID ile kullanıcı getir")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    /**
     * KULLANICI AKTİF/PASİF
     * =====================
     * PATCH /api/v1/admin/users/{id}/toggle-enabled
     *
     * Kullanıcıyı silmek yerine devre dışı bırak (soft disable).
     * Silinen kullanıcıların verileri kaybolur, disable'da korunur.
     */
    @PatchMapping("/users/{id}/toggle-enabled")
    @Secured("ROLE_ADMIN")
    @Operation(summary = "Kullanıcıyı aktif/pasif yap")
    public ResponseEntity<UserResponse> toggleUserEnabled(@PathVariable Long id) {
        return ResponseEntity.ok(userService.toggleUserEnabled(id));
    }

    /**
     * HESAP KİLİDİNİ AÇ
     * ==================
     * PATCH /api/v1/admin/users/{id}/unlock
     *
     * 5 başarısız giriş denemesinde otomatik kilitlenen hesabı açar.
     */
    @PatchMapping("/users/{id}/unlock")
    @Secured("ROLE_ADMIN")
    @Operation(summary = "Kilitli hesabı aç")
    public ResponseEntity<UserResponse> unlockUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.unlockUser(id));
    }
}
