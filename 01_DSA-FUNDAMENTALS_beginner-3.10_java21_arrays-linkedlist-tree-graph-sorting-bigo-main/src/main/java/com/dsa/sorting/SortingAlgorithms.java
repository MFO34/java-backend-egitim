package com.dsa.sorting;

import java.util.Arrays;

/**
 * SORTING ALGORİTMALARI — Sıfırdan implementasyon ve Big O analizi
 *
 * Algoritma       | Best      | Average   | Worst     | Space  | Stable
 * ----------------+-----------+-----------+-----------+--------+-------
 * Bubble Sort     | O(n)      | O(n²)     | O(n²)     | O(1)   | Evet
 * Selection Sort  | O(n²)     | O(n²)     | O(n²)     | O(1)   | Hayır
 * Insertion Sort  | O(n)      | O(n²)     | O(n²)     | O(1)   | Evet
 * Merge Sort      | O(n logn) | O(n logn) | O(n logn) | O(n)   | Evet
 * Quick Sort      | O(n logn) | O(n logn) | O(n²)     | O(logn)| Hayır
 * Heap Sort       | O(n logn) | O(n logn) | O(n logn) | O(1)   | Hayır
 * Counting Sort   | O(n+k)    | O(n+k)    | O(n+k)    | O(k)   | Evet
 */
public class SortingAlgorithms {

    // ----------------------------------------------------------------
    // 1. Bubble Sort
    // ----------------------------------------------------------------
    public void bubbleSort(int[] arr) {
        int n = arr.length;
        for (int i = 0; i < n - 1; i++) {
            boolean swapped = false;
            for (int j = 0; j < n - i - 1; j++) {
                if (arr[j] > arr[j + 1]) {
                    swap(arr, j, j + 1);
                    swapped = true;
                }
            }
            if (!swapped) break; // zaten sıralı → O(n)
        }
    }

    // ----------------------------------------------------------------
    // 2. Selection Sort
    // ----------------------------------------------------------------
    public void selectionSort(int[] arr) {
        int n = arr.length;
        for (int i = 0; i < n - 1; i++) {
            int minIdx = i;
            for (int j = i + 1; j < n; j++)
                if (arr[j] < arr[minIdx]) minIdx = j;
            swap(arr, i, minIdx);
        }
    }

    // ----------------------------------------------------------------
    // 3. Insertion Sort — küçük ve neredeyse sıralı diziler için ideal
    // ----------------------------------------------------------------
    public void insertionSort(int[] arr) {
        for (int i = 1; i < arr.length; i++) {
            int key = arr[i];
            int j = i - 1;
            while (j >= 0 && arr[j] > key) {
                arr[j + 1] = arr[j];
                j--;
            }
            arr[j + 1] = key;
        }
    }

    // ----------------------------------------------------------------
    // 4. Merge Sort — garantili O(n log n)
    // ----------------------------------------------------------------
    public void mergeSort(int[] arr, int left, int right) {
        if (left >= right) return;
        int mid = left + (right - left) / 2;
        mergeSort(arr, left, mid);
        mergeSort(arr, mid + 1, right);
        merge(arr, left, mid, right);
    }

    private void merge(int[] arr, int left, int mid, int right) {
        int[] temp = Arrays.copyOfRange(arr, left, right + 1);
        int i = 0, j = mid - left + 1, k = left;
        while (i <= mid - left && j <= right - left)
            arr[k++] = (temp[i] <= temp[j]) ? temp[i++] : temp[j++];
        while (i <= mid - left) arr[k++] = temp[i++];
        while (j <= right - left) arr[k++] = temp[j++];
    }

    // ----------------------------------------------------------------
    // 5. Quick Sort — pratikte en hızlı, pivot seçimi kritik
    // ----------------------------------------------------------------
    public void quickSort(int[] arr, int low, int high) {
        if (low >= high) return;
        int pivotIdx = partition(arr, low, high);
        quickSort(arr, low, pivotIdx - 1);
        quickSort(arr, pivotIdx + 1, high);
    }

