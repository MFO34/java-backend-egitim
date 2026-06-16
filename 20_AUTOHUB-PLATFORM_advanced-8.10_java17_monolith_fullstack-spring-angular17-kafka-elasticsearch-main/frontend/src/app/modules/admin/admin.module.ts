// Admin Modülü - Sistem yönetimi ve admin paneli

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';

/**
 * Admin Modülü
 *
 * Sistem yönetici panelini ve ilgili bileşenleri lazy loading ile yükler.
 * Bu modüle yalnızca ADMIN rolüne sahip kullanıcılar erişebilir.
 * Erişim kontrolü app-routing.module.ts'de RoleGuard ile sağlanır.
 */
const adminRoutes: Routes = [
  // Admin dashboard - varsayılan
  {
    path: '',
    loadComponent: () =>
      import('./admin-dashboard/admin-dashboard.component').then(c => c.AdminDashboardComponent)
  }
];

@NgModule({
  imports: [
    CommonModule,
    RouterModule.forChild(adminRoutes)
  ]
})
export class AdminModule {}
