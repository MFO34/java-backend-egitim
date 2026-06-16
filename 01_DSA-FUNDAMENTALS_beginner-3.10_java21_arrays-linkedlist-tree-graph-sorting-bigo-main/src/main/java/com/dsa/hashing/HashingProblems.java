package com.dsa.hashing;

import java.util.*;

/**
 * HASHING — HashMap, HashSet implementasyonu ve mülakat problemleri
 *
 * Big O (HashMap):
 *   Get    → O(1) amortized
 *   Put    → O(1) amortized
 *   Delete → O(1) amortized
 *   Worst  → O(n) (hash collision)
 */
public class HashingProblems {

    // ----------------------------------------------------------------
    // HashMap implementasyonu (Separate Chaining)
    // ----------------------------------------------------------------
    static class SimpleHashMap<K, V> {
        private static final int CAPACITY = 16;
        private final List<List<Object[]>> buckets;

        @SuppressWarnings("unchecked")
        SimpleHashMap() {
            buckets = new ArrayList<>(CAPACITY);
            for (int i = 0; i < CAPACITY; i++) buckets.add(new ArrayList<>());
        }

        private int hash(K key) { return Math.abs(key.hashCode()) % CAPACITY; }

        public void put(K key, V value) {
            int idx = hash(key);
            for (Object[] entry : buckets.get(idx))
                if (entry[0].equals(key)) { entry[1] = value; return; }
            buckets.get(idx).add(new Object[]{key, value});
        }

        @SuppressWarnings("unchecked")
        public V get(K key) {
            for (Object[] entry : buckets.get(hash(key)))
                if (entry[0].equals(key)) return (V) entry[1];
            return null;
        }

        public void remove(K key) {
            buckets.get(hash(key)).removeIf(entry -> entry[0].equals(key));
        }
    }

    // ----------------------------------------------------------------
    // 1. Anagram Grupları
    // Time: O(n * k log k) | Space: O(n)
    // ----------------------------------------------------------------
    public List<List<String>> groupAnagrams(String[] strs) {
        Map<String, List<String>> map = new HashMap<>();
        for (String s : strs) {
            char[] chars = s.toCharArray();
            Arrays.sort(chars);
            String key = new String(chars);
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
        }
        return new ArrayList<>(map.values());
    }

    // ----------------------------------------------------------------
    // 2. Longest Consecutive Sequence
    // Time: O(n) | Space: O(n)
    // ----------------------------------------------------------------
    public int longestConsecutive(int[] nums) {
        Set<Integer> set = new HashSet<>();
        for (int n : nums) set.add(n);
        int maxLen = 0;
        for (int n : set) {
            if (!set.contains(n - 1)) {
                int len = 1;
                while (set.contains(n + len)) len++;
                maxLen = Math.max(maxLen, len);
            }
        }
        return maxLen;
    }

    // ----------------------------------------------------------------
    // 3. Subarray Sum Equals K
    // Time: O(n) | Space: O(n)
    // ----------------------------------------------------------------
    public int subarraySum(int[] nums, int k) {
        Map<Integer, Integer> prefixCount = new HashMap<>();
        prefixCount.put(0, 1);
        int sum = 0, count = 0;
        for (int num : nums) {
            sum += num;
            count += prefixCount.getOrDefault(sum - k, 0);
            prefixCount.merge(sum, 1, Integer::sum);
        }
        return count;
    }

    // ----------------------------------------------------------------
    // 4. LRU Cache implementasyonu
    // Tüm operasyonlar O(1)
    // ----------------------------------------------------------------
    static class LRUCache {
        private final int capacity;
        private final Map<Integer, Integer> cache;

        LRUCache(int capacity) {
            this.capacity = capacity;
            this.cache = new LinkedHashMap<>(capacity, 0.75f, true) {
                protected boolean removeEldestEntry(Map.Entry<Integer, Integer> eldest) {
                    return size() > capacity;
                }
            };
        }

        public int get(int key) { return cache.getOrDefault(key, -1); }
        public void put(int key, int value) { cache.put(key, value); }
    }

    // ----------------------------------------------------------------
    // 5. First Non-Repeating Character
    // Time: O(n) | Space: O(1)
    // ----------------------------------------------------------------
    public char firstNonRepeating(String s) {
        Map<Character, Integer> freq = new LinkedHashMap<>();
        for (char c : s.toCharArray()) freq.merge(c, 1, Integer::sum);
        for (Map.Entry<Character, Integer> e : freq.entrySet())
            if (e.getValue() == 1) return e.getKey();
        return '#';
    }

    // ----------------------------------------------------------------
    // 6. Contains Duplicate Within K Distance
    // Time: O(n) | Space: O(k)
    // ----------------------------------------------------------------
    public boolean containsNearbyDuplicate(int[] nums, int k) {
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            if (map.containsKey(nums[i]) && i - map.get(nums[i]) <= k) return true;
            map.put(nums[i], i);
        }
        return false;
    }

    public static void main(String[] args) {
        HashingProblems hp = new HashingProblems();

        SimpleHashMap<String, Integer> map = new SimpleHashMap<>();
        map.put("java", 1); map.put("spring", 2); map.put("kafka", 3);
        System.out.println("HashMap get 'spring': " + map.get("spring"));

        System.out.println("GroupAnagrams: " + hp.groupAnagrams(new String[]{"eat","tea","tan","ate","nat","bat"}));
        System.out.println("LongestConsecutive [100,4,200,1,3,2]: " + hp.longestConsecutive(new int[]{100,4,200,1,3,2}));
        System.out.println("SubarraySum k=2 [1,1,1]: " + hp.subarraySum(new int[]{1,1,1}, 2));
        System.out.println("FirstNonRepeating 'leetcode': " + hp.firstNonRepeating("leetcode"));

        LRUCache lru = new LRUCache(3);
        lru.put(1, 10); lru.put(2, 20); lru.put(3, 30);
        System.out.println("LRU get(1): " + lru.get(1));
        lru.put(4, 40);
        System.out.println("LRU get(2) after eviction: " + lru.get(2));
    }
}
