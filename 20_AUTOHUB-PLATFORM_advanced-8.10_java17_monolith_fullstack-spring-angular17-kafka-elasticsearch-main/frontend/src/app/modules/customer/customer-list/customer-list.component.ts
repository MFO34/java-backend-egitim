// Müşteri Listesi Bileşeni - tüm müşterileri listeler ve yönetir

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
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';

import { CustomerService } from '../../../services/customer.service';
import { AuthService } from '../../../services/auth.service';
import { Customer } from '../../../models/customer.model';

/**
 * Müşteri Listesi Bileşeni
 *
 * Sistemdeki tüm müşterileri sayfalı tablo olarak listeler.
 * Ad/soyad veya TC kimlik numarası ile arama yapılabilir.
 */
@Component({
  selector: 'app-customer-list',
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
    MatFormFieldModule,
    MatInputModule,
    MatPaginatorModule
  ],
  templateUrl: './customer-list.component.html',
  styleUrls: ['./customer-list.component.scss']
})
export class CustomerListComponent implements OnInit {

  // Müşteri listesi
  customers: Customer[] = [];

  // Yükleme durumu
  isLoading = false;

  // Arama terimi
  searchTerm = '';

  // Sayfalama değişkenleri
  totalElements = 0;
  pageSize = 20;
  currentPage = 0;

  // Tabloda gösterilecek sütunlar
  displayedColumns: string[] = ['fullName', 'tcNumber', 'phone', 'email', 'city', 'actions'];

  constructor(
    private customerService: CustomerService,
    private router: Router,
    private snackBar: MatSnackBar,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadCustomers();
  }

  // Müşterileri API'den yükler
  loadCustomers(): void {
    this.isLoading = true;
    this.customerService.getCustomers(this.currentPage, this.pageSize, this.searchTerm || undefined).subscribe({
      next: (response) => {
        this.customers = response.content || [];
        this.totalElements = response.totalElements || 0;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.snackBar.open('Müşteriler yüklenemedi', 'Kapat', { duration: 3000 });
      }
    });
  }

  // Arama yapar
  onSearch(): void {
    this.currentPage = 0;
    this.loadCustomers();
  }

  // Arama temizler
  clearSearch(): void {
    this.searchTerm = '';
    this.currentPage = 0;
    this.loadCustomers();
  }

  // Sayfa değiştiğinde tetiklenir
  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadCustomers();
  }

  // Yeni müşteri ekleme sayfasına gider
  addCustomer(): void {
    this.router.navigate(['/customers/new']);
  }

  // Müşteri detay sayfasına gider
  viewCustomer(customer: Customer): void {
    this.router.navigate(['/customers', customer.id]);
  }

  // Müşteri düzenleme sayfasına gider
  editCustomer(customer: Customer): void {
    this.router.navigate(['/customers', customer.id, 'edit']);
  }

  // Müşteri siler
  deleteCustomer(customer: Customer): void {
    if (confirm(`"${customer.firstName} ${customer.lastName}" adlı müşteriyi silmek istediğinizden emin misiniz?`)) {
      this.customerService.deleteCustomer(customer.id).subscribe({
        next: () => {
          this.snackBar.open('Müşteri silindi', 'Tamam', { duration: 3000 });
          this.loadCustomers();
        },
        error: (err) => {
          const msg = err.error?.message || 'Müşteri silinirken hata oluştu';
          this.snackBar.open(msg, 'Kapat', { duration: 5000 });
        }
      });
    }
  }
}
