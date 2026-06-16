# ==============================================================================
# Makefile - Docker Advanced Demo Yönetim Komutları
# ==============================================================================
# Kullanım: make <hedef>
# Örnek:    make up        → Servisleri başlat
#           make dev       → Geliştirme ortamı başlat
#           make prod      → Üretim ortamı başlat
#           make logs      → Tüm logları izle
#           make clean     → Her şeyi temizle
# ==============================================================================

# ── DEĞİŞKENLER ───────────────────────────────────────────────────────────────
# Docker Compose temel komut
DC = docker compose
# Temel + geliştirme override dosyaları
DC_DEV = docker compose -f docker-compose.yml -f docker-compose.dev.yml
# Temel + üretim override dosyaları
DC_PROD = docker compose -f docker-compose.yml -f docker-compose.prod.yml
# Araç profili ile
DC_TOOLS = docker compose --profile tools

# Projeye özel imaj etiketi
IMAGE_TAG ?= 1.0.0
# Uygulama adı
APP_NAME = docker-advanced-demo

# PHONY hedefler: Gerçek dosya değil, komut olduklarını belirt
.PHONY: all up down dev prod build rebuild logs logs-app logs-nginx \
        ps stats clean clean-all restart shell-app shell-db shell-redis \
        db-backup db-restore health tools help

# ── VARSAYILAN HEDEF ─────────────────────────────────────────────────────────
# make komutu tek başına çağrıldığında 'help' gösterir
all: help

# ── TEMEL KOMUTLAR ───────────────────────────────────────────────────────────

## Tüm servisleri arka planda başlat
up:
	@echo "▶ Servisler başlatılıyor..."
	$(DC) up -d
	@echo "✓ Servisler başlatıldı!"
	@echo "  Uygulama (Nginx): http://localhost:80"
	@echo "  App1 (direkt):    http://localhost:8081"
	@echo "  App2 (direkt):    http://localhost:8082"

## Tüm servisleri durdur
down:
	@echo "■ Servisler durduruluyor..."
	$(DC) down
	@echo "✓ Servisler durduruldu"

## Servisleri yeniden başlat
restart:
	@echo "↺ Servisler yeniden başlatılıyor..."
	$(DC) restart
	@echo "✓ Yeniden başlatma tamamlandı"

# ── ORTAM KOMUTLARI ──────────────────────────────────────────────────────────

## Geliştirme ortamını başlat (hot reload, debug portu açık)
dev:
	@echo "▶ Geliştirme ortamı başlatılıyor..."
	@cp -n .env.example .env 2>/dev/null || true
	$(DC_DEV) up -d
	@echo "✓ Geliştirme ortamı hazır!"
	@echo "  API (Nginx):        http://localhost:80"
	@echo "  App1 (direkt):      http://localhost:8081"
	@echo "  App2 (direkt):      http://localhost:8082"
	@echo "  Redis Commander:    http://localhost:8083"
	@echo "  pgAdmin:            http://localhost:5050"
	@echo "  JVM Debug App1:     localhost:5005"
	@echo "  JVM Debug App2:     localhost:5006"

## Üretim ortamını başlat
prod:
	@echo "▶ Üretim ortamı başlatılıyor..."
	@test -f .env || (echo "HATA: .env dosyası bulunamadı!" && exit 1)
	$(DC_PROD) up -d
	@echo "✓ Üretim ortamı başlatıldı"

## Araç servislerini başlat (Redis Commander, pgAdmin)
tools:
	@echo "▶ Araç servisleri başlatılıyor..."
	$(DC_TOOLS) up -d redis-commander pgadmin
	@echo "✓ Araçlar hazır!"
	@echo "  Redis Commander: http://localhost:8083"
	@echo "  pgAdmin:         http://localhost:5050"

# ── BUILD KOMUTLARI ──────────────────────────────────────────────────────────

## Tüm imajları oluştur (cache kullanmadan)
build:
	@echo "⚙ İmajlar oluşturuluyor..."
	$(DC) build
	@echo "✓ Build tamamlandı"

