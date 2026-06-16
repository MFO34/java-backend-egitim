# Auth Service — Spring Security + JWT + OAuth2 + Redis

Tam özellikli kimlik doğrulama ve yetkilendirme servisi.

---

## Mimari

```
┌─────────────────────────────────────────────────────────────┐
│                    HTTP İSTEĞİ GELDİ                        │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              SPRING SECURITY FILTER ZİNCİRİ                 │
│                                                             │
│  CorsFilter  →  JwtAuthFilter  →  AuthorizationFilter       │
│                                                             │
│  JwtAuthFilter adımları:                                    │
│    1. Authorization: Bearer <token> header'ı var mı?        │
│    2. JWT imzası geçerli mi?                                 │
│    3. Token Redis blacklist'te mi? (logout kontrolü)        │
│    4. UserDetailsService.loadUserByUsername(email)          │
│    5. SecurityContextHolder'a Authentication koy            │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                   CONTROLLER KATMANI                        │
│                                                             │
│  AuthController   →  /api/v1/auth/**   (herkese açık)      │
│  UserController   →  /api/v1/users/**  (JWT gerekli)       │
│  AdminController  →  /api/v1/admin/**  (ROLE_ADMIN gerekli)│
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                    SERVİS KATMANI                           │
│                                                             │
│  AuthService    →  register, login, logout, refresh         │
│  TokenService   →  Redis blacklist, Refresh token rotation  │
│  UserService    →  profil görüntüleme/güncelleme            │
│  EmailService   →  JavaMailSender (@Async HTML e-posta)     │
└──────────────────────────┬──────────────────────────────────┘
                           │
              ┌────────────┼────────────┐
              ▼            ▼            ▼
        PostgreSQL       Redis       Gmail SMTP
        (kullanıcı,    (blacklist)   (e-posta)
        token, rol)
```

---

## JWT Yapısı

```
eyJhbGciOiJIUzI1NiJ9   .   eyJzdWIiOiJ1c2VyQGV4YW1...   .   SflKxwRJSMeK...
     HEADER                        PAYLOAD                       SIGNATURE
  (Base64 encoded)              (Base64 encoded)              (HMAC-SHA256)

HEADER:  { "alg": "HS256", "typ": "JWT" }

PAYLOAD: {
  "sub":         "user@example.com",   // Subject: e-posta
  "jti":         "uuid-v4",            // JWT ID: blacklist için benzersiz kimlik
  "uid":         42,                   // Kullanıcı ID
  "roles":       ["ROLE_USER"],        // Roller (ROLE_ prefix'li)
  "permissions": ["user:read"],        // İzinler (prefix'siz)
  "type":        "access",             // Token tipi
  "iat":         1700000000,           // Oluşturulma (issued at)
  "exp":         1700000900            // Son kullanma (expiration)
}

SIGNATURE: HMAC-SHA256(base64(header) + "." + base64(payload), SECRET_KEY)
```

---

## Access Token vs Refresh Token

| Özellik         | Access Token         | Refresh Token         |
|----------------|---------------------|-----------------------|
| Süre           | 15 dakika           | 7 gün                 |
| Nerede saklanır| Client (memory/cookie)| Client + DB (PostgreSQL)|
| Ne zaman gönderilir| Her istekte      | Sadece /auth/refresh  |
| İptal          | Redis blacklist     | DB'de isUsed=true     |
| Payload        | Tüm claims          | Minimal (sub + type)  |
| Amaç           | API erişimi         | Yeni access token alma|

---

## Token Rotation (Güvenli Refresh)

```
İstek:  POST /api/v1/auth/refresh
        Body: { "refreshToken": "abc" }

Sunucu adımları:
  1. "abc" DB'de var mı?                → Hayır  → 401
  2. "abc" daha önce kullanılmış mı?   → Evet   → REUSE DETECTION!
                                                    Tüm refresh token'lar iptal
                                                    (token çalınmış olabilir)
  3. "abc" süresi dolmuş mu?           → Evet   → 401
  4. "abc" → isUsed = true             (artık kullanılamaz)
  5. Yeni access token üret
  6. Yeni refresh token üret + DB'ye kaydet

Yanıt: { "accessToken": "...", "refreshToken": "def", ... }
```

---

## OAuth2 Akışı (Google / GitHub)

