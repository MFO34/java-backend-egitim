// Marka Formu Bileşeni - marka oluşturma ve düzenleme

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';

import { BrandService } from '../../../services/brand.service';
import { BrandRequest } from '../../../models/brand.model';

/**
 * Marka Formu Bileşeni
 *
 * Hem yeni marka oluşturma hem de mevcut marka düzenleme için kullanılır.
 * Mod, URL'deki :id parametresine göre belirlenir.
 */
@Component({
  selector: 'app-brand-form',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDividerModule
  ],
  templateUrl: './brand-form.component.html',
  styleUrls: ['./brand-form.component.scss']
})
export class BrandFormComponent implements OnInit {

  // Form grubu
  brandForm!: FormGroup;

  // Düzenleme modu mu?
  isEditMode = false;

  // Düzenlenen markanın ID'si
  brandId?: number;

  // Form gönderiliyor mu?
  isLoading = false;

  // Sayfa başlığı
  get pageTitle(): string {
    return this.isEditMode ? 'Marka Düzenle' : 'Yeni Marka Ekle';
  }

  // Menşei ülke seçenekleri
  countries = [
    'Japonya', 'Almanya', 'Amerika Birleşik Devletleri', 'İtalya',
    'Fransa', 'İngiltere', 'Güney Kore', 'İsveç', 'Çin', 'Türkiye', 'Diğer'
  ];

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private brandService: BrandService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.buildForm();
    // URL'de ID varsa düzenleme moduna geç
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam && idParam !== 'new') {
      this.isEditMode = true;
      this.brandId = +idParam;
      this.loadBrand(this.brandId);
    }
  }

  // Formu oluşturur
  private buildForm(): void {
    this.brandForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
      country: ['', Validators.required],
      logoUrl: ['', Validators.pattern(/^https?:\/\/.+/)]
    });
  }

  // Düzenlenecek markayı API'den yükler
  private loadBrand(id: number): void {
    this.isLoading = true;
    this.brandService.getBrandById(id).subscribe({
      next: (brand) => {
        this.brandForm.patchValue({
          name: brand.name,
          country: brand.country,
          logoUrl: brand.logoUrl || ''
        });
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.snackBar.open('Marka bilgileri yüklenemedi', 'Kapat', { duration: 3000 });
        this.router.navigate(['/brands']);
      }
    });
  }

  // Form kontrol erişimi kısaltması
  get f() { return this.brandForm.controls; }

  // Formu gönderir
  onSubmit(): void {
    if (this.brandForm.invalid) {
      this.brandForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    const request: BrandRequest = {
      name: this.f['name'].value.trim(),
      country: this.f['country'].value,
      logoUrl: this.f['logoUrl'].value || undefined
    };

    const operation = this.isEditMode
      ? this.brandService.updateBrand(this.brandId!, request)
      : this.brandService.createBrand(request);

    operation.subscribe({
      next: () => {
        const msg = this.isEditMode ? 'Marka güncellendi' : 'Marka oluşturuldu';
        this.snackBar.open(msg, 'Tamam', { duration: 3000 });
        this.router.navigate(['/brands']);
      },
      error: (err) => {
        this.isLoading = false;
        const msg = err.error?.message || 'İşlem sırasında hata oluştu';
        this.snackBar.open(msg, 'Kapat', { duration: 5000 });
      }
    });
  }

  // İptal eder ve listeye döner
  onCancel(): void {
    this.router.navigate(['/brands']);
  }
}
