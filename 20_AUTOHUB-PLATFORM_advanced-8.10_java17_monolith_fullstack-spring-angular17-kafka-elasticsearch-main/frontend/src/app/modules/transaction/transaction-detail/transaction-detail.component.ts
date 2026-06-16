// İşlem Detay Bileşeni - kiralama veya satış işleminin tüm bilgilerini gösterir

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialogModule } from '@angular/material/dialog';

import { TransactionService } from '../../../services/transaction.service';
import { AuthService } from '../../../services/auth.service';
import {
  Transaction,
  TransactionStatus,
  TransactionStatusLabels,
  TransactionTypeLabels
} from '../../../models/transaction.model';

/**
 * İşlem Detay Bileşeni
 *
 * Tek bir işlemin tüm detaylarını gösterir: araç, müşteri, tarihler ve fiyat dökümü.
 * Yetkili kullanıcılar işlemi tamamlayabilir veya iptal edebilir.
 */
@Component({
  selector: 'app-transaction-detail',
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
    MatDialogModule
  ],
  templateUrl: './transaction-detail.component.html',
  styleUrls: ['./transaction-detail.component.scss']
})
export class TransactionDetailComponent implements OnInit {

  // Gösterilecek işlem
  transaction: Transaction | null = null;

  // Yükleme durumu
  isLoading = false;

  // İşlem yapılıyor mu? (tamamla/iptal)
  isProcessing = false;

  // Etiketler
  statusLabels = TransactionStatusLabels;
  typeLabels = TransactionTypeLabels;
  TransactionStatus = TransactionStatus;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private transactionService: TransactionService,
    private snackBar: MatSnackBar,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadTransaction(+id);
    }
  }

  // İşlemi API'den yükler
  loadTransaction(id: number): void {
    this.isLoading = true;
    this.transactionService.getTransactionById(id).subscribe({
      next: (transaction) => {
        this.transaction = transaction;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.snackBar.open('İşlem bilgileri yüklenemedi', 'Kapat', { duration: 3000 });
        this.router.navigate(['/transactions']);
      }
    });
  }

  // İşlemi tamamlar
  completeTransaction(): void {
    if (!this.transaction) return;
    if (confirm('Bu işlemi tamamlamak istediğinizden emin misiniz?')) {
      this.isProcessing = true;
      this.transactionService.completeTransaction(this.transaction.id).subscribe({
        next: (updated) => {
          this.transaction = updated;
          this.isProcessing = false;
          this.snackBar.open('İşlem tamamlandı', 'Tamam', { duration: 3000 });
        },
        error: () => {
          this.isProcessing = false;
          this.snackBar.open('İşlem tamamlanamadı', 'Kapat', { duration: 3000 });
        }
      });
    }
  }

  // İşlemi iptal eder
  cancelTransaction(): void {
    if (!this.transaction) return;
    const reason = prompt('İptal nedenini giriniz (opsiyonel):');
    if (reason === null) return; // Kullanıcı iptal etti
    this.isProcessing = true;
    this.transactionService.cancelTransaction(this.transaction.id, reason || undefined).subscribe({
      next: (updated) => {
        this.transaction = updated;
        this.isProcessing = false;
        this.snackBar.open('İşlem iptal edildi', 'Tamam', { duration: 3000 });
      },
      error: () => {
        this.isProcessing = false;
        this.snackBar.open('İşlem iptal edilemedi', 'Kapat', { duration: 3000 });
      }
    });
  }

  // Listeye geri döner
  goBack(): void {
    this.router.navigate(['/transactions']);
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

  // İşlem düzenlenebilir mi? (Sadece PENDING veya ACTIVE olanlar)
  get canModify(): boolean {
    return this.transaction?.status === TransactionStatus.PENDING
      || this.transaction?.status === TransactionStatus.ACTIVE;
  }
}
