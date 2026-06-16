package com.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * ROL ENTITY — ROLE_ADMIN, ROLE_USER, ROLE_MODERATOR
 * =====================================================
 * Role → Permission (ManyToMany değil ElementCollection):
 *   Permission bir enum olduğu için ayrı tablo yerine
 *   @ElementCollection ile rol_permissions ara tablosuna yazılır.
 *
 * Önceden tanımlı roller DataInitializer'da oluşturulur.
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role extends BaseEntity {

    /**
     * Rol adı — "ADMIN", "USER", "MODERATOR"
     * Spring Security hasRole("ADMIN") çağrısında "ROLE_ADMIN" arar.
     * Bu yüzden name alanına "ROLE_" ön eki EKLEME — Spring ekler.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    /**
     * @ElementCollection: Permission enum değerleri ayrı tabloda saklanır.
     *   roles_permissions tablosu: role_id + permission (VARCHAR)
     * @Enumerated(STRING): "ADMIN_READ" gibi string olarak saklanır.
     * EAGER: Rol yüklenince izinler de yüklensin (küçük veri).
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "permission")
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();

    public Role(String name) {
        this.name = name;
        this.permissions = new HashSet<>();
    }
}
