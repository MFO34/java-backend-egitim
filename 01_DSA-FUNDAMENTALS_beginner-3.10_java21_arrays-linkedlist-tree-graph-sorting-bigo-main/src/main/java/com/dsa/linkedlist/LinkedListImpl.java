package com.dsa.linkedlist;

/**
 * LINKED LIST — Sıfırdan implementasyon + mülakat problemleri
 *
 * Big O:
 *   Erişim   → O(n)
 *   Arama    → O(n)
 *   Baş ekleme → O(1)
 *   Son ekleme → O(n)
 *   Silme    → O(n)
 */
public class LinkedListImpl {

    // ----------------------------------------------------------------
    // Node yapısı
    // ----------------------------------------------------------------
    static class Node {
        int val;
        Node next;
        Node(int val) { this.val = val; }
    }

    // ----------------------------------------------------------------
    // Singly Linked List implementasyonu
    // ----------------------------------------------------------------
    static class SinglyLinkedList {
        Node head;

        public void addFirst(int val) {
            Node node = new Node(val);
            node.next = head;
            head = node;
        }

        public void addLast(int val) {
            Node node = new Node(val);
            if (head == null) { head = node; return; }
            Node curr = head;
            while (curr.next != null) curr = curr.next;
            curr.next = node;
        }

        public void delete(int val) {
            if (head == null) return;
            if (head.val == val) { head = head.next; return; }
            Node curr = head;
            while (curr.next != null && curr.next.val != val) curr = curr.next;
            if (curr.next != null) curr.next = curr.next.next;
        }

        public void print() {
            Node curr = head;
            while (curr != null) {
                System.out.print(curr.val + (curr.next != null ? " → " : "\n"));
                curr = curr.next;
            }
        }
    }

    // ----------------------------------------------------------------
    // 1. Linked List'i Ters Çevir
    // Time: O(n) | Space: O(1)
    // ----------------------------------------------------------------
    public Node reverse(Node head) {
        Node prev = null, curr = head;
        while (curr != null) {
            Node next = curr.next;
            curr.next = prev;
            prev = curr;
            curr = next;
        }
        return prev;
    }

    // ----------------------------------------------------------------
    // 2. Döngü Tespiti (Floyd's Cycle Detection)
    // Time: O(n) | Space: O(1)
    // ----------------------------------------------------------------
    public boolean hasCycle(Node head) {
        Node slow = head, fast = head;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
            if (slow == fast) return true;
        }
        return false;
    }

    // ----------------------------------------------------------------
    // 3. Ortadaki Node'u Bul (Slow-Fast Pointer)
    // Time: O(n) | Space: O(1)
    // ----------------------------------------------------------------
    public Node findMiddle(Node head) {
        Node slow = head, fast = head;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }
        return slow;
    }

    // ----------------------------------------------------------------
    // 4. Sondan K. Node'u Bul
    // Time: O(n) | Space: O(1)
    // ----------------------------------------------------------------
    public Node findKthFromEnd(Node head, int k) {
        Node fast = head, slow = head;
        for (int i = 0; i < k; i++) {
            if (fast == null) return null;
            fast = fast.next;
        }
        while (fast != null) {
            slow = slow.next;
            fast = fast.next;
        }
        return slow;
    }

    // ----------------------------------------------------------------
    // 5. İki Sıralı Listeyi Birleştir
    // Time: O(n+m) | Space: O(1)
    // ----------------------------------------------------------------
    public Node mergeSortedLists(Node l1, Node l2) {
        Node dummy = new Node(0), curr = dummy;
        while (l1 != null && l2 != null) {
            if (l1.val <= l2.val) { curr.next = l1; l1 = l1.next; }
            else { curr.next = l2; l2 = l2.next; }
            curr = curr.next;
        }
        curr.next = (l1 != null) ? l1 : l2;
        return dummy.next;
    }

    // ----------------------------------------------------------------
    // 6. Palindrome Kontrol
    // Time: O(n) | Space: O(1)
    // ----------------------------------------------------------------
    public boolean isPalindrome(Node head) {
        Node middle = findMiddle(head);
        Node secondHalf = reverse(middle);
        Node p1 = head, p2 = secondHalf;
        while (p2 != null) {
            if (p1.val != p2.val) return false;
            p1 = p1.next;
            p2 = p2.next;
        }
        return true;
    }

    // ----------------------------------------------------------------
    // 7. İki Listeyi Kesişim Noktasında Bul
    // Time: O(n+m) | Space: O(1)
    // ----------------------------------------------------------------
    public Node getIntersectionNode(Node headA, Node headB) {
        if (headA == null || headB == null) return null;
        Node a = headA, b = headB;
        while (a != b) {
            a = (a == null) ? headB : a.next;
            b = (b == null) ? headA : b.next;
        }
        return a;
    }

    // ----------------------------------------------------------------
    // 8. Doubly Linked List implementasyonu
    // ----------------------------------------------------------------
    static class DoublyLinkedList {
        static class DNode {
            int val;
            DNode prev, next;
            DNode(int val) { this.val = val; }
        }

        DNode head, tail;

        public void addLast(int val) {
            DNode node = new DNode(val);
            if (tail == null) { head = tail = node; return; }
            tail.next = node;
            node.prev = tail;
            tail = node;
        }

        public void delete(DNode node) {
            if (node.prev != null) node.prev.next = node.next;
            else head = node.next;
            if (node.next != null) node.next.prev = node.prev;
            else tail = node.prev;
        }

        public void print() {
            DNode curr = head;
            while (curr != null) {
                System.out.print(curr.val + (curr.next != null ? " ⇄ " : "\n"));
                curr = curr.next;
            }
        }
    }

    public static void main(String[] args) {
        LinkedListImpl ll = new LinkedListImpl();

        SinglyLinkedList list = new SinglyLinkedList();
        list.addLast(1); list.addLast(2); list.addLast(3); list.addLast(4); list.addLast(5);
        list.print();

        System.out.println("Middle: " + ll.findMiddle(list.head).val);
        System.out.println("Kth(2) from end: " + ll.findKthFromEnd(list.head, 2).val);

        list.head = ll.reverse(list.head);
        list.print();

        SinglyLinkedList palindromeList = new SinglyLinkedList();
        palindromeList.addLast(1); palindromeList.addLast(2); palindromeList.addLast(2); palindromeList.addLast(1);
        System.out.println("Is Palindrome [1,2,2,1]: " + ll.isPalindrome(palindromeList.head));
    }
}
