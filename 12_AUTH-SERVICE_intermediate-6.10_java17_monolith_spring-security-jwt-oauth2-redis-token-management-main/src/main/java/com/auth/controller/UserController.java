package com.auth.controller;

import com.auth.dto.request.UpdateProfileRequest;
import com.auth.dto.response.UserResponse;
import com.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * KULLANICI CONTROLLER'I
 * ======================
 * Giriş yapmış kullanıcının kendi profil işlemleri.
 *
 * @SecurityRequirement("bearerAuth"): Swagger UI'da bu endpoint'ler için
 * "Authorize" butonuna tıklayınca JWT girişi gerekir.
 *
 * @PreAuthorize nedir?
 *   Method-level security. SecurityConfig'de @EnableMethodSecurity ile etkinleştirdik.
 *   Metod çalışmadan ÖNCE Spring Security ifadeyi değerlendirir.
 *
 *   isAuthenticated()                  → giriş yapmış olmalı
 *   hasRole("ADMIN")                   → ROLE_ADMIN yetkisi gerekli
 *   #email == authentication.name      → sadece kendi verisi
 *
 * @AuthenticationPrincipal:
 *   SecurityContext.getAuthentication().getPrincipal() kısayolu.
 *   JWT'den çözülmüş ve DB'den yüklenen UserDetails nesnesini inject eder.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Kullanıcı", description = "Profil görüntüleme ve güncelleme")
@SecurityRequirement(name = "bearerAuth") // Swagger: bu endpoint JWT gerektirir
public class UserController {

    private final UserService userService;

    /**
     * KENDİ PROFİLİMİ GETİR
     * ======================
     * GET /api/v1/users/me
     *
     * isAuthenticated() → sadece giriş yapmış kullanıcılar erişebilir.
     * UserDetails.getUsername() → email döner (User.getEmail()).
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Kendi profilimi getir")
    public ResponseEntity<UserResponse> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails) {

        // JWT'den çözülen e-posta ile DB'den kullanıcıyı al
        var profile = userService.getMyProfile(userDetails.getUsername());
        return ResponseEntity.ok(profile);
    }

    /**
     * KENDİ PROFİLİMİ GÜNCELLE
     * =========================
     * PUT /api/v1/users/me
     *
     * isAuthenticated() kontrolü yeterli — sadece kendi profilini güncelleyebilir.
     * (Admin başkasının profilini AdminController üzerinden günceller)
     */
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Kendi profilimi güncelle")
    public ResponseEntity<UserResponse> updateMyProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {

        var updated = userService.updateMyProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(updated);
    }
}
