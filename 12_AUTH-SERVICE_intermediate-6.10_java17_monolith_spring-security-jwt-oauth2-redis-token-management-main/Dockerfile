# =============================================================
# MULTI-STAGE DOCKERFILE — Auth Service
# Stage 1: Derleme (JDK + Maven)
# Stage 2: Çalışma (sadece JRE — küçük, güvenli)
# =============================================================

# ==================== STAGE 1: DERLEME ====================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Bağımlılık katmanı önce — kaynak değişmezse bu katman cache'lenir
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Kaynak kod
COPY src ./src

# enable-preview ile derle + JAR oluştur
RUN ./mvnw clean package -DskipTests -B

# ==================== STAGE 2: ÇALIŞMA ====================
FROM eclipse-temurin:21-jre-alpine AS runtime

# Root olmayan kullanıcı — güvenlik best practice
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app
RUN chown appuser:appgroup /app

# JAR'ı builder'dan kopyala
COPY --from=builder --chown=appuser:appgroup \
    /app/target/auth-service-1.0.0.jar app.jar

USER appuser

EXPOSE 8080

# Docker healthcheck — actuator/health endpoint'i
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# JVM container-aware ayarlar + enable-preview
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "--enable-preview", \
    "-jar", "app.jar"]
