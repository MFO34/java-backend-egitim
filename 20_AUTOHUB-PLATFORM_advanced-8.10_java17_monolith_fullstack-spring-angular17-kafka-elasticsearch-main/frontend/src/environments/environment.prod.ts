// Üretim Ortamı Konfigürasyonu
// ng build --configuration production ile derlenir

export const environment = {
  production: true,                     // Üretim modu - optimizasyonlar aktif
  apiUrl: 'https://api.autohub.com/api', // Gerçek API sunucusu
  appName: 'AutoHub',
  tokenKey: 'autohub_access_token',
  refreshTokenKey: 'autohub_refresh_token',
  userKey: 'autohub_user',
  defaultPageSize: 10,
  pageSizeOptions: [5, 10, 25, 50]
};
