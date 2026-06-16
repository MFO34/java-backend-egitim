import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from '../../../services/auth.service';

/**
 * Kayıt Sayfası Bileşeni
 * Yeni kullanıcı kaydı için reactive form
 */
@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent implements OnInit {

  registerForm!: FormGroup;
  isLoading = false;
  hidePassword = true;
  hideConfirmPassword = true;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.registerForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
      email: ['', [Validators.required, Validators.email]],
      firstName: ['', [Validators.maxLength(50)]],
      lastName: ['', [Validators.maxLength(50)]],
      password: ['', [
        Validators.required,
        Validators.minLength(8),
        // Şifre güçlülük kontrolü: büyük harf, küçük harf, rakam ve özel karakter
        Validators.pattern(/^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).*$/)
      ]],
      confirmPassword: ['', Validators.required]
    }, {
      // Özel validator: Şifreler eşleşiyor mu?
      validators: this.passwordMatchValidator
    });
  }

  /**
   * Şifre eşleşme validator'ı
   * FormGroup düzeyinde çalışır - iki alanı karşılaştırır
   */
  private passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.get('password');
    const confirmPassword = control.get('confirmPassword');

    if (password && confirmPassword && password.value !== confirmPassword.value) {
      // Şifreler eşleşmiyorsa confirmPassword alanına hata ekle
      confirmPassword.setErrors({ passwordMismatch: true });
      return { passwordMismatch: true };
    }
    return null;
  }

  get f() { return this.registerForm.controls; }

  onSubmit(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;

    this.authService.register(this.registerForm.value).subscribe({
      next: (response) => {
        this.snackBar.open('Hesabınız başarıyla oluşturuldu!', 'Tamam', {
          duration: 3000,
          panelClass: 'success-snackbar'
        });
        this.router.navigate(['/dashboard']);
      },
      error: (error) => {
        this.isLoading = false;
        const message = error.error?.message || 'Kayıt sırasında bir hata oluştu';
        this.snackBar.open(message, 'Kapat', { duration: 5000, panelClass: 'error-snackbar' });
      }
    });
  }
}
