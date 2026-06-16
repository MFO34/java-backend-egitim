# AutoHub Frontend - Çok Aşamalı Docker Build
# Node.js ile Angular uygulamasını derler, Nginx ile sunar

# =====================================================
# AŞAMA 1: Angular Derleme Aşaması
# =====================================================
FROM node:20-alpine AS builder

WORKDIR /app

# package.json ve package-lock.json kopyala
# node_modules cache'ini optimize etmek için kaynak koddan önce kopyala
COPY package*.json ./

# Bağımlılıkları yükle
RUN npm ci --silent

# Kaynak kodunu kopyala
COPY . .

# Angular uygulamasını üretim için derle
# --configuration production: Tree shaking, minification, AOT derleme
RUN npm run build:prod

# =====================================================
# AŞAMA 2: Nginx ile Servis Aşaması
# =====================================================
FROM nginx:alpine

# Derlenen Angular dosyalarını Nginx'in servis ettiği dizine kopyala
COPY --from=builder /app/dist/autohub-frontend /usr/share/nginx/html

# Özel Nginx konfigürasyonu - Angular SPA için history mode desteği
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Nginx standart HTTP portu
EXPOSE 80

# Nginx başlat
CMD ["nginx", "-g", "daemon off;"]
