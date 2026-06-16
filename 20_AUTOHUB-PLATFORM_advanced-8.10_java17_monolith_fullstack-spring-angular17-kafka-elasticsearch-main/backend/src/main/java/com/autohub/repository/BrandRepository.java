package com.autohub.repository;

import com.autohub.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Marka Repository - Marka veritabanı işlemleri için erişim katmanı
 *
 * OOP Prensibi - SOYUTLAMA (Abstraction):
 * Veritabanı sorgu detayları servis katmanından gizlenir.
 */
@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {

    /**
     * Marka adına göre arama - büyük/küçük harf duyarsız
     *
     * @param name Aranacak marka adı
     * @return Bulunan marka veya boş Optional
     */
    Optional<Brand> findByNameIgnoreCaseAndIsDeletedFalse(String name);

    /**
     * Ülkeye göre marka listesi
     *
     * @param country Ülke adı
     * @return O ülkeye ait markalar
     */
    List<Brand> findByCountryAndIsDeletedFalseOrderByNameAsc(String country);

    /**
     * Marka adının var olup olmadığını kontrol eder
     *
     * @param name Kontrol edilecek marka adı
     * @return Varsa true
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Modeli olan tüm aktif markaları listeler
     * LEFT JOIN ile model sayısına göre filtreler
     */
    @Query("SELECT b FROM Brand b WHERE b.isDeleted = false AND SIZE(b.models) > 0 ORDER BY b.name ASC")
    List<Brand> findAllBrandsWithModels();

    /**
     * Tüm aktif markaları ada göre sıralı listeler
     */
    List<Brand> findByIsDeletedFalseOrderByNameAsc();

    /**
     * İsme göre arama - kısmi eşleşme (autocomplete için)
     *
     * @param name Aranacak isim (kısmi)
     * @return Eşleşen markalar
     */
    @Query("SELECT b FROM Brand b WHERE LOWER(b.name) LIKE LOWER(CONCAT('%', :name, '%')) AND b.isDeleted = false")
    List<Brand> searchByName(@Param("name") String name);

    /**
     * Kuruluş yılına göre markaları listeler
     *
     * @param year Kuruluş yılı
     * @return O yılda kurulmuş markalar
     */
    List<Brand> findByFoundedYearAndIsDeletedFalse(Integer year);
}
