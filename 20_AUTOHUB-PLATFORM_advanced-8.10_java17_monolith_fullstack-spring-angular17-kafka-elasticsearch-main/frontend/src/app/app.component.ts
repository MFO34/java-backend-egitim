// Angular Core importları
import { Component, OnInit } from '@angular/core';
import { Router, NavigationEnd, NavigationStart } from '@angular/router';
import { filter } from 'rxjs/operators';

// Proje içi importlar
import { AuthService } from './services/auth.service';

/**
 * Kök Uygulama Bileşeni
 *
 * Tüm diğer bileşenlerin kapsayıcısı.
 * Router outlet ile sayfa geçişleri yönetilir.
 *
 * @Component: Angular bileşeni dekoratörü
 * selector: HTML'de <app-root> etiketi ile kullanılır
 */
@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {

  // Uygulama başlığı
  title = 'AutoHub - Otomotiv Yönetim Sistemi';

  // Sayfa yüklenme durumu - router navigasyon sırasında spinner göster
  isLoading = false;

  constructor(
    private router: Router,
    public authService: AuthService
  ) {}

  /**
   * Bileşen başlatma
   * Router olaylarını dinleyerek yükleme durumunu yönetir
   */
  ngOnInit(): void {
    // Router olaylarını dinle - navigasyon başladığında spinner göster
    this.router.events.pipe(
      filter(event =>
        event instanceof NavigationStart ||
        event instanceof NavigationEnd
      )
    ).subscribe(event => {
      // NavigationStart: Sayfa geçişi başladı - spinner göster
      this.isLoading = event instanceof NavigationStart;
    });
  }
}
