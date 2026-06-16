// Angular ve RxJS importları
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

// Proje içi importlar
import { environment } from '../../environments/environment';
import {
  Vehicle, CreateVehicleRequest, UpdateVehicleRequest,
  PageResponse, VehicleStatus, VehicleStats
} from '../models/vehicle.model';

/**
 * Araç Servisi
 *
 * Backend araç API'si ile iletişimi sağlar.
 * HttpClient ile REST API çağrıları yapılır.
 *
 * OOP Prensibi - SORUMLULUK AYIRIMI:
 * HTTP istekleri bu serviste kapsüllenmiş, bileşenler doğrudan HTTP çağrısı yapmaz.
 */
@Injectable({
  providedIn: 'root'
})
export class VehicleService {

  // Araç API endpoint'inin temel URL'i
  private apiUrl = `${environment.apiUrl}/vehicles`;

  constructor(private http: HttpClient) {}

  /**
   * Tüm araçları sayfalı olarak getirir
   *
   * @param page Sayfa numarası (0-indexed)
   * @param size Sayfa başına kayıt sayısı
   * @param sort Sıralama alanı ve yönü (örn: "price,asc")
   * @returns Sayfalı araç listesi Observable
   */
  getVehicles(page = 0, size = 10, sort = 'createdAt,desc'): Observable<PageResponse<Vehicle>> {
    // URL query parametrelerini oluştur
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    return this.http.get<PageResponse<Vehicle>>(this.apiUrl, { params });
  }

  /**
   * ID ile araç getirir
   *
   * @param id Araç ID'si
   * @returns Araç Observable
   */
  getVehicleById(id: number): Observable<Vehicle> {
    return this.http.get<Vehicle>(`${this.apiUrl}/${id}`);
  }

  /**
   * Plakaya göre araç getirir
   *
   * @param licensePlate Araç plakası
   */
  getVehicleByPlate(licensePlate: string): Observable<Vehicle> {
    return this.http.get<Vehicle>(`${this.apiUrl}/plate/${licensePlate}`);
  }

  /**
   * Yeni araç oluşturur
   *
   * @param data Araç oluşturma verileri
   * @returns Oluşturulan araç Observable
   */
  createVehicle(data: CreateVehicleRequest): Observable<Vehicle> {
    return this.http.post<Vehicle>(this.apiUrl, data);
  }

  /**
   * Mevcut aracı günceller
   *
   * @param id Güncellenecek araç ID'si
   * @param data Güncelleme verileri
   */
  updateVehicle(id: number, data: UpdateVehicleRequest): Observable<Vehicle> {
    return this.http.put<Vehicle>(`${this.apiUrl}/${id}`, data);
  }

  /**
   * Aracı siler (soft delete)
   *
   * @param id Silinecek araç ID'si
   */
  deleteVehicle(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * Araç durumunu günceller
   *
   * @param id Araç ID'si
   * @param status Yeni durum
   */
  updateVehicleStatus(id: number, status: VehicleStatus): Observable<void> {
    const params = new HttpParams().set('status', status);
    return this.http.patch<void>(`${this.apiUrl}/${id}/status`, {}, { params });
  }

  /**
   * Duruma göre araçları filtreler
   *
   * @param status Araç durumu
   * @param page Sayfa numarası
   * @param size Sayfa boyutu
   */
  getVehiclesByStatus(status: VehicleStatus, page = 0, size = 10): Observable<PageResponse<Vehicle>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<Vehicle>>(`${this.apiUrl}/status/${status}`, { params });
  }

  /**
   * Marka adına göre müsait araçları getirir
   *
   * @param brandName Marka adı
   * @param page Sayfa numarası
   */
  getAvailableByBrand(brandName: string, page = 0): Observable<PageResponse<Vehicle>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', '10');
    return this.http.get<PageResponse<Vehicle>>(
      `${this.apiUrl}/brand/${brandName}/available`, { params });
  }

  /**
   * Fiyat aralığına göre araçları filtreler
   *
   * @param minPrice Minimum fiyat
   * @param maxPrice Maksimum fiyat
   * @param page Sayfa numarası
   */
  getVehiclesByPriceRange(minPrice: number, maxPrice: number, page = 0): Observable<PageResponse<Vehicle>> {
    const params = new HttpParams()
      .set('min', minPrice.toString())
      .set('max', maxPrice.toString())
      .set('page', page.toString());
    return this.http.get<PageResponse<Vehicle>>(`${this.apiUrl}/price-range`, { params });
  }

  /**
   * Araç arama - Elasticsearch full-text arama
   *
   * @param query Arama sorgusu
   * @param page Sayfa numarası
   */
  searchVehicles(query: string, page = 0): Observable<PageResponse<Vehicle>> {
    const params = new HttpParams()
      .set('q', query)
      .set('page', page.toString());
    return this.http.get<PageResponse<Vehicle>>(`${this.apiUrl}/search`, { params });
  }

  /**
   * Son eklenen araçları getirir
   *
   * @param limit Listelenecek araç sayısı
   */
  getLatestVehicles(limit = 5): Observable<Vehicle[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<Vehicle[]>(`${this.apiUrl}/latest`, { params });
  }

  /**
   * Araç durumu istatistiklerini getirir - dashboard için
   */
  getVehicleStats(): Observable<VehicleStats> {
    return this.http.get<VehicleStats>(`${this.apiUrl}/stats`);
  }
}
