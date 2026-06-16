package com.dsa.tree;

import java.util.*;

/**
 * TREE — Binary Tree, BST, ve mülakat problemleri
 *
 * Big O (Dengeli BST):
 *   Arama  → O(log n)
 *   Ekleme → O(log n)
 *   Silme  → O(log n)
 *
 * Big O (Dengesiz / Worst Case):
 *   Arama  → O(n)
 */
public class TreeImpl {

    static class TreeNode {
        int val;
        TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    // ----------------------------------------------------------------
    // BST implementasyonu
    // ----------------------------------------------------------------
    static class BST {
        TreeNode root;

        public void insert(int val) { root = insert(root, val); }

        private TreeNode insert(TreeNode node, int val) {
            if (node == null) return new TreeNode(val);
            if (val < node.val) node.left = insert(node.left, val);
            else if (val > node.val) node.right = insert(node.right, val);
            return node;
        }

        public boolean search(int val) { return search(root, val); }

        private boolean search(TreeNode node, int val) {
            if (node == null) return false;
            if (val == node.val) return true;
            return val < node.val ? search(node.left, val) : search(node.right, val);
        }

        public void delete(int val) { root = delete(root, val); }

        private TreeNode delete(TreeNode node, int val) {
            if (node == null) return null;
            if (val < node.val) node.left = delete(node.left, val);
            else if (val > node.val) node.right = delete(node.right, val);
            else {
                if (node.left == null) return node.right;
                if (node.right == null) return node.left;
                TreeNode min = findMin(node.right);
                node.val = min.val;
                node.right = delete(node.right, min.val);
            }
            return node;
        }

        private TreeNode findMin(TreeNode node) {
            while (node.left != null) node = node.left;
            return node;
        }
    }

    // ----------------------------------------------------------------
    // Traversal'lar
    // ----------------------------------------------------------------
    public List<Integer> inorder(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        inorderHelper(root, result);
        return result;
    }

    private void inorderHelper(TreeNode node, List<Integer> result) {
        if (node == null) return;
        inorderHelper(node.left, result);
        result.add(node.val);
        inorderHelper(node.right, result);
    }

    // İteratif Inorder (stack ile)
    public List<Integer> inorderIterative(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        Deque<TreeNode> stack = new ArrayDeque<>();
        TreeNode curr = root;
        while (curr != null || !stack.isEmpty()) {
            while (curr != null) { stack.push(curr); curr = curr.left; }
            curr = stack.pop();
            result.add(curr.val);
            curr = curr.right;
        }
        return result;
    }

    // ----------------------------------------------------------------
    // 1. Ağacın Maksimum Derinliği
    // Time: O(n) | Space: O(h) — h: ağaç yüksekliği
    // ----------------------------------------------------------------
    public int maxDepth(TreeNode root) {
        if (root == null) return 0;
        return 1 + Math.max(maxDepth(root.left), maxDepth(root.right));
    }

    // ----------------------------------------------------------------
    // 2. Dengeli Ağaç Kontrolü
    // Time: O(n) | Space: O(h)
    // ----------------------------------------------------------------
    public boolean isBalanced(TreeNode root) {
        return checkHeight(root) != -1;
    }

    private int checkHeight(TreeNode node) {
        if (node == null) return 0;
        int left = checkHeight(node.left);
        if (left == -1) return -1;
        int right = checkHeight(node.right);
        if (right == -1) return -1;
        if (Math.abs(left - right) > 1) return -1;
        return 1 + Math.max(left, right);
    }

    // ----------------------------------------------------------------
    // 3. İki Ağaç Aynı mı?
    // Time: O(n) | Space: O(h)
    // ----------------------------------------------------------------
    public boolean isSameTree(TreeNode p, TreeNode q) {
        if (p == null && q == null) return true;
        if (p == null || q == null || p.val != q.val) return false;
        return isSameTree(p.left, q.left) && isSameTree(p.right, q.right);
    }

    // ----------------------------------------------------------------
    // 4. Ayna Ağaç (Symmetric Tree)
    // Time: O(n) | Space: O(h)
    // ----------------------------------------------------------------
    public boolean isSymmetric(TreeNode root) {
        return isMirror(root, root);
    }

    private boolean isMirror(TreeNode left, TreeNode right) {
        if (left == null && right == null) return true;
        if (left == null || right == null) return false;
        return (left.val == right.val)
                && isMirror(left.left, right.right)
                && isMirror(left.right, right.left);
    }

    // ----------------------------------------------------------------
    // 5. Lowest Common Ancestor (LCA)
    // Time: O(n) | Space: O(h)
    // ----------------------------------------------------------------
    public TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
        if (root == null || root == p || root == q) return root;
        TreeNode left = lowestCommonAncestor(root.left, p, q);
        TreeNode right = lowestCommonAncestor(root.right, p, q);
        if (left != null && right != null) return root;
        return left != null ? left : right;
    }

    // ----------------------------------------------------------------
    // 6. Path Sum — Root'tan Leaf'e Hedef Toplamı Var mı?
    // Time: O(n) | Space: O(h)
    // ----------------------------------------------------------------
    public boolean hasPathSum(TreeNode root, int target) {
        if (root == null) return false;
        if (root.left == null && root.right == null) return root.val == target;
        return hasPathSum(root.left, target - root.val)
                || hasPathSum(root.right, target - root.val);
    }

    // ----------------------------------------------------------------
    // 7. Binary Tree'yi Diziye Serialize/Deserialize
    // Time: O(n) | Space: O(n)
    // ----------------------------------------------------------------
    public String serialize(TreeNode root) {
        if (root == null) return "null,";
        return root.val + "," + serialize(root.left) + serialize(root.right);
    }

    public TreeNode deserialize(String data) {
        Queue<String> nodes = new LinkedList<>(Arrays.asList(data.split(",")));
        return deserializeHelper(nodes);
    }

    private TreeNode deserializeHelper(Queue<String> nodes) {
        String val = nodes.poll();
        if ("null".equals(val)) return null;
        TreeNode node = new TreeNode(Integer.parseInt(val));
        node.left = deserializeHelper(nodes);
        node.right = deserializeHelper(nodes);
        return node;
    }

    // ----------------------------------------------------------------
    // 8. Kth Smallest in BST
    // Time: O(k) | Space: O(h)
    // ----------------------------------------------------------------
    private int count = 0;
    private int result = 0;

    public int kthSmallest(TreeNode root, int k) {
        count = k;
        kthHelper(root);
        return result;
    }

    private void kthHelper(TreeNode node) {
        if (node == null) return;
        kthHelper(node.left);
        if (--count == 0) { result = node.val; return; }
        kthHelper(node.right);
    }

    public static void main(String[] args) {
        TreeImpl tree = new TreeImpl();

        BST bst = new BST();
        bst.insert(5); bst.insert(3); bst.insert(7); bst.insert(1); bst.insert(4);
        System.out.println("BST Inorder: " + tree.inorder(bst.root));
        System.out.println("BST Search 4: " + bst.search(4));
        System.out.println("Max Depth: " + tree.maxDepth(bst.root));
        System.out.println("Is Balanced: " + tree.isBalanced(bst.root));
        System.out.println("Kth Smallest k=2: " + tree.kthSmallest(bst.root, 2));

        String serialized = tree.serialize(bst.root);
        System.out.println("Serialized: " + serialized);
        TreeNode deserialized = tree.deserialize(serialized);
        System.out.println("Deserialized Inorder: " + tree.inorder(deserialized));
    }
}
