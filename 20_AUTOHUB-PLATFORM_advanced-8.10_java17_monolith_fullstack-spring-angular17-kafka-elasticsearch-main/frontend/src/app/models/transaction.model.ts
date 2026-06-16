// İşlem (Kiralama/Satış) veri modelleri

// İşlem türleri
export enum TransactionType {
  RENTAL = 'RENTAL',   // Kiralama
  SALE = 'SALE'        // Satış
}

// İşlem durum türleri
export enum TransactionStatus {
  PENDING = 'PENDING',       // Beklemede
  ACTIVE = 'ACTIVE',         // Aktif
  COMPLETED = 'COMPLETED',   // Tamamlandı
  CANCELLED = 'CANCELLED'    // İptal edildi
}

// Türkçe etiketler
export const TransactionTypeLabels: Record<TransactionType, string> = {
  [TransactionType.RENTAL]: 'Kiralama',
  [TransactionType.SALE]: 'Satış'
};

export const TransactionStatusLabels: Record<TransactionStatus, string> = {
  [TransactionStatus.PENDING]: 'Beklemede',
  [TransactionStatus.ACTIVE]: 'Aktif',
  [TransactionStatus.COMPLETED]: 'Tamamlandı',
  [TransactionStatus.CANCELLED]: 'İptal Edildi'
};

// İşlem entity modeli
export interface Transaction {
  id: number;
  vehicleId: number;
  vehiclePlate: string;        // Araç plakası (görüntüleme için)
  vehicleName: string;         // Araç adı (Marka + Model)
  customerId: number;
  customerName: string;        // Müşteri adı soyadı
  transactionType: TransactionType;
  status: TransactionStatus;
  startDate: string;           // ISO tarih formatı
  endDate?: string;            // Kiralama bitiş tarihi (RENTAL için)
  totalAmount: number;         // Toplam tutar (KDV dahil)
  baseAmount: number;          // KDV hariç tutar
  taxAmount: number;           // KDV tutarı
  notes?: string;              // Notlar
  createdAt: string;
  updatedAt?: string;
}

// İşlem oluşturma isteği
export interface TransactionRequest {
  vehicleId: number;
  customerId: number;
  transactionType: TransactionType;
  startDate: string;
  endDate?: string;
  notes?: string;
}

// Sayfalı liste yanıtı
export interface PagedTransactionResponse {
  content: Transaction[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
