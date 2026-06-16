package com.auth.dto.request;

import jakarta.validation.constraints.*;

/**
 * KAYIT İSTEĞİ — Java 21 Record
 * Immutable, compact constructor ile doğrulama.
 */
public record RegisterRequest(

    @NotBlank(message = "Ad boş olamaz")
    @Size(max = 100)
    String firstName,

    @NotBlank(message = "Soyad boş olamaz")
    @Size(max = 100)
    String lastName,

    @NotBlank(message = "E-posta boş olamaz")
    @Email(message = "Geçerli bir e-posta adresi girin")
    String email,

    @NotBlank(message = "Şifre boş olamaz")
    @Size(min = 8, max = 100, message = "Şifre 8-100 karakter arasında olmalıdır")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
        message = "Şifre en az 1 küçük harf, 1 büyük harf ve 1 rakam içermelidir"
    )
    String password
) {}
