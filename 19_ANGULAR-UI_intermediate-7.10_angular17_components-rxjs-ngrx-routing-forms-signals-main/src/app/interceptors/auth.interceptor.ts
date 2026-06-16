import { HttpInterceptorFn, HttpRequest, HttpHandlerFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { Router } from '@angular/router';

/**
 * Functional interceptor (Angular 17) — class-based yerine function
 * JWT token ekleme + 401 redirect
 */
export const authInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
) => {
  const router = inject(Router);
  const token = localStorage.getItem('access_token');

  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError(error => {
      if (error.status === 401) {
        localStorage.removeItem('access_token');
        router.navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
};

/**
 * Loading interceptor — HTTP istekleri sırasında yükleme durumu
 */
export const loadingInterceptor: HttpInterceptorFn = (req, next) => {
  // inject(LoadingService).show();
  return next(req);
  // .pipe(finalize(() => inject(LoadingService).hide()));
};
