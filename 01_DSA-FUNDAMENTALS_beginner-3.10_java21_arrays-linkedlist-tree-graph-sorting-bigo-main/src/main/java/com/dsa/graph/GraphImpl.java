package com.dsa.graph;

import java.util.*;

/**
 * GRAPH — BFS, DFS, Dijkstra, Topological Sort
 *
 * Big O (Adjacency List):
 *   BFS/DFS  → O(V + E)
 *   Dijkstra → O((V + E) log V)
 *
 * V: vertex sayısı, E: edge sayısı
 */
public class GraphImpl {

    // ----------------------------------------------------------------
    // Graph yapısı (Adjacency List)
    // ----------------------------------------------------------------
    static class Graph {
        private final int vertices;
        private final List<List<Integer>> adj;

        Graph(int v) {
            vertices = v;
            adj = new ArrayList<>();
            for (int i = 0; i < v; i++) adj.add(new ArrayList<>());
        }

        public void addEdge(int u, int v) {
            adj.get(u).add(v);
            adj.get(v).add(u); // undirected
        }

        public void addDirectedEdge(int u, int v) { adj.get(u).add(v); }
        public List<Integer> neighbors(int v) { return adj.get(v); }
        public int vertices() { return vertices; }
    }

    // ----------------------------------------------------------------
    // 1. BFS (Breadth-First Search)
    // Time: O(V+E) | Space: O(V)
    // ----------------------------------------------------------------
    public List<Integer> bfs(Graph graph, int start) {
        List<Integer> result = new ArrayList<>();
        boolean[] visited = new boolean[graph.vertices()];
        Queue<Integer> queue = new LinkedList<>();
        visited[start] = true;
        queue.offer(start);
        while (!queue.isEmpty()) {
            int node = queue.poll();
            result.add(node);
            for (int neighbor : graph.neighbors(node)) {
                if (!visited[neighbor]) {
                    visited[neighbor] = true;
                    queue.offer(neighbor);
                }
            }
        }
        return result;
    }

    // ----------------------------------------------------------------
    // 2. DFS (Depth-First Search)
    // Time: O(V+E) | Space: O(V)
    // ----------------------------------------------------------------
    public List<Integer> dfs(Graph graph, int start) {
        List<Integer> result = new ArrayList<>();
        boolean[] visited = new boolean[graph.vertices()];
        dfsHelper(graph, start, visited, result);
        return result;
    }

    private void dfsHelper(Graph graph, int node, boolean[] visited, List<Integer> result) {
        visited[node] = true;
        result.add(node);
        for (int neighbor : graph.neighbors(node)) {
            if (!visited[neighbor]) dfsHelper(graph, neighbor, visited, result);
        }
    }

