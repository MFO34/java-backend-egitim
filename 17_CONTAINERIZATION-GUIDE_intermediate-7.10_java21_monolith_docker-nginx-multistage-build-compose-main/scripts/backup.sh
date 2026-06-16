#!/bin/bash
# ==============================================================================
# PostgreSQL Volume Yedekleme Betiği
# ==============================================================================
# Kullanım: ./scripts/backup.sh
# veya Makefile: make db-backup
#
# Bu betik şunları yapar:
#   1. PostgreSQL veritabanını pg_dump ile dışarı aktarır
#   2. Çıktıyı gzip ile sıkıştırır
#   3. Tarih/saat damgalı dosyaya kaydeder
#   4. Eski yedekleri temizler (30 günden eski)
# ==============================================================================

# Hata durumunda betiği durdur
set -e

# ── RENKLİ ÇIKTI ─────────────────────────────────────────────────────────────
# Terminal renk kodları - okunabilirlik için
YESIL='\033[0;32m'    # Başarı mesajları
SARI='\033[1;33m'     # Uyarı mesajları
KIRMIZI='\033[0;31m'  # Hata mesajları
SIFIRLA='\033[0m'     # Rengi sıfırla

# ── YAPILANDIRMA ─────────────────────────────────────────────────────────────
# .env dosyasından değişkenleri yükle (mevcutsa)
if [ -f ".env" ]; then
    # .env dosyasındaki değişkenleri kaynak olarak al
    source .env
fi

# Veritabanı bağlantı bilgileri - .env'den oku veya varsayılanı kullan
DB_CONTAINER="${DB_CONTAINER:-docker-demo-postgres}" # PostgreSQL container adı
DB_USERNAME="${DB_USERNAME:-postgres}"                # Veritabanı kullanıcısı
DB_NAME="${DB_NAME:-dockerdb}"                        # Yedeklenecek veritabanı

# Yedek dosyasının kaydedileceği dizin
YEDEK_DIZIN="./backups"

# Yedek dosya adı: db_yedek_YYYY-MM-DD_HH-MM-SS.sql.gz
TARIH=$(date +"%Y-%m-%d_%H-%M-%S")
YEDEK_DOSYA="${YEDEK_DIZIN}/db_yedek_${TARIH}.sql.gz"

# Kaç günlük yedekleri sakla (eskiler silinir)
SAKLAMA_GUNU=30

# ── YEDEK DİZİNİ OLUŞTUR ─────────────────────────────────────────────────────
# Yedek dizini yoksa oluştur
mkdir -p "${YEDEK_DIZIN}"

# ── BAŞLANGIÇ BİLGİSİ ────────────────────────────────────────────────────────
echo -e "${YESIL}================================================${SIFIRLA}"
echo -e "${YESIL}  PostgreSQL Yedekleme Başlıyor${SIFIRLA}"
echo -e "${YESIL}================================================${SIFIRLA}"
echo ""
echo "  Konteyner : ${DB_CONTAINER}"    # Hangi konteyner
echo "  Veritabanı: ${DB_NAME}"          # Hangi veritabanı
echo "  Hedef     : ${YEDEK_DOSYA}"      # Nereye kaydedilecek
echo "  Zaman     : $(date)"             # Ne zaman başladı
echo ""

# ── KONTEYNER ÇALIŞIYOR MU KONTROL ET ────────────────────────────────────────
# docker ps ile konteyner adını kontrol et
if ! docker ps --format '{{.Names}}' | grep -q "^${DB_CONTAINER}$"; then
    # Konteyner bulunamadı - hata mesajı ve çıkış
    echo -e "${KIRMIZI}HATA: '${DB_CONTAINER}' konteyneri çalışmıyor!${SIFIRLA}"
    echo "Önce servisleri başlatın: make up"
    exit 1
fi

# ── POSTGRESQL YEDEKLEME ──────────────────────────────────────────────────────
echo "Yedekleme yapılıyor..."

# docker exec: Çalışan konteynerde komut çalıştır
# pg_dump: PostgreSQL veritabanını SQL olarak dışarı aktar
#   -U: Kullanıcı adı
#   -d: Veritabanı adı
#   --no-password: Şifre sorma (ortam değişkeni ile)
#   --clean: DROP TABLE komutlarını dahil et (temiz geri yükleme için)
#   --if-exists: DROP ifadelerine IF EXISTS ekle (hata önleme)
#   --format=plain: Düz SQL çıktısı (gzip ile sıkıştırılacak)
# | gzip: Çıktıyı sıkıştır (genellikle %70-90 küçülür)
# > : Sıkıştırılmış çıktıyı dosyaya yaz
docker exec "${DB_CONTAINER}" \
    pg_dump \
    -U "${DB_USERNAME}" \
    -d "${DB_NAME}" \
    --no-password \
    --clean \
    --if-exists \
    --format=plain \
    | gzip > "${YEDEK_DOSYA}"

# ── YEDEK DOSYASINI DOĞRULA ──────────────────────────────────────────────────
if [ -f "${YEDEK_DOSYA}" ] && [ -s "${YEDEK_DOSYA}" ]; then
    # Dosya var ve boyutu 0'dan büyük
    BOYUT=$(du -sh "${YEDEK_DOSYA}" | cut -f1)   # Dosya boyutunu hesapla
    echo -e "${YESIL}✓ Yedekleme başarılı!${SIFIRLA}"
    echo "  Dosya : ${YEDEK_DOSYA}"
    echo "  Boyut : ${BOYUT}"
else
    # Dosya oluşturulamadı veya boş
    echo -e "${KIRMIZI}HATA: Yedek dosyası oluşturulamadı!${SIFIRLA}"
    exit 1
fi

# ── ESKİ YEDEKLERİ TEMİZLE ───────────────────────────────────────────────────
echo ""
echo "Eski yedekler temizleniyor (${SAKLAMA_GUNU} günden eski)..."

# find: YEDEK_DIZIN içinde *.sql.gz uzantılı, 30 günden eski dosyaları bul ve sil
SILINEN=$(find "${YEDEK_DIZIN}" -name "db_yedek_*.sql.gz" \
    -mtime +${SAKLAMA_GUNU} -delete -print | wc -l)

if [ "${SILINEN}" -gt "0" ]; then
    # Silinen dosya varsa bildir
    echo -e "${SARI}  ${SILINEN} eski yedek dosyası silindi${SIFIRLA}"
else
    # Silinecek eski yedek yoktu
    echo "  Silinecek eski yedek yok"
fi

# ── MEVCUT YEDEKLER ──────────────────────────────────────────────────────────
echo ""
echo "Mevcut yedekler:"
# Yedek dizinindeki tüm sql.gz dosyalarını listele
ls -lh "${YEDEK_DIZIN}"/db_yedek_*.sql.gz 2>/dev/null || echo "  Yedek bulunamadı"

# ── TAMAMLANDI ───────────────────────────────────────────────────────────────
echo ""
echo -e "${YESIL}================================================${SIFIRLA}"
echo -e "${YESIL}  Yedekleme Tamamlandı: ${YEDEK_DOSYA}${SIFIRLA}"
echo -e "${YESIL}================================================${SIFIRLA}"