    private int partition(int[] arr, int low, int high) {
        int pivot = arr[high];
        int i = low - 1;
        for (int j = low; j < high; j++) {
            if (arr[j] <= pivot) swap(arr, ++i, j);
        }
        swap(arr, i + 1, high);
        return i + 1;
    }

    // ----------------------------------------------------------------
    // 6. Heap Sort
    // ----------------------------------------------------------------
    public void heapSort(int[] arr) {
        int n = arr.length;
        for (int i = n / 2 - 1; i >= 0; i--) heapify(arr, n, i);
        for (int i = n - 1; i > 0; i--) {
            swap(arr, 0, i);
            heapify(arr, i, 0);
        }
    }

    private void heapify(int[] arr, int n, int i) {
        int largest = i, left = 2 * i + 1, right = 2 * i + 2;
        if (left < n && arr[left] > arr[largest]) largest = left;
        if (right < n && arr[right] > arr[largest]) largest = right;
        if (largest != i) { swap(arr, i, largest); heapify(arr, n, largest); }
    }

    // ----------------------------------------------------------------
    // 7. Counting Sort — tamsayılar için O(n+k)
    // ----------------------------------------------------------------
    public void countingSort(int[] arr) {
        int max = Arrays.stream(arr).max().getAsInt();
        int[] count = new int[max + 1];
        for (int x : arr) count[x]++;
        int idx = 0;
        for (int i = 0; i <= max; i++)
            while (count[i]-- > 0) arr[idx++] = i;
    }

    // ----------------------------------------------------------------
    // 8. Binary Search — sıralı dizide O(log n) arama
    // ----------------------------------------------------------------
    public int binarySearch(int[] arr, int target) {
        int left = 0, right = arr.length - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (arr[mid] == target) return mid;
            else if (arr[mid] < target) left = mid + 1;
            else right = mid - 1;
        }
        return -1;
    }

    // Rotated Sorted Array'de Binary Search
    public int searchRotated(int[] nums, int target) {
        int left = 0, right = nums.length - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (nums[mid] == target) return mid;
            if (nums[left] <= nums[mid]) {
                if (target >= nums[left] && target < nums[mid]) right = mid - 1;
                else left = mid + 1;
            } else {
                if (target > nums[mid] && target <= nums[right]) left = mid + 1;
                else right = mid - 1;
            }
        }
        return -1;
    }

    private void swap(int[] arr, int i, int j) {
        int temp = arr[i]; arr[i] = arr[j]; arr[j] = temp;
    }

    public static void main(String[] args) {
        SortingAlgorithms sa = new SortingAlgorithms();

        int[] arr1 = {64, 34, 25, 12, 22, 11, 90};
        sa.bubbleSort(arr1.clone());

        int[] arr2 = {12, 11, 13, 5, 6, 7};
        sa.mergeSort(arr2, 0, arr2.length - 1);
        System.out.println("Merge Sort: " + Arrays.toString(arr2));

        int[] arr3 = {10, 80, 30, 90, 40, 50, 70};
        sa.quickSort(arr3, 0, arr3.length - 1);
        System.out.println("Quick Sort: " + Arrays.toString(arr3));

        int[] sorted = {1, 3, 5, 7, 9, 11, 13};
        System.out.println("Binary Search 7: index " + sa.binarySearch(sorted, 7));

        int[] rotated = {4, 5, 6, 7, 0, 1, 2};
        System.out.println("Search Rotated 0: index " + sa.searchRotated(rotated, 0));

        System.out.println("\n=== SORTING COMPLEXITY ===");
        System.out.println("Bubble:    O(n²) / O(n) best  — küçük diziler");
        System.out.println("Merge:     O(n log n) — stable, büyük diziler");
        System.out.println("Quick:     O(n log n) avg — in-place, pratikte en hızlı");
        System.out.println("Heap:      O(n log n) — in-place, garantili");
        System.out.println("Counting:  O(n+k) — tamsayılar, k küçükse ideal");
    }
}
