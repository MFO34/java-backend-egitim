package com.autohub.controller;

// Spring Web ve proje importları
import com.autohub.dto.request.VehicleCreateRequest;
import com.autohub.dto.request.VehicleUpdateRequest;
import com.autohub.dto.response.VehicleResponse;
import com.autohub.entity.Vehicle;
import com.autohub.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Araç REST Controller - HTTP isteklerini servis katmanına yönlendirir
 *
 * OOP Prensibi - SORUMLULUK AYIRIMI (Separation of Concerns):
 * Controller sadece HTTP request/response yönetir.
 * İş mantığı Service katmanında, veri erişimi Repository'de tutulur.
 *
 * OOP Prensibi - KAPSÜLLEME (Encapsulation):
 * Servis bağımlılığı final ve constructor injection ile kapsüllenir.
 *
 * @RestController: @Controller + @ResponseBody - JSON yanıt döner
 * @RequestMapping: Bu controller'ın base URL'i
 */
@RestController
@RequestMapping("/api/vehicles")  // Tüm araç endpoint'leri /api/vehicles altında
@RequiredArgsConstructor
@Slf4j
public class VehicleController {

    // Araç iş mantığı servisi - constructor injection
    private final VehicleService vehicleService;

    /**
     * Yeni araç oluşturur
     * POST /api/vehicles
     *
     * @PreAuthorize: Sadece ADMIN veya MANAGER rolüne sahip kullanıcılar erişebilir
     *
     * @param request Araç oluşturma isteği (JSON body)
     * @return 201 Created + oluşturulan araç DTO'su
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<VehicleResponse> createVehicle(
            @Valid @RequestBody VehicleCreateRequest request) {
        // İstek loglanıyor
        log.info("POST /api/vehicles - Araç oluşturma isteği: Plaka={}", request.getLicensePlate());

        // Servise iş mantığını delege et
        VehicleResponse response = vehicleService.createVehicle(request);

        // 201 Created durum kodu ile döndür
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Araç günceller
     * PUT /api/vehicles/{id}
     *
     * @param id Güncellenecek araç ID'si (URL path'den alınır)
     * @param request Güncelleme verisi (JSON body)
     * @return 200 OK + güncellenmiş araç DTO'su
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<VehicleResponse> updateVehicle(
            @PathVariable Long id,
            @Valid @RequestBody VehicleUpdateRequest request) {
        log.info("PUT /api/vehicles/{} - Araç güncelleme isteği", id);

        VehicleResponse response = vehicleService.updateVehicle(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * ID ile araç getirir
     * GET /api/vehicles/{id}
     *
     * Authenticated kullanıcılar erişebilir (SecurityConfig'den gelen kural)
     *
     * @param id Araç ID'si
     * @return 200 OK + araç detayları
     */
    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponse> getVehicleById(@PathVariable Long id) {
        log.debug("GET /api/vehicles/{} - Araç detay isteği", id);

        VehicleResponse response = vehicleService.getVehicleById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Plakaya göre araç getirir
     * GET /api/vehicles/plate/{licensePlate}
     *
     * @param licensePlate URL encode edilmiş plaka numarası
     * @return 200 OK + araç detayları
     */
    @GetMapping("/plate/{licensePlate}")
    public ResponseEntity<VehicleResponse> getVehicleByPlate(
            @PathVariable String licensePlate) {
        VehicleResponse response = vehicleService.getVehicleByLicensePlate(licensePlate);
        return ResponseEntity.ok(response);
    }

    /**
     * Tüm araçları sayfalı listeler
     * GET /api/vehicles?page=0&size=10&sort=price,asc
     *
     * @param pageable Spring Data sayfalama parametreleri (URL query params)
     * @return 200 OK + sayfalı araç listesi
     */
    @GetMapping
    public ResponseEntity<Page<VehicleResponse>> getAllVehicles(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        log.debug("GET /api/vehicles - Araç listesi isteği: Sayfa={}", pageable.getPageNumber());

        Page<VehicleResponse> response = vehicleService.getAllVehicles(pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Duruma göre araçları filtreler
     * GET /api/vehicles/status/{status}
     *
     * @param status Araç durumu (AVAILABLE, RENTED vs.)
     * @param pageable Sayfalama parametreleri
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<VehicleResponse>> getVehiclesByStatus(
            @PathVariable Vehicle.VehicleStatus status,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<VehicleResponse> response = vehicleService.getVehiclesByStatus(status, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Fiyat aralığına göre araçları filtreler
     * GET /api/vehicles/price-range?min=50000&max=200000
     *
     * @param min Minimum fiyat (query param)
     * @param max Maksimum fiyat (query param)
     * @param pageable Sayfalama
     */
    @GetMapping("/price-range")
    public ResponseEntity<Page<VehicleResponse>> getVehiclesByPriceRange(
            @RequestParam BigDecimal min,
            @RequestParam BigDecimal max,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<VehicleResponse> response = vehicleService.getVehiclesByPriceRange(min, max, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Marka adına göre müsait araçları listeler
     * GET /api/vehicles/brand/{brandName}/available
     *
     * @param brandName Marka adı
     * @param pageable Sayfalama
     */
    @GetMapping("/brand/{brandName}/available")
    public ResponseEntity<Page<VehicleResponse>> getAvailableByBrand(
            @PathVariable String brandName,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<VehicleResponse> response = vehicleService.getAvailableVehiclesByBrand(brandName, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Araç arama - Elasticsearch ile full-text arama
     * GET /api/vehicles/search?q=toyota+corolla+2022
     *
     * @param q Arama sorgusu
     * @param pageable Sayfalama
     */
    @GetMapping("/search")
    public ResponseEntity<Page<VehicleResponse>> searchVehicles(
            @RequestParam("q") String query,
            @PageableDefault(size = 10) Pageable pageable) {
        log.info("GET /api/vehicles/search - Arama: query={}", query);
        Page<VehicleResponse> response = vehicleService.searchVehicles(query, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Araç durumunu günceller
     * PATCH /api/vehicles/{id}/status
     *
     * @param id Araç ID'si
     * @param status Yeni durum (JSON body)
     * @return 200 OK
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<Void> updateVehicleStatus(
            @PathVariable Long id,
            @RequestParam Vehicle.VehicleStatus status) {
        vehicleService.updateVehicleStatus(id, status);
        return ResponseEntity.ok().build();
    }

    /**
     * Araç siler (soft delete)
     * DELETE /api/vehicles/{id}
     *
     * @param id Silinecek araç ID'si
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")  // Sadece admin silebilir
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long id) {
        log.info("DELETE /api/vehicles/{} - Araç silme isteği", id);
        vehicleService.deleteVehicle(id);
        // 204 No Content - başarılı silme için standart HTTP durum kodu
        return ResponseEntity.noContent().build();
    }

    /**
     * Son eklenen araçları listeler - dashboard için
     * GET /api/vehicles/latest?limit=5
     *
     * @param limit Listelenecek araç sayısı (varsayılan 5)
     * @return Son araçlar listesi
     */
    @GetMapping("/latest")
    public ResponseEntity<List<VehicleResponse>> getLatestVehicles(
            @RequestParam(defaultValue = "5") int limit) {
        List<VehicleResponse> response = vehicleService.getLatestVehicles(limit);
        return ResponseEntity.ok(response);
    }

    /**
     * Araç durumu istatistikleri - dashboard için
     * GET /api/vehicles/stats
     *
     * @return Durum bazlı araç sayıları (JSON map)
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Long>> getVehicleStats() {
        Map<String, Long> stats = vehicleService.getVehicleStatusStats();
        return ResponseEntity.ok(stats);
    }
}
