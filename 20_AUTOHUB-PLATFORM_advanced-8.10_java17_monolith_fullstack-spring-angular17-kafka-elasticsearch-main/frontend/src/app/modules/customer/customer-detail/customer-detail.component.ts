// Müşteri Detay Bileşeni - tek müşterinin tüm bilgilerini ve işlem geçmişini gösterir

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';

import { CustomerService } from '../../../services/customer.service';
import { TransactionService } from '../../../services/transaction.service';
import { AuthService } from '../../../services/auth.service';
import { Customer } from '../../../models/customer.model';
import { Transaction, TransactionStatusLabels, TransactionTypeLabels } from '../../../models/transaction.model';

/**
 * Müşteri Detay Bileşeni
 *
 * Müşterinin kişisel bilgilerini ve bu müşteriye ait tüm
 * kiralama/satış işlemlerinin listesini gösterir.
 */
@Component({
  selector: 'app-customer-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTableModule,
    MatChipsModule
  ],
  templateUrl: './customer-detail.component.html',
  styleUrls: ['./customer-detail.component.scss']
})
export class CustomerDetailComponent implements OnInit {

  // Gösterilecek müşteri
  customer: Customer | null = null;

  // Müşteriye ait işlemler
  transactions: Transaction[] = [];

  // Yükleme durumları
  isLoading = false;
  isLoadingTransactions = false;

  // İşlem durum etiketleri
  statusLabels = TransactionStatusLabels;
  typeLabels = TransactionTypeLabels;

  // İşlem tablosu sütunları
  transactionColumns = ['id', 'vehicleName', 'transactionType', 'startDate', 'totalAmount', 'status'];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private customerService: CustomerService,
    private transactionService: TransactionService,
    private snackBar: MatSnackBar,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadCustomer(+id);
      this.loadTransactions(+id);
    }
  }

  // Müşteriyi API'den yükler
  loadCustomer(id: number): void {
    this.isLoading = true;
    this.customerService.getCustomerById(id).subscribe({
      next: (customer) => {
        this.customer = customer;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.snackBar.open('Müşteri bilgileri yüklenemedi', 'Kapat', { duration: 3000 });
        this.router.navigate(['/customers']);
      }
    });
  }

  // Müşteriye ait işlemleri yükler
  loadTransactions(customerId: number): void {
    this.isLoadingTransactions = true;
    this.transactionService.getCustomerTransactions(customerId).subscribe({
      next: (transactions) => {
        this.transactions = transactions;
        this.isLoadingTransactions = false;
      },
      error: () => {
        this.isLoadingTransactions = false;
      }
    });
  }

  // Düzenleme sayfasına gider
  editCustomer(): void {
    this.router.navigate(['/customers', this.customer!.id, 'edit']);
  }

  // Müşteri siler
  deleteCustomer(): void {
    if (!this.customer) return;
    const name = `${this.customer.firstName} ${this.customer.lastName}`;
    if (confirm(`"${name}" adlı müşteriyi silmek istediğinizden emin misiniz?`)) {
      this.customerService.deleteCustomer(this.customer.id).subscribe({
        next: () => {
          this.snackBar.open('Müşteri silindi', 'Tamam', { duration: 3000 });
          this.router.navigate(['/customers']);
        },
        error: (err) => {
          const msg = err.error?.message || 'Müşteri silinirken hata oluştu';
          this.snackBar.open(msg, 'Kapat', { duration: 5000 });
        }
      });
    }
  }

  // Listeye geri döner
  goBack(): void {
    this.router.navigate(['/customers']);
  }

  // İşlem detayına gider
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
