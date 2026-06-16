package com.dsa.stack;

import java.util.*;

/**
 * STACK — LIFO (Last In First Out)
 *
 * Big O:
 *   Push  → O(1)
 *   Pop   → O(1)
 *   Peek  → O(1)
 *   Arama → O(n)
 */
public class StackImpl {

    // ----------------------------------------------------------------
    // Array tabanlı Stack implementasyonu
    // ----------------------------------------------------------------
    static class ArrayStack {
        private final int[] data;
        private int top = -1;

        ArrayStack(int capacity) { data = new int[capacity]; }

        public void push(int val) {
            if (top == data.length - 1) throw new RuntimeException("Stack full");
            data[++top] = val;
        }

        public int pop() {
            if (isEmpty()) throw new RuntimeException("Stack empty");
            return data[top--];
        }

        public int peek() { return data[top]; }
        public boolean isEmpty() { return top == -1; }
        public int size() { return top + 1; }
    }

    // ----------------------------------------------------------------
    // 1. Geçerli Parantezler (Valid Parentheses)
    // Time: O(n) | Space: O(n)
    // ----------------------------------------------------------------
    public boolean isValid(String s) {
        Deque<Character> stack = new ArrayDeque<>();
        for (char c : s.toCharArray()) {
            if (c == '(' || c == '{' || c == '[') {
                stack.push(c);
            } else {
                if (stack.isEmpty()) return false;
                char top = stack.pop();
                if (c == ')' && top != '(') return false;
                if (c == '}' && top != '{') return false;
                if (c == ']' && top != '[') return false;
            }
        }
        return stack.isEmpty();
    }

    // ----------------------------------------------------------------
    // 2. Min Stack — O(1)'de minimum değeri döndüren stack
    // Time: O(1) tüm operasyonlar | Space: O(n)
    // ----------------------------------------------------------------
    static class MinStack {
        private final Deque<Integer> stack = new ArrayDeque<>();
        private final Deque<Integer> minStack = new ArrayDeque<>();

        public void push(int val) {
            stack.push(val);
            int min = minStack.isEmpty() ? val : Math.min(val, minStack.peek());
            minStack.push(min);
        }

        public void pop() { stack.pop(); minStack.pop(); }
        public int top() { return stack.peek(); }
        public int getMin() { return minStack.peek(); }
    }

    // ----------------------------------------------------------------
    // 3. Infix → Postfix Dönüşümü (Shunting Yard)
    // Time: O(n) | Space: O(n)
    // ----------------------------------------------------------------
    public String infixToPostfix(String expression) {
        StringBuilder result = new StringBuilder();
        Deque<Character> stack = new ArrayDeque<>();
        Map<Character, Integer> precedence = Map.of('+', 1, '-', 1, '*', 2, '/', 2, '^', 3);

        for (char c : expression.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                result.append(c);
            } else if (c == '(') {
                stack.push(c);
            } else if (c == ')') {
                while (!stack.isEmpty() && stack.peek() != '(')
                    result.append(stack.pop());
                stack.pop();
            } else if (precedence.containsKey(c)) {
                while (!stack.isEmpty() && stack.peek() != '(' &&
                        precedence.getOrDefault(stack.peek(), 0) >= precedence.get(c))
                    result.append(stack.pop());
                stack.push(c);
            }
        }
        while (!stack.isEmpty()) result.append(stack.pop());
        return result.toString();
    }

    // ----------------------------------------------------------------
    // 4. Postfix Hesaplama
    // Time: O(n) | Space: O(n)
    // ----------------------------------------------------------------
    public int evalRPN(String[] tokens) {
        Deque<Integer> stack = new ArrayDeque<>();
        for (String token : tokens) {
            switch (token) {
                case "+" -> stack.push(stack.pop() + stack.pop());
                case "-" -> { int b = stack.pop(), a = stack.pop(); stack.push(a - b); }
                case "*" -> stack.push(stack.pop() * stack.pop());
                case "/" -> { int b = stack.pop(), a = stack.pop(); stack.push(a / b); }
                default  -> stack.push(Integer.parseInt(token));
            }
        }
        return stack.pop();
    }

    // ----------------------------------------------------------------
    // 5. Daily Temperatures — Sonraki Daha Sıcak Günü Bul
    // (Monotonic Stack)
    // Time: O(n) | Space: O(n)
    // ----------------------------------------------------------------
    public int[] dailyTemperatures(int[] temperatures) {
        int n = temperatures.length;
        int[] result = new int[n];
        Deque<Integer> stack = new ArrayDeque<>();
        for (int i = 0; i < n; i++) {
            while (!stack.isEmpty() && temperatures[i] > temperatures[stack.peek()]) {
                int idx = stack.pop();
                result[idx] = i - idx;
            }
            stack.push(i);
        }
        return result;
    }

    // ----------------------------------------------------------------
    // 6. Largest Rectangle in Histogram
    // Time: O(n) | Space: O(n)
    // ----------------------------------------------------------------
    public int largestRectangleArea(int[] heights) {
        Deque<Integer> stack = new ArrayDeque<>();
        int maxArea = 0;
        for (int i = 0; i <= heights.length; i++) {
            int h = (i == heights.length) ? 0 : heights[i];
            while (!stack.isEmpty() && h < heights[stack.peek()]) {
                int height = heights[stack.pop()];
                int width = stack.isEmpty() ? i : i - stack.peek() - 1;
                maxArea = Math.max(maxArea, height * width);
            }
            stack.push(i);
        }
        return maxArea;
    }

    public static void main(String[] args) {
        StackImpl s = new StackImpl();

        ArrayStack as = new ArrayStack(10);
        as.push(1); as.push(2); as.push(3);
        System.out.println("Pop: " + as.pop() + ", Peek: " + as.peek());

        System.out.println("Valid '()[]{}': " + s.isValid("()[]{}"));
        System.out.println("Valid '(]': " + s.isValid("(]"));
        System.out.println("Infix a+b*c: " + s.infixToPostfix("a+b*c"));
        System.out.println("RPN ['2','1','+','3','*']: " + s.evalRPN(new String[]{"2", "1", "+", "3", "*"}));

        MinStack ms = new MinStack();
        ms.push(3); ms.push(5); ms.push(1);
        System.out.println("MinStack getMin: " + ms.getMin());
    }
}
