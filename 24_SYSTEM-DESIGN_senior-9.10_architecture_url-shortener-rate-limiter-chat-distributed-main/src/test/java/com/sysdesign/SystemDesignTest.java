package com.sysdesign;

import com.sysdesign.bloom_filter.BloomFilter;
import com.sysdesign.consistent_hashing.ConsistentHashRing;
import com.sysdesign.ratelimiter.algorithm.SlidingWindowRateLimiter;
import com.sysdesign.ratelimiter.algorithm.TokenBucketRateLimiter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SystemDesignTest {

    // ── URL Shortener ─────────────────────────────────────────────────────────

    @Test
    void base62_encodeAndDecode() {
        // Need UrlShortenerService without Redis — test standalone
        var service = new com.sysdesign.urlshortener.service.UrlShortenerService(null) {
            // override Redis calls — test only Base62
        };
        // Manual Base62 test
        String code = service.toBase62(10_000_001L);
        long decoded = service.fromBase62(code);
        assertThat(code).hasSize(7);
        // different IDs → different codes
        String code2 = service.toBase62(10_000_002L);
        assertThat(code).isNotEqualTo(code2);
    }

    // ── Rate Limiter ──────────────────────────────────────────────────────────

    @Test
    void tokenBucket_allowsUpToCapacity() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(5, 1);

        // 5 istek geçmeli
        for (int i = 0; i < 5; i++) {
            assertThat(limiter.allowRequest("user1")).isTrue();
        }
        // 6. istek bloklanmalı
        assertThat(limiter.allowRequest("user1")).isFalse();
    }

    @Test
    void tokenBucket_differentClients_independent() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(3, 1);

        for (int i = 0; i < 3; i++) limiter.allowRequest("client-A");
        assertThat(limiter.allowRequest("client-A")).isFalse();

        // client-B etkilenmemeli
        assertThat(limiter.allowRequest("client-B")).isTrue();
    }

    @Test
    void slidingWindow_limitsRequests() {
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(3, 60_000L);

        for (int i = 0; i < 3; i++) assertThat(limiter.allowRequest("user")).isTrue();
        assertThat(limiter.allowRequest("user")).isFalse();
        assertThat(limiter.getCurrentCount("user")).isEqualTo(3);
    }

    // ── Consistent Hashing ────────────────────────────────────────────────────

    @Test
    void consistentHash_addNodes_distributesKeys() {
        ConsistentHashRing ring = new ConsistentHashRing(100);
        ring.addNode("node1");
        ring.addNode("node2");
        ring.addNode("node3");

        assertThat(ring.size()).isEqualTo(300); // 3 nodes × 100 vnodes

        // Tüm key'ler bir node'a atanmalı
        Set<String> validNodes = Set.of("node1", "node2", "node3");
        for (int i = 0; i < 100; i++) {
            assertThat(validNodes).contains(ring.getNode("key-" + i));
        }
    }

    @Test
    void consistentHash_removeNode_redistributes() {
        ConsistentHashRing ring = new ConsistentHashRing(50);
        ring.addNode("node1");
        ring.addNode("node2");
        ring.addNode("node3");

        String original = ring.getNode("test-key");
        ring.removeNode(original);

        // Başka bir node'a gitmiş olmalı
        String newNode = ring.getNode("test-key");
        assertThat(newNode).isNotEqualTo(original);
    }

    @Test
    void consistentHash_replicaNodes() {
        ConsistentHashRing ring = new ConsistentHashRing(100);
        ring.addNode("node1");
        ring.addNode("node2");
        ring.addNode("node3");

        List<String> replicas = ring.getReplicaNodes("my-key", 3);
        assertThat(replicas).hasSize(3);
        // Tüm node'lar farklı olmalı
        assertThat(replicas).doesNotHaveDuplicates();
    }

    // ── Bloom Filter ──────────────────────────────────────────────────────────

    @Test
    void bloomFilter_addedElement_mightContain() {
        BloomFilter filter = new BloomFilter(1000, 0.01); // 1% false positive

        filter.add("url-1");
        filter.add("url-2");
        filter.add("url-3");

        assertThat(filter.mightContain("url-1")).isTrue();
        assertThat(filter.mightContain("url-2")).isTrue();
        assertThat(filter.mightContain("url-3")).isTrue();
    }

    @Test
    void bloomFilter_notAdded_doesNotContain() {
        BloomFilter filter = new BloomFilter(1000, 0.01);

        filter.add("exists");

        // Kesinlikle yok — false negative imkânsız
        assertThat(filter.mightContain("definitely-not-exists-1234567890")).isFalse();
    }

    @Test
    void bloomFilter_optimalParameters() {
        BloomFilter filter = new BloomFilter(10_000, 0.01);
        System.out.printf("Bloom Filter: size=%d bits (%.1f KB), hashCount=%d%n",
                filter.getBitSetSize(),
                filter.getBitSetSize() / 8.0 / 1024,
                filter.getHashCount());
        assertThat(filter.getBitSetSize()).isGreaterThan(0);
        assertThat(filter.getHashCount()).isBetween(1, 20);
    }
}
