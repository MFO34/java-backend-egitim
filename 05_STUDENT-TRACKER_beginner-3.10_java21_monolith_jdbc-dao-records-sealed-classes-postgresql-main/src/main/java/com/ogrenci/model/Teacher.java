package com.ogrenci.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Öğretmen Record modeli.
 * BigDecimal: para/maaş hesaplamalarında double yerine kullanılır
 * çünkü double kayan nokta hataları yapar (0.1 + 0.2 = 0.30000000000000004)
 */
public record Teacher(
    int id,
    String firstName,
    String lastName,
    String email,
    String department,
    BigDecimal salary,     // DECIMAL(10,2) → Java'da BigDecimal
    boolean isActive,
    LocalDateTime createdAt
) {

    // Tam ad yardımcı metodu
    public String fullName() {
        return firstName + " " + lastName.toUpperCase();
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private int id = 0;
        private String firstName;
        private String lastName;
        private String email;
        private String department;
        private BigDecimal salary = BigDecimal.ZERO;
        private boolean isActive = true;
        private LocalDateTime createdAt = LocalDateTime.now();

        public Builder id(int id)                     { this.id = id; return this; }
        public Builder firstName(String v)            { this.firstName = v; return this; }
        public Builder lastName(String v)             { this.lastName = v; return this; }
        public Builder email(String v)                { this.email = v; return this; }
        public Builder department(String v)           { this.department = v; return this; }
        public Builder salary(BigDecimal v)           { this.salary = v; return this; }
        public Builder isActive(boolean v)            { this.isActive = v; return this; }
        public Builder createdAt(LocalDateTime v)     { this.createdAt = v; return this; }

        public Teacher build() {
            return new Teacher(id, firstName, lastName, email,
                               department, salary, isActive, createdAt);
        }
    }
}
