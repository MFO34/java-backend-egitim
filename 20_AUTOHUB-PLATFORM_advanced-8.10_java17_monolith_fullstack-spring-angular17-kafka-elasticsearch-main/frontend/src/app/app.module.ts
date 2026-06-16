// Angular Core importları
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';

// Angular Material importları
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

// Proje içi importlar
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { JwtInterceptor } from './interceptors/jwt.interceptor';

/**
 * Kök Uygulama Modülü
 *
 * Tüm Angular modüllerini ve servisleri bir araya getirir.
 * Uygulama bu modül ile başlatılır.
 *
 * @NgModule: Angular modülü tanımlama dekoratörü
 */
@NgModule({
  declarations: [
    AppComponent      // Kök bileşen
  ],
  imports: [
    // Angular temel modüller
    BrowserModule,                    // Browser desteği için
    BrowserAnimationsModule,          // Angular Material animasyonları için
    HttpClientModule,                 // HTTP istekleri için

    // Form modülleri
    ReactiveFormsModule,              // Reactive forms (FormGroup, FormControl)
    FormsModule,                      // Template-driven forms

    // Angular Material modülleri (global)
    MatSnackBarModule,                // Bildirim toast'ları için
    MatProgressSpinnerModule,         // Yükleme spinner'ı için

    // Uygulama routing
    AppRoutingModule
  ],
  providers: [
    // JWT Interceptor - her HTTP isteğine token ekler
    // useClass: Interceptor'ın sınıfı
    // multi: true - birden fazla interceptor olabilir
    {
      provide: HTTP_INTERCEPTORS,
      useClass: JwtInterceptor,
      multi: true
    }
  ],
  bootstrap: [AppComponent]          // Başlangıç bileşeni
})
export class AppModule {}
