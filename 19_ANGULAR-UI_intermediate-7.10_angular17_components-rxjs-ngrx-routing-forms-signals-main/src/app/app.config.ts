import { ApplicationConfig, isDevMode } from '@angular/core';
import { provideRouter, withComponentInputBinding, withViewTransitions } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideStore } from '@ngrx/store';
import { provideEffects } from '@ngrx/effects';
import { provideStoreDevtools } from '@ngrx/store-devtools';
import { routes } from './app.routes';
import { productReducer } from './store/reducers/product.reducer';
import { ProductEffects } from './store/effects/product.effects';
import { authInterceptor } from './interceptors/auth.interceptor';

/**
 * Angular 17 — standalone bootstrap (no AppModule)
 * provideXxx() fonksiyonları ile modüler DI
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(
      routes,
      withComponentInputBinding(),  // route params → @Input() binding
      withViewTransitions()         // View Transitions API (smooth navigation)
    ),

    provideHttpClient(
      withInterceptors([authInterceptor])  // functional interceptor
    ),

    // NgRx Store
    provideStore({ products: productReducer }),
    provideEffects(ProductEffects),
    provideStoreDevtools({
      maxAge: 25,
      logOnly: !isDevMode(),  // prod'da sadece log
      connectInZone: true,
    }),
  ],
};
