# 19 — Angular UI

**Difficulty:** Intermediate (7/10) · **Angular 17** · **NgRx** · **RxJS**

Angular 17 Standalone Components, Signals, NgRx State Management ve RxJS reactive patterns.

---

## Angular 17 Yenilikleri

### Standalone Components (no NgModule)
```typescript
@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [AsyncPipe, NgFor, RouterLink],  // import direkt component'a
  template: `...`,
})
export class ProductListComponent { }

// Bootstrap (main.ts)
bootstrapApplication(AppComponent, appConfig);
```

### inject() — Constructor DI yerine
```typescript
// Eski (constructor)
constructor(private store: Store, private route: ActivatedRoute) {}

// Yeni (Angular 14+)
private store = inject(Store);
private route = inject(ActivatedRoute);
```

### Signals — Reactive Primitive
```typescript
// Signal: değer tutan reaktif konteyner
count = signal(0);

// Güncelleme
count.set(5);
count.update(v => v + 1);

// Computed — türetilmiş signal (memoized)
doubled = computed(() => this.count() * 2);

// Effect — side effect
effect(() => console.log('count changed:', this.count()));
```

### New Control Flow (@for, @if, @switch)
```html
<!-- Eski *ngFor -->
<div *ngFor="let p of products; trackBy: trackById">

<!-- Yeni @for (Angular 17) -->
@for (product of products; track product.id) {
  <div>{{ product.name }}</div>
} @empty {
  <p>Ürün bulunamadı.</p>
}

@if (loading) {
  <spinner />
} @else if (error) {
  <error-banner />
} @else {
  <content />
}
```

---

## NgRx State Management

### Store Yapısı
```
Action → Reducer → Store → Selector → Component
  ↑                              |
  └──── Effect (HTTP) ───────────┘
```

### Action
```typescript
export const loadProducts = createAction(
  '[Product List] Load Products',
  props<{ params: Partial<SearchParams> }>()
);
```

### Reducer (pure function)
```typescript
export const productReducer = createReducer(
  initialState,
  on(loadProducts, state => ({ ...state, loading: true })),
  on(loadProductsSuccess, (state, { response }) => ({
    ...state,
    loading: false,
    products: response.content,
  }))
);
```

### Selector (memoized)
```typescript
export const selectCartTotal = createSelector(
  selectCart,
  cart => cart.reduce((sum, item) => sum + item.product.price * item.quantity, 0)
);

// Component'ta kullanım
cartTotal$ = this.store.select(selectCartTotal);
// Template: {{ cartTotal$ | async | currency }}
```

### Effect (HTTP side effects)
```typescript
loadProducts$ = createEffect(() =>
  this.actions$.pipe(
    ofType(loadProducts),
    switchMap(({ params }) =>
      this.productService.getAll(params).pipe(
        map(response => loadProductsSuccess({ response })),
        catchError(error => of(loadProductsFailure({ error: error.message })))
      )
    )
  )
);
```

---

## RxJS Operators

### switchMap vs concatMap vs mergeMap vs exhaustMap
```
switchMap  → yeni değer gelince öncekini iptal (search, navigation)
concatMap  → sıralı, kayıpsız (form submit, sıralı upload)
mergeMap   → paralel (bağımsız işlemler)
exhaustMap → işlem bitene kadar yeni değerleri yoksay (tek submit)
```

### Search-as-you-type
```typescript
searchControl.valueChanges.pipe(
  debounceTime(300),          // 300ms bekle
  distinctUntilChanged(),     // aynı değer tekrar tetikleme
  switchMap(query =>          // önceki istek iptal
    this.service.search(query)
  )
).subscribe(results => ...);
```

### shareReplay — HTTP Cache
```typescript
products$ = this.http.get<Product[]>('/api/products').pipe(
  shareReplay(1)  // son değeri cache'le, yeni subscriber tekrar HTTP yapmaz
);
```

---

## Routing

### Lazy Loading
```typescript
{
  path: 'products',
  loadComponent: () => import('./product-list.component')
    .then(m => m.ProductListComponent)
}
```

### Functional Guards
```typescript
export const authGuard: CanActivateFn = (route, state) => {
  const token = localStorage.getItem('access_token');
  return token ? true : inject(Router).createUrlTree(['/login']);
};

// Route'da kullan
{ path: 'cart', canActivate: [authGuard], ... }
```

### Route Inputs (Angular 16+)
```typescript
// withComponentInputBinding() ile route params → @Input()
@Component(...)
export class ProductDetailComponent {
  @Input() id!: string;  // /products/:id → otomatik bind
}
```

---

## HTTP Interceptors (Functional)

```typescript
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('token');
  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;
  return next(authReq).pipe(
    catchError(error => {
      if (error.status === 401) inject(Router).navigate(['/login']);
      return throwError(() => error);
    })
  );
};

// app.config.ts'te kayıt
provideHttpClient(withInterceptors([authInterceptor]))
```

---

## Mülakat Soruları

**Q: Angular Signal nedir? Observable'dan farkı ve ne zaman tercih edilir?**
A: Signal (Angular 16+): Senkron reaktif primitive — anlık değer tutar, `signal(0)` ile oluştur, `count()` ile oku, `count.set(1)` ile güncelle. Angular change detection otomatik tracking yapar — `computed()` ve `effect()` bağımlı signal'lar değişince yeniden çalışır. Observable: Asenkron, lazy stream — subscribe edilene kadar çalışmaz. Operatör pipeline (map, filter, switchMap). RxJS ile güçlü. Tercih: HTTP istekleri, WebSocket, event stream → Observable. UI state, form değerleri, basit reaktivite → Signal. `toSignal(observable$)`: Observable'ı Signal'e dönüştür; Angular template'de her ikisini birlikte kullan.

