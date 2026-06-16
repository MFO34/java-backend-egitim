import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { AsyncPipe, NgFor, NgIf, CurrencyPipe } from '@angular/common';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Store } from '@ngrx/store';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import * as ProductActions from '../../store/actions/product.actions';
import {
  selectAllProducts,
  selectCartItemCount,
  selectLoading,
  selectError,
  selectSearchResults
} from '../../store/selectors/product.selectors';
import { Product } from '../../models/product.model';

/**
 * Angular 17 Features:
 *   - inject()        → constructor DI yerine
 *   - signal()        → reactive primitive
 *   - computed()      → derived signal
 *   - @for / @if      → new control flow (template)
 *   - AsyncPipe       → observable unwrap + auto-unsubscribe
 *   - Standalone component (no NgModule)
 */
@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [AsyncPipe, NgFor, NgIf, CurrencyPipe, ReactiveFormsModule, RouterLink],
  template: `
    <div class="product-list">
      <!-- Search bar -->
      <div class="search-bar">
        <input
          [formControl]="searchControl"
          placeholder="Ürün ara..."
          class="search-input"
        />
        @if (loading$ | async) {
          <span class="spinner">Yükleniyor...</span>
        }
      </div>

      <!-- Filter signals -->
      <div class="filters">
        <select (change)="setCategory($event)">
          <option value="">Tüm Kategoriler</option>
          @for (cat of categories(); track cat) {
            <option [value]="cat">{{ cat }}</option>
          }
        </select>

        <span class="cart-badge">
          Sepet ({{ cartCount$ | async }})
        </span>
      </div>

      <!-- Error -->
      @if (error$ | async; as error) {
        <div class="error-banner">{{ error }}</div>
      }

      <!-- Product grid — new @for syntax -->
      <div class="product-grid">
        @for (product of displayedProducts(); track product.id) {
          <div class="product-card">
            <h3>{{ product.name }}</h3>
            <p class="category">{{ product.category }}</p>
            <p class="price">{{ product.price | currency:'TRY':'symbol':'1.0-0' }}</p>
            <div class="rating">
              ⭐ {{ product.avgRating | number:'1.1-1' }}
            </div>
            <div class="actions">
              <button [routerLink]="['/products', product.id]">Detay</button>
              <button (click)="addToCart(product)" [disabled]="product.stock === 0">
                {{ product.stock === 0 ? 'Stok Yok' : 'Sepete Ekle' }}
              </button>
            </div>
          </div>
        } @empty {
          <p class="empty">Ürün bulunamadı.</p>
        }
      </div>

      <!-- Pagination -->
      <div class="pagination">
        <button (click)="prevPage()" [disabled]="currentPage() === 0">‹</button>
        <span>Sayfa {{ currentPage() + 1 }}</span>
        <button (click)="nextPage()">›</button>
      </div>
    </div>
  `,
})
export class ProductListComponent implements OnInit {
  private store = inject(Store);

  // Observables (AsyncPipe ile template'de kullanılır)
  loading$ = this.store.select(selectLoading);
  error$ = this.store.select(selectError);
  cartCount$ = this.store.select(selectCartItemCount);
  products$ = this.store.select(selectAllProducts);
  searchResults$ = this.store.select(selectSearchResults);

  // Signals (Angular 17)
  currentPage = signal(0);
  selectedCategory = signal('');
  isSearchMode = signal(false);

  // Computed signal — derived state
  displayedProducts = computed(() => this.isSearchMode() ? [] : []);
  categories = signal(['Elektronik', 'Giyim', 'Kitap', 'Spor', 'Ev & Yaşam']);

  // Reactive form control for search
  searchControl = new FormControl('');

  ngOnInit(): void {
    this.loadProducts();

    // Search-as-you-type with debounce
    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(query => {
      if (query && query.length >= 2) {
        this.isSearchMode.set(true);
        this.store.dispatch(ProductActions.searchProducts({ query }));
      } else {
        this.isSearchMode.set(false);
        this.loadProducts();
      }
    });
  }

  private loadProducts(): void {
    this.store.dispatch(ProductActions.loadProducts({
      params: {
        page: this.currentPage(),
        size: 12,
        sortBy: 'price',
        category: this.selectedCategory() || undefined,
      }
    }));
  }

  setCategory(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.selectedCategory.set(value);
    this.currentPage.set(0);
    this.loadProducts();
  }

  addToCart(product: Product): void {
    this.store.dispatch(ProductActions.addToCart({ product, quantity: 1 }));
  }

  prevPage(): void {
    if (this.currentPage() > 0) {
      this.currentPage.update(p => p - 1);
      this.loadProducts();
    }
  }

  nextPage(): void {
    this.currentPage.update(p => p + 1);
    this.loadProducts();
  }
}
