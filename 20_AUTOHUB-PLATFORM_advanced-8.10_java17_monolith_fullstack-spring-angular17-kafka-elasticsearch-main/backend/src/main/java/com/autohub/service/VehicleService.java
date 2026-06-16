package com.autohub.service;

// Proje içi ve Spring importları
import com.autohub.dto.request.VehicleCreateRequest;
import com.autohub.dto.request.VehicleUpdateRequest;
import com.autohub.dto.response.VehicleResponse;
import com.autohub.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

/**
 * Araç Servis Arayüzü - İş Mantığı Tanımları
 *
 * OOP Prensibi - SOYUTLAMA (Abstraction):
 * Bu arayüz, araç yönetimi için gerekli tüm iş operasyonlarını tanımlar.
 * İmplementasyon detayları VehicleServiceImpl sınıfında gizlenir.
 * Bağımlılık enjeksiyonunda bu arayüz kullanılır, sınıf değil.
 *
 * OOP Prensibi - POLİMORFİZM (Polymorphism):
 * Farklı VehicleService implementasyonları olabilir (test, üretim vs.)
 * ve hepsi bu arayüze uygun çalışır.
 */
public interface VehicleService {

    /**
     * Yeni araç kaydı oluşturur
     *
     * @param request Araç oluşturma isteği (plaka, model, fiyat vs.)
     * @return Oluşturulan aracın detayları
     */
    VehicleResponse createVehicle(VehicleCreateRequest request);

    /**
     * Mevcut aracı günceller
     *
     * @param id Güncellenecek araç ID'si
     * @param request Güncelleme isteği
     * @return Güncellenmiş araç detayları
     */
    VehicleResponse updateVehicle(Long id, VehicleUpdateRequest request);

    /**
     * ID ile araç getirir
     *
     * @param id Araç ID'si
     * @return Araç detayları
     */
    VehicleResponse getVehicleById(Long id);

    /**
     * Plakaya göre araç getirir
     *
     * @param licensePlate Araç plakası
     * @return Araç detayları
     */
    VehicleResponse getVehicleByLicensePlate(String licensePlate);

    /**
     * Tüm araçları sayfalı olarak listeler
     *
     * @param pageable Sayfalama ve sıralama bilgisi
     * @return Sayfalı araç listesi
     */
    Page<VehicleResponse> getAllVehicles(Pageable pageable);

    /**
     * Duruma göre araçları filtreler
     *
     * @param status Araç durumu
     * @param pageable Sayfalama bilgisi
     * @return Filtrelenmiş araç listesi
     */
    Page<VehicleResponse> getVehiclesByStatus(Vehicle.VehicleStatus status, Pageable pageable);

    /**
     * Fiyat aralığına göre araçları filtreler
     *
     * @param minPrice Minimum fiyat
     * @param maxPrice Maksimum fiyat
     * @param pageable Sayfalama bilgisi
     * @return Filtrelenmiş araç listesi
     */
    Page<VehicleResponse> getVehiclesByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    /**
     * Marka adına göre müsait araçları filtreler
     *
     * @param brandName Marka adı
     * @param pageable Sayfalama bilgisi
     * @return Müsait araç listesi
     */
    Page<VehicleResponse> getAvailableVehiclesByBrand(String brandName, Pageable pageable);

    /**
     * Elasticsearch ile araç full-text araması yapar
     *
     * @param keyword Aranacak kelime
     * @param pageable Sayfalama bilgisi
     * @return Arama sonuçları
     */
    Page<VehicleResponse> searchVehicles(String keyword, Pageable pageable);

    /**
     * Araç durumunu günceller
     *
     * @param id Araç ID'si
     * @param status Yeni durum
     */
    void updateVehicleStatus(Long id, Vehicle.VehicleStatus status);

    /**
     * Aracı soft-delete ile siler
     *
     * @param id Silinecek araç ID'si
     */
    void deleteVehicle(Long id);

    /**
     * Son eklenen araçları döndürür - dashboard için
     *
     * @param limit Listelenecek araç sayısı
     * @return Son araçlar listesi
     */
    List<VehicleResponse> getLatestVehicles(int limit);

    /**
     * Araç durumu istatistiklerini döndürür - dashboard için
     *
     * @return Durum bazlı araç sayıları
     */
    java.util.Map<String, Long> getVehicleStatusStats();
}
