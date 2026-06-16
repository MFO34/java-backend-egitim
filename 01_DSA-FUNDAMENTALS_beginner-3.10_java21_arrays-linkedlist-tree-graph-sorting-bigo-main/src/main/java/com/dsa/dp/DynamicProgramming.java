package com.dsa.dp;

import java.util.Arrays;

/**
 * DYNAMIC PROGRAMMING — Memoization & Tabulation
 *
 * DP ne zaman kullanılır?
 *   1. Optimal alt yapı (optimal substructure)
 *   2. Örtüşen alt problemler (overlapping subproblems)
 *
 * Yaklaşımlar:
 *   Top-Down (Memoization) → recursive + cache
 *   Bottom-Up (Tabulation) → iterative + table
 */
public class DynamicProgramming {

    // ----------------------------------------------------------------
    // 1. Fibonacci — Klasik DP başlangıcı
    // Brute Force: O(2^n) | DP: O(n) time, O(1) space
    // ----------------------------------------------------------------
    public long fibonacci(int n) {
        if (n <= 1) return n;
        long prev = 0, curr = 1;
        for (int i = 2; i <= n; i++) {
            long next = prev + curr;
            prev = curr;
            curr = next;
        }
        return curr;
    }

    // ----------------------------------------------------------------
    // 2. Climbing Stairs
    // Time: O(n) | Space: O(1)
    // ----------------------------------------------------------------
    public int climbStairs(int n) {
        if (n <= 2) return n;
        int prev = 1, curr = 2;
        for (int i = 3; i <= n; i++) {
            int next = prev + curr;
            prev = curr;
            curr = next;
        }
        return curr;
    }

    // ----------------------------------------------------------------
    // 3. Coin Change — Minimum para sayısı
    // Time: O(amount * coins.length) | Space: O(amount)
    // ----------------------------------------------------------------
    public int coinChange(int[] coins, int amount) {
        int[] dp = new int[amount + 1];
        Arrays.fill(dp, amount + 1);
        dp[0] = 0;
        for (int i = 1; i <= amount; i++)
            for (int coin : coins)
                if (coin <= i) dp[i] = Math.min(dp[i], dp[i - coin] + 1);
        return dp[amount] > amount ? -1 : dp[amount];
    }

    // ----------------------------------------------------------------
    // 4. Longest Common Subsequence (LCS)
    // Time: O(m*n) | Space: O(m*n)
    // ----------------------------------------------------------------
    public int lcs(String s1, String s2) {
        int m = s1.length(), n = s2.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++)
            for (int j = 1; j <= n; j++)
                if (s1.charAt(i-1) == s2.charAt(j-1)) dp[i][j] = dp[i-1][j-1] + 1;
                else dp[i][j] = Math.max(dp[i-1][j], dp[i][j-1]);
        return dp[m][n];
    }

    // ----------------------------------------------------------------
    // 5. Longest Increasing Subsequence (LIS)
    // Time: O(n log n) | Space: O(n)  — Binary Search optimizasyonu
    // ----------------------------------------------------------------
    public int lengthOfLIS(int[] nums) {
        int[] tails = new int[nums.length];
        int size = 0;
        for (int num : nums) {
            int left = 0, right = size;
            while (left < right) {
                int mid = (left + right) / 2;
                if (tails[mid] < num) left = mid + 1;
                else right = mid;
            }
            tails[left] = num;
            if (left == size) size++;
        }
        return size;
    }

    // ----------------------------------------------------------------
    // 6. 0/1 Knapsack
    // Time: O(n*W) | Space: O(W)
    // ----------------------------------------------------------------
    public int knapsack(int[] weights, int[] values, int capacity) {
        int n = weights.length;
        int[] dp = new int[capacity + 1];
        for (int i = 0; i < n; i++)
            for (int w = capacity; w >= weights[i]; w--)
                dp[w] = Math.max(dp[w], dp[w - weights[i]] + values[i]);
        return dp[capacity];
    }

    // ----------------------------------------------------------------
    // 7. Edit Distance (Levenshtein)
    // Time: O(m*n) | Space: O(m*n)
    // ----------------------------------------------------------------
    public int editDistance(String word1, String word2) {
        int m = word1.length(), n = word2.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;
        for (int i = 1; i <= m; i++)
            for (int j = 1; j <= n; j++)
                if (word1.charAt(i-1) == word2.charAt(j-1)) dp[i][j] = dp[i-1][j-1];
                else dp[i][j] = 1 + Math.min(dp[i-1][j-1], Math.min(dp[i-1][j], dp[i][j-1]));
        return dp[m][n];
    }

    // ----------------------------------------------------------------
    // 8. House Robber
    // Time: O(n) | Space: O(1)
    // ----------------------------------------------------------------
    public int rob(int[] nums) {
        int prev = 0, curr = 0;
        for (int num : nums) {
            int next = Math.max(curr, prev + num);
            prev = curr;
            curr = next;
        }
        return curr;
    }

    // ----------------------------------------------------------------
    // 9. Unique Paths (Grid DP)
    // Time: O(m*n) | Space: O(n)
    // ----------------------------------------------------------------
    public int uniquePaths(int m, int n) {
        int[] dp = new int[n];
        Arrays.fill(dp, 1);
        for (int i = 1; i < m; i++)
            for (int j = 1; j < n; j++)
                dp[j] += dp[j - 1];
        return dp[n - 1];
    }

    // ----------------------------------------------------------------
    // 10. Palindromic Substrings — Expand Around Center
    // Time: O(n²) | Space: O(1)
    // ----------------------------------------------------------------
    public int countSubstrings(String s) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            count += expand(s, i, i);   // tek
            count += expand(s, i, i+1); // çift
        }
        return count;
    }

    private int expand(String s, int left, int right) {
        int count = 0;
        while (left >= 0 && right < s.length() && s.charAt(left) == s.charAt(right)) {
            count++;
            left--;
            right++;
        }
        return count;
    }

    public static void main(String[] args) {
        DynamicProgramming dp = new DynamicProgramming();

        System.out.println("=== DYNAMIC PROGRAMMING ===");
        System.out.println("Fibonacci(10): " + dp.fibonacci(10));
        System.out.println("ClimbStairs(5): " + dp.climbStairs(5));
        System.out.println("CoinChange [1,5,11] amount=15: " + dp.coinChange(new int[]{1,5,11}, 15));
        System.out.println("LCS 'abcde' 'ace': " + dp.lcs("abcde", "ace"));
        System.out.println("LIS [10,9,2,5,3,7,101,18]: " + dp.lengthOfLIS(new int[]{10,9,2,5,3,7,101,18}));
        System.out.println("Knapsack W=5: " + dp.knapsack(new int[]{2,3,4,5}, new int[]{3,4,5,6}, 5));
        System.out.println("EditDistance 'horse' 'ros': " + dp.editDistance("horse", "ros"));
        System.out.println("HouseRobber [2,7,9,3,1]: " + dp.rob(new int[]{2,7,9,3,1}));
        System.out.println("UniquePaths 3x7: " + dp.uniquePaths(3, 7));
        System.out.println("PalindromicSubstrings 'abc': " + dp.countSubstrings("abc"));
    }
}
