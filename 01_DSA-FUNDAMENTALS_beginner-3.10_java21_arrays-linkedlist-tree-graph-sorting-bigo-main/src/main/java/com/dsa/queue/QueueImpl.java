package com.dsa.queue;

import java.util.*;

/**
 * QUEUE — FIFO (First In First Out)
 *
 * Big O:
 *   Enqueue → O(1)
 *   Dequeue → O(1)
 *   Peek    → O(1)
 */
public class QueueImpl {

    // ----------------------------------------------------------------
    // Array tabanlı Circular Queue implementasyonu
    // ----------------------------------------------------------------
    static class CircularQueue {
        private final int[] data;
        private int front, rear, size;

        CircularQueue(int capacity) { data = new int[capacity]; }

        public boolean enqueue(int val) {
            if (size == data.length) return false;
            data[rear] = val;
            rear = (rear + 1) % data.length;
            size++;
            return true;
        }

        public int dequeue() {
            if (size == 0) throw new RuntimeException("Queue empty");
            int val = data[front];
            front = (front + 1) % data.length;
            size--;
            return val;
        }

        public int peek() { return data[front]; }
        public boolean isEmpty() { return size == 0; }
        public int size() { return size; }
    }

    // ----------------------------------------------------------------
    // 1. Stack kullanarak Queue implementasyonu
    // Enqueue: O(1) | Dequeue: Amortized O(1)
    // ----------------------------------------------------------------
    static class QueueUsingStacks {
        private final Deque<Integer> inbox = new ArrayDeque<>();
        private final Deque<Integer> outbox = new ArrayDeque<>();

        public void enqueue(int val) { inbox.push(val); }

        public int dequeue() {
            if (outbox.isEmpty()) {
                while (!inbox.isEmpty()) outbox.push(inbox.pop());
            }
            return outbox.pop();
        }

        public int peek() {
            if (outbox.isEmpty()) {
                while (!inbox.isEmpty()) outbox.push(inbox.pop());
            }
            return outbox.peek();
        }

        public boolean isEmpty() { return inbox.isEmpty() && outbox.isEmpty(); }
    }

    // ----------------------------------------------------------------
    // 2. Binary Tree Level Order Traversal (BFS)
    // Time: O(n) | Space: O(n)
    // ----------------------------------------------------------------
    static class TreeNode {
        int val;
        TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    public List<List<Integer>> levelOrder(TreeNode root) {
        List<List<Integer>> result = new ArrayList<>();
        if (root == null) return result;
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);
        while (!queue.isEmpty()) {
            int size = queue.size();
            List<Integer> level = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                TreeNode node = queue.poll();
                level.add(node.val);
                if (node.left != null) queue.offer(node.left);
                if (node.right != null) queue.offer(node.right);
            }
            result.add(level);
        }
        return result;
    }

    // ----------------------------------------------------------------
    // 3. Sliding Window Maximum (Monotonic Deque)
    // Time: O(n) | Space: O(k)
    // ----------------------------------------------------------------
    public int[] maxSlidingWindow(int[] nums, int k) {
        int n = nums.length;
        int[] result = new int[n - k + 1];
        Deque<Integer> deque = new ArrayDeque<>();
        for (int i = 0; i < n; i++) {
            while (!deque.isEmpty() && deque.peekFirst() < i - k + 1)
                deque.pollFirst();
            while (!deque.isEmpty() && nums[deque.peekLast()] < nums[i])
                deque.pollLast();
            deque.offerLast(i);
            if (i >= k - 1) result[i - k + 1] = nums[deque.peekFirst()];
        }
        return result;
    }

    // ----------------------------------------------------------------
    // 4. Priority Queue — En Sık K Eleman
    // Time: O(n log k) | Space: O(n)
    // ----------------------------------------------------------------
    public int[] topKFrequent(int[] nums, int k) {
        Map<Integer, Integer> freq = new HashMap<>();
        for (int n : nums) freq.merge(n, 1, Integer::sum);
        PriorityQueue<Integer> minHeap = new PriorityQueue<>(Comparator.comparingInt(freq::get));
        for (int num : freq.keySet()) {
            minHeap.offer(num);
            if (minHeap.size() > k) minHeap.poll();
        }
        int[] result = new int[k];
        for (int i = k - 1; i >= 0; i--) result[i] = minHeap.poll();
        return result;
    }

    // ----------------------------------------------------------------
    // 5. Rotten Oranges — Multi-source BFS
    // Time: O(m*n) | Space: O(m*n)
    // ----------------------------------------------------------------
    public int orangesRotting(int[][] grid) {
        int rows = grid.length, cols = grid[0].length;
        Queue<int[]> queue = new LinkedList<>();
        int fresh = 0;
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] == 2) queue.offer(new int[]{r, c});
                else if (grid[r][c] == 1) fresh++;
            }
        int[][] dirs = {{0,1},{0,-1},{1,0},{-1,0}};
        int minutes = 0;
        while (!queue.isEmpty() && fresh > 0) {
            minutes++;
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                int[] pos = queue.poll();
                for (int[] d : dirs) {
                    int r = pos[0] + d[0], c = pos[1] + d[1];
                    if (r >= 0 && r < rows && c >= 0 && c < cols && grid[r][c] == 1) {
                        grid[r][c] = 2;
                        fresh--;
                        queue.offer(new int[]{r, c});
                    }
                }
            }
        }
        return fresh == 0 ? minutes : -1;
    }

    public static void main(String[] args) {
        QueueImpl q = new QueueImpl();

        CircularQueue cq = new CircularQueue(5);
        cq.enqueue(1); cq.enqueue(2); cq.enqueue(3);
        System.out.println("Dequeue: " + cq.dequeue() + ", Size: " + cq.size());

        QueueUsingStacks qs = new QueueUsingStacks();
        qs.enqueue(1); qs.enqueue(2); qs.enqueue(3);
        System.out.println("Queue peek: " + qs.peek() + ", dequeue: " + qs.dequeue());

        System.out.println("TopKFrequent k=2 [1,1,1,2,2,3]: " + Arrays.toString(q.topKFrequent(new int[]{1,1,1,2,2,3}, 2)));
        System.out.println("MaxSlidingWindow k=3 [1,3,-1,-3,5,3,6,7]: " + Arrays.toString(q.maxSlidingWindow(new int[]{1,3,-1,-3,5,3,6,7}, 3)));
    }
}
