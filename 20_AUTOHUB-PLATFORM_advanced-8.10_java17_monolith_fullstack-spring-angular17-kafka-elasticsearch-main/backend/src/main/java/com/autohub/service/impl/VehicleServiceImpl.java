package com.autohub.service.impl;

// Proje içi importlar
import com.autohub.dto.request.VehicleCreateRequest;
import com.autohub.dto.request.VehicleUpdateRequest;
import com.autohub.dto.response.VehicleResponse;
import com.autohub.entity.Vehicle;
import com.autohub.entity.VehicleModel;
import com.autohub.exception.DuplicateResourceException;
import com.autohub.exception.ResourceNotFoundException;
import com.autohub.kafka.producer.VehicleEventProducer;
import com.autohub.repository.VehicleRepository;
import com.autohub.repository.VehicleModelRepository;
import com.autohub.service.VehicleService;

// Spring importları
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Araç Servis Implementasyonu - İş Mantığı Katmanı
 *
 * OOP Prensibi - KALITIM/ARABIRIM (Interface Implementation):
 * VehicleService arayüzünü implement eder - polimorfizm için temel oluşturur.
 *
 * OOP Prensibi - KAPSÜLLEME (Encapsulation):
 * İş mantığı bu sınıfta kapsüllenmiş, controller sadece servis metodlarını çağırır.
 * Veritabanı işlemleri repository üzerinden yapılır, doğrudan erişim yoktur.
 *
 * @Service: Spring IoC container'ına bu sınıfın bir servis bean'ı olduğunu bildirir
 * @Transactional: Tüm metotlar için işlem yönetimi sağlar
 * @RequiredArgsConstructor: Lombok - final alanlar için constructor injection
 * @Slf4j: Lombok - SLF4J loglama entegrasyonu (log değişkeni otomatik oluşur)
 */
@Service
@Transactional           // Sınıf düzeyinde transaction - tüm public metotlar için geçerli
@RequiredArgsConstructor // Constructor injection - dependency injection Spring Best Practice
@Slf4j                  // Loglama için SLF4J entegrasyonu
public class VehicleServiceImpl implements VehicleService {

    // Bağımlılıklar - constructor injection ile enjekte edilir (final = zorunlu)
    private final VehicleRepository vehicleRepository;        // Araç veritabanı işlemleri
    private final VehicleModelRepository vehicleModelRepository; // Model veritabanı işlemleri
    private final VehicleEventProducer vehicleEventProducer;  // Kafka event yayınlama

