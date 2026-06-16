package com.dsa.arrays;

import java.util.*;

/**
 * ARRAYS — En sık sorulan mülakat problemleri
 *
 * Big O Hatırlatıcı:
 *   Erişim   → O(1)
 *   Arama    → O(n)
 *   Ekleme   → O(n)  (shift gerekir)
 *   Silme    → O(n)  (shift gerekir)
 */
public class ArrayProblems {

    // ----------------------------------------------------------------
    // 1. İki Sayının Toplamı (Two Sum)
    // Hedef: array içinde toplamı target olan iki indisi bul
    // Brute Force → O(n²) | HashMap ile → O(n)
    // ----------------------------------------------------------------
    public int[] twoSum(int[] nums, int target) {
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            int complement = target - nums[i];
            if (map.containsKey(complement)) {
                return new int[]{map.get(complement), i};
            }
            map.put(nums[i], i);
        }
        return new int[]{};
    }

    // ----------------------------------------------------------------
    // 2. Maximum Subarray (Kadane's Algorithm)
    // Hedef: ardışık elemanların en büyük toplamını bul
    // Time: O(n) | Space: O(1)
    // ----------------------------------------------------------------
    public int maxSubArray(int[] nums) {
        int maxSum = nums[0];
        int currentSum = nums[0];
        for (int i = 1; i < nums.length; i++) {
            currentSum = Math.max(nums[i], currentSum + nums[i]);
            maxSum = Math.max(maxSum, currentSum);
        }
        return maxSum;
    }

    // ----------------------------------------------------------------
    // 3. Sliding Window — Maksimum K Elemanlı Alt Dizi Toplamı
    // Time: O(n) | Space: O(1)
    // ----------------------------------------------------------------
    public int maxSumSubarrayOfSizeK(int[] nums, int k) {
        int windowSum = 0;
        int maxSum = 0;
        for (int i = 0; i < k; i++) windowSum += nums[i];
        maxSum = windowSum;
        for (int i = k; i < nums.length; i++) {
            windowSum += nums[i] - nums[i - k];
            maxSum = Math.max(maxSum, windowSum);
        }
        return maxSum;
    }

    // ----------------------------------------------------------------
    // 4. İki Pointer — Sıralı Dizide Hedef Toplamı Bul
    // Time: O(n) | Space: O(1)
    // ----------------------------------------------------------------
    public int[] twoSumSorted(int[] nums, int target) {
        int left = 0, right = nums.length - 1;
        while (left < right) {
            int sum = nums[left] + nums[right];
            if (sum == target) return new int[]{left + 1, right + 1};
            else if (sum < target) left++;
            else right--;
        }
        return new int[]{};
    }

    // ----------------------------------------------------------------
    // 5. Rotate Array — Diziyi K adım sağa döndür
    // Time: O(n) | Space: O(1)
    // ----------------------------------------------------------------
    public void rotate(int[] nums, int k) {
        k %= nums.length;
        reverse(nums, 0, nums.length - 1);
        reverse(nums, 0, k - 1);
        reverse(nums, k, nums.length - 1);
    }

    private void reverse(int[] nums, int start, int end) {
        while (start < end) {
            int temp = nums[start];
            nums[start++] = nums[end];
            nums[end--] = temp;
        }
    }

    // ----------------------------------------------------------------
    // 6. Prefix Sum — Dizi aralığı toplamı sorguları
    // Ön işleme: O(n) | Sorgu: O(1)
    // ----------------------------------------------------------------
    public static class PrefixSum {
        private final int[] prefix;

        public PrefixSum(int[] nums) {
            prefix = new int[nums.length + 1];
            for (int i = 0; i < nums.length; i++)
                prefix[i + 1] = prefix[i] + nums[i];
        }

        public int rangeSum(int left, int right) {
            return prefix[right + 1] - prefix[left];
        }
    }

    // ----------------------------------------------------------------
    // 7. Tekrar Eden Sayıyı Bul (Floyd's Cycle Detection)
    // Time: O(n) | Space: O(1)
    // ----------------------------------------------------------------
    public int findDuplicate(int[] nums) {
        int slow = nums[0], fast = nums[0];
        do {
            slow = nums[slow];
            fast = nums[nums[fast]];
        } while (slow != fast);
        slow = nums[0];
        while (slow != fast) {
            slow = nums[slow];
            fast = nums[fast];
        }
        return slow;
    }

    // ----------------------------------------------------------------
    // 8. Merge Intervals
    // Time: O(n log n) | Space: O(n)
    // ----------------------------------------------------------------
    public int[][] merge(int[][] intervals) {
        Arrays.sort(intervals, (a, b) -> a[0] - b[0]);
        List<int[]> result = new ArrayList<>();
        int[] current = intervals[0];
        for (int i = 1; i < intervals.length; i++) {
            if (intervals[i][0] <= current[1]) {
                current[1] = Math.max(current[1], intervals[i][1]);
            } else {
                result.add(current);
                current = intervals[i];
            }
        }
        result.add(current);
        return result.toArray(new int[0][]);
    }

    // ----------------------------------------------------------------
    // 9. Product of Array Except Self
    // Time: O(n) | Space: O(1) — bölme kullanmadan
    // ----------------------------------------------------------------
    public int[] productExceptSelf(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        result[0] = 1;
        for (int i = 1; i < n; i++)
            result[i] = result[i - 1] * nums[i - 1];
        int right = 1;
        for (int i = n - 1; i >= 0; i--) {
            result[i] *= right;
            right *= nums[i];
        }
        return result;
    }

    // ----------------------------------------------------------------
    // 10. Trapping Rain Water
    // Time: O(n) | Space: O(1)
    // ----------------------------------------------------------------
    public int trap(int[] height) {
        int left = 0, right = height.length - 1;
        int leftMax = 0, rightMax = 0, water = 0;
        while (left < right) {
            if (height[left] < height[right]) {
                if (height[left] >= leftMax) leftMax = height[left];
                else water += leftMax - height[left];
                left++;
            } else {
                if (height[right] >= rightMax) rightMax = height[right];
                else water += rightMax - height[right];
                right--;
            }
        }
        return water;
    }

    public static void main(String[] args) {
        ArrayProblems ap = new ArrayProblems();

        System.out.println("=== ARRAY PROBLEMS ===");

        System.out.println("TwoSum [2,7,11,15] target=9: " + Arrays.toString(ap.twoSum(new int[]{2, 7, 11, 15}, 9)));
        System.out.println("MaxSubArray [-2,1,-3,4,-1,2,1,-5,4]: " + ap.maxSubArray(new int[]{-2, 1, -3, 4, -1, 2, 1, -5, 4}));
        System.out.println("MaxSumK=3 [2,1,5,1,3,2]: " + ap.maxSumSubarrayOfSizeK(new int[]{2, 1, 5, 1, 3, 2}, 3));
        System.out.println("Trap Rain [0,1,0,2,1,0,1,3,2,1,2,1]: " + ap.trap(new int[]{0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1}));

        PrefixSum ps = new PrefixSum(new int[]{1, 2, 3, 4, 5});
        System.out.println("PrefixSum[1,4]: " + ps.rangeSum(1, 4));
    }
}
