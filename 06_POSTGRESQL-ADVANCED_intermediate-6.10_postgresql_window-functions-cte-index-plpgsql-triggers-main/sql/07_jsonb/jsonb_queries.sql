-- ============================================================
-- 07 — JSONB — Yarı yapısal veri
-- JSON vs JSONB: JSONB binary, indexlenebilir, daha hızlı
-- ============================================================

-- JSONB kolonlu tablo
CREATE TABLE sales.product_metadata (
    id         SERIAL PRIMARY KEY,
    product_id INTEGER REFERENCES sales.products(id),
    attributes JSONB,
    tags       JSONB,  -- array
    created_at TIMESTAMPTZ DEFAULT NOW()
);

INSERT INTO sales.product_metadata (product_id, attributes, tags) VALUES
(1, '{"brand": "Apple", "ram": "16GB", "storage": "512GB", "color": "Space Gray", "warranty": 2}',
    '["laptop", "apple", "premium", "m3-pro"]'),
(2, '{"brand": "Apple", "storage": "256GB", "color": "Titanium", "camera": "48MP"}',
    '["phone", "apple", "iphone15"]'),
(3, '{"brand": "Apple", "storage": "256GB", "chip": "M2"}',
    '["tablet", "apple", "ipad"]');

-- ----------------------------------------------------------------
-- JSONB Operatörleri
-- ----------------------------------------------------------------
-- Alan erişimi
SELECT attributes->'brand'           AS brand_json   FROM sales.product_metadata WHERE id = 1;
SELECT attributes->>'brand'          AS brand_text   FROM sales.product_metadata WHERE id = 1;
SELECT attributes->'warranty'        AS warranty     FROM sales.product_metadata WHERE id = 1;
SELECT (attributes->>'warranty')::INTEGER FROM sales.product_metadata WHERE id = 1;

-- İç içe erişim
SELECT attributes #> '{specs,display}'  FROM sales.product_metadata;
SELECT attributes #>> '{brand}'         FROM sales.product_metadata;

-- Key varlık kontrolü
SELECT * FROM sales.product_metadata WHERE attributes ? 'warranty';       -- key var mı
SELECT * FROM sales.product_metadata WHERE attributes ?| ARRAY['ram', 'camera']; -- herhangi biri var mı
SELECT * FROM sales.product_metadata WHERE attributes ?& ARRAY['brand', 'storage']; -- hepsi var mı

-- Containment
SELECT * FROM sales.product_metadata WHERE attributes @> '{"brand": "Apple"}'; -- Apple mı
SELECT * FROM sales.product_metadata WHERE tags @> '["laptop"]';               -- laptop tag'i var mı

-- ----------------------------------------------------------------
-- JSONB Filtreleme & Dönüşüm
-- ----------------------------------------------------------------
SELECT
    p.name,
    pm.attributes->>'brand'   AS brand,
    pm.attributes->>'color'   AS color,
    (pm.attributes->>'warranty')::INTEGER AS warranty_years
FROM sales.product_metadata pm
JOIN sales.products p ON p.id = pm.product_id
WHERE (pm.attributes->>'warranty')::INTEGER >= 2;

-- ----------------------------------------------------------------
-- JSONB Array işlemleri
-- ----------------------------------------------------------------
-- Array elemanlarını satır olarak aç
SELECT p.name, jsonb_array_elements_text(pm.tags) AS tag
FROM sales.product_metadata pm
JOIN sales.products p ON p.id = pm.product_id;

-- Array uzunluğu
SELECT id, jsonb_array_length(tags) AS tag_count FROM sales.product_metadata;

-- ----------------------------------------------------------------
-- JSONB Güncelleme
-- ----------------------------------------------------------------
-- Alan ekle/güncelle
UPDATE sales.product_metadata
SET attributes = attributes || '{"new_field": "new_value", "warranty": 3}'
WHERE id = 1;

-- Derin güncelleme
UPDATE sales.product_metadata
SET attributes = jsonb_set(attributes, '{warranty}', '3')
WHERE id = 1;

-- Alan sil
UPDATE sales.product_metadata
SET attributes = attributes - 'old_field'
WHERE id = 1;

-- ----------------------------------------------------------------
-- GIN Index ile JSONB arama
-- ----------------------------------------------------------------
CREATE INDEX idx_product_meta_attrs ON sales.product_metadata USING GIN(attributes);
CREATE INDEX idx_product_meta_tags  ON sales.product_metadata USING GIN(tags);

-- Index kullanan sorgu
EXPLAIN ANALYZE
SELECT * FROM sales.product_metadata WHERE attributes @> '{"brand": "Apple"}';

-- ----------------------------------------------------------------
-- JSONB Aggregation
-- ----------------------------------------------------------------
SELECT
    attributes->>'brand' AS brand,
    COUNT(*) AS product_count,
    jsonb_agg(attributes->>'color') AS colors
FROM sales.product_metadata
GROUP BY attributes->>'brand';

-- Row'ları JSONB'ye dönüştür
SELECT jsonb_agg(row_to_json(e.*)) AS employees_json
FROM hr.employees e
WHERE department_id = 1;
