// Kimlik Doğrulama Model Tanımlamaları
// JWT token yönetimi ve kullanıcı bilgileri için

/**
 * Giriş isteği modeli
 */
export interface LoginRequest {
  username: string;     // Kullanıcı adı
  password: string;     // Şifre (güvenlik: loglarda görünmemeli)
}

/**
 * Kayıt isteği modeli
 */
export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
  firstName?: string;
  lastName?: string;
}

/**
 * Kimlik doğrulama yanıtı - başarılı giriş sonrası döner
 * Backend AuthResponse ile eşleşir
 */
export interface AuthResponse {
  accessToken: string;            // JWT access token
  refreshToken: string;           // JWT refresh token
  tokenType: string;              // "Bearer"
  expiresIn: number;              // Token ömrü (saniye)
  username: string;               // Kullanıcı adı
  email: string;                  // E-posta
  fullName: string;               // Tam ad
  roles: string[];                // Kullanıcı rolleri
}

/**
 * Mevcut kullanıcı bilgileri - SessionStorage'da saklanır
 */
export interface CurrentUser {
  username: string;
  email: string;
  fullName: string;
  roles: string[];
  isAdmin: boolean;               // Computed: ADMIN rolü var mı?
  isManager: boolean;             // Computed: MANAGER rolü var mı?
  isEmployee: boolean;            // Computed: EMPLOYEE rolü var mı?
}
