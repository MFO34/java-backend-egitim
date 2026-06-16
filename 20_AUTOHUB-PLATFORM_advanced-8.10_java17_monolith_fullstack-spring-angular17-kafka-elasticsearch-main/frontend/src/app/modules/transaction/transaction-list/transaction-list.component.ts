// İşlem Listesi Bileşeni - kiralama ve satış işlemlerini listeler

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatChipsModule } from '@angular/material/chips';
import { MatTabsModule } from '@angular/material/tabs';

import { TransactionService } from '../../../services/transaction.service';
import { AuthService } from '../../../services/auth.service';
import {
  Transaction,
  TransactionStatus,
  TransactionStatusLabels,
  TransactionTypeLabels
} from '../../../models/transaction.model';

/**
 * İşlem Listesi Bileşeni
 *
 * Kiralama ve satış işlemlerini sayfalı tablo olarak listeler.
 * Durum filtresi ile aktif, tamamlanan veya iptal edilen işlemler ayrıca görüntülenebilir.
 */
@Component({
  selector: 'app-transaction-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatSelectModule,
    MatFormFieldModule,
    MatPaginatorModule,
    MatChipsModule,
    MatTabsModule
  ],
  templateUrl: './transaction-list.component.html',
  styleUrls: ['./transaction-list.component.scss']
})
export class TransactionListComponent implements OnInit {

  // İşlem listesi
  transactions: Transaction[] = [];

  // Yükleme durumu
  isLoading = false;

  // Durum filtresi
  selectedStatus?: TransactionStatus;

  // Sayfalama
  totalElements = 0;
  pageSize = 20;
  currentPage = 0;

  // Tüm durum seçenekleri
  statusOptions = Object.values(TransactionStatus);

  // Etiketler
  statusLabels = TransactionStatusLabels;
  typeLabels = TransactionTypeLabels;

  // Tablo sütunları
  displayedColumns = ['id', 'customerName', 'vehicleName', 'transactionType', 'startDate', 'totalAmount', 'status', 'actions'];

  constructor(
    private transactionService: TransactionService,
    private router: Router,
    private snackBar: MatSnackBar,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadTransactions();
  }

  // İşlemleri API'den yükler
  loadTransactions(): void {
    this.isLoading = true;
    this.transactionService.getTransactions(this.currentPage, this.pageSize, this.selectedStatus).subscribe({
      next: (response) => {
        this.transactions = response.content || [];
        this.totalElements = response.totalElements || 0;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.snackBar.open('İşlemler yüklenemedi', 'Kapat', { duration: 3000 });
      }
    });
  }

  // Filtre değiştiğinde yeniden yükler
  onFilterChange(): void {
    this.currentPage = 0;
    this.loadTransactions();
  }

  // Sayfa değiştiğinde tetiklenir
  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadTransactions();
  }

  // Yeni işlem oluşturma sayfasına gider
  newTransaction(): void {
    this.router.navigate(['/transactions/new']);
  }

  // İşlem detay sayfasına gider
  viewTransaction(transaction: Transaction): void {
    this.router.navigate(['/transactions', transaction.id]);
  }

  // İşlem durum CSS sınıfı
  getStatusClass(status: string): string {
    const map: Record<string, string> = {
      'PENDING': 'status-pending',
      'ACTIVE': 'status-active',
      'COMPLETED': 'status-completed',
      'CANCELLED': 'status-cancelled'
    };
    return map[status] || '';
  }
}
