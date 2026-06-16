package com.eticaret.dto.request;

import jakarta.validation.constraints.*;

/**
 * CREATE USER REQUEST — Java 21 Record
 * Kayıt alanları immutable, compact constructor ile doğrulama.
 */
public record CreateUserRequest(

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
    String password,

    @Pattern(regexp = "^[0-9+\\-\\s]{7,20}$", message = "Geçerli bir telefon numarası girin")
    String phone
) {}