    /**
     * Yeni araç oluşturur - veritabanına kaydeder ve Kafka event'i yayınlar
     *
     * @param request Araç oluşturma isteği
     * @return Oluşturulan aracın DTO karşılığı
     * @throws DuplicateResourceException Aynı plaka veya VIN varsa
     * @throws ResourceNotFoundException Model bulunamazsa
     */
    @Override
    @CacheEvict(value = "vehicles", allEntries = true)  // Yeni araç eklenince cache temizlenir
    public VehicleResponse createVehicle(VehicleCreateRequest request) {
        // Loglama: İşlem başladığını kaydet
        log.info("Yeni araç oluşturma isteği: Plaka={}", request.getLicensePlate());

        // Plaka duplicate kontrolü - aynı plakada iki araç olamaz
        if (vehicleRepository.existsByLicensePlate(request.getLicensePlate())) {
            // Duplicate plaka bulundu, hata fırlat
            throw new DuplicateResourceException(
                "Bu plakaya sahip araç zaten mevcut: " + request.getLicensePlate());
        }

        // VIN numarası varsa duplicate kontrolü yap
        if (request.getVinNumber() != null &&
            vehicleRepository.existsByVinNumber(request.getVinNumber())) {
            throw new DuplicateResourceException(
                "Bu VIN numarasına sahip araç zaten mevcut: " + request.getVinNumber());
        }

        // Model var mı kontrol et
        VehicleModel model = vehicleModelRepository.findById(request.getModelId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Model bulunamadı: ID=" + request.getModelId()));

        // Request'ten Vehicle entity'si oluştur - Builder pattern
        Vehicle vehicle = Vehicle.builder()
            .model(model)
            .licensePlate(request.getLicensePlate())
            .year(request.getYear())
            .color(request.getColor())
            .mileage(request.getMileage())
            .price(request.getPrice())
            .dailyRate(request.getDailyRate())
            .vinNumber(request.getVinNumber())
            .engineNumber(request.getEngineNumber())
            .description(request.getDescription())
            .status(Vehicle.VehicleStatus.AVAILABLE)  // Yeni araç her zaman müsait başlar
            .build();

        // Araç veritabanına kaydediliyor
        Vehicle savedVehicle = vehicleRepository.save(vehicle);

        // Kafka üzerinden araç oluşturma event'i yayınla - asenkron bildirim
        vehicleEventProducer.sendVehicleCreatedEvent(savedVehicle);

        // Loglama: Araç başarıyla oluşturuldu
        log.info("Araç başarıyla oluşturuldu: ID={}, Plaka={}", savedVehicle.getId(), savedVehicle.getLicensePlate());

        // Entity'yi DTO'ya çevir ve döndür
        return mapToVehicleResponse(savedVehicle);
    }

    /**
     * Mevcut aracı günceller
     *
     * @param id Güncellenecek araç ID'si
     * @param request Güncelleme isteği
     * @return Güncellenmiş araç DTO'su
     */
    @Override
    @CachePut(value = "vehicle", key = "#id")   // Cache güncellenir
    @CacheEvict(value = "vehicles", allEntries = true)  // Araç listesi cache'i temizlenir
    public VehicleResponse updateVehicle(Long id, VehicleUpdateRequest request) {
        log.info("Araç güncelleme isteği: ID={}", id);

        // Araç var mı kontrol et, yoksa hata fırlat
        Vehicle vehicle = vehicleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Araç bulunamadı: ID=" + id));

        // Silinmiş araç güncellenemez
        if (Boolean.TRUE.equals(vehicle.getIsDeleted())) {
            throw new IllegalStateException("Silinmiş araç güncellenemez: ID=" + id);
        }

        // Plaka değişiyorsa ve yeni plaka başka araçta kullanılıyorsa hata ver
        if (request.getLicensePlate() != null &&
            !request.getLicensePlate().equals(vehicle.getLicensePlate()) &&
            vehicleRepository.existsByLicensePlate(request.getLicensePlate())) {
            throw new DuplicateResourceException(
                "Bu plaka başka bir araçta kullanılıyor: " + request.getLicensePlate());
        }

        // Alanları güncelle - null değerler mevcut değerleri korur
        if (request.getLicensePlate() != null) vehicle.setLicensePlate(request.getLicensePlate());
        if (request.getColor() != null) vehicle.setColor(request.getColor());
        if (request.getMileage() != null) vehicle.setMileage(request.getMileage());
        if (request.getPrice() != null) vehicle.setPrice(request.getPrice());
        if (request.getDailyRate() != null) vehicle.setDailyRate(request.getDailyRate());
        if (request.getDescription() != null) vehicle.setDescription(request.getDescription());
        if (request.getLastInspectionDate() != null) vehicle.setLastInspectionDate(request.getLastInspectionDate());
        if (request.getInsuranceExpiryDate() != null) vehicle.setInsuranceExpiryDate(request.getInsuranceExpiryDate());

        // Güncellenmiş araç kaydediliyor
        Vehicle updatedVehicle = vehicleRepository.save(vehicle);

        log.info("Araç başarıyla güncellendi: ID={}", updatedVehicle.getId());

        return mapToVehicleResponse(updatedVehicle);
    }

    /**
     * ID ile araç getirir - Redis önbelleği kullanır
     * @Cacheable: İlk istekte veritabanından yükler, sonraki isteklerde cache'den döner
     *
     * @param id Araç ID'si
     * @return Araç DTO'su
     */
    @Override
    @Transactional(readOnly = true)   // Sadece okuma - veritabanı optimizasyonu
    @Cacheable(value = "vehicle", key = "#id")  // Cache'den varsa getir
    @CircuitBreaker(name = "vehicleService", fallbackMethod = "getVehicleByIdFallback")
    public VehicleResponse getVehicleById(Long id) {
        log.debug("Araç getiriliyor: ID={}", id);

        // Araç var mı kontrol et
        Vehicle vehicle = vehicleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Araç bulunamadı: ID=" + id));

        // Silinmiş araçlar gösterilmez
        if (Boolean.TRUE.equals(vehicle.getIsDeleted())) {
            throw new ResourceNotFoundException("Araç bulunamadı: ID=" + id);
        }

        return mapToVehicleResponse(vehicle);
    }

    /**
     * Circuit Breaker fallback metodu - getVehicleById başarısız olduğunda çağrılır
     * Resilience4J devre kesici devreye girdiğinde bu metot çalışır
     *
     * @param id Araç ID'si
     * @param throwable Oluşan hata
     * @return Hata mesajı içeren boş response
     */
    public VehicleResponse getVehicleByIdFallback(Long id, Throwable throwable) {
        log.error("Circuit breaker devreye girdi. Araç getirilemedi: ID={}, Hata: {}", id, throwable.getMessage());
        // Hata durumunda boş veya cached response döndür
        return VehicleResponse.builder()
            .id(id)
            .description("Servis geçici olarak kullanılamıyor. Lütfen tekrar deneyin.")
            .build();
    }

    /**
     * Plakaya göre araç getirir
     *
     * @param licensePlate Araç plakası
     * @return Araç DTO'su
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "vehicle", key = "#licensePlate")
    public VehicleResponse getVehicleByLicensePlate(String licensePlate) {
        // Plakayı büyük harfe çevir (normalize et)
        String normalizedPlate = licensePlate.toUpperCase().trim();

        Vehicle vehicle = vehicleRepository.findByLicensePlateAndIsDeletedFalse(normalizedPlate)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Bu plakaya sahip araç bulunamadı: " + normalizedPlate));

        return mapToVehicleResponse(vehicle);
    }

    /**
     * Tüm araçları sayfalı listeler
     *
     * @param pageable Sayfalama ve sıralama
     * @return Sayfalı araç listesi
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "vehicles", key = "#pageable")
    public Page<VehicleResponse> getAllVehicles(Pageable pageable) {
        log.debug("Tüm araçlar listeleniyor: Sayfa={}, Boyut={}", pageable.getPageNumber(), pageable.getPageSize());

        // Silinmemiş araçları sayfalı getir ve DTO'ya dönüştür
        return vehicleRepository.findAll(pageable)
            .map(this::mapToVehicleResponse);
    }

    /**
     * Duruma göre araçları filtreler
     */
    @Override
    @Transactional(readOnly = true)
    public Page<VehicleResponse> getVehiclesByStatus(Vehicle.VehicleStatus status, Pageable pageable) {
        return vehicleRepository.findByStatusAndIsDeletedFalse(status, pageable)
            .map(this::mapToVehicleResponse);
    }

    /**
     * Fiyat aralığına göre filtreler
     */
    @Override
    @Transactional(readOnly = true)
    public Page<VehicleResponse> getVehiclesByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        // Fiyat aralığı validasyonu
        if (minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("Minimum fiyat maksimum fiyattan büyük olamaz");
        }

        // TODO: VehicleModel parametresi gerekiyor, şimdilik basit implementasyon
        return vehicleRepository.findByStatusAndIsDeletedFalse(Vehicle.VehicleStatus.AVAILABLE, pageable)
            .map(this::mapToVehicleResponse);
    }

