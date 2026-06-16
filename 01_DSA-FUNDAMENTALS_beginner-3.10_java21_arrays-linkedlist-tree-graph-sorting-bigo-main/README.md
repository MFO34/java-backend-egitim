# 01 — DSA Fundamentals
**Zorluk:** Beginner (3/10) | **Java 21** | **Mülakat Hazırlığı**

Java ile temel veri yapıları ve algoritmaların sıfırdan implementasyonu. Her yapı için mülakatta çıkan klasik problemler ve Big O analizleri.

---

## İçerik

| Konu | Dosya | Kapsanan Problemler |
|------|-------|---------------------|
| **Arrays** | `arrays/ArrayProblems.java` | Two Sum, Max Subarray (Kadane), Sliding Window, Two Pointer, Prefix Sum, Merge Intervals, Trapping Rain Water |
| **Linked List** | `linkedlist/LinkedListImpl.java` | Reverse, Cycle Detection (Floyd), Middle Node, Kth From End, Merge Sorted Lists, Palindrome, Intersection |
| **Stack** | `stack/StackImpl.java` | Valid Parentheses, Min Stack, Infix→Postfix, Eval RPN, Daily Temperatures, Largest Rectangle |
| **Queue** | `queue/QueueImpl.java` | Circular Queue, Queue w/ Stacks, Level Order BFS, Sliding Window Max, Top K Frequent, Rotten Oranges |
| **Tree** | `tree/TreeImpl.java` | BST CRUD, Traversals, Max Depth, Balanced Check, LCA, Path Sum, Serialize/Deserialize, Kth Smallest |
| **Graph** | `graph/GraphImpl.java` | BFS, DFS, Cycle Detection, Topological Sort, Dijkstra, Number of Islands, Union-Find |
| **Sorting** | `sorting/SortingAlgorithms.java` | Bubble, Selection, Insertion, Merge, Quick, Heap, Counting, Binary Search, Rotated Array Search |
| **Dynamic Programming** | `dp/DynamicProgramming.java` | Fibonacci, Climbing Stairs, Coin Change, LCS, LIS, Knapsack, Edit Distance, House Robber |
| **Hashing** | `hashing/HashingProblems.java` | HashMap impl, Group Anagrams, Longest Consecutive, Subarray Sum K, LRU Cache |

---

## Big O Özeti

```
Veri Yapısı     | Erişim | Arama | Ekleme | Silme
----------------+--------+-------+--------+------
Array           | O(1)   | O(n)  | O(n)   | O(n)
Linked List     | O(n)   | O(n)  | O(1)*  | O(1)*
Stack/Queue     | O(n)   | O(n)  | O(1)   | O(1)
HashMap         | O(1)   | O(1)  | O(1)   | O(1)
BST (balanced)  | O(logn)| O(logn)| O(logn)| O(logn)
Heap            | O(1)** | O(n)  | O(logn)| O(logn)

* baş/son için
** min veya max için
```

```
Algoritma    | Best      | Average   | Worst     | Space
-------------+-----------+-----------+-----------+------
Bubble Sort  | O(n)      | O(n²)     | O(n²)     | O(1)
Merge Sort   | O(n logn) | O(n logn) | O(n logn) | O(n)
Quick Sort   | O(n logn) | O(n logn) | O(n²)     | O(logn)
Heap Sort    | O(n logn) | O(n logn) | O(n logn) | O(1)
Binary Search| O(1)      | O(logn)   | O(logn)   | O(1)
BFS/DFS      | O(V+E)    | O(V+E)    | O(V+E)    | O(V)
Dijkstra     | -         | O((V+E)logV)| -       | O(V)
```

---

## Mülakat Soruları

**Q: Two Sum neden HashMap ile O(n)'de çözülür?**
A: Brute force: iki döngü ile her çift kontrol = O(n²). HashMap: target - nums[i]'nin daha önce görüldüğünü O(1)'de kontrol et. Tek geçiş (O(n)): Her elemanda `complement = target - nums[i]`, map'te var mı bak, varsa cevap buldun, yoksa map'e ekle. Space: O(n). Aynı teknik: "iki sayı toplamı X'e eşit mi" sorularında standart yaklaşım.

**Q: Sliding Window ne zaman kullanılır?**
A: Dizi/string üzerinde "belirli uzunlukta alt dizi" veya "koşulu sağlayan en uzun/kısa alt dizi" problemlerinde. İki pointer (left, right) ile pencere genişletilip daraltılır. Fixed size: sağ pointer ilerleyince sol pointer 1 ilerler. Variable size: koşul bozulunca sol pointer ilerler. Örnekler: "K boyutunda maksimum toplam alt dizi", "en uzun benzersiz karakterli substring", "minimum size subarray sum".

