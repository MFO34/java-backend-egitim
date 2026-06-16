# AutoHub Backend - Çok Aşamalı Docker Build (Multi-Stage Build)
# Çok aşamalı build, final image'ı küçük tutmak için kullanılır
# Build araçları (Maven, JDK) final image'a dahil edilmez

# =====================================================
# AŞAMA 1: Derleme Aşaması (Build Stage)
# Maven ile projeyi derler ve JAR dosyası oluşturur
# =====================================================
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

# Çalışma dizinini belirle
WORKDIR /build

# Önce sadece pom.xml kopyala - bağımlılık cache'ini optimize etmek için
# Kaynak kod değişmeden pom.xml değişmemişse Maven bağımlılıkları yeniden indirmez
COPY pom.xml .

# Maven bağımlılıklarını önceden indir - Docker layer cache'inden yararlan
# Bu komut, kaynak kod değişse bile bağımlılıkları yeniden indirmez
RUN mvn dependency:go-offline -B

# Kaynak kodunu kopyala
COPY src ./src

# Projeyi derle - testleri atla (CI/CD'de ayrıca çalıştırılır)
RUN mvn clean package -DskipTests -B

# =====================================================
# AŞAMA 2: Çalışma Aşaması (Runtime Stage)
# Sadece JRE (JDK değil) ile minimal image oluşturur
# =====================================================
FROM eclipse-temurin:17-jre-alpine

# Güvenlik: Root olmayan kullanıcı ile çalıştır
# Docker container'ı root ile çalıştırmak güvenlik riski oluşturur
RUN addgroup -S autohub && adduser -S autohub -G autohub

# Çalışma dizini
WORKDIR /app

# Derleme aşamasından JAR dosyasını kopyala
COPY --from=builder /build/target/autohub-backend-*.jar app.jar

# Log dizini oluştur - uygulama log dosyaları buraya yazılır
RUN mkdir -p /app/logs && chown autohub:autohub /app/logs

# Uygulama kullanıcısına geç
USER autohub

# Uygulama portu - dışarıya açılan port
EXPOSE 8080

# JVM optimizasyon parametreleri
# -XX:+UseContainerSupport: Container bellek limitlerini algılar
# -XX:MaxRAMPercentage=75: Container RAM'inin %75'ini kullan
# -XX:+HeapDumpOnOutOfMemoryError: Bellek hatasında heap dump al
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75 \
               -XX:+HeapDumpOnOutOfMemoryError \
               -XX:HeapDumpPath=/app/logs/heap.hprof \
               -Djava.security.egd=file:/dev/./urandom \
               -Dfile.encoding=UTF-8 \
               -Duser.timezone=Europe/Istanbul"

# Sağlık kontrolü - Docker Swarm ve Kubernetes için
# Her 30 saniyede bir /actuator/health endpoint'ini kontrol eder
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/actuator/health || exit 1

# Uygulamayı başlat
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
