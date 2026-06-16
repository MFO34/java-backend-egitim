import { Component, inject } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { Store } from '@ngrx/store';
import { selectCartItemCount } from '../../store/selectors/product.selectors';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [AsyncPipe, RouterLink, RouterLinkActive],
  template: `
    <nav class="navbar">
      <a routerLink="/products" class="logo">ShopApp</a>
      <div class="nav-links">
        <a routerLink="/products" routerLinkActive="active">Ürünler</a>
        <a routerLink="/cart" routerLinkActive="active">
          Sepet
          @if ((cartCount$ | async) ?? 0; as count) {
            <span class="badge">{{ count }}</span>
          }
        </a>
        <a routerLink="/login">Giriş</a>
      </div>
    </nav>
  `,
})
export class NavbarComponent {
  private store = inject(Store);
  cartCount$ = this.store.select(selectCartItemCount);
}
