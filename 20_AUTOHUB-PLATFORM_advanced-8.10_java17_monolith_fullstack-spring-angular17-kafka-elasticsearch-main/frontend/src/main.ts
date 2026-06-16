// Angular uygulamasının başlangıç noktası
// Bu dosya tarayıcı tarafından yüklenen ilk dosyadır

import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import { AppModule } from './app/app.module';

// AppModule'ü tarayıcıda çalıştır
// JIT (Just-In-Time) derleme - geliştirme ortamı için
platformBrowserDynamic()
  .bootstrapModule(AppModule)
  .catch(err => console.error('Uygulama başlatma hatası:', err));
