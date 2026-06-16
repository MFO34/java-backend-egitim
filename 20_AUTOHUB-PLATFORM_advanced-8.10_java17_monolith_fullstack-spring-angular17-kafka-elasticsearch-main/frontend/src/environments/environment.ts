// Geliştirme Ortamı Konfigürasyonu
// Bu dosya ng serve ile geliştirme ortamında kullanılır

export const environment = {
  // Üretim modu kapalı - debug özellikleri aktif
  production: false,

  // Backend API temel URL'i - geliştirme ortamı
  apiUrl: 'http://localhost:8080/api',

  // Uygulama adı
  appName: 'AutoHub',

  // JWT token yerel depolama anahtarları
  tokenKey: 'autohub_access_token',
  refreshTokenKey: 'autohub_refresh_token',
  userKey: 'autohub_user',

  // Sayfalama varsayılan değerleri
  defaultPageSize: 10,
  pageSizeOptions: [5, 10, 25, 50]
};
