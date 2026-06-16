// Müşteri servisi - API iletişimi

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Customer, CustomerRequest, PagedCustomerResponse } from '../models/customer.model';

@Injectable({
  providedIn: 'root'
})
export class CustomerService {

  // API temel yolu
  private readonly apiUrl = '/api/customers';

  constructor(private http: HttpClient) {}

  // Müşteri listesini sayfalı olarak getirir
  getCustomers(page = 0, size = 20, search?: string): Observable<PagedCustomerResponse> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size);
    if (search) {
      params = params.set('search', search);
    }
    return this.http.get<PagedCustomerResponse>(this.apiUrl, { params });
  }

  // ID ile müşteri getirir
  getCustomerById(id: number): Observable<Customer> {
    return this.http.get<Customer>(`${this.apiUrl}/${id}`);
  }

  // Yeni müşteri oluşturur
  createCustomer(request: CustomerRequest): Observable<Customer> {
    return this.http.post<Customer>(this.apiUrl, request);
  }

  // Müşteri günceller
  updateCustomer(id: number, request: CustomerRequest): Observable<Customer> {
    return this.http.put<Customer>(`${this.apiUrl}/${id}`, request);
  }

  // Müşteri siler (soft delete)
  deleteCustomer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