**Q: RxJS `switchMap`, `concatMap`, `mergeMap`, `exhaustMap` farkları nelerdir?**
A: `switchMap`: Yeni değer gelince önceki inner Observable'ı iptal et — arama kutusu (her tuş önceki HTTP isteğini iptal eder). `concatMap`: Sıralı — önceki bitmeden yeni başlamaz. Form submit sonrası sıralı işlemler için. `mergeMap`: Paralel — hepsi aynı anda çalışır, sıra yok. Bağımsız paralel HTTP istekleri. `exhaustMap`: Önceki bitmeden yeni değeri ignore et — çift tıklama önleme, submit butonu. Hata: `switchMap` ile form submit → kullanıcı hızlı tıklarsa önceki request iptal olur, DB'ye iki kez kayıt gidebilir. Çözüm: Submit için `exhaustMap`.

**Q: NgRx neden kullanılır? Ne zaman tercih edilmez?**
A: NgRx (Redux pattern): Single source of truth (tek store), immutable state (her action yeni state üretir), Redux DevTools ile time-travel debugging, Effect ile side effect izolasyonu (HTTP, localStorage). Büyük takım: Her değişiklik action → reducer → selector zincirinden geçer, takip edilebilir. `createSelector` memoized: Girdi selector'lar değişmezse sonucu cache'den döner — büyük state'lerde performans kritik. Ne zaman tercih edilmez: Küçük/orta uygulama — fazla boilerplate (action + reducer + selector + effect = 4 dosya). Alternatif: Signal + Service (basit state), BehaviorSubject + Service (orta karmaşıklık). Kural: 5+ geliştirici + karmaşık cross-component state → NgRx; aksi halde overkill.

**Q: Angular change detection nasıl çalışır? `OnPush` ne zaman kullanılır?**
A: Default strategy: Herhangi bir event (click, HTTP, timer) sonrası tüm component tree kontrol edilir — küçük appde sorun yok, büyük appde yavaş. `ChangeDetectionStrategy.OnPush`: Component yalnızca şu durumlarda check edilir: `@Input` referansı değişti, component içinden event fırlatıldı, `async` pipe ile Observable'dan yeni değer geldi, `markForCheck()` çağrıldı. Performans: Büyük listeler için `OnPush` + immutable data → check sayısı dramatik düşer. Signal ile: Angular değişimi otomatik tracking yapar, `OnPush` ile sinerjik. `trackBy`: `*ngFor` ile liste elemanlarını identity ile takip et — tüm listeyi yeniden render etme.

**Q: Standalone component ile NgModule farkı? Angular 17'deki değişiklik?**
A: NgModule (klasik): `@NgModule` ile component, directive, pipe bildirimi. Lazy loading için `loadChildren()` → feature module. Merkezi bağımlılık yönetimi. Standalone (Angular 14+, Angular 17'de default): `@Component({ standalone: true, imports: [CommonModule, RouterModule] })` — modülsüz. Direkt `imports` ile bağımlılık. `bootstrapApplication()` ile başlatma. Lazy loading: `loadComponent(() => import('./feature.component'))`. Avantaj: Daha az boilerplate, tree-shaking daha etkili, test için `TestBed.configureTestingModule` daha küçük. Migration: `ng generate @angular/core:standalone` ile mevcut kodu otomatik dönüştür.

**Q: Angular lazy loading ve bundle optimization nasıl çalışır?**
A: Lazy loading: Route'a gidilene kadar JavaScript bundle indirilmez — ilk yükleme hızı (TTI). `loadComponent()` veya `loadChildren()` ile chunk ayrılır, Webpack/esbuild ayrı `.js` dosyası üretir. Preloading: `PreloadAllModules` — arka planda diğer route'ları önceden indir. Bundle optimization: `ng build --configuration=production` — tree-shaking (kullanılmayan kod elenir), minification, dead code elimination. `source-map-explorer` ile bundle analiz et. Standalone + OnPush + Signal kombinasyonu en küçük bundle. `@defer` (Angular 17): Template içinde component'ı lazy load — viewport'a girinceye kadar indirme (`@defer (on viewport)`).

**Q: Angular Reactive Forms ile Template-driven Forms farkı?**
A: Reactive Forms: `FormBuilder`, `FormGroup`, `FormControl` — TypeScript'te tanımlanır, test kolay, dinamik form için. `valueChanges` Observable ile anlık validasyon, debounce. `Validators.required`, custom validator. Template-driven: `[(ngModel)]` ile two-way binding — küçük formlar için hızlı, ancak test'te `@ViewChild` gerekir, dinamik form zor. Tercih: Karmaşık validasyon, dinamik alan ekleme/çıkarma, unit test → Reactive Forms. Basit login formu → Template-driven. `AbstractControl.statusChanges` + `switchMap` ile form değişince backend'e anlık gönder (otosave).

---

## Çalıştırma

```bash
# Bağımlılıkları kur
npm install

# Geliştirme sunucusu
ng serve  → http://localhost:4200

# Prod build
ng build --configuration production

# Test
ng test --no-watch --browsers=ChromeHeadless
```
