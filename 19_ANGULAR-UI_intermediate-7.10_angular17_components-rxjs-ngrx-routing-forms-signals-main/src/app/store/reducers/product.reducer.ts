/**
 * NgRx Reducer — Pure Function + Immutable State
 *
 * Reducer kuralları:
 *   1. Pure function: aynı (state, action) → her zaman aynı yeni state
 *   2. Immutable: state'i asla mutate etme — her zaman yeni nesne döndür
 *   3. Senkron: side effect yok (HTTP, localStorage vs. → Effect'te)
 *
 * Immutability neden önemli:
 *   - Redux DevTools time-travel: her action önceki state'i korur
 *   - OnPush change detection: referans değişirse Angular yeniden render eder
 *   - state.products.push(x) → referans aynı kalır, Angular değişim algılamaz!
 *
 * {...state, products: [...]} = spread operator ile yeni nesne:
 *   - state'in tüm alanlarını kopyala
 *   - sadece belirtilen alanları değiştir
 *   - orijinal state değişmez (immutable update)
 */

import { createReducer, on } from '@ngrx/store';
import { CartItem, PaginatedResponse, Product } from '../../models/product.model';
import * as ProductActions from '../actions/product.actions';

// State tipi — store'da tutulan tüm alan tanımları
export interface ProductState {
  products: Product[];
  selectedProduct: Product | null;
  searchResults: Product[];
  pagination: Omit<PaginatedResponse<Product>, 'content'> | null;
  cart: CartItem[];
  loading: boolean;   // HTTP isteği devam ediyor → spinner göster
  error: string | null;  // son hata mesajı → kullanıcıya göster
}

// initialState: uygulama ilk açıldığında store'un başlangıç değeri
export const initialState: ProductState = {
  products: [],
  selectedProduct: null,
  searchResults: [],
  pagination: null,
  cart: [],
  loading: false,
  error: null,
};

export const productReducer = createReducer(
  initialState,

  // ── Ürün listesi yükleme ──────────────────────────────────────────────────
  // Action dispatch edilince: loading=true, error temizle (önceki hatayı sil)
  on(ProductActions.loadProducts, state => ({ ...state, loading: true, error: null })),

  // HTTP başarılı: products güncelle, loading kapat, pagination sakla
  on(ProductActions.loadProductsSuccess, (state, { response }) => ({
    ...state,
    loading: false,
    products: response.content,           // sayfa içeriği
    pagination: {                          // sayfalama meta bilgisi (content hariç)
      totalElements: response.totalElements,
      totalPages: response.totalPages,
      size: response.size,
      number: response.number,
    },
  })),

  // HTTP hata: loading kapat, hata mesajını store'a yaz
  on(ProductActions.loadProductsFailure, (state, { error }) => ({
    ...state, loading: false, error,
  })),

  // ── Tek ürün yükleme ──────────────────────────────────────────────────────
  on(ProductActions.loadProduct, state => ({ ...state, loading: true })),
  on(ProductActions.loadProductSuccess, (state, { product }) => ({
    ...state, loading: false, selectedProduct: product,
  })),
  on(ProductActions.loadProductFailure, (state, { error }) => ({
    ...state, loading: false, error,
  })),

  // ── Arama ─────────────────────────────────────────────────────────────────
  on(ProductActions.searchProducts, state => ({ ...state, loading: true })),
  on(ProductActions.searchProductsSuccess, (state, { products }) => ({
    ...state, loading: false, searchResults: products,
  })),
  on(ProductActions.searchProductsFailure, (state, { error }) => ({
    ...state, loading: false, error,
  })),

  // ── Sepet işlemleri ───────────────────────────────────────────────────────
  // addToCart: ürün zaten sepette mi? → miktarı artır; yoksa yeni kayıt ekle
  // .map() ve [...array] ile immutable güncelleme — state.cart.push() YASAK
  on(ProductActions.addToCart, (state, { product, quantity }) => {
    const existing = state.cart.find(i => i.product.id === product.id);
    const cart = existing
      ? state.cart.map(i =>                       // mevcut elemanı yeni nesneyle değiştir
          i.product.id === product.id
            ? { ...i, quantity: i.quantity + quantity }  // immutable update
            : i
        )
      : [...state.cart, { product, quantity }];   // yeni eleman ekle (spread ile kopya)
    return { ...state, cart };
  }),

  // .filter() → eleman çıkarma (immutable, orijinal dizi değişmez)
  on(ProductActions.removeFromCart, (state, { productId }) => ({
    ...state,
    cart: state.cart.filter(i => i.product.id !== productId),
  })),

  // .map() → belirli elemanı güncelle, diğerlerini olduğu gibi bırak
  on(ProductActions.updateQuantity, (state, { productId, quantity }) => ({
    ...state,
    cart: state.cart.map(i =>
      i.product.id === productId ? { ...i, quantity } : i
    ),
  })),

  // Sepeti temizle — boş dizi yeni referans (OnPush change detection tetiklenir)
  on(ProductActions.clearCart, state => ({ ...state, cart: [] }))
);
