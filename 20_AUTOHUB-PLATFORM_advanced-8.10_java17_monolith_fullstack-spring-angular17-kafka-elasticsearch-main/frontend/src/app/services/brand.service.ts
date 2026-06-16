// Marka servisi - API iletişimi

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Brand, CarModel, BrandRequest, CarModelRequest } from '../models/brand.model';

@Injectable({
  providedIn: 'root'
})
export class BrandService {

  // API temel yolu
  private readonly brandUrl = '/api/brands';
  private readonly modelUrl = '/api/models';

  constructor(private http: HttpClient) {}

  // Tüm markaları getirir (sayfalı)
  getBrands(page = 0, size = 20): Observable<any> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size);
    return this.http.get<any>(this.brandUrl, { params });
  }

  // ID ile marka getirir
  getBrandById(id: number): Observable<Brand> {
    return this.http.get<Brand>(`${this.brandUrl}/${id}`);
  }

  // Yeni marka oluşturur
  createBrand(request: BrandRequest): Observable<Brand> {
    return this.http.post<Brand>(this.brandUrl, request);
  }

  // Marka günceller
  updateBrand(id: number, request: BrandRequest): Observable<Brand> {
    return this.http.put<Brand>(`${this.brandUrl}/${id}`, request);
  }

  // Marka siler
  deleteBrand(id: number): Observable<void> {
    return this.http.delete<void>(`${this.brandUrl}/${id}`);
  }

  // Markaya ait modelleri getirir
  getBrandModels(brandId: number): Observable<CarModel[]> {
    return this.http.get<CarModel[]>(`${this.brandUrl}/${brandId}/models`);
  }

  // Tüm modelleri getirir
  getModels(): Observable<CarModel[]> {
    return this.http.get<CarModel[]>(this.modelUrl);
  }

  // Model oluşturur
  createModel(request: CarModelRequest): Observable<CarModel> {
    return this.http.post<CarModel>(this.modelUrl, request);
  }

  // Model günceller
  updateModel(id: number, request: CarModelRequest): Observable<CarModel> {
    return this.http.put<CarModel>(`${this.modelUrl}/${id}`, request);
  }

  // Model siler
  deleteModel(id: number): Observable<void> {
    return this.http.delete<void>(`${this.modelUrl}/${id}`);
  }
}
