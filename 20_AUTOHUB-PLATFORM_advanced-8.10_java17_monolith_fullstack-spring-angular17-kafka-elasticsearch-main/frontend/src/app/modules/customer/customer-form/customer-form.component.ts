// Müşteri Formu Bileşeni - müşteri oluşturma ve düzenleme

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
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { CustomerService } from '../../../services/customer.service';
import { CustomerRequest } from '../../../models/customer.model';

/**
 * Müşteri Formu Bileşeni
 *
 * Yeni müşteri kaydı oluşturma ve mevcut müşteri düzenleme için kullanılır.
 * TC kimlik doğrulama, telefon formatlama ve zorunlu alan kontrollerini içerir.
 */
@Component({
  selector: 'app-customer-form',
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
    MatDividerModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './customer-form.component.html',
  styleUrls: ['./customer-form.component.scss']
})
export class CustomerFormComponent implements OnInit {

  // Müşteri formu
  customerForm!: FormGroup;

  // Düzenleme modu
  isEditMode = false;
  customerId?: number;

  // İşlem durumu
  isLoading = false;

  // Sayfa başlığı
  get pageTitle(): string {
    return this.isEditMode ? 'Müşteri Düzenle' : 'Yeni Müşteri';
  }

  // Türkiye şehirleri listesi
  cities = [
    'Adana', 'Ankara', 'Antalya', 'Bursa', 'Denizli', 'Diyarbakır',
    'Eskişehir', 'Gaziantep', 'İstanbul', 'İzmir', 'Kayseri', 'Konya',
    'Malatya', 'Mersin', 'Samsun', 'Trabzon', 'Diğer'
  ];

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private customerService: CustomerService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.buildForm();
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam && idParam !== 'new') {
      this.isEditMode = true;
      this.customerId = +idParam;
      this.loadCustomer(this.customerId);
    }
  }

  // Formu oluşturur ve validasyon kurallarını tanımlar
  private buildForm(): void {
    this.customerForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
      lastName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
      // TC kimlik: tam 11 rakam
      tcNumber: ['', [Validators.minLength(11), Validators.maxLength(11), Validators.pattern(/^\d{11}$/)]],
      phone: ['', [Validators.required, Validators.pattern(/^(\+90|0)?[5][0-9]{9}$/)]],
      email: ['', [Validators.email]],
      birthDate: [''],
      address: ['', Validators.maxLength(500)],
      city: [''],
      drivingLicenseNumber: [''],
      notes: ['', Validators.maxLength(1000)]
    });
  }

  // Müşteriyi API'den yükler
  private loadCustomer(id: number): void {
    this.isLoading = true;
    this.customerService.getCustomerById(id).subscribe({
      next: (customer) => {
        this.customerForm.patchValue({
          firstName: customer.firstName,
          lastName: customer.lastName,
          tcNumber: customer.tcNumber || '',
          phone: customer.phone || '',
          email: customer.email || '',
          birthDate: customer.birthDate || '',
          address: customer.address || '',
          city: customer.city || '',
          drivingLicenseNumber: customer.drivingLicenseNumber || '',
          notes: customer.notes || ''
        });
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.snackBar.open('Müşteri bilgileri yüklenemedi', 'Kapat', { duration: 3000 });
        this.router.navigate(['/customers']);
      }
    });
  }

  // Form kontrollerine kısayol erişim
  get f() { return this.customerForm.controls; }

  // Formu gönderir
  onSubmit(): void {
    if (this.customerForm.invalid) {
      this.customerForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    const formValue = this.customerForm.value;

    const request: CustomerRequest = {
      firstName: formValue.firstName.trim(),
      lastName: formValue.lastName.trim(),
      tcNumber: formValue.tcNumber || undefined,
      phone: formValue.phone,
      email: formValue.email || undefined,
      birthDate: formValue.birthDate || undefined,
      address: formValue.address || undefined,
      city: formValue.city || undefined,
      drivingLicenseNumber: formValue.drivingLicenseNumber || undefined,
      notes: formValue.notes || undefined
    };

    const operation = this.isEditMode
      ? this.customerService.updateCustomer(this.customerId!, request)
      : this.customerService.createCustomer(request);

    operation.subscribe({
      next: (saved) => {
        const msg = this.isEditMode ? 'Müşteri güncellendi' : 'Müşteri oluşturuldu';
        this.snackBar.open(msg, 'Tamam', { duration: 3000 });
        this.router.navigate(['/customers', saved.id]);
      },
      error: (err) => {
        this.isLoading = false;
        const msg = err.error?.message || 'İşlem sırasında hata oluştu';
        this.snackBar.open(msg, 'Kapat', { duration: 5000 });
      }
    });
  }

  // Formu iptal eder
  onCancel(): void {
    if (this.isEditMode && this.customerId) {
      this.router.navigate(['/customers', this.customerId]);
    } else {
      this.router.navigate(['/customers']);
    }
  }
}
