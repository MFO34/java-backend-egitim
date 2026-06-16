// Marka ve Model veri modelleri

// Marka entity modeli
export interface Brand {
  id: number;
  name: string;           // Marka adı (Toyota, BMW vb.)
  country: string;        // Menşei ülke
  logoUrl?: string;       // Logo URL'i (opsiyonel)
  isActive: boolean;      // Aktif/pasif durumu
  createdAt: string;
  updatedAt?: string;
  modelCount?: number;    // Bu markaya ait model sayısı
}

// Model entity modeli
export interface CarModel {
  id: number;
  name: string;           // Model adı (Corolla, 3 Serisi vb.)
  brandId: number;
  brandName: string;
  segment?: string;       // Segment (Sedan, SUV, Hatchback vb.)
  engineCapacity?: number;// Motor hacmi (cc)
  isActive: boolean;
  createdAt: string;
  updatedAt?: string;
}

// Marka oluşturma/güncelleme isteği
export interface BrandRequest {
  name: string;
  country: string;
  logoUrl?: string;
}

// Model oluşturma/güncelleme isteği
export interface CarModelRequest {
  name: string;
  brandId: number;
  segment?: string;
  engineCapacity?: number;
}
