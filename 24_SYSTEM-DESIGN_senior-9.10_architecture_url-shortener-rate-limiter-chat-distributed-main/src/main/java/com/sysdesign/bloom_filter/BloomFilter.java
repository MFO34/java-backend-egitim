package com.sysdesign.bloom_filter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.BitSet;

/**
 * Bloom Filter — Probabilistic membership test
 *
 * Özellikler:
 *   - "Yok" → kesinlikle yok (false negative imkânsız)
 *   - "Var" → muhtemelen var (false positive mümkün)
 *   - Space-efficient: milyonlarca eleman, kilobytes
 *   - Silme yok (Counting Bloom Filter gerekir)
 *
 * Kullanım alanları:
 *   - URL shortener: kısa kod kullanılmış mı? (DB'ye gitmeden)
 *   - Cache: cache miss mi? (unneeded DB call önle)
 *   - Cassandra: sstable'da row var mı? (disk okumadan)
 *   - Chrome malware URL filter
 *   - Bitcoin UTXO set
 *
 * False Positive Rate ≈ (1 - e^(-k*n/m))^k
 *   k = hash fonksiyon sayısı
 *   n = eleman sayısı
 *   m = bit array boyutu
 */
public class BloomFilter {

    private final BitSet bitSet;
    private final int size;
    private final int hashCount;

    public BloomFilter(int expectedElements, double falsePositiveRate) {
        // Optimal m = -n*ln(p) / (ln2)^2
        this.size = optimalSize(expectedElements, falsePositiveRate);
        // Optimal k = (m/n) * ln2
        this.hashCount = optimalHashCount(size, expectedElements);
        this.bitSet = new BitSet(size);
    }

    public void add(String element) {
        for (int seed : getSeeds()) {
            int index = Math.abs(murmurHash(element, seed) % size);
            bitSet.set(index);
        }
    }

    public boolean mightContain(String element) {
        for (int seed : getSeeds()) {
            int index = Math.abs(murmurHash(element, seed) % size);
            if (!bitSet.get(index)) return false; // kesinlikle yok
        }
        return true; // muhtemelen var
    }

    private int[] getSeeds() {
        int[] seeds = new int[hashCount];
        for (int i = 0; i < hashCount; i++) seeds[i] = i + 1;
        return seeds;
    }

    private int murmurHash(String data, int seed) {
        // Simplified hash (production'da MurmurHash3 kullan)
        int hash = seed;
        for (byte b : data.getBytes(StandardCharsets.UTF_8)) {
            hash = hash * 31 + b;
        }
        return hash;
    }

    private static int optimalSize(int n, double p) {
        return (int) Math.ceil(-n * Math.log(p) / (Math.log(2) * Math.log(2)));
    }

    private static int optimalHashCount(int m, int n) {
        return Math.max(1, (int) Math.round((double) m / n * Math.log(2)));
    }

    public int getBitSetSize() { return size; }
    public int getHashCount() { return hashCount; }
    public int getSetBitsCount() { return bitSet.cardinality(); }
}