    // ----------------------------------------------------------------
    // 3. Döngü Tespiti (Directed Graph)
    // Time: O(V+E) | Space: O(V)
    // ----------------------------------------------------------------
    public boolean hasCycle(int n, int[][] edges) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] e : edges) adj.get(e[0]).add(e[1]);

        int[] state = new int[n]; // 0:unvisited, 1:visiting, 2:visited

        for (int i = 0; i < n; i++)
            if (state[i] == 0 && dfsHasCycle(i, adj, state)) return true;
        return false;
    }

    private boolean dfsHasCycle(int node, List<List<Integer>> adj, int[] state) {
        state[node] = 1;
        for (int neighbor : adj.get(node)) {
            if (state[neighbor] == 1) return true;
            if (state[neighbor] == 0 && dfsHasCycle(neighbor, adj, state)) return true;
        }
        state[node] = 2;
        return false;
    }

    // ----------------------------------------------------------------
    // 4. Topological Sort (Kahn's Algorithm — BFS)
    // Time: O(V+E) | Space: O(V)
    // ----------------------------------------------------------------
    public int[] topologicalSort(int n, int[][] edges) {
        int[] inDegree = new int[n];
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] e : edges) { adj.get(e[0]).add(e[1]); inDegree[e[1]]++; }

        Queue<Integer> queue = new LinkedList<>();
        for (int i = 0; i < n; i++) if (inDegree[i] == 0) queue.offer(i);

        int[] result = new int[n];
        int idx = 0;
        while (!queue.isEmpty()) {
            int node = queue.poll();
            result[idx++] = node;
            for (int neighbor : adj.get(node))
                if (--inDegree[neighbor] == 0) queue.offer(neighbor);
        }
        return idx == n ? result : new int[]{};
    }

    // ----------------------------------------------------------------
    // 5. Dijkstra — En Kısa Yol
    // Time: O((V+E) log V) | Space: O(V)
    // ----------------------------------------------------------------
    public int[] dijkstra(int n, int[][] edges, int src) {
        List<List<int[]>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] e : edges) {
            adj.get(e[0]).add(new int[]{e[1], e[2]});
            adj.get(e[1]).add(new int[]{e[0], e[2]});
        }

        int[] dist = new int[n];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[src] = 0;

        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[1]));
        pq.offer(new int[]{src, 0});

        while (!pq.isEmpty()) {
            int[] curr = pq.poll();
            int node = curr[0], d = curr[1];
            if (d > dist[node]) continue;
            for (int[] neighbor : adj.get(node)) {
                int newDist = dist[node] + neighbor[1];
                if (newDist < dist[neighbor[0]]) {
                    dist[neighbor[0]] = newDist;
                    pq.offer(new int[]{neighbor[0], newDist});
                }
            }
        }
        return dist;
    }

    // ----------------------------------------------------------------
    // 6. Number of Islands (BFS/DFS on Grid)
    // Time: O(m*n) | Space: O(m*n)
    // ----------------------------------------------------------------
    public int numIslands(char[][] grid) {
        int count = 0;
        for (int r = 0; r < grid.length; r++)
            for (int c = 0; c < grid[0].length; c++)
                if (grid[r][c] == '1') { sinkIsland(grid, r, c); count++; }
        return count;
    }

    private void sinkIsland(char[][] grid, int r, int c) {
        if (r < 0 || r >= grid.length || c < 0 || c >= grid[0].length || grid[r][c] != '1') return;
        grid[r][c] = '0';
        sinkIsland(grid, r+1, c); sinkIsland(grid, r-1, c);
        sinkIsland(grid, r, c+1); sinkIsland(grid, r, c-1);
    }

    // ----------------------------------------------------------------
    // 7. Union-Find (Disjoint Set) — Bağlı Bileşenler
    // Time: O(α(n)) ≈ O(1) amortized
    // ----------------------------------------------------------------
    static class UnionFind {
        private final int[] parent, rank;

        UnionFind(int n) {
            parent = new int[n];
            rank = new int[n];
            for (int i = 0; i < n; i++) parent[i] = i;
        }

        public int find(int x) {
            if (parent[x] != x) parent[x] = find(parent[x]); // path compression
            return parent[x];
        }

        public boolean union(int x, int y) {
            int px = find(x), py = find(y);
            if (px == py) return false;
            if (rank[px] < rank[py]) parent[px] = py;
            else if (rank[px] > rank[py]) parent[py] = px;
            else { parent[py] = px; rank[px]++; }
            return true;
        }
    }

    public static void main(String[] args) {
        GraphImpl g = new GraphImpl();

        Graph graph = new Graph(6);
        graph.addEdge(0, 1); graph.addEdge(0, 2);
        graph.addEdge(1, 3); graph.addEdge(2, 4); graph.addEdge(3, 5);

        System.out.println("BFS from 0: " + g.bfs(graph, 0));
        System.out.println("DFS from 0: " + g.dfs(graph, 0));

        int[][] topoEdges = {{0,1},{0,2},{1,3},{2,3}};
        System.out.println("Topological Sort: " + Arrays.toString(g.topologicalSort(4, topoEdges)));

        int[][] dijkEdges = {{0,1,4},{0,2,1},{2,1,2},{1,3,1},{2,3,5}};
        System.out.println("Dijkstra from 0: " + Arrays.toString(g.dijkstra(4, dijkEdges, 0)));

        char[][] grid = {
            {'1','1','0','0','0'},
            {'1','1','0','0','0'},
            {'0','0','1','0','0'},
            {'0','0','0','1','1'}
        };
        System.out.println("Number of Islands: " + g.numIslands(grid));
    }
}
