#!/bin/bash
# ==============================================================================
# PostgreSQL Volume Geri Yükleme Betiği
# ==============================================================================
# Kullanım: ./scripts/restore.sh <yedek_dosyasi.sql.gz>
# veya Makefile: make db-restore BACKUP_FILE=backups/db_yedek_2024-01-15_10-00-00.sql.gz
#
# Bu betik şunları yapar:
#   1. Belirtilen yedek dosyasını doğrular
#   2. Kullanıcıdan onay ister (mevcut veri silinir!)
#   3. Veritabanını kaldırır ve yeniden oluşturur
#   4. Yedek dosyasını gzip ile açar ve psql ile geri yükler
# ==============================================================================

# Hata durumunda betiği durdur
set -e

# ── RENKLİ ÇIKTI ─────────────────────────────────────────────────────────────
YESIL='\033[0;32m'    # Başarı
SARI='\033[1;33m'     # Uyarı
KIRMIZI='\033[0;31m'  # Hata
SIFIRLA='\033[0m'     # Sıfırla

# ── PARAMETRE KONTROLÜ ────────────────────────────────────────────────────────
# Yedek dosya adı parametre olarak verilmeli
if [ -z "$1" ]; then
    # Parametre verilmedi - yardım göster
    echo -e "${KIRMIZI}HATA: Yedek dosyası belirtilmedi!${SIFIRLA}"
    echo ""
    echo "Kullanım: $0 <yedek_dosyasi.sql.gz>"
    echo "Örnek:    $0 backups/db_yedek_2024-01-15_10-00-00.sql.gz"
    echo ""
    echo "Mevcut yedekler:"
    # Mevcut yedekleri listele
    ls -lh backups/db_yedek_*.sql.gz 2>/dev/null || echo "  Yedek bulunamadı"
    exit 1
fi

# Yedek dosya yolu
YEDEK_DOSYA="$1"

# ── YAPILANDIRMA ─────────────────────────────────────────────────────────────
# .env dosyasından değişkenleri yükle
if [ -f ".env" ]; then
    source .env
fi

# Veritabanı bağlantı bilgileri
DB_CONTAINER="${DB_CONTAINER:-docker-demo-postgres}"
DB_USERNAME="${DB_USERNAME:-postgres}"
DB_NAME="${DB_NAME:-dockerdb}"

# ── YEDEK DOSYASINI DOĞRULA ──────────────────────────────────────────────────
# Dosya var mı?
if [ ! -f "${YEDEK_DOSYA}" ]; then
    echo -e "${KIRMIZI}HATA: Yedek dosyası bulunamadı: ${YEDEK_DOSYA}${SIFIRLA}"
    exit 1
fi

# Dosya gzip formatında mı?
if ! gzip -t "${YEDEK_DOSYA}" 2>/dev/null; then
    echo -e "${KIRMIZI}HATA: Dosya geçerli bir gzip dosyası değil: ${YEDEK_DOSYA}${SIFIRLA}"
    exit 1
fi

# ── BAŞLANGIÇ BİLGİSİ ────────────────────────────────────────────────────────
echo -e "${SARI}================================================${SIFIRLA}"
echo -e "${SARI}  PostgreSQL Geri Yükleme${SIFIRLA}"
echo -e "${SARI}================================================${SIFIRLA}"
echo ""
echo "  Konteyner : ${DB_CONTAINER}"
echo "  Veritabanı: ${DB_NAME}"
echo "  Kaynak    : ${YEDEK_DOSYA} ($(du -sh "${YEDEK_DOSYA}" | cut -f1))"
echo "  Zaman     : $(date)"
echo ""

# ── UYARI VE ONAY ────────────────────────────────────────────────────────────
# Kullanıcıyı uyar - mevcut veri silinecek!
echo -e "${KIRMIZI}⚠  UYARI: Bu işlem '${DB_NAME}' veritabanındaki${SIFIRLA}"
echo -e "${KIRMIZI}   TÜM MEVCUT VERİYİ SİLECEK ve yedekten geri yükleyecek!${SIFIRLA}"
echo ""
echo -n "Devam etmek istediğinizden emin misiniz? (evet/hayır): "
read ONAY

# Kullanıcı "evet" yazmadıysa iptal et
if [ "${ONAY}" != "evet" ]; then
    echo "Geri yükleme iptal edildi."
    exit 0
fi

# ── KONTEYNER ÇALIŞIYOR MU? ──────────────────────────────────────────────────
if ! docker ps --format '{{.Names}}' | grep -q "^${DB_CONTAINER}$"; then
    echo -e "${KIRMIZI}HATA: '${DB_CONTAINER}' konteyneri çalışmıyor!${SIFIRLA}"
    exit 1
fi

# ── GERİ YÜKLEME ─────────────────────────────────────────────────────────────
echo ""
echo "Geri yükleme başlıyor..."

# gunzip: Gzip dosyasını aç ve içeriği stdout'a gönder
# | docker exec -i: Açık içeriği konteyner içindeki psql'e gönder
# psql: SQL dosyasını çalıştır
#   -U: Kullanıcı adı
#   -d: Veritabanı adı
#   --no-password: Şifre sorma
gunzip -c "${YEDEK_DOSYA}" | docker exec -i "${DB_CONTAINER}" \
    psql \
    -U "${DB_USERNAME}" \
    -d "${DB_NAME}" \
    --no-password \
    --quiet             # Satır satır çıktı gösterme (büyük dosyalarda yavaşlatır)

# ── SONUÇ ────────────────────────────────────────────────────────────────────
echo -e "${YESIL}✓ Geri yükleme başarıyla tamamlandı!${SIFIRLA}"
echo ""
echo "  Kaynak  : ${YEDEK_DOSYA}"
echo "  Hedef   : ${DB_NAME} @ ${DB_CONTAINER}"
echo "  Zaman   : $(date)"
echo ""
echo -e "${YESIL}================================================${SIFIRLA}"
