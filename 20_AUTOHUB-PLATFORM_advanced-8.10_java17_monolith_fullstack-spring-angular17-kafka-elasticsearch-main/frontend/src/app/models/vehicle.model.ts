// Araç ile ilgili TypeScript model tanımlamaları
// Backend DTO'ları ile birebir eşleşir

/**
 * Araç durumu enum - backend ile senkron
 */
export enum VehicleStatus {
  AVAILABLE = 'AVAILABLE',       // Müsait
  RENTED = 'RENTED',             // Kiralanmış
  SOLD = 'SOLD',                 // Satılmış
  MAINTENANCE = 'MAINTENANCE',   // Bakımda
  RESERVED = 'RESERVED',         // Rezerve
  DAMAGED = 'DAMAGED'            // Hasarlı
}

/**
 * Araç tipi enum
 */
export enum VehicleType {
  SEDAN = 'SEDAN',
  HATCHBACK = 'HATCHBACK',
  SUV = 'SUV',
  PICKUP = 'PICKUP',
  COUPE = 'COUPE',
  CONVERTIBLE = 'CONVERTIBLE',
  MINIVAN = 'MINIVAN',
  TRUCK = 'TRUCK',
  MOTORCYCLE = 'MOTORCYCLE',
  OTHER = 'OTHER'
}

/**
 * Yakıt tipi enum
 */
export enum FuelType {
  GASOLINE = 'GASOLINE',         // Benzin
  DIESEL = 'DIESEL',             // Dizel
  ELECTRIC = 'ELECTRIC',         // Elektrikli
  HYBRID = 'HYBRID',             // Hibrit
  LPG = 'LPG',                   // LPG
  NATURAL_GAS = 'NATURAL_GAS'    // Doğalgaz
}

/**
 * Araç yanıt modeli - API'den gelen veri yapısı
 * Backend VehicleResponse DTO ile eşleşir
 */
export interface Vehicle {
  id: number;                     // Benzersiz kimlik
  licensePlate: string;           // Plaka numarası
  year: number;                   // Üretim yılı
  color: string;                  // Renk
  mileage: number;                // Kilometre
  price: number;                  // Fiyat
  dailyRate: number;              // Günlük kiralama ücreti
  vinNumber?: string;             // VIN numarası (opsiyonel)
  description?: string;           // Açıklama
  status: VehicleStatus;          // Araç durumu
  modelId: number;                // Model ID
  modelName: string;              // Model adı
  brandName: string;              // Marka adı
  imageUrls?: string[];           // Fotoğraf URL'leri
  lastInspectionDate?: string;    // Son muayene tarihi
  insuranceExpiryDate?: string;   // Sigorta bitiş tarihi
  createdAt: string;              // Oluşturulma tarihi
  updatedAt?: string;             // Son güncelleme tarihi
}

/**
 * Araç oluşturma isteği modeli
 * Backend VehicleCreateRequest ile eşleşir
 */
export interface CreateVehicleRequest {
  modelId: number;
  licensePlate: string;
  year: number;
  color?: string;
  mileage?: number;
  price: number;
  dailyRate?: number;
  vinNumber?: string;
  engineNumber?: string;
  description?: string;
  imageUrls?: string[];
  lastInspectionDate?: string;
  insuranceExpiryDate?: string;
  features?: string[];
}

/**
 * Araç güncelleme isteği modeli
 * Tüm alanlar opsiyonel (PATCH pattern)
 */
export interface UpdateVehicleRequest {
  licensePlate?: string;
  color?: string;
  mileage?: number;
  price?: number;
  dailyRate?: number;
  description?: string;
  imageUrls?: string[];
  lastInspectionDate?: string;
  insuranceExpiryDate?: string;
  features?: string[];
}

/**
 * Sayfalı API yanıtı için genel model
 * Spring Data Page nesnesinin TypeScript karşılığı
 */
export interface PageResponse<T> {
  content: T[];                   // Veri listesi
  totalElements: number;          // Toplam kayıt sayısı
  totalPages: number;             // Toplam sayfa sayısı
  size: number;                   // Sayfa boyutu
  number: number;                 // Mevcut sayfa numarası (0-indexed)
  first: boolean;                 // İlk sayfa mı?
  last: boolean;                  // Son sayfa mı?
  empty: boolean;                 // Boş mu?
}

/**
 * Araç durumu istatistikleri - dashboard için
 */
export interface VehicleStats {
  AVAILABLE: number;
  RENTED: number;
  SOLD: number;
  MAINTENANCE: number;
  RESERVED: number;
  DAMAGED: number;
}

/**
 * Araç durumu Türkçe etiketleri
 * UI'da gösterilecek metin değerleri
 */
export const VehicleStatusLabels: Record<VehicleStatus, string> = {
  [VehicleStatus.AVAILABLE]: 'Müsait',
  [VehicleStatus.RENTED]: 'Kiralandı',
  [VehicleStatus.SOLD]: 'Satıldı',
  [VehicleStatus.MAINTENANCE]: 'Bakımda',
  [VehicleStatus.RESERVED]: 'Rezerve',
  [VehicleStatus.DAMAGED]: 'Hasarlı'
};

/**
 * Araç durumu renk kodları - Material chip renkleri için
 */
export const VehicleStatusColors: Record<VehicleStatus, string> = {
  [VehicleStatus.AVAILABLE]: 'primary',     // Mavi - müsait
  [VehicleStatus.RENTED]: 'accent',         // Sarı - kiralandı
  [VehicleStatus.SOLD]: 'warn',             // Kırmızı - satıldı
  [VehicleStatus.MAINTENANCE]: '',           // Gri - bakımda
  [VehicleStatus.RESERVED]: 'accent',       // Sarı - rezerve
  [VehicleStatus.DAMAGED]: 'warn'           // Kırmızı - hasarlı
};
