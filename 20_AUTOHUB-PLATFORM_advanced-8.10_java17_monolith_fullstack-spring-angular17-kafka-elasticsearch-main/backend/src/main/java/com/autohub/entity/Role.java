package com.autohub.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Kullanıcı Rolü Entity Sınıfı
 *
 * Sistem içindeki roller: ADMIN, MANAGER, EMPLOYEE, CUSTOMER
 * Her rol farklı yetki seviyesine sahiptir.
 *
 * OOP Prensibi - KAPSÜLLEME (Encapsulation):
 * Rol adı ve yetkileri kapsüllenmiş, dışarıdan kontrollü erişim sağlanır.
 */
@Entity
@Table(name = "roles",
        indexes = {
            // Rol adı benzersiz olmalı
            @Index(name = "idx_role_name", columnList = "name", unique = true)
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role extends BaseEntity {

    /**
     * Rol adı - ADMIN, MANAGER, EMPLOYEE, CUSTOMER gibi değerler alır
     * Spring Security "ROLE_" prefix'i ekleyerek kullanır
     */
    @NotBlank(message = "Rol adı zorunludur")
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    /**
     * Rol açıklaması - rolün ne yapabileceğini açıklar
     */
    @Column(name = "description", length = 200)
    private String description;

    /**
     * Bu role sahip kullanıcılar - Many-to-Many geri referansı
     * mappedBy: İlişki User tarafında yönetiliyor
     */
    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<User> users = new HashSet<>();

    /**
     * Önceden tanımlanmış sistem rolleri için sabit değerler
     * Bu sabitler kod içinde rol isimleri için kullanılır
     */
    public static final String ADMIN = "ADMIN";
    public static final String MANAGER = "MANAGER";
    public static final String EMPLOYEE = "EMPLOYEE";
    public static final String CUSTOMER = "CUSTOMER";
}
