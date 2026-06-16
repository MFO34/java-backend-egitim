// Marka Listesi Bileşeni - tüm markaları ve modellerini gösterir

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
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';

import { BrandService } from '../../../services/brand.service';
import { AuthService } from '../../../services/auth.service';
import { Brand } from '../../../models/brand.model';

/**
 * Marka Listesi Bileşeni
 *
 * Sistemdeki tüm araç markalarını ve her markaya ait model sayısını listeler.
 * Yöneticiler yeni marka ekleyebilir, mevcut markaları düzenleyip silebilir.
 */
@Component({
  selector: 'app-brand-list',
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
    MatDialogModule,
    MatTooltipModule,
    MatChipsModule
  ],
  templateUrl: './brand-list.component.html',
  styleUrls: ['./brand-list.component.scss']
})
export class BrandListComponent implements OnInit {

  // Marka listesi
  brands: Brand[] = [];

  // Yükleme durumu
  isLoading = false;

  // Tabloda gösterilecek sütunlar
  displayedColumns: string[] = ['name', 'country', 'modelCount', 'isActive', 'actions'];

  constructor(
    private brandService: BrandService,
    private router: Router,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadBrands();
  }

  // Markaları API'den yükler
  loadBrands(): void {
    this.isLoading = true;
    this.brandService.getBrands(0, 100).subscribe({
      next: (response) => {
        // API sayfalı veya düz liste döndürebilir
        this.brands = response.content || response || [];
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.snackBar.open('Markalar yüklenemedi', 'Kapat', { duration: 3000 });
      }
    });
  }

  // Yeni marka ekleme sayfasına gider
  addBrand(): void {
    this.router.navigate(['/brands/new']);
  }

  // Marka düzenleme sayfasına gider
  editBrand(brand: Brand): void {
    this.router.navigate(['/brands', brand.id, 'edit']);
  }

  // Marka siler
  deleteBrand(brand: Brand): void {
    if (confirm(`"${brand.name}" markasını silmek istediğinizden emin misiniz?`)) {
      this.brandService.deleteBrand(brand.id).subscribe({
        next: () => {
          this.snackBar.open('Marka silindi', 'Tamam', { duration: 3000 });
          this.loadBrands();
        },
        error: (err) => {
          const msg = err.error?.message || 'Marka silinirken hata oluştu';
          this.snackBar.open(msg, 'Kapat', { duration: 5000 });
        }
      });
    }
  }

  // Markaya ait araç listesine gider (filtrelenmiş)
  viewVehicles(brand: Brand): void {
    this.router.navigate(['/vehicles'], { queryParams: { brandId: brand.id } });
  }
}
