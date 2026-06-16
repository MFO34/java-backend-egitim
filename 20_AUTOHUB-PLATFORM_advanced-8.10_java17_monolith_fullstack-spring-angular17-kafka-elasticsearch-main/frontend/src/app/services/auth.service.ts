// Angular ve RxJS importları
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap, catchError, throwError } from 'rxjs';

// Proje içi importlar
import { environment } from '../../environments/environment';
import { AuthResponse, CurrentUser, LoginRequest, RegisterRequest } from '../models/auth.model';

/**
 * Kimlik Doğrulama Servisi
 *
 * JWT token yönetimi, giriş/çıkış işlemleri ve kullanıcı oturum durumunu yönetir.
 * Reaktif programlama (RxJS BehaviorSubject) ile oturum durumu takip edilir.
 *
 * @Injectable: Dependency injection ile diğer bileşenlere enjekte edilebilir
 * providedIn: 'root' - Uygulama genelinde tek instance (Singleton pattern)
 */
@Injectable({
  providedIn: 'root'    // Root injector'a kayıt - uygulama genelinde paylaşılır
})
export class AuthService {

  // Backend API temel URL'i
  private apiUrl = `${environment.apiUrl}/auth`;

  /**
   * Mevcut kullanıcı durumu için reaktif değişken
   * BehaviorSubject: Son değeri saklar, yeni abone son değeri alır
   * null = oturum yok, CurrentUser = oturum açık
   */
  private currentUserSubject = new BehaviorSubject<CurrentUser | null>(
    this.getUserFromStorage()   // Sayfa yenilenince localStorage'dan yükle
  );

  /**
   * Dışarıya salt okunur Observable olarak sunulan kullanıcı durumu
   * Bileşenler bu Observable'a abone olarak kullanıcı değişikliklerini takip eder
   */
  currentUser$ = this.currentUserSubject.asObservable();

  /**
   * Constructor - Angular DI ile bağımlılıklar enjekte edilir
   */
  constructor(
    private http: HttpClient,    // HTTP istekleri için
    private router: Router       // Yönlendirme için
  ) {}

