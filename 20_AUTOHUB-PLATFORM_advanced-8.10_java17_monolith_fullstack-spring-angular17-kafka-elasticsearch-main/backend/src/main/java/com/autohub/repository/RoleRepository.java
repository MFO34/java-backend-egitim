package com.autohub.repository;

import com.autohub.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Rol Repository - Kullanıcı rolleri veritabanı erişim katmanı
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Rol adına göre rol bulur
     * Kullanıcı kaydı sırasında varsayılan rol atamak için kullanılır
     *
     * @param name Rol adı (ADMIN, MANAGER, EMPLOYEE, CUSTOMER)
     * @return Bulunan rol veya boş Optional
     */
    Optional<Role> findByName(String name);

    /**
     * Rol adının var olup olmadığını kontrol eder
     *
     * @param name Kontrol edilecek rol adı
     * @return Varsa true
     */
    boolean existsByName(String name);
}
