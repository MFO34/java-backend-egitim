# ==================== NGINX DOCKERFILE ====================
# Nginx için özelleştirilmiş Docker imajı
# Temel imaj: nginx:alpine - minimal boyut, maksimum güvenlik

# Resmi Nginx Alpine imajı - stabil sürüm
# nginx:alpine  → ~7 MB  (Alpine Linux tabanlı)
# nginx:latest  → ~130 MB (Debian tabanlı)
# Boyut farkı: Alpine ~18x daha küçük!
FROM nginx:alpine

# ── META BİLGİ ────────────────────────────────────────────────────────────────
# LABEL direktifi: imaj hakkında meta bilgi ekler
LABEL maintainer="Docker Advanced Demo"
LABEL description="Nginx Reverse Proxy + Load Balancer"
LABEL role="reverse-proxy"

# ── VARSAYILAN NGINX KONFIGÜRASYONUNU SİL ────────────────────────────────────
# Alpine Nginx imajı /etc/nginx/conf.d/default.conf ile gelir
# Kendi yapılandırmamızı kullanacağımız için varsayılanı sil
RUN rm -f /etc/nginx/conf.d/default.conf

# ── ÖZEL KONFİGÜRASYON KOPYALA ───────────────────────────────────────────────
# Kendi nginx.conf dosyamızı imaja kopyala
# /etc/nginx/nginx.conf: Nginx'in ana konfigürasyon dosyası
COPY nginx.conf /etc/nginx/nginx.conf

# ── SSL SERTİFİKA DİZİNİ ─────────────────────────────────────────────────────
# SSL sertifikaları için dizin oluştur
# Üretim ortamında gerçek sertifikalar buraya bağlanır (volume mount)
RUN mkdir -p /etc/nginx/ssl

# ── LOG DİZİNİ OLUŞTUR ────────────────────────────────────────────────────────
# Nginx log dizininin var olduğundan emin ol
RUN mkdir -p /var/log/nginx

# ── GÜVENLİK: YETKI MİNİMİZASYONU ────────────────────────────────────────────
# Nginx master process root ile başlamalı (80/443 portlarını açmak için)
# Ancak worker process'ler nginx kullanıcısıyla çalışır (nginx.conf'ta ayarlı)
# Bu Alpine Nginx imajında varsayılan olarak gelir

# ── PORT BELGELEME ────────────────────────────────────────────────────────────
# HTTP portu
EXPOSE 80
# HTTPS portu (SSL aktif edilince)
EXPOSE 443

# ── SAĞLIK KONTROLÜ ───────────────────────────────────────────────────────────
# Nginx'in çalışıp çalışmadığını kontrol et
# wget ile localhost:80'e istek at - başarılı yanıt gelirse sağlıklı
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -q --spider http://localhost:80/nginx-status || exit 1

# ── BAŞLATMA KOMUTU ───────────────────────────────────────────────────────────
# CMD: Nginx'i ön planda çalıştır (-g daemon off)
# Daemon modunda çalıştırılırsa Docker container hemen durur (process biter)
# "-g daemon off": Nginx'i arka plana gönderme, ön planda tut
CMD ["nginx", "-g", "daemon off;"]