## Cache'i temizleyerek yeniden build et
rebuild:
	@echo "⚙ Cache'siz yeniden build ediliyor..."
	$(DC) build --no-cache
	@echo "✓ Temiz build tamamlandı"

## Sadece uygulama imajını rebuild et
rebuild-app:
	@echo "⚙ Uygulama imajı yeniden oluşturuluyor..."
	$(DC) build --no-cache app1 app2
	@echo "✓ Uygulama build tamamlandı"

# ── LOG KOMUTLARI ────────────────────────────────────────────────────────────

## Tüm servislerin loglarını takip et
logs:
	$(DC) logs -f

## Sadece uygulama logları
logs-app:
	$(DC) logs -f app1 app2

## Sadece Nginx logları
logs-nginx:
	$(DC) logs -f nginx

## Sadece veritabanı logları
logs-db:
	$(DC) logs -f db

## Son N satır log göster (varsayılan: 100)
logs-tail:
	$(DC) logs --tail=${N:-100}

# ── İZLEME KOMUTLARI ─────────────────────────────────────────────────────────

## Çalışan servisleri listele
ps:
	$(DC) ps

## Gerçek zamanlı kaynak kullanımı (CPU, RAM, Network, Disk I/O)
stats:
	@echo "Çıkmak için Ctrl+C"
	docker stats $(shell docker compose ps -q)

## Tüm servislerin sağlık durumunu kontrol et
health:
	@echo "=== Sağlık Durumu ==="
	@echo ""
	@echo "▸ Nginx:"
	@curl -sf http://localhost:80/nginx-status 2>/dev/null && echo "✓ Çalışıyor" || echo "✗ Yanıt vermiyor"
	@echo ""
	@echo "▸ App1:"
	@curl -sf http://localhost:8081/actuator/health 2>/dev/null | python3 -m json.tool 2>/dev/null || echo "✗ Yanıt vermiyor"
	@echo ""
	@echo "▸ App2:"
	@curl -sf http://localhost:8082/actuator/health 2>/dev/null | python3 -m json.tool 2>/dev/null || echo "✗ Yanıt vermiyor"
	@echo ""
	@echo "▸ Redis:"
	@docker exec docker-demo-redis redis-cli ping 2>/dev/null || echo "✗ Yanıt vermiyor"
	@echo ""
	@echo "▸ PostgreSQL:"
	@docker exec docker-demo-postgres pg_isready -U postgres 2>/dev/null || echo "✗ Yanıt vermiyor"

## Load balancing testini çalıştır (10 istek at, hangi instance karşıladı?)
load-test:
	@echo "=== Load Balancing Testi (10 istek) ==="
	@for i in $$(seq 1 10); do \
		echo "İstek $$i:"; \
		curl -sf http://localhost:80/instance 2>/dev/null | python3 -c "import sys,json; d=json.load(sys.stdin); print(f'  Instance: {d[\"instanceAdi\"]} | Hostname: {d[\"hostname\"]}')" 2>/dev/null || echo "  Bağlantı hatası"; \
	done
	@echo ""
	@echo "Redis sayacı:"
	@curl -sf http://localhost:80/redis-counter 2>/dev/null | python3 -c "import sys,json; d=json.load(sys.stdin); print(f'  Toplam istek: {d[\"toplamIstek\"]}')" 2>/dev/null

# ── SHELL KOMUTLARI ──────────────────────────────────────────────────────────

## App1 container'ına shell aç
shell-app:
	docker exec -it docker-demo-app1 sh

## PostgreSQL shell'i aç
shell-db:
	docker exec -it docker-demo-postgres psql -U ${DB_USERNAME:-postgres} -d ${DB_NAME:-dockerdb}

## Redis CLI'ya bağlan
shell-redis:
	docker exec -it docker-demo-redis redis-cli

## Nginx container'ına shell aç
shell-nginx:
	docker exec -it docker-demo-nginx sh

