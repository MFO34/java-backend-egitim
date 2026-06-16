package com.ogrenci.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JAVA 21 RECORD KAVRAMI:
 *
 * Record, immutable (değiştirilemez) veri taşıyıcı sınıflarını kısaltır.
 * Geleneksel class ile karşılaştırma:
 *
 *   // Geleneksel yol: ~50 satır
 *   public class Student {
 *       private final int id;
 *       private final String firstName;
 *       // ... tüm alanlar
 *       public Student(int id, String firstName, ...) { ... }
 *       public int getId() { return id; }
 *       // ... getter'lar, equals, hashCode, toString
 *   }
 *
 *   // Record ile: 1 satır!
 *   public record Student(int id, String firstName, ...) {}
 *
 * Record otomatik üretir:
 *   ✓ Constructor (tüm alanlar)
 *   ✓ Getter'lar (id(), firstName() şeklinde — get prefix'i yok!)
 *   ✓ equals() ve hashCode()
 *   ✓ toString()
 *
 * JDBC + Record uyumu:
 *   ResultSet'ten okunan veriler Record'a aktarılır.
 *   Immutable olduğu için thread-safe — güvenli.
 *
 * BUILDER PATTERN: Record'lar için Builder iç sınıf ile ekliyoruz.
 * Çok parametreli constructor'lar karışık olur → Builder okunabilir yapar.
 */
public record Student(
    int id,                  // Veritabanı primary key
    String firstName,        // Ad
    String lastName,         // Soyad
    String email,            // E-posta (benzersiz)
    LocalDate birthDate,     // Doğum tarihi
    String nationalId,       // TC Kimlik
    int gradeLevel,          // Sınıf (1-4)
    boolean isActive,        // Aktif mi?
    LocalDateTime createdAt  // Kayıt tarihi
) {

    // COMPACT CONSTRUCTOR: validation için kullanılır
    // Parametreler otomatik atanmadan önce burada kontrol edilir
    public Student {
        // firstName boş olamaz
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("Ad boş olamaz.");
        }
        // Email @ içermeli
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Geçersiz e-posta: " + email);
        }
        // Sınıf 1-4 arasında olmalı
        if (gradeLevel < 1 || gradeLevel > 4) {
            throw new IllegalArgumentException("Sınıf 1-4 arasında olmalı: " + gradeLevel);
        }
    }

    // Tam adı döndüren yardımcı metod (record içinde metod yazılabilir)
    public String fullName() {
        return firstName + " " + lastName.toUpperCase();
    }

    // ================================================================
    // BUILDER PATTERN
    // İç sınıf (nested static class) olarak tanımlanır
    // ================================================================

    /** Fluent Builder: Student nesnesi adım adım oluşturmak için. */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        // Varsayılan değerler — sadece gerekli alanlar doldurulur
        private int id = 0;
        private String firstName;
        private String lastName;
        private String email;
        private LocalDate birthDate = null;
        private String nationalId = null;
        private int gradeLevel = 1;
        private boolean isActive = true;
        private LocalDateTime createdAt = LocalDateTime.now();

        // Her setter metodu Builder'ı döner → zincirleme çağrı (method chaining)
        public Builder id(int id)                   { this.id = id; return this; }
        public Builder firstName(String firstName)   { this.firstName = firstName; return this; }
        public Builder lastName(String lastName)     { this.lastName = lastName; return this; }
        public Builder email(String email)           { this.email = email; return this; }
        public Builder birthDate(LocalDate birthDate){ this.birthDate = birthDate; return this; }
        public Builder nationalId(String nationalId) { this.nationalId = nationalId; return this; }
        public Builder gradeLevel(int gradeLevel)    { this.gradeLevel = gradeLevel; return this; }
        public Builder isActive(boolean isActive)    { this.isActive = isActive; return this; }
        public Builder createdAt(LocalDateTime t)    { this.createdAt = t; return this; }

        // build(): tüm değerleri doğrula ve Record oluştur
        public Student build() {
            return new Student(id, firstName, lastName, email,
                               birthDate, nationalId, gradeLevel, isActive, createdAt);
        }
    }
}
