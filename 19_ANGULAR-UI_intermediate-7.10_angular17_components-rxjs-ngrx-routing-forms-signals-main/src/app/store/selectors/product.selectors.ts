import { createFeatureSelector, createSelector } from '@ngrx/store';
import { ProductState } from '../reducers/product.reducer';

export const selectProductState = createFeatureSelector<ProductState>('products');

export const selectAllProducts = createSelector(
  selectProductState,
  state => state.products
);

export const selectSelectedProduct = createSelector(
  selectProductState,
  state => state.selectedProduct
);

export const selectSearchResults = createSelector(
  selectProductState,
  state => state.searchResults
);

export const selectCart = createSelector(
  selectProductState,
  state => state.cart
);

export const selectCartItemCount = createSelector(
  selectCart,
  cart => cart.reduce((sum, item) => sum + item.quantity, 0)
);

export const selectCartTotal = createSelector(
  selectCart,
  cart => cart.reduce((sum, item) => sum + item.product.price * item.quantity, 0)
);

export const selectLoading = createSelector(
  selectProductState,
  state => state.loading
);

export const selectError = createSelector(
  selectProductState,
  state => state.error
);

export const selectPagination = createSelector(
  selectProductState,
  state => state.pagination
);
