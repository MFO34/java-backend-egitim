// Angular importları
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';

// Proje içi importlar
import { VehicleService } from '../../../services/vehicle.service';
import { AuthService } from '../../../services/auth.service';
import { Vehicle, VehicleStatus, VehicleStatusLabels } from '../../../models/vehicle.model';

/**
 * Araç Detay Bileşeni
 *
 * Seçilen aracın tüm detaylarını gösterir.
 * Yetkili kullanıcılar için düzenleme ve durum değiştirme butonları sunar.
 */
@Component({
  selector: 'app-vehicle-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDialogModule
  ],
  templateUrl: './vehicle-detail.component.html',
  styleUrls: ['./vehicle-detail.component.scss']
})
export class VehicleDetailComponent implements OnInit {

  // Gösterilecek araç nesnesi
  vehicle: Vehicle | null = null;

  // Yükleme durumu
  isLoading = false;

  // Durum etiketleri Türkçe
  statusLabels = VehicleStatusLabels;

  // VehicleStatus enum'una template'den erişim için
  VehicleStatus = VehicleStatus;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private vehicleService: VehicleService,
    public authService: AuthService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    // URL'den araç ID'sini oku ve aracı yükle
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadVehicle(+id);
    }
  }

  /**
   * API'den araç detaylarını yükler
   */
  loadVehicle(id: number): void {
    this.isLoading = true;
    this.vehicleService.getVehicleById(id).subscribe({
      next: (vehicle) => {
        this.vehicle = vehicle;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.snackBar.open('Araç bilgileri yüklenemedi', 'Kapat', { duration: 3000 });
        this.router.navigate(['/vehicles']);
      }
    });
  }

  /**
   * Araç düzenleme sayfasına yönlendirir
   */
  editVehicle(): void {
    this.router.navigate(['/vehicles', this.vehicle!.id, 'edit']);
  }

  /**
   * Aracı siler ve listeye döner
   */
  deleteVehicle(): void {
    if (!this.vehicle) return;
    if (confirm(`"${this.vehicle.licensePlate}" plakalı aracı silmek istediğinizden emin misiniz?`)) {
      this.vehicleService.deleteVehicle(this.vehicle.id).subscribe({
        next: () => {
          this.snackBar.open('Araç silindi', 'Tamam', { duration: 3000 });
          this.router.navigate(['/vehicles']);
        },
        error: (err) => {
          const msg = err.error?.message || 'Araç silinirken hata oluştu';
          this.snackBar.open(msg, 'Kapat', { duration: 5000 });
        }
      });
    }
  }

  /**
   * Araç durumunu değiştirir
   */
  changeStatus(newStatus: VehicleStatus): void {
    if (!this.vehicle) return;
    this.vehicleService.updateVehicleStatus(this.vehicle.id, newStatus).subscribe({
      next: () => {
        this.vehicle!.status = newStatus;
        this.snackBar.open('Araç durumu güncellendi', 'Tamam', { duration: 3000 });
      },
      error: () => {
        this.snackBar.open('Durum güncellenemedi', 'Kapat', { duration: 3000 });
      }
    });
  }

  /**
   * Araç listesine geri döner
   */
  goBack(): void {
    this.router.navigate(['/vehicles']);
  }

  /**
   * Duruma göre CSS sınıfı döndürür
   */
  getStatusClass(status: VehicleStatus): string {
    const map: Record<VehicleStatus, string> = {
      [VehicleStatus.AVAILABLE]: 'status-available',
      [VehicleStatus.RENTED]: 'status-rented',
      [VehicleStatus.SOLD]: 'status-sold',
      [VehicleStatus.MAINTENANCE]: 'status-maintenance',
      [VehicleStatus.RESERVED]: 'status-reserved',
      [VehicleStatus.DAMAGED]: 'status-damaged'
    };
    return map[status] || '';
  }
}
