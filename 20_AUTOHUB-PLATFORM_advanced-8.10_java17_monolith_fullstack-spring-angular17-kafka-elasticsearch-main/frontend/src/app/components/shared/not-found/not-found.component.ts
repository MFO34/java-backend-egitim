// Sayfa Bulunamadı Bileşeni - 404 Not Found sayfası

import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';

/**
 * Sayfa Bulunamadı Bileşeni
 *
 * Kullanıcı var olmayan bir URL'e gittiğinde gösterilir.
 * Angular router'ın wildcard route'u ('**') bu bileşeni gösterir.
 */
@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule
  ],
  templateUrl: './not-found.component.html',
  styleUrls: ['./not-found.component.scss']
})
export class NotFoundComponent {

  constructor(private router: Router) {}

  // Ana sayfaya gider
  goHome(): void {
    this.router.navigate(['/dashboard']);
  }

  // Önceki sayfaya döner
  goBack(): void {
    window.history.back();
  }
}
