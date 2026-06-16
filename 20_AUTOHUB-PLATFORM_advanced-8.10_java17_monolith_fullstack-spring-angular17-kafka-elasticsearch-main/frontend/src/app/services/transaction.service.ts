// İşlem servisi - API iletişimi

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Transaction,
  TransactionRequest,
  TransactionStatus,
  PagedTransactionResponse
} from '../models/transaction.model';

@Injectable({
  providedIn: 'root'
})
export class TransactionService {

  // API temel yolu
  private readonly apiUrl = '/api/transactions';

  constructor(private http: HttpClient) {}

  // İşlem listesini sayfalı olarak getirir
  getTransactions(page = 0, size = 20, status?: TransactionStatus): Observable<PagedTransactionResponse> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size);
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<PagedTransactionResponse>(this.apiUrl, { params });
  }

  // ID ile işlem getirir
  getTransactionById(id: number): Observable<Transaction> {
    return this.http.get<Transaction>(`${this.apiUrl}/${id}`);
  }

  // Yeni işlem oluşturur (kiralama veya satış)
  createTransaction(request: TransactionRequest): Observable<Transaction> {
    return this.http.post<Transaction>(this.apiUrl, request);
  }

  // İşlemi tamamlar
  completeTransaction(id: number): Observable<Transaction> {
    return this.http.patch<Transaction>(`${this.apiUrl}/${id}/complete`, {});
  }

  // İşlemi iptal eder
  cancelTransaction(id: number, reason?: string): Observable<Transaction> {
    return this.http.patch<Transaction>(`${this.apiUrl}/${id}/cancel`, { reason });
  }

  // Müşteriye ait işlemleri getirir
  getCustomerTransactions(customerId: number): Observable<Transaction[]> {
    return this.http.get<Transaction[]>(`${this.apiUrl}/customer/${customerId}`);
  }

  // Araca ait işlemleri getirir
  getVehicleTransactions(vehicleId: number): Observable<Transaction[]> {
    return this.http.get<Transaction[]>(`${this.apiUrl}/vehicle/${vehicleId}`);
  }
}
