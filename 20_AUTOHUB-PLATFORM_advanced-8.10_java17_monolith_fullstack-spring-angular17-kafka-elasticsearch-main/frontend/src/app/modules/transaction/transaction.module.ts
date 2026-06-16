// İşlem Modülü - Kiralama ve satış işlemlerini yönetir

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';

/**
 * İşlem Modülü
 *
 * Kiralama ve satış işlemleri için liste, detay ve form bileşenlerini
 * lazy loading ile yükler.
 */
const transactionRoutes: Routes = [
  // İşlem listesi - varsayılan
  {
    path: '',
    loadComponent: () =>
      import('./transaction-list/transaction-list.component').then(c => c.TransactionListComponent)
  },
  // Yeni işlem oluşturma
  {
    path: 'new',
    loadComponent: () =>
      import('./transaction-form/transaction-form.component').then(c => c.TransactionFormComponent)
  },
  // İşlem detayı
  {
    path: ':id',
    loadComponent: () =>
      import('./transaction-detail/transaction-detail.component').then(c => c.TransactionDetailComponent)
  }
];

@NgModule({
  imports: [
    CommonModule,
    RouterModule.forChild(transactionRoutes)
  ]
})
export class TransactionModule {}
