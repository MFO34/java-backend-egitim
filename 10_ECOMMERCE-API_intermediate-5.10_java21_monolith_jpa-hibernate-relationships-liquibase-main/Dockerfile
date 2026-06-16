# =============================================================
# MULTI-STAGE DOCKERFILE — E-Ticaret API
# =============================================================
# Stage 1 (builder): Kaynak kodu derle → JAR oluştur
# Stage 2 (runtime): Sadece JRE + JAR → küçük final image
# Neden multi-stage?
#   builder image ~600MB, runtime image ~150MB
#   Derleme araçları (javac, mvn) son image'a girmez → güvenli
# =============================================================

# ==================== STAGE 1: DERLEME ====================
# eclipse-temurin: Adoptium OpenJDK — en popüler production JDK
# 21-jdk-alpine: JDK (derleyici dahil) + Alpine Linux (küçük)
FROM eclipse-temurin:21-jdk-alpine AS builder

# Çalışma dizini oluştur (container içinde /app klasörü)
WORKDIR /app

# Maven wrapper ve pom.xml'i ÖNCE kopyala
# Docker layer cache: bağımlılıklar değişmezse bu adım tekrar çalışmaz
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Bağımlılıkları indir (kaynak kod olmadan — sadece pom.xml ile)
# -B: batch mode (interaktif değil)
# dependency:go-offline: tüm bağımlılıkları önceden indir
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Kaynak kodları kopyala
COPY src ./src

# Projeyi derle ve JAR oluştur
# -DskipTests: testleri atla (CI'da ayrı adım)
# --enable-preview: Java 21 preview özellikleri (Sealed Classes için)
RUN ./mvnw clean package -DskipTests -B \
    -Dspring-boot.run.jvmArguments="--enable-preview"

# ==================== STAGE 2: ÇALIŞMA ====================
# 21-jre-alpine: Sadece JRE (Java Runtime Environment)
# JDK gereksiz → daha küçük image (~150MB)
FROM eclipse-temurin:21-jre-alpine AS runtime

# Güvenlik: root kullanıcı yerine özel kullanıcı oluştur
# Container'ın root haklarıyla çalışması güvenlik riski
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Uygulama dizini oluştur ve sahipliği ver
WORKDIR /app
RUN chown appuser:appgroup /app

# Builder stage'den JAR'ı kopyala
COPY --from=builder --chown=appuser:appgroup \
    /app/target/eticaret-api-1.0.0.jar app.jar

# appuser olarak çalış (root değil)
USER appuser

# Spring Boot varsayılan portu
EXPOSE 8080

# Sağlık kontrolü — Docker her 30s'de kontrol eder
# Actuator /health endpoint'i: {"status":"UP"} döndürüyorsa sağlıklı
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# JVM Optimizasyon Bayrakları:
# -XX:+UseContainerSupport  → Docker CPU/bellek sınırlarını tanır
# -XX:MaxRAMPercentage=75.0 → Container belleğinin %75'ini kullan
# --enable-preview          → Java 21 preview özellikleri
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "--enable-preview", \
    "-jar", "app.jar"]
