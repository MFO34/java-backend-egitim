// Marka Modülü - Marka ve Model yönetimi bileşenlerini içerir

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';

/**
 * Marka Modülü
 *
 * Marka listesi ve form bileşenlerini lazy loading ile yükler.
 * Tüm bileşenler standalone olduğu için yalnızca router konfigürasyonu gerekir.
 */
const brandRoutes: Routes = [
  // Marka listesi - varsayılan
  {
    path: '',
    loadComponent: () =>
      import('./brand-list/brand-list.component').then(c => c.BrandListComponent)
  },
  // Yeni marka oluşturma
  {
    path: 'new',
    loadComponent: () =>
      import('./brand-form/brand-form.component').then(c => c.BrandFormComponent)
  },
  // Marka düzenleme
  {
    path: ':id/edit',
    loadComponent: () =>
      import('./brand-form/brand-form.component').then(c => c.BrandFormComponent)
  }
];

@NgModule({
  imports: [
    CommonModule,
    RouterModule.forChild(brandRoutes)
  ]
})
export class BrandModule {}