  /**
   * Kullanıcı girişi
   * Başarılı girişte token'ları saklar ve kullanıcı durumunu günceller
   *
   * @param credentials Kullanıcı adı ve şifre
   * @returns AuthResponse Observable
   */
  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, credentials).pipe(
      // tap: Yan etki - Observable akışını değiştirmeden işlem yapar
      tap(response => {
        // Token'ları güvenli şekilde sakla
        this.storeTokens(response);
        // Kullanıcı bilgilerini güncelle
        this.setCurrentUser(response);
      }),
      catchError(error => {
        // Hata durumunda hata mesajını ilet
        console.error('Giriş hatası:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Kullanıcı kaydı
   *
   * @param data Kayıt bilgileri
   * @returns AuthResponse Observable
   */
  register(data: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, data).pipe(
      tap(response => {
        // Kayıt başarılı - otomatik giriş yap
        this.storeTokens(response);
        this.setCurrentUser(response);
      }),
      catchError(error => {
        return throwError(() => error);
      })
    );
  }

  /**
   * Kullanıcı çıkışı
   * Token'ları temizler ve login sayfasına yönlendirir
   */
  logout(): void {
    // Backend'e çıkış bildirimi gönder (opsiyonel)
    this.http.post(`${this.apiUrl}/logout`, {}).subscribe({
      error: () => {}  // Hata olsa bile çıkış yap
    });

    // Local token ve kullanıcı bilgilerini temizle
    this.clearStorage();

    // Kullanıcı durumunu null yap - oturum yok
    this.currentUserSubject.next(null);

    // Login sayfasına yönlendir
    this.router.navigate(['/auth/login']);
  }

  /**
   * Access token yenileme
   * Token süresi dolmadan önce çağrılmalı
   *
   * @returns Yeni AuthResponse Observable
   */
  refreshToken(): Observable<AuthResponse> {
    const refreshToken = this.getRefreshToken();

    if (!refreshToken) {
      // Refresh token yoksa çıkış yap
      this.logout();
      return throwError(() => new Error('Refresh token bulunamadı'));
    }

    return this.http.post<AuthResponse>(`${this.apiUrl}/refresh`, {}, {
      headers: { 'X-Refresh-Token': refreshToken }
    }).pipe(
      tap(response => {
        // Yeni token'ları sakla
        this.storeTokens(response);
      }),
      catchError(error => {
        // Refresh token geçersizse çıkış yap
        this.logout();
        return throwError(() => error);
      })
    );
  }

  /**
   * Kullanıcı giriş yapmış mı kontrol eder
   *
   * @returns Giriş yapılmışsa true
   */
  isLoggedIn(): boolean {
    return !!this.getAccessToken() && !!this.currentUserSubject.value;
  }

  /**
   * Kullanıcının belirtilen role sahip olup olmadığını kontrol eder
   *
   * @param role Kontrol edilecek rol (ADMIN, MANAGER vs.)
   * @returns Role sahipse true
   */
  hasRole(role: string): boolean {
    const user = this.currentUserSubject.value;
    if (!user) return false;
    // "ROLE_" prefix'i ile kontrol (Spring Security formatı)
    return user.roles.includes(`ROLE_${role}`) || user.roles.includes(role);
  }

  /**
   * Admin yetkisi var mı?
   */
  isAdmin(): boolean {
    return this.hasRole('ADMIN');
  }

  /**
   * Yönetici yetkisi var mı? (Admin veya Manager)
   */
  isManager(): boolean {
    return this.hasRole('ADMIN') || this.hasRole('MANAGER');
  }

  /**
   * Çalışan yetkisi var mı? (Admin, Manager veya Employee)
   */
  isEmployee(): boolean {
    return this.hasRole('ADMIN') || this.hasRole('MANAGER') || this.hasRole('EMPLOYEE');
  }

  /**
   * Access token döndürür
   */
  getAccessToken(): string | null {
    return localStorage.getItem(environment.tokenKey);
  }

  /**
   * Refresh token döndürür
   */
  getRefreshToken(): string | null {
    return localStorage.getItem(environment.refreshTokenKey);
  }

  /**
   * Mevcut kullanıcıyı döndürür (snapshot - anlık değer)
   */
  getCurrentUser(): CurrentUser | null {
    return this.currentUserSubject.value;
  }

  // ============================================
  // Private Yardımcı Metodlar
  // ============================================

  /**
   * Token'ları localStorage'a güvenli şekilde saklar
   * localStorage: Tarayıcı kapatılsa bile devam eder
   * sessionStorage: Sekme kapanınca sona erer
   */
  private storeTokens(response: AuthResponse): void {
    localStorage.setItem(environment.tokenKey, response.accessToken);
    localStorage.setItem(environment.refreshTokenKey, response.refreshToken);
  }

  /**
   * Kullanıcı bilgilerini BehaviorSubject'e ve localStorage'a kaydeder
   */
  private setCurrentUser(response: AuthResponse): void {
    const user: CurrentUser = {
      username: response.username,
      email: response.email,
      fullName: response.fullName,
      roles: response.roles,
      isAdmin: response.roles.some(r => r.includes('ADMIN')),
      isManager: response.roles.some(r => r.includes('MANAGER') || r.includes('ADMIN')),
      isEmployee: response.roles.some(r => r.includes('EMPLOYEE'))
    };

    // Kullanıcı bilgilerini sakla
    localStorage.setItem(environment.userKey, JSON.stringify(user));
    // BehaviorSubject'i güncelle - abone bileşenlere yeni değer iletilir
    this.currentUserSubject.next(user);
  }

  /**
   * localStorage'dan kullanıcı bilgisini yükler
   * Sayfa yenilenince kullanıcı oturumunu korumak için
   */
  private getUserFromStorage(): CurrentUser | null {
    try {
      const userStr = localStorage.getItem(environment.userKey);
      return userStr ? JSON.parse(userStr) : null;
    } catch {
      // JSON parse hatası - geçersiz veri, null döndür
      return null;
    }
  }

  /**
   * Tüm kimlik doğrulama verilerini temizler
   */
  private clearStorage(): void {
    localStorage.removeItem(environment.tokenKey);
    localStorage.removeItem(environment.refreshTokenKey);
    localStorage.removeItem(environment.userKey);
  }
}