# ── VERİTABANI YEDEKLEME ─────────────────────────────────────────────────────

## PostgreSQL veritabanını yedekle
db-backup:
	@echo "Veritabanı yedekleniyor..."
	@./scripts/backup.sh
	@echo "✓ Yedekleme tamamlandı"

## PostgreSQL veritabanını geri yükle (BACKUP_FILE değişkeni gerekli)
db-restore:
	@test -n "$(BACKUP_FILE)" || (echo "HATA: BACKUP_FILE değişkeni gerekli! Örn: make db-restore BACKUP_FILE=backup.sql.gz" && exit 1)
	@./scripts/restore.sh $(BACKUP_FILE)

# ── TEMİZLEME KOMUTLARI ──────────────────────────────────────────────────────

## Servisleri durdur ve container'ları sil (volume'ler korunur)
clean:
	@echo "Servisler durduruluyor ve container'lar siliniyor..."
	$(DC) down --remove-orphans
	@echo "✓ Temizleme tamamlandı (volume'ler korundu)"

## Her şeyi sil: container'lar + volume'ler + ağlar (VERİ KAYBI!)
clean-all:
	@echo "⚠ UYARI: Tüm container'lar, volume'ler ve ağlar silinecek!"
	@echo "Onaylamak için 'yes' yazın:"
	@read ONAY && [ "$$ONAY" = "yes" ] || (echo "İptal edildi" && exit 1)
	$(DC) down --volumes --remove-orphans
	docker system prune -f
	@echo "✓ Tam temizleme tamamlandı"

## Kullanılmayan Docker imajlarını temizle (disk tasarrufu)
prune:
	@echo "Kullanılmayan Docker kaynakları temizleniyor..."
	docker system prune -f
	docker volume prune -f
	@echo "✓ Prune tamamlandı"

# ── YARDIM ───────────────────────────────────────────────────────────────────

## Kullanılabilir komutları listele
help:
	@echo ""
	@echo "╔══════════════════════════════════════════════════════╗"
	@echo "║     Docker Advanced Demo - Makefile Komutları       ║"
	@echo "╚══════════════════════════════════════════════════════╝"
	@echo ""
	@echo "TEMEL KOMUTLAR:"
	@echo "  make up          → Tüm servisleri başlat"
	@echo "  make down        → Tüm servisleri durdur"
	@echo "  make restart     → Servisleri yeniden başlat"
	@echo ""
	@echo "ORTAM KOMUTLARI:"
	@echo "  make dev         → Geliştirme ortamı (debug, hot reload)"
	@echo "  make prod        → Üretim ortamı (optimize, güvenli)"
	@echo "  make tools       → Araç servisleri (pgAdmin, Redis Commander)"
	@echo ""
	@echo "BUILD KOMUTLARI:"
	@echo "  make build       → İmajları oluştur"
	@echo "  make rebuild     → Cache'siz yeniden oluştur"
	@echo ""
	@echo "İZLEME KOMUTLARI:"
	@echo "  make ps          → Servisleri listele"
	@echo "  make stats       → Kaynak kullanımı izle"
	@echo "  make health      → Sağlık durumu kontrol"
	@echo "  make logs        → Tüm loglar"
	@echo "  make logs-app    → Uygulama logları"
	@echo "  make load-test   → Load balancing testi"
	@echo ""
	@echo "SHELL KOMUTLARI:"
	@echo "  make shell-app   → App container shell"
	@echo "  make shell-db    → PostgreSQL psql"
	@echo "  make shell-redis → Redis CLI"
	@echo ""
	@echo "YEDEKLEME:"
	@echo "  make db-backup   → Veritabanı yedekle"
	@echo "  make db-restore BACKUP_FILE=dosya.sql.gz → Geri yükle"
	@echo ""
	@echo "TEMİZLEME:"
	@echo "  make clean       → Container'ları sil"
	@echo "  make clean-all   → Her şeyi sil (VERİ KAYBI!)"
	@echo "  make prune       → Kullanılmayan kaynakları sil"
	@echo ""
