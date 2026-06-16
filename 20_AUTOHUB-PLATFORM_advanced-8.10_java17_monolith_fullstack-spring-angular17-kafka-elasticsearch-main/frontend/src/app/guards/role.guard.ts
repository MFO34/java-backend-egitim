// Angular importları
import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router } from '@angular/router';

// Proje içi importlar
import { AuthService } from '../services/auth.service';

/**
 * Rol Bazlı Erişim Kontrolü Guard'ı
 *
 * Route'lara erişimi kullanıcı rolüne göre kontrol eder.
 * Admin sayfalarına sadece admin kullanıcılar erişebilir.
 *
 * Kullanım örneği (routing module'de):
 * { path: 'admin', component: AdminComponent, canActivate: [RoleGuard], data: { roles: ['ADMIN'] } }
 */
@Injectable({
  providedIn: 'root'
})
export class RoleGuard implements CanActivate {

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  /**
   * Kullanıcının gerekli role sahip olup olmadığını kontrol eder
   *
   * @param route Route snapshot - data.roles alanında gerekli roller var
   */
  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    // Route'un data alanından gerekli rolleri al
    // Örnek: { path: 'admin', data: { roles: ['ADMIN', 'MANAGER'] } }
    const requiredRoles: string[] = route.data['roles'] || [];

    // Gerekli rol yoksa erişime izin ver
    if (requiredRoles.length === 0) {
      return true;
    }

    // Kullanıcı giriş yapmış mı?
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/auth/login']);
      return false;
    }

    // Kullanıcının gerekli rollerden birine sahip olup olmadığını kontrol et
    const hasRequiredRole = requiredRoles.some(role =>
      this.authService.hasRole(role)
    );

    if (!hasRequiredRole) {
      // Yetersiz yetki - erişim reddedildi sayfasına yönlendir
      this.router.navigate(['/forbidden']);
      return false;
    }

    return true;  // Yetki var - erişime izin ver
  }
}
