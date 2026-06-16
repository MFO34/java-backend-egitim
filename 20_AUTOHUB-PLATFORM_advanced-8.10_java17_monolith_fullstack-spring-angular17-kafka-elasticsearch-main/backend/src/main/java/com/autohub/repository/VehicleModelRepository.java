package com.autohub.repository;

import com.autohub.entity.Brand;
import com.autohub.entity.VehicleModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Araç Modeli Repository - Model veritabanı erişim katmanı
 */
@Repository
public interface VehicleModelRepository extends JpaRepository<VehicleModel, Long> {

    /**
     * Markaya ait tüm modelleri listeler
     *
     * @param brand Araç markası
     * @return Markaya ait modeller
     */
    List<VehicleModel> findByBrandAndIsDeletedFalseOrderByNameAsc(Brand brand);

    /**
     * Marka ID'sine göre modelleri listeler
     *
     * @param brandId Marka ID'si
     * @return Markaya ait modeller
     */
    List<VehicleModel> findByBrandIdAndIsDeletedFalseOrderByNameAsc(Long brandId);

    /**
     * Araç tipine göre modelleri listeler
     *
     * @param vehicleType Araç tipi (SUV, SEDAN vs.)
     * @return Modeller listesi
     */
    List<VehicleModel> findByVehicleTypeAndIsDeletedFalse(VehicleModel.VehicleType vehicleType);

    /**
     * Marka ve model adına göre model bulur
     *
     * @param brandId Marka ID
     * @param name Model adı
     * @return Bulunan model
     */
    Optional<VehicleModel> findByBrandIdAndNameIgnoreCaseAndIsDeletedFalse(Long brandId, String name);

    /**
     * Model adına göre kısmi arama - autocomplete için
     *
     * @param name Aranacak ad (kısmi)
     * @return Eşleşen modeller
     */
    @Query("SELECT m FROM VehicleModel m WHERE LOWER(m.name) LIKE LOWER(CONCAT('%', :name, '%')) AND m.isDeleted = false")
    List<VehicleModel> searchByName(@Param("name") String name);

    /**
     * Yakıt tipine göre modelleri listeler
     */
    List<VehicleModel> findByFuelTypeAndIsDeletedFalse(VehicleModel.FuelType fuelType);

    /**
     * Model yılına göre listeler
     */
    List<VehicleModel> findByModelYearAndIsDeletedFalseOrderByNameAsc(Integer modelYear);
}
