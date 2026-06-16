import { Component, inject } from '@angular/core';
import { AsyncPipe, CurrencyPipe, NgFor } from '@angular/common';
import { Store } from '@ngrx/store';
import * as ProductActions from '../../store/actions/product.actions';
import { selectCart, selectCartTotal } from '../../store/selectors/product.selectors';
import { CartItem } from '../../models/product.model';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [AsyncPipe, CurrencyPipe, NgFor],
  template: `
    <div class="cart">
      <h2>Sepetim</h2>

      @for (item of cart$ | async; track item.product.id) {
        <div class="cart-item">
          <span class="name">{{ item.product.name }}</span>
          <div class="quantity-control">
            <button (click)="decrement(item)">−</button>
            <span>{{ item.quantity }}</span>
            <button (click)="increment(item)">+</button>
          </div>
          <span class="subtotal">
            {{ item.product.price * item.quantity | currency:'TRY':'symbol':'1.0-0' }}
          </span>
          <button class="remove" (click)="remove(item.product.id)">✕</button>
        </div>
      } @empty {
        <p>Sepetiniz boş.</p>
      }

      <div class="cart-footer">
        <strong>Toplam: {{ cartTotal$ | async | currency:'TRY':'symbol':'1.0-0' }}</strong>
        <button (click)="clearCart()" class="clear-btn">Sepeti Temizle</button>
        <button class="checkout-btn">Satın Al</button>
      </div>
    </div>
  `,
})
export class CartComponent {
  private store = inject(Store);

  cart$ = this.store.select(selectCart);
  cartTotal$ = this.store.select(selectCartTotal);

  increment(item: CartItem): void {
    this.store.dispatch(ProductActions.updateQuantity({
      productId: item.product.id,
      quantity: item.quantity + 1,
    }));
  }

  decrement(item: CartItem): void {
    if (item.quantity <= 1) {
      this.remove(item.product.id);
    } else {
      this.store.dispatch(ProductActions.updateQuantity({
        productId: item.product.id,
        quantity: item.quantity - 1,
      }));
    }
  }

  remove(productId: string): void {
    this.store.dispatch(ProductActions.removeFromCart({ productId }));
  }

  clearCart(): void {
    this.store.dispatch(ProductActions.clearCart());
  }
}
