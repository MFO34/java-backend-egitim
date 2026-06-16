package com.autohub.repository;

// Spring Data JPA ve entity importları
import com.autohub.entity.Vehicle;
import com.autohub.entity.VehicleModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Araç Repository - Veritabanı Erişim Katmanı
 *
 * Spring Data JPA'nın JpaRepository'sini extend eder.
 * Temel CRUD işlemleri otomatik sağlanır, ek sorgular burada tanımlanır.
 *
 * OOP Prensibi - SOYUTLAMA (Abstraction):
 * Repository arayüzü, veritabanı implementasyon detaylarını soyutlar.
 * Servis katmanı sadece bu arayüzü kullanır, altta hangi DB olduğunu bilmez.
 *
 * JpaSpecificationExecutor: Dinamik filtreleme sorguları için (Specification pattern)
 */
@Repository  // Spring tarafından Repository bean'ı olarak tanınır
public interface VehicleRepository
        extends JpaRepository<Vehicle, Long>,
                JpaSpecificationExecutor<Vehicle> {

    /**
     * Plaka numarasına göre araç arar
     * Spring Data JPA metod adından sorguyu otomatik oluşturur
     *
     * @param licensePlate Aranacak plaka numarası
     * @return Bulunan araç veya boş Optional
     */
    Optional<Vehicle> findByLicensePlateAndIsDeletedFalse(String licensePlate);

    /**
     * VIN numarasına göre araç arar
     *
     * @param vinNumber 17 haneli VIN numarası
     * @return Bulunan araç veya boş Optional
     */
    Optional<Vehicle> findByVinNumberAndIsDeletedFalse(String vinNumber);

    /**
     * Belirli durumdaki araçları sayfalı olarak listeler
     *
     * @param status Araç durumu (AVAILABLE, RENTED vs.)
     * @param pageable Sayfalama ve sıralama bilgisi
     * @return Sayfalı araç listesi
     */
    Page<Vehicle> findByStatusAndIsDeletedFalse(Vehicle.VehicleStatus status, Pageable pageable);

    /**
     * Modele ve fiyat aralığına göre araçları filtreler
     * @Query ile özel JPQL sorgusu tanımlanır
     *
     * @param model Araç modeli
     * @param minPrice Minimum fiyat
     * @param maxPrice Maksimum fiyat
     * @param pageable Sayfalama bilgisi
     * @return Filtrelenmiş araç listesi
     */
    @Query("SELECT v FROM Vehicle v WHERE v.model = :model " +
           "AND v.price BETWEEN :minPrice AND :maxPrice " +
           "AND v.isDeleted = false " +
           "ORDER BY v.price ASC")
    Page<Vehicle> findByModelAndPriceBetween(
            @Param("model") VehicleModel model,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);

    /**
     * Marka adına göre müsait araçları listeler
     * JOIN FETCH ile N+1 problemi çözülür
     *
     * @param brandName Marka adı
     * @param pageable Sayfalama bilgisi
     * @return Müsait araç listesi
     */
    @Query("SELECT v FROM Vehicle v " +
           "JOIN FETCH v.model m " +
           "JOIN FETCH m.brand b " +
           "WHERE b.name = :brandName " +
           "AND v.status = 'AVAILABLE' " +
           "AND v.isDeleted = false")
    Page<Vehicle> findAvailableByBrandName(@Param("brandName") String brandName, Pageable pageable);

    /**
     * Yıl aralığına göre araçları filtreler
     *
     * @param startYear Başlangıç yılı
     * @param endYear Bitiş yılı
     * @param pageable Sayfalama bilgisi
     */
    Page<Vehicle> findByYearBetweenAndIsDeletedFalse(Integer startYear, Integer endYear, Pageable pageable);

    /**
     * Araç sayısını statüye göre döndürür - dashboard için
     *
     * @param status Araç durumu
     * @return Belirtilen durumdaki araç sayısı
     */
    long countByStatusAndIsDeletedFalse(Vehicle.VehicleStatus status);

    /**
     * Plaka numarasının var olup olmadığını kontrol eder - duplicate kontrolü için
     *
     * @param licensePlate Kontrol edilecek plaka
     * @return Plaka varsa true
     */
    boolean existsByLicensePlate(String licensePlate);

    /**
     * VIN numarasının var olup olmadığını kontrol eder
     *
     * @param vinNumber Kontrol edilecek VIN
     * @return VIN varsa true
     */
    boolean existsByVinNumber(String vinNumber);

    /**
     * Araç durumunu toplu günceller - Modifying sorgu ile
     * @Modifying: Bu sorgunun veri değiştirdiğini Spring'e bildirir
     * clearAutomatically: Birincil önbelleği temizler (stale data önleme)
     *
     * @param vehicleId Güncellenecek araç ID'si
     * @param status Yeni araç durumu
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Vehicle v SET v.status = :status WHERE v.id = :vehicleId")
    void updateVehicleStatus(@Param("vehicleId") Long vehicleId,
                             @Param("status") Vehicle.VehicleStatus status);

    /**
     * Bakımı gerekli araçları bulur - son muayene 1 yıldan eskiyse
     * Native SQL sorgusu - PostgreSQL'e özgü tarih fonksiyonları kullanılıyor
     *
     * @return Bakım gerektiren araçların ID listesi
     */
    @Query(value = "SELECT v.id FROM vehicles v " +
                   "WHERE v.last_inspection_date < NOW() - INTERVAL '1 year' " +
                   "AND v.status != 'SOLD' " +
                   "AND v.is_deleted = false",
           nativeQuery = true)  // Native SQL - JPQL yerine gerçek SQL kullanılıyor
    List<Long> findVehiclesNeedingMaintenance();

    /**
     * Tüm müsait araçları fiyata göre sıralı listeler - basit listeleme için
     *
     * @return Müsait araçlar, fiyata göre artan sırada
     */
    List<Vehicle> findByStatusAndIsDeletedFalseOrderByPriceAsc(Vehicle.VehicleStatus status);

    /**
     * Son eklenen N aracı getirir - dashboard son araçlar widget'ı için
     *
     * @param pageable Sayfalama (N sayısını belirler)
     * @return Son eklenen araçlar
     */
    @Query("SELECT v FROM Vehicle v WHERE v.isDeleted = false ORDER BY v.createdAt DESC")
    List<Vehicle> findLatestVehicles(Pageable pageable);
}
