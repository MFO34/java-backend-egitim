package com.autohub.repository;

import com.autohub.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * İşlem Repository - Kiralama ve satış işlemleri için veri erişim katmanı
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * İşlem numarasına göre işlem bulur
     */
    Optional<Transaction> findByTransactionNumberAndIsDeletedFalse(String transactionNumber);

    /**
     * Müşteriye göre işlemleri sayfalı listeler
     *
     * @param customerId Müşteri ID'si
     * @param pageable Sayfalama bilgisi
     * @return Müşteriye ait işlemler
     */
    Page<Transaction> findByCustomerIdAndIsDeletedFalseOrderByTransactionDateDesc(
            Long customerId, Pageable pageable);

    /**
     * Araca göre işlemleri listeler
     *
     * @param vehicleId Araç ID'si
     * @return Araca ait tüm işlemler
     */
    List<Transaction> findByVehicleIdAndIsDeletedFalseOrderByTransactionDateDesc(Long vehicleId);

    /**
     * Belirli tarihe kadar gecikmiş kiralamaları bulur
     * Bu sorgu ile zamanında iade edilmemiş araçlar tespit edilir
     */
    @Query("SELECT t FROM Transaction t WHERE " +
           "t.transactionType = 'RENTAL' AND " +
           "t.status = 'ACTIVE' AND " +
           "t.rentalEndDate < CURRENT_DATE AND " +
           "t.isDeleted = false")
    List<Transaction> findOverdueRentals();

    /**
     * Belirli tarih aralığındaki işlemleri listeler - raporlama için
     *
     * @param startDate Başlangıç tarihi
     * @param endDate Bitiş tarihi
     * @param pageable Sayfalama
     */
    @Query("SELECT t FROM Transaction t WHERE " +
           "t.transactionDate BETWEEN :startDate AND :endDate " +
           "AND t.isDeleted = false " +
           "ORDER BY t.transactionDate DESC")
    Page<Transaction> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate,
                                       Pageable pageable);

    /**
     * Belirli dönemdeki toplam geliri hesaplar - finans raporu için
     *
     * @param startDate Dönem başlangıcı
     * @param endDate Dönem sonu
     * @return Toplam gelir (BigDecimal)
     */
    @Query("SELECT SUM(t.totalAmount) FROM Transaction t WHERE " +
           "t.transactionDate BETWEEN :startDate AND :endDate " +
           "AND t.status IN ('CONFIRMED', 'COMPLETED') " +
           "AND t.isDeleted = false")
    BigDecimal calculateTotalRevenue(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    /**
     * İşlem tipine ve duruma göre sayı döndürür - dashboard istatistikleri için
     */
    long countByTransactionTypeAndStatusAndIsDeletedFalse(
            Transaction.TransactionType type, Transaction.TransactionStatus status);

    /**
     * Aracın aktif kiralaması var mı kontrol eder
     */
    @Query("SELECT COUNT(t) > 0 FROM Transaction t WHERE " +
           "t.vehicle.id = :vehicleId AND " +
           "t.transactionType = 'RENTAL' AND " +
           "t.status = 'ACTIVE' AND " +
           "t.isDeleted = false")
    boolean existsActiveRentalForVehicle(@Param("vehicleId") Long vehicleId);
}