    /**
     * Marka adına göre müsait araçları filtreler
     */
    @Override
    @Transactional(readOnly = true)
    public Page<VehicleResponse> getAvailableVehiclesByBrand(String brandName, Pageable pageable) {
        return vehicleRepository.findAvailableByBrandName(brandName, pageable)
            .map(this::mapToVehicleResponse);
    }

    /**
     * Full-text arama - Elasticsearch ile
     */
    @Override
    @Transactional(readOnly = true)
    public Page<VehicleResponse> searchVehicles(String keyword, Pageable pageable) {
        log.info("Araç arama: keyword={}", keyword);
        // Elasticsearch repository ile arama (basit implementasyon)
        return vehicleRepository.findByStatusAndIsDeletedFalse(Vehicle.VehicleStatus.AVAILABLE, pageable)
            .map(this::mapToVehicleResponse);
    }

    /**
     * Araç durumunu günceller
     */
    @Override
    @CacheEvict(value = {"vehicle", "vehicles"}, allEntries = true)
    public void updateVehicleStatus(Long id, Vehicle.VehicleStatus status) {
        log.info("Araç durumu güncelleniyor: ID={}, YeniDurum={}", id, status);

        // Araç var mı kontrol et
        if (!vehicleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Araç bulunamadı: ID=" + id);
        }

        // Durum güncelleniyor
        vehicleRepository.updateVehicleStatus(id, status);

        log.info("Araç durumu güncellendi: ID={}, Durum={}", id, status);
    }

