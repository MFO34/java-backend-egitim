// Angular importları
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';

// Proje içi importlar
import { AuthService } from '../../../services/auth.service';

/**
 * Giriş Sayfası Bileşeni
 *
 * Reactive Form ile kullanıcı girişi yapılır.
 * JWT token alınır ve localStorage'a kaydedilir.
 *
 * OOP Prensibi - SORUMLULUK AYIRIMI:
 * Bileşen sadece UI ve kullanıcı etkileşimini yönetir.
 * İş mantığı AuthService'te kapsüllenmiştir.
 */
@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {

  // Reactive Form - type-safe form yönetimi
  loginForm!: FormGroup;

  // Yükleme durumu - form submit sırasında butonu devre dışı bırakmak için
  isLoading = false;

  // Şifre görünür/gizli toggle
  hidePassword = true;

  // Giriş sonrası yönlendirilecek URL
  private returnUrl = '/dashboard';

  constructor(
    private formBuilder: FormBuilder,    // Reactive form oluşturmak için
    private authService: AuthService,    // Giriş işlemi için
    private router: Router,              // Yönlendirme için
    private route: ActivatedRoute,       // URL query params okumak için
    private snackBar: MatSnackBar        // Bildirim toast'ları için
  ) {}

  /**
   * Bileşen başlatma
   * Form tanımlanır ve returnUrl alınır
   */
  ngOnInit(): void {
    // Reactive Form oluştur - doğrulama kuralları ile birlikte
    this.loginForm = this.formBuilder.group({
      // Kullanıcı adı: Zorunlu, minimum 3 karakter
      username: ['', [
        Validators.required,
        Validators.minLength(3)
      ]],
      // Şifre: Zorunlu, minimum 6 karakter
      password: ['', [
        Validators.required,
        Validators.minLength(6)
      ]]
    });

    // returnUrl query param'ı oku - başarılı girişte buraya yönlendir
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/dashboard';

    // Zaten giriş yapmışsa dashboard'a yönlendir
    if (this.authService.isLoggedIn()) {
      this.router.navigate(['/dashboard']);
    }
  }

  /**
   * Form alanına kolay erişim getter'ları
   * Template'de this.f.username.errors şeklinde kullanılır
   */
  get f() {
    return this.loginForm.controls;
  }

  /**
   * Form submit handler
   * Form geçerliyse giriş isteği gönderir
   */
  onSubmit(): void {
    // Form geçerliliğini kontrol et
    if (this.loginForm.invalid) {
      // Tüm alanları dokunulmuş olarak işaretle - hata mesajlarını göster
      this.loginForm.markAllAsTouched();
      return;
    }

    // Yükleme durumunu aktif et
    this.isLoading = true;

    // AuthService üzerinden giriş yap
    this.authService.login(this.loginForm.value).subscribe({
      next: (response) => {
        // Başarılı giriş - bildirim göster
        this.snackBar.open(
          `Hoş geldiniz, ${response.fullName}!`,
          'Tamam',
          { duration: 3000, panelClass: 'success-snackbar' }
        );
        // ReturnUrl'e veya dashboard'a yönlendir
        this.router.navigate([this.returnUrl]);
      },
      error: (error) => {
        // Hata durumu - kullanıcıya bildirim göster
        this.isLoading = false;
        const errorMessage = error.status === 401
          ? 'Kullanıcı adı veya şifre hatalı!'
          : 'Giriş yapılırken bir hata oluştu. Lütfen tekrar deneyin.';

        this.snackBar.open(errorMessage, 'Kapat', {
          duration: 5000,
          panelClass: 'error-snackbar'
        });
      }
    });
  }
}
