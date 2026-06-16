package com.autohub.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Kullanıcı Kayıt İsteği DTO
 *
 * Yeni kullanıcı kayıt işlemi için gerekli bilgileri taşır.
 * Şifre doğrulama ve komplekslite kuralları burada uygulanır.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    /**
     * Kullanıcı adı - 3-50 karakter, benzersiz olmalı
     */
    @NotBlank(message = "Kullanıcı adı zorunludur")
    @Size(min = 3, max = 50, message = "Kullanıcı adı 3-50 karakter arasında olmalıdır")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$",
             message = "Kullanıcı adı sadece harf, rakam, nokta, alt çizgi ve tire içerebilir")
    private String username;

    /**
     * E-posta adresi - benzersiz olmalı
     */
    @NotBlank(message = "E-posta zorunludur")
    @Email(message = "Geçersiz e-posta formatı")
    private String email;

    /**
     * Şifre - minimum güvenlik gereksinimlerini karşılamalı
     * En az 8 karakter, büyük/küçük harf, rakam ve özel karakter içermeli
     */
    @NotBlank(message = "Şifre zorunludur")
    @Size(min = 8, message = "Şifre en az 8 karakter olmalıdır")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).*$",
             message = "Şifre en az bir büyük harf, bir küçük harf, bir rakam ve bir özel karakter içermelidir")
    @ToString.Exclude
    private String password;

    /**
     * Şifre tekrarı - password alanı ile eşleşmeli
     */
    @NotBlank(message = "Şifre tekrarı zorunludur")
    @ToString.Exclude
    private String confirmPassword;

    /**
     * Kullanıcının adı
     */
    @Size(max = 50)
    private String firstName;

    /**
     * Kullanıcının soyadı
     */
    @Size(max = 50)
    private String lastName;
}
