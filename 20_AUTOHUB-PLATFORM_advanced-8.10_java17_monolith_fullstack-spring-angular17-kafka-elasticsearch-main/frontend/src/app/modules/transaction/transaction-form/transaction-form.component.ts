// İşlem Formu Bileşeni - yeni kiralama veya satış işlemi oluşturur

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
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatRadioModule } from '@angular/material/radio';

import { TransactionService } from '../../../services/transaction.service';
import { VehicleService } from '../../../services/vehicle.service';
import { CustomerService } from '../../../services/customer.service';
import { TransactionRequest, TransactionType } from '../../../models/transaction.model';
import { Vehicle, VehicleStatus } from '../../../models/vehicle.model';
import { Customer } from '../../../models/customer.model';

/**
 * İşlem Formu Bileşeni
 *
 * Kiralama veya satış işlemi oluşturmak için kullanılır.
 * Araç ve müşteri seçimi sonrası işlem türüne göre form alanları dinamik olarak değişir.
 * Kiralama için bitiş tarihi zorunludur; satış için opsiyoneldir.
 */
@Component({
  selector: 'app-transaction-form',
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
    MatDatepickerModule,
    MatNativeDateModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatRadioModule
  ],
  templateUrl: './transaction-form.component.html',
  styleUrls: ['./transaction-form.component.scss']
})
export class TransactionFormComponent implements OnInit {

  // Form grubu
  transactionForm!: FormGroup;

  // Form gönderiliyor mu?
  isLoading = false;

  // Açılır listelerin verileri
  availableVehicles: Vehicle[] = [];
  customers: Customer[] = [];

  // İşlem türleri
  TransactionType = TransactionType;
  transactionTypes = Object.values(TransactionType);

  // Müşteri arama terimi
  customerSearch = '';

  // URL'den gelebilecek ön seçimler
  preselectedVehicleId?: number;
  preselectedCustomerId?: number;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private transactionService: TransactionService,
    private vehicleService: VehicleService,
    private customerService: CustomerService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.buildForm();
    this.loadVehicles();
    this.loadCustomers();

    // URL query parametrelerinden ön seçim
    this.route.queryParams.subscribe(params => {
      if (params['vehicleId']) {
        this.preselectedVehicleId = +params['vehicleId'];
        this.transactionForm.patchValue({ vehicleId: this.preselectedVehicleId });
      }
      if (params['customerId']) {
        this.preselectedCustomerId = +params['customerId'];
        this.transactionForm.patchValue({ customerId: this.preselectedCustomerId });
      }
    });
  }

  // Formu oluşturur
  private buildForm(): void {
    this.transactionForm = this.fb.group({
      vehicleId: [null, Validators.required],
      customerId: [null, Validators.required],
      transactionType: [TransactionType.RENTAL, Validators.required],
      startDate: [new Date(), Validators.required],
      endDate: [null],
      notes: ['', Validators.maxLength(500)]
    });

    // Kiralama seçilince endDate zorunlu olur
    this.transactionForm.get('transactionType')!.valueChanges.subscribe(type => {
      const endDateControl = this.transactionForm.get('endDate')!;
      if (type === TransactionType.RENTAL) {
        endDateControl.setValidators(Validators.required);
      } else {
        endDateControl.clearValidators();
        endDateControl.setValue(null);
      }
      endDateControl.updateValueAndValidity();
    });
  }

  // Uygun araçları yükler (sadece AVAILABLE durumundakiler)
  loadVehicles(): void {
    this.vehicleService.getVehiclesByStatus(VehicleStatus.AVAILABLE, 0, 100).subscribe({
      next: (response) => {
        this.availableVehicles = response.content || [];
      },
      error: () => {
        this.snackBar.open('Araçlar yüklenemedi', 'Kapat', { duration: 3000 });
      }
    });
  }

  // Müşteri listesini yükler
  loadCustomers(): void {
    this.customerService.getCustomers(0, 100).subscribe({
      next: (response) => {
        this.customers = response.content || [];
      },
      error: () => {
        this.snackBar.open('Müşteriler yüklenemedi', 'Kapat', { duration: 3000 });
      }
    });
  }

  // Seçilen işlem türü kiralama mı?
  get isRental(): boolean {
    return this.transactionForm.get('transactionType')?.value === TransactionType.RENTAL;
  }

  // Form kontrolü kısayolu
  get f() { return this.transactionForm.controls; }

  // Formu gönderir
  onSubmit(): void {
    if (this.transactionForm.invalid) {
      this.transactionForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    const formValue = this.transactionForm.value;

    const request: TransactionRequest = {
      vehicleId: formValue.vehicleId,
      customerId: formValue.customerId,
      transactionType: formValue.transactionType,
      startDate: this.formatDate(formValue.startDate),
      endDate: formValue.endDate ? this.formatDate(formValue.endDate) : undefined,
      notes: formValue.notes || undefined
    };

    this.transactionService.createTransaction(request).subscribe({
      next: (transaction) => {
        this.snackBar.open('İşlem başarıyla oluşturuldu', 'Tamam', { duration: 3000 });
        this.router.navigate(['/transactions', transaction.id]);
      },
      error: (err) => {
        this.isLoading = false;
        const msg = err.error?.message || 'İşlem oluşturulurken hata oluştu';
        this.snackBar.open(msg, 'Kapat', { duration: 5000 });
      }
    });
  }

  // Tarihi ISO formatına çevirir
  private formatDate(date: Date | string): string {
    if (typeof date === 'string') return date;
    return date.toISOString().split('T')[0];
  }

  // İptal eder
  onCancel(): void {
    this.router.navigate(['/transactions']);
  }
}
