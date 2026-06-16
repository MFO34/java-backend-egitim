// Angular importları
import { Component, OnInit, ViewChild } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { FormControl } from '@angular/forms';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

// Proje içi importlar
import { VehicleService } from '../../../services/vehicle.service';
import { AuthService } from '../../../services/auth.service';
import {
  Vehicle, VehicleStatus, VehicleStatusLabels,
  VehicleStatusColors, PageResponse
} from '../../../models/vehicle.model';

/**
 * Araç Listesi Bileşeni
 *
 * Tüm araçları tabloda listeler, arama, filtreleme ve sayfalama desteği sunar.
 *
 * Özellikler:
 * - Angular Material Table ile listeleme
 * - Gerçek zamanlı arama (debounce ile)
 * - Durum bazlı filtreleme
 * - Sayfalama
 * - Rol bazlı eylem butonları
 */
@Component({
  selector: 'app-vehicle-list',
  templateUrl: './vehicle-list.component.html',
  styleUrls: ['./vehicle-list.component.scss']
})
export class VehicleListComponent implements OnInit {

  // Tablo veri kaynağı - Material Table'ın kullandığı veri yapısı
  dataSource = new MatTableDataSource<Vehicle>([]);

  // Tabloda gösterilecek sütunlar
  displayedColumns = ['licensePlate', 'brand', 'model', 'year', 'color', 'price', 'status', 'actions'];

  // Sayfalama durumu
  totalElements = 0;
  pageSize = 10;
  currentPage = 0;

  // Yükleme durumu
  isLoading = false;

  // Arama formu kontrolü - debounce ile
  searchControl = new FormControl('');

  // Durum filtresi
  selectedStatus: VehicleStatus | '' = '';

  // Araç durumu seçenekleri - dropdown için
  vehicleStatuses = Object.values(VehicleStatus);
  statusLabels = VehicleStatusLabels;
  statusColors = VehicleStatusColors;

  // Angular Material referansları
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor(
    private vehicleService: VehicleService,
    public authService: AuthService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    // Araçları yükle
    this.loadVehicles();

    // Arama kutusunu dinle - 500ms bekleyerek arama yap
    this.searchControl.valueChanges.pipe(
      debounceTime(500),              // 500ms bekle - her tuş vuruşunda arama yapma
      distinctUntilChanged()           // Değişmemişse yeniden arama yapma
    ).subscribe(query => {
      if (query && query.length >= 2) {
        // En az 2 karakter girilmişse arama yap
        this.searchVehicles(query);
      } else if (!query) {
        // Arama kutusu temizlendiyse normal listeyi göster
        this.loadVehicles();
      }
    });
  }

  /**
   * Araçları yükler
   */
  loadVehicles(): void {
    this.isLoading = true;

    // Durum filtresi varsa filtreli getir, yoksa tümünü getir
    const request$ = this.selectedStatus
      ? this.vehicleService.getVehiclesByStatus(this.selectedStatus as VehicleStatus, this.currentPage, this.pageSize)
      : this.vehicleService.getVehicles(this.currentPage, this.pageSize);

    request$.subscribe({
      next: (page: PageResponse<Vehicle>) => {
        this.dataSource.data = page.content;
        this.totalElements = page.totalElements;
        this.isLoading = false;
      },
      error: (error) => {
        this.isLoading = false;
        this.snackBar.open('Araçlar yüklenirken hata oluştu', 'Kapat', { duration: 3000 });
        console.error('Araç yükleme hatası:', error);
      }
    });
  }

  /**
   * Full-text arama
   */
  searchVehicles(query: string): void {
    this.isLoading = true;
    this.vehicleService.searchVehicles(query, 0).subscribe({
      next: (page) => {
        this.dataSource.data = page.content;
        this.totalElements = page.totalElements;
        this.isLoading = false;
      },
      error: () => { this.isLoading = false; }
    });
  }

  /**
   * Sayfa değişikliğinde çağrılır
   */
  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadVehicles();
  }

  /**
   * Durum filtresi değiştiğinde
   */
  onStatusFilterChange(): void {
    this.currentPage = 0;
    this.loadVehicles();
  }

  /**
   * Araç detayına git
   */
  viewVehicle(id: number): void {
    this.router.navigate(['/vehicles', id]);
  }

  /**
   * Araç düzenleme sayfasına git
   */
  editVehicle(id: number): void {
    this.router.navigate(['/vehicles', id, 'edit']);
  }

  /**
   * Araç sil
   */
  deleteVehicle(vehicle: Vehicle): void {
    if (confirm(`"${vehicle.licensePlate}" plakalı aracı silmek istediğinizden emin misiniz?`)) {
      this.vehicleService.deleteVehicle(vehicle.id).subscribe({
        next: () => {
          this.snackBar.open('Araç başarıyla silindi', 'Tamam', { duration: 3000 });
          this.loadVehicles();
        },
        error: (error) => {
          const msg = error.error?.message || 'Araç silinirken hata oluştu';
          this.snackBar.open(msg, 'Kapat', { duration: 5000 });
        }
      });
    }
  }

  /**
   * Yeni araç oluşturma sayfasına git
   */
  createVehicle(): void {
    this.router.navigate(['/vehicles/new']);
  }

  /**
   * Filtreleri temizle
   */
  clearFilters(): void {
    this.searchControl.setValue('');
    this.selectedStatus = '';
    this.currentPage = 0;
    this.loadVehicles();
  }

  /**
   * Durum badge'inin renk sınıfını döndürür
   */
  getStatusClass(status: VehicleStatus): string {
    const colors: Record<VehicleStatus, string> = {
      [VehicleStatus.AVAILABLE]: 'status-available',
      [VehicleStatus.RENTED]: 'status-rented',
      [VehicleStatus.SOLD]: 'status-sold',
      [VehicleStatus.MAINTENANCE]: 'status-maintenance',
      [VehicleStatus.RESERVED]: 'status-reserved',
      [VehicleStatus.DAMAGED]: 'status-damaged'
    };
    return colors[status] || '';
  }
}
