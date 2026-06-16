package com.autohub.repository;

import com.autohub.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Müşteri Repository - Müşteri veritabanı işlemleri için erişim katmanı
 *
 * Tüm müşteri sorgulama ve veri erişim işlemleri bu arayüzde tanımlanır.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * TC Kimlik Numarasına göre müşteri bulur
     *
     * @param tcIdentityNumber 11 haneli TC kimlik numarası
     * @return Bulunan müşteri veya boş Optional
     */
    Optional<Customer> findByTcIdentityNumberAndIsDeletedFalse(String tcIdentityNumber);

    /**
     * E-posta adresine göre müşteri bulur
     *
     * @param email E-posta adresi
     * @return Bulunan müşteri veya boş Optional
     */
    Optional<Customer> findByEmailAndIsDeletedFalse(String email);

    /**
     * İsim veya soyadına göre arama - kısmi eşleşme, büyük/küçük harf duyarsız
     *
     * @param name Aranacak ad/soyad
     * @param pageable Sayfalama bilgisi
     * @return Eşleşen müşteriler
     */
    @Query("SELECT c FROM Customer c WHERE " +
           "(LOWER(c.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
           " LOWER(c.lastName) LIKE LOWER(CONCAT('%', :name, '%'))) " +
           "AND c.isDeleted = false")
    Page<Customer> searchByName(@Param("name") String name, Pageable pageable);

    /**
     * Müşteri tipine göre listeler
     *
     * @param type Müşteri tipi (INDIVIDUAL veya CORPORATE)
     * @param pageable Sayfalama bilgisi
     */
    Page<Customer> findByCustomerTypeAndIsDeletedFalse(Customer.CustomerType type, Pageable pageable);

    /**
     * TC kimlik numarasının var olup olmadığını kontrol eder - duplicate önleme
     *
     * @param tcIdentityNumber Kontrol edilecek TC
     * @return Varsa true
     */
    boolean existsByTcIdentityNumber(String tcIdentityNumber);

    /**
     * E-posta adresinin var olup olmadığını kontrol eder
     *
     * @param email Kontrol edilecek email
     * @return Varsa true
     */
    boolean existsByEmail(String email);

    /**
     * En çok işlem yapan müşterileri listeler - VIP müşteri tespiti için
     *
     * @param pageable Limit için sayfalama (ilk N müşteri)
     * @return İşlem sayısına göre sıralı müşteriler
     */
    @Query("SELECT c FROM Customer c " +
           "JOIN c.transactions t " +
           "WHERE c.isDeleted = false " +
           "GROUP BY c " +
           "ORDER BY COUNT(t) DESC")
    List<Customer> findTopCustomersByTransactionCount(Pageable pageable);

    /**
     * Şehre göre müşteri listesi
     *
     * @param city Şehir adı
     * @return O şehirdeki müşteriler
     */
    List<Customer> findByCityAndIsDeletedFalseOrderByFirstNameAsc(String city);

    /**
     * Aktif işlemi olan müşterileri bulur
     */
    @Query("SELECT DISTINCT c FROM Customer c " +
           "JOIN c.transactions t " +
           "WHERE t.status = 'ACTIVE' AND c.isDeleted = false")
    List<Customer> findCustomersWithActiveTransactions();
}
