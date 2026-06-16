# Docker İleri Seviye — Containerization

Java 21 + Spring Boot 3.3 + Nginx + Redis + PostgreSQL ile production-ready Docker altyapısı.

---

## İçindekiler

1. [Docker Nedir? VM'den Farkı](#1-docker-nedir-vmden-farkı)
2. [Temel Kavramlar](#2-temel-kavramlar)
3. [Dockerfile Direktifleri](#3-dockerfile-direktifleri)
4. [Multi-Stage Build](#4-multi-stage-build)
5. [Docker Compose](#5-docker-compose)
6. [Docker Network](#6-docker-network)
7. [Docker Volume](#7-docker-volume)
8. [Nginx Reverse Proxy & Load Balancing](#8-nginx-reverse-proxy--load-balancing)
9. [Güvenlik Best Practices](#9-güvenlik-best-practices)
10. [Development vs Production](#10-development-vs-production)
11. [Makefile Komutları](#11-makefile-komutları)
12. [Docker Komutları Cheat Sheet](#12-docker-komutları-cheat-sheet)
13. [Volume Yedekleme & Geri Yükleme](#13-volume-yedekleme--geri-yükleme)
14. [Hızlı Başlangıç](#14-hızlı-başlangıç)
15. [Proje Yapısı](#15-proje-yapısı)

---

## 1. Docker Nedir? VM'den Farkı

### Geleneksel VM (Sanal Makine) Mimarisi

```
┌─────────────────────────────────┐
│         Uygulama A              │
│         Uygulama B              │
│   Guest OS (Ubuntu 22.04)       │  ← Her VM'de tam işletim sistemi!
│─────────────────────────────────│
│         Hypervisor              │  ← VMware, VirtualBox, Hyper-V
│─────────────────────────────────│
│      Host OS (Windows/Linux)    │
│─────────────────────────────────│
│      Fiziksel Donanım           │
└─────────────────────────────────┘
```

### Docker Container Mimarisi

```
┌───────────────────────────────────────────────────┐
│ Container A      │ Container B     │ Container C  │
│ (Spring Boot)    │ (PostgreSQL)    │ (Redis)      │
│  Uygulama        │  Uygulama       │  Uygulama    │
│  Libs            │  Libs           │  Libs        │
│─────────────────────────────────────────────────  │
│              Docker Engine (Daemon)               │  ← Paylaşılan kernel
│─────────────────────────────────────────────────  │
│              Host OS (Linux Kernel)               │  ← TEK kernel
│─────────────────────────────────────────────────  │
│              Fiziksel Donanım                     │
└───────────────────────────────────────────────────┘
```

### Karşılaştırma Tablosu

| Özellik | VM | Docker Container |
|---------|-----|-----------------|
| Başlangıç süresi | 30s - 2dk | < 1 saniye |
| Boyut | 1-20 GB | 5-500 MB |
| RAM kullanımı | Yüksek (Guest OS dahil) | Düşük (paylaşılan kernel) |
| İzolasyon | Tam (donanım seviyesi) | Süreç seviyesi |
| Taşınabilirlik | Orta | Çok yüksek |
| Kaynak verimliliği | Düşük | Yüksek |
| Güvenlik | Çok güçlü | Güçlü (yapılandırmaya bağlı) |

**Kısaca**: Docker, uygulamayı **işletim sistemi değil** container olarak paketler. Kernel paylaşılır → çok daha hafif.

---

## 2. Temel Kavramlar

### Image (İmaj)

Uygulamanın çalışması için gereken her şeyi içeren **salt okunur şablon**.

```
Dockerfile → docker build → Image → docker run → Container
```

- `FROM eclipse-temurin:21-jre-alpine` → Base image katmanı
- `RUN mvn package` → Yeni katman ekler
- Her direktif yeni bir **layer** oluşturur
- Değişmeyen katmanlar cache'lenir → hızlı build

### Container (Konteyner)

Image'ın **çalışan örneği**. Bir image'dan sınırsız container çalıştırılabilir.

```bash
# Aynı image'dan 3 container
docker run -d --name app1 myapp:1.0
docker run -d --name app2 myapp:1.0
docker run -d --name app3 myapp:1.0
```

### Volume (Depolama Birimi)

Container'ın **kalıcı veri** deposu. Container silinse bile volume korunur.

```
Container (geçici) + Volume (kalıcı) = Kalıcı veri
```

### Network (Ağ)

Container'ların birbiriyle iletişim kurduğu **sanal ağ**.

```
app1 ──→ Docker Network (bridge) ──→ db (container adı = hostname)
```

---

## 3. Dockerfile Direktifleri

| Direktif | Açıklama | Örnek |
|----------|----------|-------|
| `FROM` | Temel imajı belirtir | `FROM eclipse-temurin:21-jre-alpine` |
| `WORKDIR` | Çalışma dizinini ayarlar | `WORKDIR /app` |
| `COPY` | Host'tan imaja dosya kopyalar | `COPY pom.xml .` |
| `ADD` | COPY gibi + URL ve tar.gz desteği | `ADD https://... /app/` |
| `RUN` | Build sırasında komut çalıştırır | `RUN mvn package -DskipTests` |
| `ENV` | Kalıcı ortam değişkeni | `ENV JAVA_OPTS="-Xmx512m"` |
| `ARG` | Sadece build zamanı değişkeni | `ARG BUILD_PROFILE=default` |
| `EXPOSE` | Port belgeleme (zorunlu değil) | `EXPOSE 8080` |
| `CMD` | Varsayılan başlangıç komutu | `CMD ["java", "-jar", "app.jar"]` |
| `ENTRYPOINT` | Sabit başlangıç komutu | `ENTRYPOINT ["java"]` |
| `USER` | Kullanıcı değiştir | `USER appuser` |
| `HEALTHCHECK` | Sağlık kontrol komutu | `HEALTHCHECK CMD wget -q ...` |
| `LABEL` | Meta bilgi ekler | `LABEL version="1.0"` |
| `VOLUME` | Volume bağlama noktası belirtir | `VOLUME /data` |

### COPY vs ADD

```dockerfile
# COPY: Basit dosya kopyalama (tercih edilir)
COPY pom.xml .
COPY src ./src

# ADD: Özel özellikler - sadece gerektiğinde kullan
ADD https://example.com/file.jar /app/   # URL'den indirme
ADD archive.tar.gz /app/                 # tar.gz otomatik açma
```

### CMD vs ENTRYPOINT

```dockerfile
# Sadece CMD - docker run image <komut> ile override edilebilir
CMD ["java", "-jar", "app.jar"]

# Sadece ENTRYPOINT - override etmek için --entrypoint gerekir
ENTRYPOINT ["java", "-jar", "app.jar"]

# İkisi birden - CMD, ENTRYPOINT'e parametre verir
ENTRYPOINT ["java"]
CMD ["-jar", "app.jar"]   # docker run image -jar farklı.jar ile override

# Shell form (sinyal yönetimi sorunu var!)
CMD java -jar app.jar     # /bin/sh -c üzerinden çalışır, SIGTERM almaz

# Exec form (önerilen)
CMD ["java", "-jar", "app.jar"]   # Direkt çalışır, SIGTERM alır
```

### ARG vs ENV

```dockerfile
# ARG: Sadece build zamanında var
ARG BUILD_PROFILE=default
# docker build --build-arg BUILD_PROFILE=prod .
RUN mvn package -P${BUILD_PROFILE}
# Container çalışırken ARG değişkeni ARTIK YOK

# ENV: Build + çalışma zamanında var
ENV JAVA_OPTS="-Xmx512m"
# docker run -e JAVA_OPTS="-Xmx1g" ile override edilebilir
# Container çalışırken hala mevcut
```

---

## 4. Multi-Stage Build

### Neden Gerekli?

```
Aşama 1 (Build):  JDK 21 + Maven + Kaynak Kod → ~700-900 MB
Aşama 2 (Runtime): JRE 21 + JAR dosyası       → ~180-250 MB
                                                  ──────────
                              Kazanım            → ~500-700 MB
```

### Nasıl Çalışır?

```dockerfile
# ── AŞAMA 1: BUILD ──────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /build

# Layer Cache Optimizasyonu:
COPY pom.xml .                          # 1. Sadece pom.xml (nadiren değişir)
RUN mvn dependency:go-offline -B        # 2. Bağımlılıkları indir (cache'lenir!)
COPY src ./src                          # 3. Kaynak kodu (sık değişir)
RUN mvn package -DskipTests -B          # 4. Derle

# ── AŞAMA 2: RUNTIME ─────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime
# Build aşamasından SADECE JAR dosyasını al
# JDK, Maven, kaynak kodu BURAYA GELMEz
COPY --from=build /build/target/*.jar app.jar
```

### İmaj Boyutu Karşılaştırması

| İmaj | Boyut | İçerik |
|------|-------|--------|
| `eclipse-temurin:21-jdk` | ~400 MB | JDK + Debian |
| `eclipse-temurin:21-jdk-alpine` | ~200 MB | JDK + Alpine |
| `eclipse-temurin:21-jre` | ~230 MB | JRE + Debian |
| `eclipse-temurin:21-jre-alpine` | ~100 MB | JRE + Alpine |
| **Multi-stage sonuç** | **~150-200 MB** | **JRE + Alpine + JAR** |

---

## 5. Docker Compose

### Yapı

```yaml
version: '3.9'      # Compose dosya sürümü

services:           # Servisler (container'lar)
  app:
    image: myapp
    ports: ["8080:8080"]

  db:
    image: postgres:16

networks:           # Sanal ağlar
  app-network:
    driver: bridge

volumes:            # Kalıcı depolama
  pgdata:
    driver: local
```

### depends_on ve Sıralı Başlatma

```yaml
services:
  app:
    depends_on:
      db:
        condition: service_healthy   # DB sağlıklı olana kadar bekle
      redis:
        condition: service_healthy

  db:
    image: postgres:16
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5
```

### Named Volume vs Bind Mount

```yaml
volumes:
  pgdata:    # Named Volume tanımı
    driver: local

services:
  db:
    volumes:
      # Named Volume: Docker tarafından yönetilir
      # Konum: /var/lib/docker/volumes/pgdata/_data
      - pgdata:/var/lib/postgresql/data

  app:
    volumes:
      # Bind Mount: Host dizinini direkt bağla
      # Geliştirmede hot reload için kullanılır
      - ./app/src:/build/src:ro    # :ro = salt okunur
      - ./config:/app/config       # :rw = okuma/yazma (varsayılan)
```

| Özellik | Named Volume | Bind Mount |
|---------|-------------|------------|
| Konumu | Docker yönetir | Host dizini |
| Taşınabilirlik | Yüksek | Düşük |
| Kullanım amacı | Üretim verisi | Geliştirme |
| Yedekleme | `docker volume` ile | Dosya sistemi ile |

### Profiles

```yaml
services:
  pgadmin:
    profiles: ["tools"]    # Sadece "tools" profili aktifken çalışır

  redis-commander:
    profiles: ["tools"]
```

```bash
# Araçlarla başlat
docker compose --profile tools up

# Araçsız başlat (varsayılan)
docker compose up
```

### Resource Limits

```yaml
services:
  app:
    deploy:
      resources:
        limits:
          cpus: '0.5'      # Maksimum 0.5 CPU çekirdeği
          memory: 512M     # Maksimum 512MB RAM
        reservations:
          cpus: '0.25'     # Garantili 0.25 CPU
          memory: 256M     # Garantili 256MB RAM
```

---

## 6. Docker Network

### Network Türleri

| Tip | Açıklama | Kullanım |
|-----|----------|----------|
| `bridge` | İzole sanal ağ (varsayılan) | Tek host, container-container iletişim |
| `host` | Container, host ağını kullanır | Maksimum performans, izolasyon yok |
| `overlay` | Çoklu host ağı | Docker Swarm, dağıtık sistemler |
| `none` | Ağ yok | Tamamen izole container |
| `macvlan` | MAC adresi atar | Legacy uygulamalar |

### Container'lar Arası İletişim

```yaml
networks:
  app-network:
    driver: bridge
    ipam:
      config:
        - subnet: "172.20.0.0/16"  # Özel subnet

services:
  app:
    networks: [app-network]
  db:
    networks: [app-network]
```

```java
// Spring Boot'ta container adı = hostname!
// DB_HOST=db → jdbc:postgresql://db:5432/dockerdb
// REDIS_HOST=redis → redis://redis:6379
```

```bash
# Container içinden DNS testi
docker exec docker-demo-app1 ping db        # db container'ına ping
docker exec docker-demo-app1 nslookup db    # DNS çözümle
docker exec docker-demo-app1 wget -qO- http://redis:6379  # Redis'e HTTP
```

---

## 7. Docker Volume

### Volume Backup

```bash
# Yöntem 1: pg_dump (bu projede kullanılan)
./scripts/backup.sh
# → backups/db_yedek_2024-01-15_10-00-00.sql.gz

# Yöntem 2: Volume tar arşivi
docker run --rm \
  -v docker-advanced-pgdata:/data \       # Volume'ü bağla
  -v $(pwd)/backups:/backup \             # Yedek dizinini bağla
  alpine \
  tar czf /backup/volume-backup.tar.gz -C /data .
```

### Volume Restore

```bash
# Yöntem 1: psql (bu projede kullanılan)
./scripts/restore.sh backups/db_yedek_2024-01-15_10-00-00.sql.gz

# Yöntem 2: Volume tar geri yükleme
docker run --rm \
  -v docker-advanced-pgdata:/data \
  -v $(pwd)/backups:/backup \
  alpine \
  sh -c "rm -rf /data/* && tar xzf /backup/volume-backup.tar.gz -C /data"
```

---

## 8. Nginx Reverse Proxy & Load Balancing

### Reverse Proxy Nedir?

```
İstemci → Nginx (80/443) → Spring Boot (8080)
           ↑
      Reverse Proxy
      - SSL sonlandırma
      - Rate limiting
      - Güvenlik başlıkları
      - Gzip sıkıştırma
      - Load balancing
```

### Load Balancing Algoritmaları

```nginx
upstream spring_app {
    # 1. Round Robin (varsayılan) - sırayla dağıt
    server app1:8080;
    server app2:8080;

    # 2. Least Connections - en az bağlantılıya gönder
    least_conn;
    server app1:8080;
    server app2:8080;

    # 3. IP Hash - aynı IP'yi aynı sunucuya gönder
    ip_hash;
    server app1:8080;
    server app2:8080;

    # 4. Ağırlıklı Round Robin
    server app1:8080 weight=3;   # Trafiğin %75'i
    server app2:8080 weight=1;   # Trafiğin %25'i
}
```

### Load Balancing Testi

```bash
# 10 istek at - hangi instance karşıladı?
make load-test

# Veya manuel:
for i in {1..10}; do
  curl -s http://localhost:80/instance | python3 -c \
    "import sys,json; d=json.load(sys.stdin); print(d['instanceAdi'])"
done
# Çıktı: app1, app2, app1, app2, app1, app2, ...  ← Round Robin çalışıyor!
```

### SSL Termination

```
İstemci ←→ HTTPS ←→ Nginx ←→ HTTP ←→ Spring Boot
              ↑
         SSL burada biter
    (Şifreleme/çözme yükü Nginx'te)
    Backend düz HTTP alır → Basit
```

---

## 9. Güvenlik Best Practices

### Non-Root User

```dockerfile
# Kötü: Root ile çalışma (güvenlik açığı!)
# Container'dan kaçılırsa host root erişimi

# İyi: Dedicated non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser   # Artık root değil
```

### Minimal Base Image

```dockerfile
# Kötü: Büyük, saldırı yüzeyi geniş
FROM ubuntu:22.04

# Daha iyi: Slim
FROM eclipse-temurin:21-jre

# En iyi: Alpine (minimal, hardened)
FROM eclipse-temurin:21-jre-alpine
# Alpine = musl libc, BusyBox, ~5MB base
```

### Secrets Yönetimi

```yaml
# Kötü: Şifre hardcode
environment:
  - DB_PASSWORD=supersecret123

# İyi: .env dosyasından oku (.env → .gitignore)
environment:
  - DB_PASSWORD=${DB_PASSWORD}

# En iyi: Docker Secrets (Swarm)
secrets:
  db_password:
    file: ./secrets/db_password.txt

services:
  db:
    secrets: [db_password]
```

### Port Güvenliği

```yaml
# Kötü: Veritabanı portunu dışarıya aç
ports:
  - "5432:5432"   # Herkes erişebilir!

# İyi: Sadece iç ağda görünür (port mapping yok)
# Sadece expose: veya hiç port tanımlama
# Docker ağı üzerinden erişilir
```

---

## 10. Development vs Production

| Özellik | Development | Production |
|---------|------------|------------|
| Spring profili | `dev` | `prod` |
| SQL log | Açık | Kapalı |
| Log seviyesi | DEBUG | WARN/ERROR |
| Debug port | 5005 açık | Kapalı |
| DB portu dışarı | Açık (IDE için) | Kapalı |
| Redis portu dışarı | Açık | Kapalı |
| Bind mount | Var (hot reload) | Yok |
| JVM heap | 128-256MB | 512MB-1GB |
| Restart policy | `unless-stopped` | `always` |
| Araçlar | Otomatik açık | Profile ile |
| SSL | Yok | Var |

---

## 11. Makefile Komutları

```bash
make up           # Servisleri başlat
make down         # Servisleri durdur
make dev          # Geliştirme ortamı başlat
make prod         # Üretim ortamı başlat
make build        # İmajları oluştur
make rebuild      # Cache'siz yeniden oluştur
make logs         # Tüm loglar
make logs-app     # Uygulama logları
make logs-nginx   # Nginx logları
make ps           # Servisleri listele
make stats        # Kaynak kullanımı
make health       # Sağlık durumu
make load-test    # Load balancing testi
make shell-app    # App container shell
make shell-db     # PostgreSQL psql
make shell-redis  # Redis CLI
make db-backup    # Veritabanı yedekle
make db-restore BACKUP_FILE=dosya.sql.gz
make clean        # Container sil
make clean-all    # Her şeyi sil (!)
make help         # Yardım
```

---

## 12. Docker Komutları Cheat Sheet

### Image Komutları

```bash
docker images                           # İmajları listele
docker pull nginx:alpine                # İmaj indir
docker build -t myapp:1.0 .             # İmaj oluştur
docker build --no-cache -t myapp .      # Cache'siz build
docker rmi myapp:1.0                    # İmaj sil
docker image prune -f                   # Kullanılmayan imajları sil
docker inspect myapp:1.0               # İmaj detayı
docker history myapp:1.0               # Katman geçmişi ve boyutları
docker save myapp:1.0 | gzip > myapp.tar.gz  # İmajı dışa aktar
docker load < myapp.tar.gz             # İmajı içe aktar
```

### Container Komutları

```bash
docker ps                               # Çalışan container'lar
docker ps -a                            # Tüm container'lar
docker run -d -p 8080:8080 myapp        # Arka planda başlat
docker run -it ubuntu sh                # İnteraktif başlat
docker stop container_id               # Durdur (SIGTERM)
docker kill container_id               # Zorla durdur (SIGKILL)
docker rm container_id                 # Sil
docker rm -f $(docker ps -aq)          # Tümünü sil
docker exec -it myapp sh               # Shell aç
docker logs -f myapp                   # Logları takip et
docker logs --tail=100 myapp           # Son 100 satır log
docker cp myapp:/app/log.txt ./        # Dosya kopyala
docker top myapp                       # Container'daki süreçler
docker stats                           # Kaynak kullanımı
docker inspect myapp                   # Container detayı
```

### Volume Komutları

```bash
docker volume ls                        # Volume listesi
docker volume create myvolume           # Volume oluştur
docker volume inspect myvolume          # Volume detayı
docker volume rm myvolume              # Volume sil
docker volume prune -f                 # Kullanılmayan volume'leri sil
```

### Network Komutları

```bash
docker network ls                       # Ağları listele
docker network create mynet            # Ağ oluştur
docker network inspect mynet           # Ağ detayı
docker network connect mynet myapp     # Container'ı ağa bağla
docker network disconnect mynet myapp  # Ağdan ayır
docker network rm mynet                # Ağ sil
docker network prune -f               # Kullanılmayan ağları sil
```

### Docker Compose Komutları

```bash
docker compose up -d                   # Başlat (arka planda)
docker compose up --build              # Yeniden build et ve başlat
docker compose down                    # Durdur ve sil
docker compose down -v                 # Volume'lerle birlikte sil
docker compose ps                      # Servisleri listele
docker compose logs -f app1            # Servis logu takip et
docker compose exec app1 sh            # Servise shell aç
docker compose restart app1            # Servisi yeniden başlat
docker compose scale app=3             # 3 instance çalıştır
docker compose config                  # Birleştirilmiş yapılandırmayı göster
docker compose pull                    # İmajları güncelle
```

### Temizleme Komutları

```bash
docker system prune -f                 # Kullanılmayan her şeyi sil
docker system prune --volumes -f       # Volume dahil sil
docker system df                       # Docker disk kullanımı
```

---

## 13. Volume Yedekleme & Geri Yükleme

```bash
# Yedekleme
make db-backup
# Çıktı: backups/db_yedek_2024-01-15_10-30-00.sql.gz

# Geri yükleme
make db-restore BACKUP_FILE=backups/db_yedek_2024-01-15_10-30-00.sql.gz

# Manuel yedekleme
./scripts/backup.sh

# Manuel geri yükleme
./scripts/restore.sh backups/db_yedek_2024-01-15_10-30-00.sql.gz
```

---

## 14. Hızlı Başlangıç

```bash
# 1. Projeyi klonla
git clone https://github.com/kullanici/docker-advanced.git
cd docker-advanced

# 2. Ortam değişkenlerini ayarla
cp .env.example .env

# 3. Geliştirme ortamını başlat
make dev

# 4. Servisleri kontrol et
make health

# 5. Load balancing testini çalıştır
make load-test
```

### Servis URL'leri

| Servis | URL | Açıklama |
|--------|-----|----------|
| Nginx (Ana giriş) | http://localhost:80 | Load balancer |
| App1 (Direkt) | http://localhost:8081 | İlk instance |
| App2 (Direkt) | http://localhost:8082 | İkinci instance |
| Redis Commander | http://localhost:8083 | Redis UI |
| pgAdmin | http://localhost:5050 | PostgreSQL UI |
| Actuator (App1) | http://localhost:8081/actuator/health | Sağlık durumu |

### API Endpoint'leri

```bash
# Ana sayfa
GET http://localhost:80/

# Instance bilgisi (hangi container karşıladı?)
GET http://localhost:80/instance

# Redis paylaşımlı sayaç (load balancing kanıtı)
GET http://localhost:80/redis-counter

# Ortam değişkenleri
GET http://localhost:80/env

# Sistem bilgisi (JVM, CPU, RAM)
GET http://localhost:80/system

# Yavaş istek simülasyonu (ms parametresi)
GET http://localhost:80/slow?ms=2000

# Spring Boot health
GET http://localhost:80/actuator/health

# Nginx durumu
GET http://localhost:80/nginx-status
```

---

## 15. Proje Yapısı

```
docker-advanced/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/dockerdemo/
│   │       │   ├── DockerDemoApplication.java    # Ana sınıf
│   │       │   └── controller/
│   │       │       └── DemoController.java        # REST endpoint'ler
│   │       └── resources/
│   │           └── application.yml               # Yapılandırma (dev/prod profil)
│   ├── Dockerfile                                # Multi-stage build
│   └── pom.xml                                   # Maven bağımlılıkları
│
├── nginx/
│   ├── Dockerfile                                # Nginx özel imaj
│   └── nginx.conf                               # Reverse proxy + Load balancer
│
├── scripts/
│   ├── backup.sh                                 # PostgreSQL yedekleme
│   └── restore.sh                               # PostgreSQL geri yükleme
│
├── docker-compose.yml                           # Temel yapılandırma (2 app + db + redis + nginx)
├── docker-compose.dev.yml                       # Geliştirme override (debug, hot reload)
├── docker-compose.prod.yml                      # Üretim override (optimize, güvenli)
├── .env.example                                 # Ortam değişkenleri örneği
├── .dockerignore                               # Docker build context dışlamaları
├── .gitignore                                   # Git dışlamaları
├── Makefile                                     # Kısayol komutları
└── README.md                                    # Bu dosya
```

---

## Teknoloji Yığını

| Teknoloji | Sürüm | Rol |
|-----------|-------|-----|
| Java | 21 LTS | Programlama dili |
| Spring Boot | 3.3.4 | Uygulama framework |
| Maven | 3.9+ | Build aracı |
| Docker | 24+ | Container motoru |
| Docker Compose | 2.x | Çoklu container yönetimi |
| Nginx | 1.25 Alpine | Reverse proxy + LB |
| PostgreSQL | 16 Alpine | İlişkisel veritabanı |
| Redis | 7 Alpine | In-memory önbellek |
| Eclipse Temurin | 21 JRE Alpine | JVM |

---

## Mülakat Soruları

**Q: Docker container ile VM farkı nedir?**
A: VM: Her VM kendi işletim sistemi kernel'ını çalıştırır (GB boyutunda). Hypervisor (VMware, VirtualBox) host OS üzerinde çalışır. Başlatma süresi dakikalar. Container: Host OS kernel'ını paylaşır, sadece uygulama + bağımlılıkları izole eder (MB boyutunda). Docker Engine üzerinde çalışır. Başlatma süresi saniyeler. Container daha hafif, hızlı, taşınabilir. VM daha güçlü izolasyon (farklı OS çalıştırabilir). Production'da genellikle container içinde uygulama, VM üzerinde Kubernetes.

**Q: Multi-stage build neden önemlidir?**
A: Tek aşamalı build: JDK (400MB+) + Maven + kaynak kod + jar → final image büyük, güvenlik açığı riski. Multi-stage: Stage 1 (builder): JDK + Maven → jar derle. Stage 2 (runtime): sadece JRE + jar kopyala. Final image: JDK yok, Maven yok, kaynak kod yok → 100-150MB, minimal attack surface. `FROM eclipse-temurin:21-jdk AS builder` → `FROM eclipse-temurin:21-jre-alpine` — sadece jar kopyalanır. Production image'ı küçük ve güvenli.

**Q: Docker layer caching nasıl çalışır ve neden önemlidir?**
A: Her Dockerfile direktifi bir layer oluşturur. Layer değişmediyse cache'den kullanılır. `COPY pom.xml .` → `RUN mvn dependency:go-offline` — bağımlılıklar ayrı layer'da. pom.xml değişmediğinde Maven bağımlılıkları indirilmez (cache hit). `COPY src .` → kaynak kod değişince sadece bu layer + sonrası yeniden oluşturulur. Yanlış sıra: `COPY . .` → `RUN mvn package` — bir dosya değişse tüm mvn tekrar çalışır. Doğru sıra: az değişen önce, çok değişen sonra.

**Q: Spring Boot layered jar nedir?**
A: Spring Boot 2.3+ ile `spring-boot-maven-plugin` layered jar üretir. Jar içi katmanlar: dependencies (nadiren değişir) → spring-boot-loader → snapshot-dependencies → application (sık değişir). `COPY --from=builder /app/layers/dependencies/ ./` → her katman ayrı Docker layer. application katmanı değişince diğerleri cache'den → build hızlanır, registry'e push azalır. `java -Djarmode=layertools -jar app.jar extract` ile katmanları çıkar.

**Q: Docker network türleri nelerdir?**
A: bridge (default): Container'lar aynı ağda birbirini isimle bulabilir (`db`, `redis`). Dış dünyaya NAT. En yaygın. host: Container, host ağını doğrudan kullanır. Port mapping gerekmez ama izolasyon yok. overlay: Swarm/Kubernetes'te farklı host'lardaki container'ları birleştirir. none: Ağ yoktur, tamamen izole. Compose'da `networks:` tanımlamayla: `app-network` — sadece bu ağdaki container'lar birbirini görür. Güvenlik için servisler farklı ağlarda izole edilebilir.

**Q: HEALTHCHECK direktifi neden önemlidir?**
A: Docker, container çalışıyor ama uygulama hazır olmayabilir (DB bağlantısı bekleniyor). HEALTHCHECK: `/actuator/health` endpoint'ini periyodik kontrol eder. `healthy` / `unhealthy` durumu. Kubernetes readiness probe ile benzer amaç. Docker Compose `depends_on: condition: service_healthy` — DB healthy olana kadar app başlamaz. Olmadan: `depends_on: db` — sadece container başladığını bekler, DB gerçekten hazır değilse app crash olur → restart döngüsü.

**Q: Non-root user neden Dockerfile'da zorunludur?**
A: Container root olarak çalışırsa: Uygulama exploiti ile container'a sızan saldırgan root yetkisine sahip olur. Volume mount ile host dosya sistemi erişimi. `USER appuser` ile minimal yetki. Dockerfile: `RUN groupadd -r appgroup && useradd -r -g appgroup appuser` → `USER appuser`. Production best practice: `--read-only` flag ile dosya sistemi salt okunur, sadece gerekli volume'lar yazılabilir. `securityContext.runAsNonRoot: true` Kubernetes'te zorunlu olabilir.

**Q: Docker volume ile bind mount farkı nedir?**
A: Volume: Docker yönetir (`/var/lib/docker/volumes/`), taşınabilir, backup kolay. `docker volume create pgdata` → `volumes: pgdata: {}`. Bind mount: Host dizinini container'a bağlar. Geliştirmede hot-reload için: `./src:/app/src`. Production'da volume tercih edilir (taşınabilir, Docker kontrollü). `docker volume inspect pgdata` → nerede saklandığını gösterir. Volume backup: `docker run --rm -v pgdata:/data -v $(pwd):/backup alpine tar czf /backup/pgdata.tar.gz /data`.

**Q: Nginx reverse proxy neden kullanılır?**
A: Spring Boot doğrudan 8080'de dinlenebilir. Neden Nginx? SSL termination: HTTPS → HTTP dönüşümü Nginx'te, app HTTP konuşur. Static dosya serving: CSS/JS/images Nginx'ten hızlı. Load balancing: 2 app container → round-robin dağıtım. Rate limiting: `limit_req_zone`. Caching: statik yanıtlar cache'lenebilir. Gzip compression. Security headers. `proxy_pass http://app:8080` — container ismi DNS çözümler. Upstream group ile load balancing: `upstream backend { server app1:8080; server app2:8080; }`.
