# ==============================================================================
# Docker Multi-Stage Build - Spring Boot 3.3 + Java 21
# ==============================================================================
#
# MULTI-STAGE BUILD NEDEN KULLANILIR?
# ─────────────────────────────────────────────────────────────────────────────
# Tek aşamalı build ile boyut karşılaştırması:
#
#   Tek aşama (JDK + Maven + kaynak)  : ~700-900 MB
#   Multi-stage (sadece JRE + JAR)    : ~180-250 MB
#   ────────────────────────────────────────────
#   Tasarruf                          : ~500-700 MB (%60-75 küçük)
#
# Avantajlar:
#   ✓ Küçük imaj → hızlı dağıtım, az bant genişliği
#   ✓ JDK build araçları (Maven, javac) üretim imajına gitmez → güvenli
#   ✓ Kaynak kodu üretim imajında bulunmaz → güvenlik
#   ✓ Layer caching → değişmeyen katmanlar yeniden kullanılır (hızlı build)
#
# ==============================================================================

# ── AŞAMA 1: BUILD (DERLEME) ──────────────────────────────────────────────────
# Eclipse Temurin JDK 21 + Alpine (küçük Linux dağıtımı)
# Bu aşama sadece derleme için kullanılır, üretim imajına GEÇMEz
FROM eclipse-temurin:21-jdk-alpine AS build

# ── ALPINE VS SLIM KARŞILAŞTIRMASI ────────────────────────────────────────────
# eclipse-temurin:21-jdk-alpine  → ~200 MB  (Alpine Linux tabanlı, musl libc)
# eclipse-temurin:21-jdk         → ~400 MB  (Debian slim tabanlı, glibc)
# eclipse-temurin:21-jdk-slim    → ~300 MB  (Debian minimal, glibc)
# ────────────────────────────────────────────────────────────────────────────
# Alpine tercih nedeni: en küçük boyut, minimal saldırı yüzeyi
# Uyarı: Alpine musl libc kullanır - bazı native kütüphaneler sorun çıkarabilir

# ── YAZAR BİLGİSİ ─────────────────────────────────────────────────────────────
# LABEL: imaja meta bilgi ekler - docker inspect ile okunabilir
LABEL maintainer="Docker Advanced Demo"
LABEL description="Spring Boot 3.3 + Java 21 Multi-Stage Build"
LABEL version="1.0.0"

# ── ARG vs ENV FARK AÇIKLAMASI ────────────────────────────────────────────────
# ARG: sadece BUILD zamanında kullanılır, konteyner çalışırken YOK
#      docker build --build-arg MAVEN_OPTS=... ile geçilir
# ENV: hem build hem çalışma zamanında kullanılır
#      docker run -e SERVER_PORT=9090 ile geçilir

# BUILD zamanı değişkeni - Maven ayarları
ARG MAVEN_OPTS="-Xmx512m -Xms256m"
# BUILD zamanı değişkeni - hangi Maven profili kullanılacak
ARG BUILD_PROFILE=default

# Maven build seçeneklerini ENV olarak da ayarla (Maven bu değişkeni okur)
ENV MAVEN_OPTS=${MAVEN_OPTS}

# ── ÇALIŞMA DİZİNİ ────────────────────────────────────────────────────────────
# WORKDIR: Sonraki tüm komutların çalışacağı dizini ayarlar
#          Dizin yoksa otomatik oluşturur (mkdir + cd yerine)
#          Mutlak veya göreli path kullanılabilir
WORKDIR /build

# ── BAĞIMLILIK CACHE OPTİMİZASYONU ───────────────────────────────────────────
# Docker layer caching prensibinden yararlanılır:
# Bir katman değişirse, sonraki TÜM katmanlar yeniden oluşturulur.
# Ancak önceki katmanlar değişmezse cache'den kullanılır.
#
# Strateji: Önce pom.xml kopyala → bağımlılıkları indir (bu katman nadiren değişir)
#           Sonra kaynak kodu kopyala → kaynak her değiştiğinde sadece build yenilenir
#
# pom.xml değişmediği sürece "dependency:go-offline" adımı cache'den gelir!
# Bu build süresini 2-3 dakikadan 30 saniyeye düşürür.

# COPY: Dosyayı host'tan imaja kopyalar
# Sadece pom.xml kopyala (kaynak kodu henüz kopyalama)
COPY pom.xml .

# Maven bağımlılıklarını önceden indir - bu katman pom.xml değişmediği sürece cache'lenir
# -B: batch mode (interaktif değil)
# --no-transfer-progress: indirme ilerlemesini gösterme (log temizliği)
RUN mvn dependency:go-offline -B --no-transfer-progress

# Kaynak kodunu kopyala - bu katman her kod değişikliğinde yenilenir
# Ancak bağımlılık katmanı (yukarısı) değişmediğinden hızlı gelir
COPY src ./src

