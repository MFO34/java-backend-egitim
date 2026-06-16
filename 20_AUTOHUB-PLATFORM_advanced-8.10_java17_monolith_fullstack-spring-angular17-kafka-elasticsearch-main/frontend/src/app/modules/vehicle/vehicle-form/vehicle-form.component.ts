// Angular importları
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';

// Proje içi importlar
import { VehicleService } from '../../../services/vehicle.service';
import { Vehicle } from '../../../models/vehicle.model';

/**
 * Araç Oluşturma / Düzenleme Formu Bileşeni
 *
 * Yeni araç eklemek veya mevcut aracı düzenlemek için kullanılır.
 * Route parametresinden ID geliyorsa düzenleme modu, gelmiyorsa oluşturma modu.
 *
 * Standalone component olarak tanımlandı - Angular 17 önerilen yaklaşım.
 */
@Component({
  selector: 'app-vehicle-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDividerModule
  ],
  templateUrl: './vehicle-form.component.html',
  styleUrls: ['./vehicle-form.component.scss']
})
export class VehicleFormComponent implements OnInit {

  // Araç formu - tüm alanları reaktif form ile yönetilir
  vehicleForm!: FormGroup;

  // Düzenleme modu mu yoksa oluşturma modu mu?
  isEditMode = false;

  // Düzenleme modunda mevcut araç ID'si
  vehicleId: number | null = null;

  // Yükleme durumu - API çağrısı sırasında buton devre dışı kalır
  isLoading = false;

  // Form başlığı - modaya göre değişir
  get pageTitle(): string {
    return this.isEditMode ? 'Araç Düzenle' : 'Yeni Araç Ekle';
  }

  constructor(
    private fb: FormBuilder,
    private vehicleService: VehicleService,
    private route: ActivatedRoute,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    // Formu başlat
    this.initForm();

    // Route'dan ID parametresini oku
    const id = this.route.snapshot.paramMap.get('id');
    if (id && id !== 'new') {
      // ID varsa düzenleme moduna geç
      this.isEditMode = true;
      this.vehicleId = +id; // String'i sayıya çevir
      this.loadVehicle(this.vehicleId);
    }
  }

  /**
   * Reactive formu tüm validasyon kurallarıyla oluşturur
   */
  private initForm(): void {
    this.vehicleForm = this.fb.group({
      // Model ID - zorunlu
      modelId: [null, Validators.required],
      // Plaka - zorunlu, Türkiye formatı
      licensePlate: ['', [
        Validators.required,
        Validators.pattern(/^\d{2}\s[A-Z]{1,3}\s\d{2,4}$/)
      ]],
      // Üretim yılı - zorunlu, geçerli aralık
      year: ['', [
        Validators.required,
        Validators.min(1886),
        Validators.max(2030)
      ]],
      // Renk - opsiyonel
      color: ['', Validators.maxLength(50)],
      // Kilometre - opsiyonel, negatif olamaz
      mileage: [0, Validators.min(0)],
      // Satış fiyatı - zorunlu
      price: ['', [Validators.required, Validators.min(0.01)]],
      // Günlük kiralama ücreti - opsiyonel
      dailyRate: [null, Validators.min(0)],
      // VIN numarası - opsiyonel, tam 17 karakter
      vinNumber: ['', [Validators.minLength(17), Validators.maxLength(17)]],
      // Motor numarası - opsiyonel
      engineNumber: ['', Validators.maxLength(50)],
      // Açıklama - opsiyonel
      description: ['', Validators.maxLength(2000)],
      // Son muayene tarihi - opsiyonel
      lastInspectionDate: [null],
      // Sigorta bitiş tarihi - opsiyonel
      insuranceExpiryDate: [null]
    });
  }

  /**
   * Düzenleme modunda mevcut araç verilerini API'den yükler ve forma doldurur
   */
  private loadVehicle(id: number): void {
    this.isLoading = true;
    this.vehicleService.getVehicleById(id).subscribe({
      next: (vehicle: Vehicle) => {
        // Gelen araç verilerini forma yükle (patchValue: eksik alanları sıfırlamaz)
        this.vehicleForm.patchValue({
          modelId: vehicle.modelId,
          licensePlate: vehicle.licensePlate,
          year: vehicle.year,
          color: vehicle.color,
          mileage: vehicle.mileage,
          price: vehicle.price,
          dailyRate: vehicle.dailyRate,
          vinNumber: vehicle.vinNumber,
          description: vehicle.description,
          lastInspectionDate: vehicle.lastInspectionDate,
          insuranceExpiryDate: vehicle.insuranceExpiryDate
        });
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
   * Form gönderildiğinde çağrılır
   * Oluşturma veya güncelleme isteği gönderir
   */
  onSubmit(): void {
    // Form geçersizse alanları işaretle ve dur
    if (this.vehicleForm.invalid) {
      this.vehicleForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    const formValue = this.vehicleForm.value;

    // Düzenleme moduna göre farklı API çağrısı yap
    const request$ = this.isEditMode
      ? this.vehicleService.updateVehicle(this.vehicleId!, formValue)
      : this.vehicleService.createVehicle(formValue);

    request$.subscribe({
      next: (vehicle) => {
        const msg = this.isEditMode ? 'Araç başarıyla güncellendi' : 'Araç başarıyla eklendi';
        this.snackBar.open(msg, 'Tamam', { duration: 3000, panelClass: 'success-snackbar' });
        // Kaydedilen aracın detay sayfasına git
        this.router.navigate(['/vehicles', vehicle.id]);
      },
      error: (err) => {
        this.isLoading = false;
        const msg = err.error?.message || 'İşlem sırasında bir hata oluştu';
        this.snackBar.open(msg, 'Kapat', { duration: 5000, panelClass: 'error-snackbar' });
      }
    });
  }

  /**
   * Formu iptal edip araç listesine döner
   */
  onCancel(): void {
    this.router.navigate(['/vehicles']);
  }

  // Kolay erişim için form kontrol getter'ları
  get f() { return this.vehicleForm.controls; }
}
