import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';

/**
 * Lazy loading ile route konfigürasyonu
 * loadComponent → standalone component lazy load
 * loadChildren  → feature module lazy load
 */
export const routes: Routes = [
  {
    path: '',
    redirectTo: '/products',
    pathMatch: 'full',
  },
  {
    path: 'products',
    // Lazy load — ilk yüklemede bundle'a dahil değil
    loadComponent: () =>
      import('./components/product-list/product-list.component').then(
        m => m.ProductListComponent
      ),
    title: 'Ürünler',
  },
  {
    path: 'products/:id',
    loadComponent: () =>
      import('./components/product-detail/product-detail.component').then(
        m => m.ProductDetailComponent
      ),
    title: 'Ürün Detayı',
  },
  {
    path: 'cart',
    loadComponent: () =>
      import('./components/cart/cart.component').then(m => m.CartComponent),
    canActivate: [authGuard],  // route guard
    title: 'Sepetim',
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./components/navbar/navbar.component').then(m => m.NavbarComponent),
  },
  {
    path: '**',
    redirectTo: '/products',
  },
];
