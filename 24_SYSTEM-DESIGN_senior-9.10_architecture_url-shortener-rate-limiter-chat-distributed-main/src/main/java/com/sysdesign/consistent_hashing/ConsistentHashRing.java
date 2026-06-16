package com.sysdesign.consistent_hashing;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Consistent Hashing — Dağıtık sistemlerde node ekleme/çıkarma
 *
 * Problem: Modulo hash (hash(key) % N) — N değişince tüm key'ler yeniden dağıtılır.
 * Çözüm: Hash ring — node sadece komşu key'leri etkiler.
 *
 * Virtual Nodes (vnodes):
 *   - Her fiziksel node → K sanal node
 *   - Ring'e daha dengeli dağılım
 *   - Node ekleme/çıkarmada daha az yük kayması
 *
 * Kullanım alanları:
 *   - Cassandra, DynamoDB (partition)
 *   - Redis Cluster
 *   - CDN (Akamai)
 *   - Load balancer (session affinity)
 */
public class ConsistentHashRing {

    private final TreeMap<Long, String> ring = new TreeMap<>();
    private final int virtualNodes;
    private final MessageDigest md;

    public ConsistentHashRing(int virtualNodes) {
        this.virtualNodes = virtualNodes;
        try {
            this.md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public void addNode(String node) {
        for (int i = 0; i < virtualNodes; i++) {
            long hash = hash(node + "#" + i);
            ring.put(hash, node);
        }
    }

    public void removeNode(String node) {
        for (int i = 0; i < virtualNodes; i++) {
            long hash = hash(node + "#" + i);
            ring.remove(hash);
        }
    }

    // Key için sorumlu node'u bul
    public String getNode(String key) {
        if (ring.isEmpty()) throw new IllegalStateException("No nodes in ring");
        long hash = hash(key);
        Map.Entry<Long, String> entry = ring.ceilingEntry(hash);
        // Ring wrap-around: sağda node yoksa en soldaki
        return entry != null ? entry.getValue() : ring.firstEntry().getValue();
    }

    // Key için N replica node (replication)
    public List<String> getReplicaNodes(String key, int replicas) {
        List<String> nodes = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        long hash = hash(key);

        NavigableMap<Long, String> tailMap = ring.tailMap(hash);
        Iterator<Map.Entry<Long, String>> it = tailMap.entrySet().iterator();

        while (nodes.size() < replicas) {
            if (!it.hasNext()) {
                it = ring.entrySet().iterator(); // wrap
            }
            if (!it.hasNext()) break;
            String node = it.next().getValue();
            if (seen.add(node)) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    // Ring dağılımı (debug)
    public Map<String, Long> getDistribution() {
        Map<String, Long> dist = new HashMap<>();
        ring.values().forEach(node -> dist.merge(node, 1L, Long::sum));
        return dist;
    }

    public int size() { return ring.size(); }

    private long hash(String key) {
        md.reset();
        md.update(key.getBytes());
        byte[] digest = md.digest();
        long hash = 0;
        for (int i = 0; i < 4; i++) {
            hash <<= 8;
            hash |= (digest[i] & 0xFF);
        }
        return hash;
    }
}
