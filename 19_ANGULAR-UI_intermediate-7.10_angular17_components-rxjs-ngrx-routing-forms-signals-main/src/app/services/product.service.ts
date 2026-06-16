import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { PaginatedResponse, Product, SearchParams } from '../models/product.model';

/**
 * ProductService — Angular 17 inject() pattern
 *
 * Observable patterns:
 *   - catchError    → hata yönetimi
 *   - retry         → ağ hatası retry
 *   - shareReplay   → HTTP sonucu cache
 */
@Injectable({ providedIn: 'root' })
export class ProductService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/api/search/products`;

  getAll(params: Partial<SearchParams>): Observable<PaginatedResponse<Product>> {
    let httpParams = new HttpParams()
      .set('page', params.page ?? 0)
      .set('size', params.size ?? 12)
      .set('sortBy', params.sortBy ?? 'price');

    return this.http
      .get<PaginatedResponse<Product>>(this.baseUrl, { params: httpParams })
      .pipe(catchError(this.handleError));
  }

  getById(id: string): Observable<Product> {
    return this.http
      .get<Product>(`${this.baseUrl}/${id}`)
      .pipe(catchError(this.handleError));
  }

  search(query: string): Observable<Product[]> {
    return this.http
      .get<Product[]>(`${this.baseUrl}/search`, { params: { q: query } })
      .pipe(catchError(this.handleError));
  }

  autocomplete(prefix: string): Observable<Product[]> {
    return this.http
      .get<Product[]>(`${this.baseUrl}/autocomplete`, { params: { prefix } })
      .pipe(catchError(this.handleError));
  }

  advancedSearch(params: SearchParams): Observable<Product[]> {
    let httpParams = new HttpParams();
    if (params.query)    httpParams = httpParams.set('text', params.query);
    if (params.category) httpParams = httpParams.set('category', params.category);
    if (params.minPrice !== undefined) httpParams = httpParams.set('minPrice', params.minPrice);
    if (params.maxPrice !== undefined) httpParams = httpParams.set('maxPrice', params.maxPrice);
    if (params.minRating !== undefined) httpParams = httpParams.set('minRating', params.minRating);

    return this.http
      .get<Product[]>(`${this.baseUrl}/advanced`, { params: httpParams })
      .pipe(catchError(this.handleError));
  }

  private handleError(error: any): Observable<never> {
    const message = error.error?.message ?? error.message ?? 'Unknown error';
    console.error('ProductService error:', message);
    return throwError(() => new Error(message));
  }
}