```
Kullanıcı
   │
   │  1. GET /oauth2/authorize/google  (browser redirect)
   ▼
Google Login Sayfası
   │
   │  2. Kullanıcı giriş yapar → code=xxx ile geri yönlendirir
   ▼
Spring Security Callback: /login/oauth2/code/google?code=xxx
   │
   │  3. Spring: code → access token (Google API)
   │  4. Spring: access token → user info (Google API)
   ▼
CustomOAuth2UserService.loadUser()
   │
   │  5. Google attributes: { sub, email, given_name, picture, ... }
   │  6. DB'de bu e-posta var mı?
   │     → Evet: profil güncelle
   │     → Hayır: yeni kullanıcı oluştur (emailVerified=true, password=null)
   ▼
OAuth2SuccessHandler.onAuthenticationSuccess()
   │
   │  7. JWT access + refresh token üret
   │  8. Redirect: http://localhost:3000/oauth2/callback?token=xxx&refreshToken=yyy
   ▼
Frontend → token'ları sakla → API istekleri
```

---

## AuthResult Sealed Interface

```java
// Service → Controller arası tip güvenli iletişim
// HTTP kararını Controller verir, Service sadece sonucu bildirir

public sealed interface AuthResult
        permits AuthResult.Success, AuthResult.Failure, AuthResult.EmailNotVerified {

    record Success(TokenResponse tokenResponse) implements AuthResult {}

    record Failure(String errorCode, String message, int httpStatus) implements AuthResult {}

    record EmailNotVerified(String email) implements AuthResult {}
}

// Controller'da Pattern Matching for switch (Java 21):
return switch (result) {
    case AuthResult.Success s        → ResponseEntity.ok(s.tokenResponse());
    case AuthResult.EmailNotVerified → ResponseEntity.status(403)...;
    case AuthResult.Failure f        → ResponseEntity.status(f.httpStatus())...;
};
// Tüm case'ler zorunlu (sealed = exhaustive) — derleme zamanında kontrol!
```

---

## Redis Blacklist Mekanizması

```
Normal akış:
  istek → JwtAuthFilter → Redis'te JTI var mı? → Hayır → devam et

Logout sonrası:
  POST /auth/logout
    → token JTI Redis'e eklenir: SET "blacklist:jti:<uuid>" "revoked" EX <kalan_saniye>
    → TTL token süresiyle aynı (token expire olunca Redis'ten otomatik silinir)

  istek → JwtAuthFilter → Redis'te JTI var mı? → Evet → 401 Unauthorized

Neden DB değil Redis?
  Access token kısa ömürlü (15 dk) → Redis TTL ile otomatik temizlenir
  DB'de blacklist tutmak sorgu yükü yaratır
  Redis in-memory → nanosaniye erişim süresi
```

---

## Role vs Permission (Authority) Farkı

```java
// ROLE: Geniş kategori, "ROLE_" prefix'li
//   hasRole("ADMIN")  → Spring otomatik "ROLE_ADMIN" arar
//   @Secured("ROLE_ADMIN")

// AUTHORITY/PERMISSION: İnce taneli izin
//   hasAuthority("user:read") → tam eşleşme
//   @PreAuthorize("hasAuthority('admin:write')")

// User.getAuthorities() şunu döner:
//   ROLE_USER        → hasRole("USER") çalışır
//   user:read        → hasAuthority("user:read") çalışır
//   user:write       → hasAuthority("user:write") çalışır
```

---

## Güvenlik Mekanizmaları

| Mekanizma | Açıklama |
|-----------|----------|
| BCrypt (work=10) | Şifre hashleme. Her şifre için farklı salt üretir |
| JWT imzası | HMAC-SHA256. Secret key olmadan token üretilemez/değiştirilemez |
| JTI Blacklist | Logout'ta token Redis'e eklenir → geçersiz kılınır |
| Token Rotation | Her refresh'te yeni refresh token, eski geçersiz |
| Reuse Detection | Kullanılmış refresh token → tüm session'lar kapatılır |
| Email Doğrulama | LOCAL provider → emailVerified=true olmadan giriş yapılamaz |
| Hesap Kilitleme | 5 başarısız giriş → accountLocked=true → Admin açar |
| CSRF Devre Dışı | JWT stateless → CSRF korumasına gerek yok |
| CORS | Sadece izin verilen origin'ler (frontend URL'leri) |
| OAuth2 State | Spring otomatik CSRF state parametresi ekler |

---

## API Endpoint'leri

### Authentication (`/api/v1/auth`) — Herkese Açık

| Method | Endpoint | Açıklama |
|--------|----------|----------|
| POST | `/register` | Yeni kullanıcı kaydı |
| POST | `/login` | Giriş (access + refresh token döner) |
| POST | `/logout` | Çıkış (JWT gerekli) |
| POST | `/refresh` | Token yenile |
| GET | `/verify-email?token=xxx` | E-posta doğrula |
| POST | `/resend-verification?email=xxx` | Doğrulama e-postası tekrar gönder |
| POST | `/forgot-password` | Şifre sıfırlama e-postası |
| POST | `/reset-password` | Şifre sıfırla |

