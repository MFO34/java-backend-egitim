import { Component, OnInit } from '@angular/core';
import { VehicleService } from '../../services/vehicle.service';
import { AuthService } from '../../services/auth.service';
import { Vehicle, VehicleStats } from '../../models/vehicle.model';

/**
 * Dashboard Bileşeni
 * Sistem istatistikleri ve son araçları gösterir.
 */
@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {

  // İstatistikler
  vehicleStats: VehicleStats | null = null;

  // Son eklenen araçlar
  latestVehicles: Vehicle[] = [];

  // Yükleme durumu
  isLoading = false;

  constructor(
    private vehicleService: VehicleService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadDashboardData();
  }

  /**
   * Dashboard verilerini yükler
   */
  loadDashboardData(): void {
    this.isLoading = true;

    // Araç istatistiklerini yükle
    this.vehicleService.getVehicleStats().subscribe({
      next: (stats) => { this.vehicleStats = stats; },
      error: (err) => console.error('İstatistik yükleme hatası:', err)
    });

    // Son araçları yükle
    this.vehicleService.getLatestVehicles(6).subscribe({
      next: (vehicles) => {
        this.latestVehicles = vehicles;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Son araçlar yükleme hatası:', err);
        this.isLoading = false;
      }
    });
  }
}
