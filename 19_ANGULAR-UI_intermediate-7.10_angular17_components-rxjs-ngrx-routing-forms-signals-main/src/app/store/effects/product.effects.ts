import { Injectable, inject } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { catchError, debounceTime, distinctUntilChanged, map, of, switchMap } from 'rxjs';
import { ProductService } from '../../services/product.service';
import * as ProductActions from '../actions/product.actions';

/**
 * NgRx Effects — side effects (HTTP calls) dışarı taşınır
 *
 * Pattern: action → effect (HTTP) → success/failure action
 * switchMap: yeni action gelince önceki HTTP isteği iptal edilir (autocomplete gibi)
 * exhaustMap: işlem bitene kadar yeni action'ları yoksay (submit button)
 * concatMap: sıralı, kayıpsız (cart update)
 * mergeMap: paralel (bağımsız işlemler)
 */
@Injectable()
export class ProductEffects {
  private actions$ = inject(Actions);
  private productService = inject(ProductService);

  // switchMap: sayfa değişince önceki istek iptal
  loadProducts$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ProductActions.loadProducts),
      switchMap(({ params }) =>
        this.productService.getAll(params).pipe(
          map(response => ProductActions.loadProductsSuccess({ response })),
          catchError(error =>
            of(ProductActions.loadProductsFailure({ error: error.message }))
          )
        )
      )
    )
  );

  loadProduct$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ProductActions.loadProduct),
      switchMap(({ id }) =>
        this.productService.getById(id).pipe(
          map(product => ProductActions.loadProductSuccess({ product })),
          catchError(error =>
            of(ProductActions.loadProductFailure({ error: error.message }))
          )
        )
      )
    )
  );

  // debounceTime + distinctUntilChanged → search-as-you-type
  searchProducts$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ProductActions.searchProducts),
      debounceTime(300),              // 300ms bekle — gereksiz istek önle
      distinctUntilChanged(           // aynı query tekrar istek yapma
        (prev, curr) => prev.query === curr.query
      ),
      switchMap(({ query }) =>
        this.productService.search(query).pipe(
          map(products => ProductActions.searchProductsSuccess({ products })),
          catchError(error =>
            of(ProductActions.searchProductsFailure({ error: error.message }))
          )
        )
      )
    )
  );
}