### Kullanıcı (`/api/v1/users`) — JWT Gerekli

| Method | Endpoint | Açıklama |
|--------|----------|----------|
| GET | `/me` | Kendi profilim |
| PUT | `/me` | Profilimi güncelle |

### Admin (`/api/v1/admin`) — ROLE_ADMIN Gerekli

| Method | Endpoint | Açıklama |
|--------|----------|----------|
| GET | `/users` | Tüm kullanıcıları listele |
| GET | `/users/{id}` | ID ile kullanıcı |
| PATCH | `/users/{id}/toggle-enabled` | Aktif/Pasif yap |
| PATCH | `/users/{id}/unlock` | Kilitli hesabı aç |

### OAuth2 — Tarayıcıdan Erişilir

| Method | Endpoint | Açıklama |
|--------|----------|----------|
| GET | `/oauth2/authorize/google` | Google ile giriş başlat |
| GET | `/oauth2/authorize/github` | GitHub ile giriş başlat |

---

## Kurulum ve Çalıştırma

### 1. Gereksinimler
- Java 21
- Docker Desktop
- Google OAuth2 / GitHub OAuth2 credentials
- Gmail uygulama şifresi (2FA etkinse)

### 2. Ortam Değişkenlerini Ayarla

```bash
cp .env.example .env
# .env dosyasını düzenle — Google/GitHub client-id/secret, mail, vb.
```

### 3. JWT Secret Üret

```bash
# 32 byte = 256 bit (minimum HS256 için)
openssl rand -base64 32
# Çıktıyı .env dosyasındaki JWT_SECRET'a yapıştır
```

### 4. Google OAuth2 Kurulumu