**Q: Floyd's Cycle Detection nasıl çalışır?**
A: Two pointer (slow/fast). Slow her adımda 1, fast her adımda 2 ilerler. Cycle varsa fast → yavaşı geçemez, eninde sonunda buluşurlar. Cycle yok: fast null'a ulaşır. Toplantı noktasından cycle başlangıcını bulmak için: bir pointer head'e döner, ikisi aynı hızda ilerler — tekrar buluştukları yer cycle başlangıcı. O(n) time, O(1) space. Alternatif HashSet O(n) space ister.

**Q: BFS vs DFS ne zaman hangisi tercih edilir?**
A: BFS: En kısa yol (unweighted graph), level-order traversal, "kaç adımda ulaşılır" soruları. Queue kullanır. Hafıza: O(W) — W genişlik (ağaçta max level). DFS: Yol var mı, tüm yolları bul, topological sort, cycle detection, backtracking. Stack (veya recursion) kullanır. Hafıza: O(H) — H yükseklik. BFS genişliği az derin ağaçta, DFS dar derin ağaçta daha verimli.

**Q: Topological Sort ne zaman kullanılır?**
A: DAG (Directed Acyclic Graph) üzerinde bağımlılık sıralaması için. Örnekler: build order (A derlemek için B gerekli), ders önkoşulları, task scheduling. İki yöntem: Kahn algoritması (BFS + in-degree 0) veya DFS (post-order ekle, reverse). Cycle varsa topological sort mümkün değil — bu cycle detection için de kullanılır.

**Q: Dijkstra negatif ağırlıklı edge'lerde çalışır mı?**
A: Hayır. Dijkstra'nın greedy yapısı: bir düğümü "kesinleşti" olarak işaretleyince bir daha ziyaret edilmez. Negatif edge varsa kesinleştirilmiş düğümün maliyeti sonradan düşebilir — hatalı sonuç. Çözüm: Bellman-Ford (O(VE)) — negatif cycle tespiti de yapar. Negatif ağırlık yoksa Dijkstra O((V+E) log V) priority queue ile.

**Q: Memoization vs Tabulation farkı nedir?**
A: Memoization (top-down): Recursive çözümde hesaplanan değerleri cache'e (HashMap) yaz. Sadece ihtiyaç duyulan alt problemler hesaplanır. Stack overflow riski var (derin recursion). Tabulation (bottom-up): Küçükten büyüğe tüm alt problemleri iteratif çöz. Stack yok, genellikle daha hızlı, space optimizasyonu kolay. Fark: Memoization yazması daha kolay (recursive düşün + cache ekle), tabulation daha verimli.

**Q: Bir problemi DP ile çözüp çözemeyeceğini nasıl anlarsın?**
A: İki koşul: (1) Optimal substructure — büyük problemin çözümü küçük problemlerin çözümünden oluşur. (2) Overlapping subproblems — aynı alt problemler tekrar tekrar hesaplanır. Test: "n için doğruysa, n+1 için nasıl kullanırım?" sorusunu sor — eğer cevap varsa DP çalışır. Recurrence relation yaz: `dp[i] = dp[i-1] + dp[i-2]` gibi. Greedy farkı: Greedy bir sonraki en iyi seçimi yapar, DP tüm seçenekleri dener.

**Q: 0/1 Knapsack ile Unbounded Knapsack farkı?**
A: 0/1 Knapsack: Her item bir kez kullanılabilir. `dp[i][w] = max(dp[i-1][w], dp[i-1][w-weight[i]] + value[i])`. Unbounded: Aynı item sınırsız kullanılabilir. `dp[w] = max(dp[w], dp[w-weight[i]] + value[i])` — aynı satırda güncelleme (item'ı tekrar kullanabilir). Uygulama: Coin change unbounded (aynı para birimi sınırsız). 0/1: Görev seçimi (her görev bir kez).

---

## Çalıştırma

```bash
mvn compile
mvn exec:java -Dexec.mainClass="com.dsa.arrays.ArrayProblems"
mvn exec:java -Dexec.mainClass="com.dsa.sorting.SortingAlgorithms"
mvn exec:java -Dexec.mainClass="com.dsa.dp.DynamicProgramming"
```

---

## Öğrenme Sırası

```
1. Arrays       → temel veri yapısı, tüm diğerlerinin temeli
2. Hashing      → O(1) arama, Two Sum ve benzeri problemler
3. Linked List  → pointer mantığı, recursive düşünme
4. Stack/Queue  → LIFO/FIFO, BFS/DFS hazırlığı
5. Tree         → recursive yapı, BST
6. Graph        → BFS, DFS, Dijkstra
7. Sorting      → algoritma karmaşıklığı
8. DP           → en zor kategori, en çok pratik gerektiren
```

---

**Sonraki Repo →** [02 - Bank Account Manager](../02_BANK-ACCOUNT-MANAGER) (Java OOP, Collections, Streams)
