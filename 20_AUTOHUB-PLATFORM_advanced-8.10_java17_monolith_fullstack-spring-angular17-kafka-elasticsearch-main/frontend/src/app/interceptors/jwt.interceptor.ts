// Angular importları
import { Injectable } from '@angular/core';
import {
  HttpRequest, HttpHandler, HttpEvent, HttpInterceptor,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError, BehaviorSubject } from 'rxjs';
import { catchError, filter, take, switchMap } from 'rxjs/operators';

// Proje içi importlar
import { AuthService } from '../services/auth.service';

/**
 * JWT HTTP İnterceptor
 *
 * Her HTTP isteğine JWT token'ı otomatik ekler.
 * Token yenileme mekanizmasını yönetir.
 *
 * OOP Prensibi - DEKORATÖR (Decorator Pattern):
 * Mevcut HTTP isteklerini sararak (wrapping) JWT bilgisini ekler.
 *
 * OOP Prensibi - SORUMLULUK AYIRIMI:
 * Token yönetimi bu interceptor'da kapsüllenmiş,
 * servisler ve bileşenler token ile uğraşmak zorunda değil.
 */
@Injectable()
export class JwtInterceptor implements HttpInterceptor {

  // Token yenileme işlemi devam ediyor mu?
  private isRefreshing = false;

  // Token yenileme tamamlanana kadar bekleyen istekler için
  // BehaviorSubject: Tüm bekleyen istekler yeni token'ı alacak
  private refreshTokenSubject = new BehaviorSubject<string | null>(null);

  constructor(private authService: AuthService) {}

  /**
   * Her HTTP isteğine JWT token ekler
   *
   * @param request Orijinal HTTP isteği
   * @param next Zincirdeki sonraki handler
   * @returns Modifiye edilmiş HTTP isteği Observable
   */
  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    // Auth endpoint'lerine token eklemiyoruz (login, register, refresh)
    if (this.isAuthRequest(request)) {
      return next.handle(request);
    }

    // Access token varsa isteğe ekle
    const token = this.authService.getAccessToken();
    if (token) {
      request = this.addTokenToRequest(request, token);
    }

    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        // 401 Unauthorized - token süresi dolmuş olabilir
        if (error.status === 401) {
          return this.handle401Error(request, next);
        }
        // Diğer hatalar olduğu gibi ilet
        return throwError(() => error);
      })
    );
  }

  /**
   * 401 hatasını yönetir - token yenileme mekanizması
   * Eş zamanlı çoklu 401 hatası için Race Condition önleme
   *
   * @param request Başarısız olan istek
   * @param next Handler
   */
  private handle401Error(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    if (!this.isRefreshing) {
      // Token yenileme başlatılıyor
      this.isRefreshing = true;
      this.refreshTokenSubject.next(null);  // Bekleyen istekleri kilitle

      return this.authService.refreshToken().pipe(
        switchMap(response => {
          // Token yenilendi - bekleyen isteklerin devam etmesini sağla
          this.isRefreshing = false;
          this.refreshTokenSubject.next(response.accessToken);
          // Orijinal isteği yeni token ile tekrar gönder
          return next.handle(this.addTokenToRequest(request, response.accessToken));
        }),
        catchError(error => {
          // Token yenileme başarısız - kullanıcıyı çıkış yaptır
          this.isRefreshing = false;
          this.authService.logout();
          return throwError(() => error);
        })
      );
    } else {
      // Token zaten yenileniyor - yeni token gelene kadar bekle
      return this.refreshTokenSubject.pipe(
        filter(token => token !== null),      // Null değerleri filtrele
        take(1),                               // Sadece bir değer al
        switchMap(token => {
          // Yeni token ile orijinal isteği tekrar gönder
          return next.handle(this.addTokenToRequest(request, token!));
        })
      );
    }
  }

  /**
   * HTTP isteğine Authorization header ekler
   *
   * @param request Orijinal istek
   * @param token JWT access token
   * @returns Token eklenmiş istek (immutable - yeni kopya oluşturulur)
   */
  private addTokenToRequest(request: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
    // HttpRequest immutable - clone ile yeni kopya oluştur
    return request.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`   // "Bearer " prefix standart JWT formatı
      }
    });
  }

  /**
   * İsteğin auth endpoint'i olup olmadığını kontrol eder
   * Auth endpoint'lerine token eklenmez
   */
  private isAuthRequest(request: HttpRequest<unknown>): boolean {
    return request.url.includes('/auth/login') ||
           request.url.includes('/auth/register') ||
           request.url.includes('/auth/refresh');
  }
}