1. [Google Cloud Console](https://console.cloud.google.com) → APIs & Services → Credentials
2. Create Credentials → OAuth 2.0 Client IDs
3. Application type: Web application
4. Authorized redirect URIs ekle:
   ```
   http://localhost:8080/login/oauth2/code/google
   ```
5. Client ID ve Secret'ı `.env`'e koy

### 5. GitHub OAuth2 Kurulumu

1. GitHub → Settings → Developer settings → OAuth Apps → New OAuth App
2. Homepage URL: `http://localhost:8080`
3. Authorization callback URL: `http://localhost:8080/login/oauth2/code/github`
4. Client ID ve Secret'ı `.env`'e koy

### 6. Docker ile Çalıştır

```bash
docker-compose up -d

# Servisler:
#   PostgreSQL  → localhost:5432
#   Redis       → localhost:6379
#   App         → localhost:8080
#   pgAdmin     → localhost:5050
#   Redis Commander → localhost:8081
```

### 7. Sadece DB/Redis (geliştirme için)

```bash
docker-compose up -d db redis

# Uygulamayı IDE'den çalıştır (application-dev.yml kullanılır)
```

---

## Test (curl)

### Kayıt ve Giriş

```bash
# Kayıt
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Ali","lastName":"Veli","email":"ali@test.com","password":"Test1234!"}'

# Giriş
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ali@test.com","password":"Test1234!"}'
# → {"accessToken":"eyJ...","refreshToken":"eyJ...","tokenType":"Bearer","expiresIn":900000}

# TOKEN'I KAYDET
export TOKEN="eyJ..."

# Profil
curl http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer $TOKEN"

# Çıkış
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer $TOKEN"

# Token yenile
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"eyJ..."}'
```

### Admin İşlemleri

```bash
# Önce admin olarak giriş yap
export ADMIN_TOKEN="..."

# Tüm kullanıcılar
curl http://localhost:8080/api/v1/admin/users \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Hesap kilidi aç
curl -X PATCH http://localhost:8080/api/v1/admin/users/2/unlock \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

---

## Swagger UI

```
http://localhost:8080/swagger-ui/index.html
```

1. `/api/v1/auth/login` → token al
2. Sağ üstte **Authorize** → `Bearer <token>` gir
3. Korumalı endpoint'leri test et

---

## Redis Commander (Web UI)

```
http://localhost:8081
```

Blacklist key'lerini görmek için: `blacklist:jti:*` ara

---

## Proje Yapısı

```
src/main/java/com/auth/
├── AuthApplication.java           # @SpringBootApplication + @EnableAsync
├── config/
│   ├── SecurityConfig.java        # SecurityFilterChain, CORS, OAuth2
│   ├── RedisConfig.java           # StringRedisTemplate bean
│   ├── OpenApiConfig.java         # Swagger JWT desteği
│   └── DataInitializer.java       # Default rol ve admin oluşturucu
├── controller/
│   ├── AuthController.java        # /api/v1/auth/** + AuthResult Pattern Matching
│   ├── UserController.java        # /api/v1/users/me — @PreAuthorize
│   └── AdminController.java       # /api/v1/admin/** — @Secured("ROLE_ADMIN")
├── dto/
│   ├── AuthResult.java            # Sealed interface: Success | Failure | EmailNotVerified
│   ├── request/                   # RegisterRequest, LoginRequest, vb.
│   └── response/                  # TokenResponse, UserResponse, MessageResponse
├── entity/
│   ├── BaseEntity.java            # id, createdAt, updatedAt (@MappedSuperclass)
│   ├── User.java                  # implements UserDetails
│   ├── Role.java                  # @ElementCollection permissions
│   ├── Permission.java            # Enum: USER_READ/WRITE, ADMIN_READ/WRITE, vb.
│   ├── AuthProvider.java          # Enum: LOCAL, GOOGLE, GITHUB
│   ├── RefreshToken.java          # DB'de saklanan refresh token
│   ├── EmailVerificationToken.java
│   └── PasswordResetToken.java
├── exception/
│   └── GlobalExceptionHandler.java # @RestControllerAdvice
├── repository/                    # JpaRepository implementasyonları
├── security/
│   ├── JwtService.java            # Token üretme/doğrulama (jjwt 0.12.x)
│   ├── CustomUserDetailsService.java
│   ├── filter/
│   │   └── JwtAuthFilter.java     # OncePerRequestFilter
│   └── oauth2/
│       ├── CustomOAuth2User.java
│       ├── CustomOAuth2UserService.java
│       ├── OAuth2SuccessHandler.java
│       └── OAuth2FailureHandler.java
└── service/
    ├── AuthService.java           # register, login, logout, refresh, verify, reset
    ├── TokenService.java          # Redis blacklist + Refresh token rotation
    ├── UserService.java           # profil işlemleri
    └── EmailService.java          # JavaMailSender (@Async HTML e-posta)
```

---

## Öğrenilen Kavramlar

| Kavram | Nerede? |
|--------|---------|
| SecurityFilterChain (lambda DSL) | SecurityConfig.java |
| OncePerRequestFilter | JwtAuthFilter.java |
| UserDetails / UserDetailsService | User.java, CustomUserDetailsService.java |
| ROLE vs Authority (Permission) | Permission.java, User.getAuthorities() |
| @PreAuthorize / @Secured | UserController, AdminController |
| JWT (Header.Payload.Signature) | JwtService.java |
| jjwt 0.12.x API | JwtService.java |
| Token Rotation + Reuse Detection | TokenService.java |
| Redis Blacklist (TTL) | TokenService.java |
| OAuth2 Authorization Code Flow | CustomOAuth2UserService, OAuth2SuccessHandler |
| Sealed Interface + Pattern Matching | AuthResult.java, AuthController.java |
| @Async (JavaMailSender) | EmailService.java |
| BCrypt PasswordEncoder | SecurityConfig.java |
| CORS / CSRF konfigürasyonu | SecurityConfig.java |
| ApplicationRunner (veri başlatıcı) | DataInitializer.java |
| Virtual Threads (Java 21) | application.yml: spring.threads.virtual.enabled |

---

## Mülakat Soruları

**Q: Spring Security filter chain nasıl çalışır?**
A: Her HTTP isteği bir filtre zincirinden sırayla geçer. `CorsFilter → JwtAuthFilter → UsernamePasswordAuthenticationFilter → AuthorizationFilter → ...`. JwtAuthFilter (OncePerRequestFilter) isteği inceleyerek JWT varsa doğrular ve `SecurityContextHolder`'a `UsernamePasswordAuthenticationToken` koyar. AuthorizationFilter bu context'i okuyarak `hasRole("ADMIN")` gibi kuralları uygular. Filtre zinciri `SecurityFilterChain` bean'ı ile yapılandırılır.

**Q: OncePerRequestFilter neden kullanılır?**
A: Bazı filter'lar bir istek için birden fazla kez çalışabilir (Spring MVC forward/include). `OncePerRequestFilter` bunu önler — her istek için tam olarak bir kez çalışacağını garanti eder. JWT doğrulamasını her istekte sadece bir kez yapmak gerekir. `doFilterInternal()` metodunu override ederek implementasyon yapılır.

**Q: JWT stateless mimaride refresh token neden DB'de saklanır?**
A: Access token (kısa ömürlü, 15dk) stateless — doğrulama için DB gerekmez. Refresh token (uzun ömürlü, 7gün) DB'de saklanır çünkü: (1) Kullanıcı logout olduğunda refresh token geçersiz kılınabilsin. (2) Token rotation: her kullanımda yeni token verilir, eskisi silinir. (3) Reuse detection: eski token tekrar kullanılırsa (token theft) — hepsini sil ve kullanıcıyı uyar.

**Q: ROLE vs Authority (Permission/GrantedAuthority) farkı?**
A: `ROLE_ADMIN` gibi roller geniş yetkiler grubu. `user:read`, `admin:write` gibi izinler (authority) ince taneli yetkiler. `hasRole("ADMIN")` → `ROLE_ADMIN` prefix'i ekler ve kontrol eder. `hasAuthority("user:read")` → tam string eşleşmesi. `@PreAuthorize("hasRole('ADMIN') or hasAuthority('user:manage')")` — kombinasyon mümkün. `User.getAuthorities()` hem rolleri (`ROLE_` prefix ile) hem izinleri döner.

**Q: OAuth2 Authorization Code Flow nasıl çalışır?**
A: 1. Kullanıcı Google ile giriş yapar → Spring `oauth2Login` Google'a yönlendirir. 2. Kullanıcı Google'da onaylar → Google callback URL'e `code` parametresi ile döner. 3. Spring arka planda Google'a bu `code` ile access token ister. 4. `CustomOAuth2UserService.loadUser()` çağrılır — Google'dan kullanıcı bilgisi alınır. 5. `OAuth2SuccessHandler`: Kullanıcı DB'de yoksa kaydet, JWT token üret, frontend'e redirect. Tüm sır değişimi arka planda gerçekleşir — kullanıcı sadece giriş+onay sayfasını görür.

**Q: Token Rotation nedir? Neden gerekli?**
A: Her refresh token kullanıldığında yeni bir refresh token verilir, eskisi silinir. Amaç: Refresh token çalınsa bile saldırgan onu kullanamaz — ilk meşru kullanım eski tokeni geçersiz kılar. Reuse detection: Zaten silinmiş token tekrar kullanılırsa (yani biri çaldı ve kullandı) — tüm session'lar sonlandırılır ve şüpheli aktivite loglanır. Bu `TokenService`'de gerçekleşir.

**Q: Redis'te JWT blacklist nasıl çalışır?**
A: Logout olunca access token'ın JTI (JWT ID) Redis'e yazılır: `SET jti:{jtiValue} "revoked" EX {remainingSeconds}`. `JwtAuthFilter`'da her istekte: (1) JWT imzası doğrula. (2) Redis'te JTI var mı kontrol et. (3) Varsa → 401 Unauthorized. TTL = token'ın kalan geçerlilik süresi → otomatik temizlenir. Avantaj: Stateless JWT'ye rağmen logout efektif olur.

**Q: @Async e-posta gönderimi neden önemli?**
A: Kayıt olunca doğrulama e-postası gönderilir. E-posta sunucusu yavaş veya hata verirse kullanıcı bunu beklemesin. `@Async` ile e-posta gönderimi ayrı thread'de çalışır, HTTP response anında döner. `@EnableAsync` ile etkinleştirilir. E-posta hatası → kullanıcı kaydını etkilemez (transaction dışı çalışır). Hata durumunda retry için Kafka ile async queue kullanılabilir.

**Q: sealed interface AuthResult neden enum'dan daha iyi?**
A: `enum` değer taşıyamaz (her durum aynı tip). `sealed interface AuthResult permits Success, Failure, EmailNotVerified` — her durum farklı veri taşır: `Success(String accessToken, String refreshToken)`, `Failure(String message, int statusCode)`, `EmailNotVerified(String email)`. `switch` ile pattern matching: `case Success s → return s.accessToken()`. Compile-time exhaustiveness — tüm durumlar ele alınmazsa hata. Tip güvenli domain modeling.

**Q: BCrypt neden MD5 veya SHA-256'dan daha iyi şifre hash'leme algoritmasıdır?**
A: MD5/SHA-256: hız için tasarlanmış — GPU ile saniyede milyarlarca hash hesaplanabilir (brute force riski). BCrypt: kasıtlı yavaş (work factor 10 → ~100ms) — brute force saldırıyı milyonlarca kat yavaşlatır. Her seferinde farklı salt üretir — rainbow table saldırıları işe yaramaz. Aynı şifre → farklı hash. `BCryptPasswordEncoder.matches(rawPassword, storedHash)` hem hash hem salt kontrolü yapar.
