import { createAction, props } from '@ngrx/store';
import { PaginatedResponse, Product, SearchParams } from '../../models/product.model';

// Load products
export const loadProducts = createAction(
  '[Product List] Load Products',
  props<{ params: Partial<SearchParams> }>()
);
export const loadProductsSuccess = createAction(
  '[Product API] Load Products Success',
  props<{ response: PaginatedResponse<Product> }>()
);
export const loadProductsFailure = createAction(
  '[Product API] Load Products Failure',
  props<{ error: string }>()
);

// Load single product
export const loadProduct = createAction(
  '[Product Detail] Load Product',
  props<{ id: string }>()
);
export const loadProductSuccess = createAction(
  '[Product API] Load Product Success',
  props<{ product: Product }>()
);
export const loadProductFailure = createAction(
  '[Product API] Load Product Failure',
  props<{ error: string }>()
);

// Search
export const searchProducts = createAction(
  '[Search] Search Products',
  props<{ query: string }>()
);
export const searchProductsSuccess = createAction(
  '[Search API] Search Products Success',
  props<{ products: Product[] }>()
);
export const searchProductsFailure = createAction(
  '[Search API] Search Products Failure',
  props<{ error: string }>()
);

// Cart actions
export const addToCart = createAction(
  '[Cart] Add To Cart',
  props<{ product: Product; quantity: number }>()
);
export const removeFromCart = createAction(
  '[Cart] Remove From Cart',
  props<{ productId: string }>()
);
export const updateQuantity = createAction(
  '[Cart] Update Quantity',
  props<{ productId: string; quantity: number }>()
);
export const clearCart = createAction('[Cart] Clear Cart');
