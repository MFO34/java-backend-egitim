// Angular importları
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';

// Angular Material importları
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';

// Bileşenler
import { VehicleListComponent } from './vehicle-list/vehicle-list.component';

/**
 * Araç Modülü - Araç yönetimi ile ilgili tüm bileşenleri içerir
 */
const vehicleRoutes: Routes = [
  { path: '', component: VehicleListComponent },
  { path: 'new', loadComponent: () =>
      import('./vehicle-form/vehicle-form.component').then(c => c.VehicleFormComponent) },
  { path: ':id', loadComponent: () =>
      import('./vehicle-detail/vehicle-detail.component').then(c => c.VehicleDetailComponent) },
  { path: ':id/edit', loadComponent: () =>
      import('./vehicle-form/vehicle-form.component').then(c => c.VehicleFormComponent) }
];

@NgModule({
  declarations: [VehicleListComponent],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule.forChild(vehicleRoutes),
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatSelectModule,
    MatProgressBarModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatChipsModule
  ]
})
export class VehicleModule {}
