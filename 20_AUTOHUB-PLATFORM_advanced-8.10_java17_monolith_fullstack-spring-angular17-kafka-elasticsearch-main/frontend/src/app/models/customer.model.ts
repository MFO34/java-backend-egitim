// Müşteri Model Tanımlamaları

// Müşteri türleri
export enum CustomerType {
  INDIVIDUAL = 'INDIVIDUAL',   // Bireysel müşteri
  CORPORATE = 'CORPORATE'      // Kurumsal müşteri
}

/**
 * Müşteri modeli - backend Customer entity ile eşleşir
 */
export interface Customer {
  id: number;
  firstName: string;
  lastName: string;
  tcNumber?: string;                 // TC kimlik numarası
  phone?: string;                    // Telefon numarası
  email?: string;                    // E-posta adresi
  birthDate?: string;                // Doğum tarihi (ISO)
  drivingLicenseNumber?: string;     // Ehliyet numarası
  licenseExpiryDate?: string;        // Ehliyet geçerlilik tarihi
  address?: string;                  // Açık adres
  city?: string;                     // Şehir
  postalCode?: string;               // Posta kodu
  customerType: CustomerType;        // Bireysel / Kurumsal
  companyName?: string;              // Şirket adı (kurumsal için)
  taxNumber?: string;                // Vergi numarası (kurumsal için)
  notes?: string;                    // Ek notlar
  createdAt: string;
  updatedAt?: string;
}

// Müşteri oluşturma/güncelleme isteği
export interface CustomerRequest {
  firstName: string;
  lastName: string;
  tcNumber?: string;
  phone?: string;
  email?: string;
  birthDate?: string;
  drivingLicenseNumber?: string;
  licenseExpiryDate?: string;
  address?: string;
  city?: string;
  postalCode?: string;
  customerType?: CustomerType;
  companyName?: string;
  taxNumber?: string;
  notes?: string;
}

// Sayfalı liste yanıtı
export interface PagedCustomerResponse {
  content: Customer[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
