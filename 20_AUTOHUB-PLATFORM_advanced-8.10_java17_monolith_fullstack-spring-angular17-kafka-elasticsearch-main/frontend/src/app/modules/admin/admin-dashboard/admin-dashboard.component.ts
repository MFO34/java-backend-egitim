// Admin Dashboard Bileşeni - sistem yönetimi ve kullanıcı istatistikleri

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { HttpClient } from '@angular/common/http';

/**
 * Admin Dashboard Bileşeni
 *
 * Sistem yöneticisi için genel bakış paneli.
 * Kullanıcı listesi, sistem sağlık durumu ve istatistikler gösterilir.
 * Sadece ADMIN rolüne sahip kullanıcılar erişebilir.
 */
@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatChipsModule,
    MatDividerModule,
    MatTooltipModule
  ],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent implements OnInit {

  // Sistem kullanıcıları listesi
  users: any[] = [];

  // Yükleme durumu
  isLoading = false;

  // Sistem sağlık durumu
  systemHealth: 'UP' | 'DOWN' | 'UNKNOWN' = 'UNKNOWN';

  // Kullanıcı tablosu sütunları
  userColumns = ['id', 'username', 'email', 'role', 'isActive', 'createdAt', 'actions'];

  constructor(
    private http: HttpClient,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadUsers();
    this.checkHealth();
  }

  // Sistem kullanıcılarını yükler
  loadUsers(): void {
    this.isLoading = true;
    this.http.get<any[]>('/api/admin/users').subscribe({
      next: (users) => {
        this.users = users;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.snackBar.open('Kullanıcılar yüklenemedi', 'Kapat', { duration: 3000 });
      }
    });
  }

  // Sistem sağlık kontrolü yapar (Spring Actuator)
  checkHealth(): void {
    this.http.get<any>('/api/actuator/health').subscribe({
      next: (health) => {
        this.systemHealth = health.status === 'UP' ? 'UP' : 'DOWN';
      },
      error: () => {
        this.systemHealth = 'DOWN';
      }
    });
  }

  // Kullanıcı aktif/pasif durumunu değiştirir
  toggleUserStatus(user: any): void {
    const action = user.isActive ? 'devre dışı bırakmak' : 'aktif etmek';
    if (!confirm(`"${user.username}" kullanıcısını ${action} istediğinizden emin misiniz?`)) return;

    this.http.patch(`/api/admin/users/${user.id}/toggle-status`, {}).subscribe({
      next: () => {
        user.isActive = !user.isActive;
        this.snackBar.open('Kullanıcı durumu güncellendi', 'Tamam', { duration: 3000 });
      },
      error: () => {
        this.snackBar.open('Durum güncellenemedi', 'Kapat', { duration: 3000 });
      }
    });
  }

  // Rol renk sınıfı
  getRoleClass(role: string): string {
    const map: Record<string, string> = {
      'ADMIN': 'role-admin',
      'MANAGER': 'role-manager',
      'EMPLOYEE': 'role-employee',
      'CUSTOMER': 'role-customer'
    };
    return map[role] || '';
  }
}
