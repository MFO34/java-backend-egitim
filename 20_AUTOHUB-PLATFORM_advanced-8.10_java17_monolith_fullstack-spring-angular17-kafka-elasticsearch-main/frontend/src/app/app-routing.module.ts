// Angular Router importları
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

// Guard importları
import { AuthGuard } from './guards/auth.guard';
import { RoleGuard } from './guards/role.guard';

/**
 * Uygulama Yönlendirme Modülü
 *
 * Tüm URL route tanımlarını içerir.
 * Lazy loading ile performans optimizasyonu yapılır:
 * Modüller sadece ihtiyaç duyulduğunda yüklenir.
 *
 * Guard'lar ile korumalı route'lar tanımlanır.
 */
const routes: Routes = [
  // Varsayılan sayfa - dashboard'a yönlendir
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },

  // Kimlik Doğrulama Sayfaları - Giriş yapmak gerekmez (guard yok)
  {
    path: 'auth',
    loadChildren: () =>
      import('./modules/auth/auth.module').then(m => m.AuthModule)
  },

  // Dashboard - Giriş yapılmış olmalı
  {
    path: 'dashboard',
    loadChildren: () =>
      import('./modules/dashboard/dashboard.module').then(m => m.DashboardModule),
    canActivate: [AuthGuard]      // Sadece giriş yapanlar erişebilir
  },

  // Araç Yönetimi - Giriş yapılmış olmalı
  {
    path: 'vehicles',
    loadChildren: () =>
      import('./modules/vehicle/vehicle.module').then(m => m.VehicleModule),
    canActivate: [AuthGuard]
  },

  // Marka ve Model Yönetimi - Admin veya Manager
  {
    path: 'brands',
    loadChildren: () =>
      import('./modules/brand/brand.module').then(m => m.BrandModule),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ADMIN', 'MANAGER'] }  // Sadece admin ve manager
  },

  // Müşteri Yönetimi - Personel erişebilir
  {
    path: 'customers',
    loadChildren: () =>
      import('./modules/customer/customer.module').then(m => m.CustomerModule),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ADMIN', 'MANAGER', 'EMPLOYEE'] }
  },

  // İşlem Yönetimi (Kiralama/Satış) - Personel erişebilir
  {
    path: 'transactions',
    loadChildren: () =>
      import('./modules/transaction/transaction.module').then(m => m.TransactionModule),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ADMIN', 'MANAGER', 'EMPLOYEE'] }
  },

  // Admin Paneli - Sadece Admin
  {
    path: 'admin',
    loadChildren: () =>
      import('./modules/admin/admin.module').then(m => m.AdminModule),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ADMIN'] }
  },

  // Erişim Reddedildi sayfası
  {
    path: 'forbidden',
    loadComponent: () =>
      import('./components/shared/forbidden/forbidden.component')
        .then(c => c.ForbiddenComponent)
  },

  // 404 - Sayfa Bulunamadı (wildcard - en sona koyulmalı)
  {
    path: '**',
    loadComponent: () =>
      import('./components/shared/not-found/not-found.component')
        .then(c => c.NotFoundComponent)
  }
];

@NgModule({
  imports: [
    RouterModule.forRoot(routes, {
      // URL hash kullanma (#/path yerine /path)
      // enableTracing: true  // Geliştirme: Route olaylarını logla
    })
  ],
  exports: [RouterModule]
})
export class AppRoutingModule {}
