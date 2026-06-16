import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AsyncPipe, CurrencyPipe, NgIf } from '@angular/common';
import { Store } from '@ngrx/store';
import * as ProductActions from '../../store/actions/product.actions';
import { selectSelectedProduct, selectLoading } from '../../store/selectors/product.selectors';
import { Product } from '../../models/product.model';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [AsyncPipe, CurrencyPipe, NgIf],
  template: `
    <div class="product-detail">
      @if (loading$ | async) {
        <div class="loading">Yükleniyor...</div>
      }

      @if (product$ | async; as product) {
        <div class="detail-card">
          <h1>{{ product.name }}</h1>
          <p class="description">{{ product.description }}</p>
          <div class="meta">
            <span class="category">{{ product.category }}</span>
            <span class="rating">⭐ {{ product.avgRating }}</span>
            <span class="stock">Stok: {{ product.stock }}</span>
          </div>
          <div class="price">{{ product.price | currency:'TRY':'symbol':'1.0-0' }}</div>
          <div class="tags">
            @for (tag of product.tags; track tag) {
              <span class="tag">{{ tag }}</span>
            }
          </div>
          <button
            (click)="addToCart(product)"
            [disabled]="product.stock === 0"
            class="add-to-cart-btn">
            {{ product.stock > 0 ? 'Sepete Ekle' : 'Stok Yok' }}
          </button>
        </div>
      }
    </div>
  `,
})
export class ProductDetailComponent implements OnInit {
  private store = inject(Store);
  private route = inject(ActivatedRoute);

  product$ = this.store.select(selectSelectedProduct);
  loading$ = this.store.select(selectLoading);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.store.dispatch(ProductActions.loadProduct({ id }));
  }

  addToCart(product: Product): void {
    this.store.dispatch(ProductActions.addToCart({ product, quantity: 1 }));
  }
}
