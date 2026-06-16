// Müşteri Modülü - Müşteri yönetimi bileşenlerini içerir

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';

/**
 * Müşteri Modülü
 *
 * Müşteri listesi, detay ve form bileşenlerini lazy loading ile yükler.
 * Tüm bileşenler standalone olduğu için yalnızca router konfigürasyonu gerekir.
 */
const customerRoutes: Routes = [
  // Müşteri listesi - varsayılan
  {
    path: '',
    loadComponent: () =>
      import('./customer-list/customer-list.component').then(c => c.CustomerListComponent)
  },
  // Yeni müşteri oluşturma
  {
    path: 'new',
    loadComponent: () =>
      import('./customer-form/customer-form.component').then(c => c.CustomerFormComponent)
  },
  // Müşteri detayı
  {
    path: ':id',
    loadComponent: () =>
      import('./customer-detail/customer-detail.component').then(c => c.CustomerDetailComponent)
  },
  // Müşteri düzenleme
  {
    path: ':id/edit',
    loadComponent: () =>
      import('./customer-form/customer-form.component').then(c => c.CustomerFormComponent)
  }
];

@NgModule({
  imports: [
    CommonModule,
    RouterModule.forChild(customerRoutes)
  ]
})
export class CustomerModule {}