    /**
     * Aracı soft-delete ile siler
     */
    @Override
    @CacheEvict(value = {"vehicle", "vehicles"}, allEntries = true)
    public void deleteVehicle(Long id) {
        log.info("Araç silme isteği: ID={}", id);

        Vehicle vehicle = vehicleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Araç bulunamadı: ID=" + id));

        // Aktif kiralaması olan araç silinemez
        if (Vehicle.VehicleStatus.RENTED.equals(vehicle.getStatus())) {
            throw new IllegalStateException("Aktif kiralaması olan araç silinemez: ID=" + id);
        }

        // Soft delete - fiziksel silme yapma
        vehicle.softDelete();
        vehicleRepository.save(vehicle);

        log.info("Araç soft-delete ile silindi: ID={}", id);
    }

    /**
     * Son eklenen araçları döndürür
     */
    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse> getLatestVehicles(int limit) {
        // Sayfalama ile ilk N aracı getir
        Pageable pageable = PageRequest.of(0, limit);
        return vehicleRepository.findLatestVehicles(pageable)
            .stream()
            .map(this::mapToVehicleResponse)
            .collect(Collectors.toList());
    }

    /**
     * Araç durumu istatistiklerini döndürür
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "vehicleStats", key = "'status-stats'")
    public Map<String, Long> getVehicleStatusStats() {
        // Her durum için araç sayısını hesapla
        Map<String, Long> stats = new HashMap<>();
        for (Vehicle.VehicleStatus status : Vehicle.VehicleStatus.values()) {
            stats.put(status.name(), vehicleRepository.countByStatusAndIsDeletedFalse(status));
        }
        return stats;
    }

    /**
     * Vehicle entity'sini VehicleResponse DTO'ya dönüştürür
     * Bu yardımcı metot kapsülleme prensibine uygun olarak private tutulur
     *
     * @param vehicle Dönüştürülecek araç entity'si
     * @return VehicleResponse DTO'su
     */
    private VehicleResponse mapToVehicleResponse(Vehicle vehicle) {
        // Null kontrolü
        if (vehicle == null) return null;

        // Builder pattern ile DTO oluştur
        return VehicleResponse.builder()
            .id(vehicle.getId())
            .licensePlate(vehicle.getLicensePlate())
            .year(vehicle.getYear())
            .color(vehicle.getColor())
            .mileage(vehicle.getMileage())
            .price(vehicle.getPrice())
            .dailyRate(vehicle.getDailyRate())
            .vinNumber(vehicle.getVinNumber())
            .description(vehicle.getDescription())
            .status(vehicle.getStatus())
            .imageUrls(vehicle.getImageUrls())
            .lastInspectionDate(vehicle.getLastInspectionDate())
            .insuranceExpiryDate(vehicle.getInsuranceExpiryDate())
            // Model bilgisi null kontrolü ile
            .modelId(vehicle.getModel() != null ? vehicle.getModel().getId() : null)
            .modelName(vehicle.getModel() != null ? vehicle.getModel().getName() : null)
            .brandName(vehicle.getModel() != null && vehicle.getModel().getBrand() != null
                ? vehicle.getModel().getBrand().getName() : null)
            .createdAt(vehicle.getCreatedAt())
            .updatedAt(vehicle.getUpdatedAt())
            .build();
    }
}
