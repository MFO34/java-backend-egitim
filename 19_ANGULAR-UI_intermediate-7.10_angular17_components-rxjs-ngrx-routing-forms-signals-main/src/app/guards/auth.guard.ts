import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

/**
 * Functional guard (Angular 17) — class-based CanActivate yerine
 */
export const authGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const token = localStorage.getItem('access_token');

  if (token) {
    return true;
  }

  return router.createUrlTree(['/login'], {
    queryParams: { returnUrl: state.url }
  });
};

export const adminGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const role = localStorage.getItem('user_role');

  if (role === 'ADMIN') {
    return true;
  }

  return router.createUrlTree(['/forbidden']);
};
