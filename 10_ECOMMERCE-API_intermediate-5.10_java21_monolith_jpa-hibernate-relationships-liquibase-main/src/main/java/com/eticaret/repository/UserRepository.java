package com.eticaret.repository;

import com.eticaret.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * USER REPOSITORY
 * Spring Data JPA: JpaRepository'yi extend edince
 * save(), findById(), findAll(), delete() gibi metodlar hazır gelir.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Method Naming: findBy + Alan adı + Condition
    // Spring bunu otomatik: SELECT * FROM users WHERE email = ?
    Optional<User> findByEmail(String email);

    // Kayıt var mı kontrolü — SELECT COUNT(*) > 0
    boolean existsByEmail(String email);

    /**
     * JPQL: Java sınıf ve field adlarıyla sorgu (tablo adı değil!)
     * "User" → users tablosu, "u.isActive" → is_active sütunu
     * @Param("email"): :email placeholder'ına bağlar
     */
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.isActive = true")
    Optional<User> findActiveUserByEmail(@Param("email") String email);

    /**
     * Native SQL: Doğrudan PostgreSQL sözdizimi
     * nativeQuery = true: Hibernate JPQL çevirmez, SQL olduğu gibi gider
     * Kullanım: JPQL ile yapılamayan PostgreSQL özel fonksiyonlar
     */
    @Query(value = "SELECT COUNT(*) FROM users WHERE is_active = true",
           nativeQuery = true)
    long countActiveUsers();
}