# ── UYGULAMA DERLEME ──────────────────────────────────────────────────────────
# Maven ile JAR oluştur:
# package: derleme + test + JAR üretimi
# -DskipTests: test'leri atla (CI/CD'de ayrı adımda çalışır)
# -B: batch mode
RUN mvn package -DskipTests -B --no-transfer-progress

# ── AŞAMA 2: ÇALIŞMA (RUNTIME) ────────────────────────────────────────────────
# Önceki aşamadan SADECE JAR dosyası alınır
# JDK, Maven, kaynak kodu üretim imajına GEÇMEz → küçük ve güvenli imaj
FROM eclipse-temurin:21-jre-alpine AS runtime

# ── GÜVENLİK: NON-ROOT USER ───────────────────────────────────────────────────
# Docker container'lar varsayılan olarak root kullanıcısıyla çalışır.
# Bu ciddi bir güvenlik açığıdır - container'dan çıkılırsa host'a erişilebilir.
#
# Best Practice: Yetki kısıtlaması için özel non-root kullanıcı oluştur
# addgroup: yeni grup oluştur (-S: sistem grubu)
# adduser:  yeni kullanıcı oluştur (-S: sistem kullanıcısı, -G: gruba ekle)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# ── ÇALIŞMA DİZİNİ ────────────────────────────────────────────────────────────
# Uygulama dizinini oluştur
WORKDIR /app

# ── JAR KOPYALAMA ─────────────────────────────────────────────────────────────
# COPY --from=build: önceki (build) aşamasından dosya al
# target/*.jar → /app/app.jar olarak kopyala
COPY --from=build /build/target/*.jar app.jar

# ── DOSYA SAHİPLİĞİ ───────────────────────────────────────────────────────────
# JAR dosyasının sahibini appuser/appgroup yap
# Root olmayan kullanıcı okuyabilsin diye
RUN chown appuser:appgroup app.jar

# ── NON-ROOT KULLANICIYA GEÇ ──────────────────────────────────────────────────
# USER: Sonraki komutları ve ENTRYPOINT'i bu kullanıcıyla çalıştır
USER appuser

# ── ÇALIŞMA ZAMANI ORTAM DEĞİŞKENLERİ ────────────────────────────────────────
# ENV: Container çalışırken kullanılacak değişkenler
#      docker run -e ile override edilebilir
#      docker-compose.yml'deki environment: bölümünde de ayarlanabilir

# JVM bellek ayarları - Container kaynak limitleriyle uyumlu
ENV JAVA_OPTS="\
    -Xms256m \
    -Xmx512m \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.backgroundpreinitializer.ignore=true"

# Spring Boot aktif profili (dev/prod) - docker-compose tarafından override edilir
ENV SPRING_PROFILES_ACTIVE=default

# ── PORT ─────────────────────────────────────────────────────────────────────
# EXPOSE: Container'ın dinleyeceği portu belgeler
# Bu sadece DOKÜMANTASYON amaçlı! Gerçek port açma docker-compose'da olur.
# docker run -p 8080:8080 veya docker-compose ports: ile açılır
EXPOSE 8080

# ── SAĞLIK KONTROLÜ ───────────────────────────────────────────────────────────
# HEALTHCHECK: Docker bu komutu periyodik çalıştırarak container'ın sağlığını kontrol eder
# Sağlıklı değilse: depends_on:condition:service_healthy bekleyen servisler başlamaz
#
# --interval=30s  : Her 30 saniyede bir kontrol et
# --timeout=10s   : Kontrol 10 saniyeden uzun sürerse "unhealthy"
# --start-period=30s : İlk 30 saniyede kontrol yapma (uygulama başlama süresi)
# --retries=3     : 3 ardışık başarısızlıkta "unhealthy" olarak işaretle
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# ── ENTRYPOINT vs CMD ────────────────────────────────────────────────────────
# ENTRYPOINT: Container başladığında DAIMA çalışır, değiştirilemez (--entrypoint ile değiştirilebilir)
# CMD: Varsayılan komut, docker run <image> <komut> ile override edilebilir
#
# Kullanım Şekilleri:
# 1. Sadece CMD:        ["java", "-jar", "app.jar"]     → override edilebilir
# 2. Sadece ENTRYPOINT: ["java", "-jar", "app.jar"]     → override zor
# 3. İkisi birden:      ENTRYPOINT = java, CMD = -jar app.jar → CMD override edilebilir
# 4. Shell form:        CMD java -jar app.jar           → /bin/sh -c ile çalışır (sinyal sorunları!)
# 5. Exec form:         CMD ["java", "-jar", "app.jar"] → direkt çalışır (sinyal yönetimi iyi)
#
# BU PROJEDEKİ TERCIH: Shell form - JAVA_OPTS env değişkenini genişletmek için
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
