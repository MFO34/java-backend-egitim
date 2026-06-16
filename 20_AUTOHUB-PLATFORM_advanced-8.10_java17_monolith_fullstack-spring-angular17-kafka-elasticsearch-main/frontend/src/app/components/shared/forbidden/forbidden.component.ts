// Erişim Yasak Bileşeni - 403 Forbidden sayfası

import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';

/**
 * Erişim Yasak Bileşeni
 *
 * Kullanıcı yetkisi olmayan bir sayfaya erişmeye çalıştığında gösterilir.
 * RoleGuard tarafından yetki reddi durumunda bu sayfaya yönlendirilir.
 */
@Component({
  selector: 'app-forbidden',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule
  ],
  templateUrl: './forbidden.component.html',
  styleUrls: ['./forbidden.component.scss']
})
export class ForbiddenComponent {

  constructor(private router: Router) {}

  // Ana sayfaya döner
  goHome(): void {
    this.router.navigate(['/dashboard']);
  }

  // Geri gider
  goBack(): void {
    window.history.back();
  }
}
