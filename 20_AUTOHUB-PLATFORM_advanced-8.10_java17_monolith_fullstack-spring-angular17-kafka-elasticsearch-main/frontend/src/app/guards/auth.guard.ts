// Angular importları
import { Injectable } from '@angular/core';
import { CanActivate, CanActivateChild, ActivatedRouteSnapshot, RouterStateSnapshot, Router } from '@angular/router';

// Proje içi importlar
import { AuthService } from '../services/auth.service';

/**
 * Kimlik Doğrulama Guard'ı
 *
 * Korumalı route'lara erişimi kontrol eder.
 * Giriş yapmamış kullanıcıları login sayfasına yönlendirir.
 *
 * OOP Prensibi - SORUMLULUK AYIRIMI:
 * Yetkilendirme kontrolü bu guard'da kapsüllenmiş.
 *
 * CanActivate: Route etkinleştirilmeden önce çalışır
 * CanActivateChild: Child route'lar için de çalışır
 */
@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate, CanActivateChild {

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  /**
   * Route'a erişim izni verir veya reddeder
   *
   * @param route Aktif route snapshot
   * @param state Router durumu (URL bilgisi içerir)
   * @returns true: erişim ver, false veya redirect: engelle
   */
  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    return this.checkAuth(state.url);
  }

  /**
   * Child route'lar için erişim kontrolü
   */
  canActivateChild(childRoute: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    return this.checkAuth(state.url);
  }

  /**
   * Kimlik doğrulama kontrolü yapan yardımcı metot
   *
   * @param url Erişilmek istenen URL
   * @returns Erişim izni varsa true
   */
  private checkAuth(url: string): boolean {
    // Kullanıcı giriş yapmış mı kontrol et
    if (this.authService.isLoggedIn()) {
      return true;  // Erişime izin ver
    }

    // Giriş yapılmamış - login sayfasına yönlendir
    // returnUrl: Giriş sonrası kullanıcının gitmek istediği sayfa
    this.router.navigate(['/auth/login'], {
      queryParams: { returnUrl: url }   // Giriş sonrası geri dön
    });

    return false;  // Erişimi engelle
  }
}
