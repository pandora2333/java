import java.util.*;

import static java.lang.Math.min;

/**
 * the question in the shortest path for Bellman-Ford/Dijkstra/Floyd/SPFA
 */
public class ShortestPath {
    final int MAX_NUM = 0x3f;
    int[] dest = new int[MAX_NUM];

    Edge[] edge = new Edge[MAX_NUM*2];
    int N, M;  //point N, paths M
    class Edge implements Comparable<Edge>{
        int from ,to,cost;

        public Edge(int to, int cost) {
            this.to = to;
            this.cost = cost;
        }

        public Edge() {
        }
        @Override
        public int compareTo(Edge o) {
            return this.cost - o.cost ;
        }
    }

    /**
     *  Bellman-Ford O(VE) space O(E)
     */
    boolean bellman_ford(int start) {
        Arrays.fill(dest,Integer.MAX_VALUE);//设置初始距离max
        dest[start] = 0;
        for(int i=0; i<N-1; ++i)
            for(int j=0; j<2*M; ++j) {
                Edge e=edge[j];
                if(dest[e.to] > dest[e.from] + e.cost)
                    dest[e.to] = dest[e.from] + e.cost;
            }
        boolean flag = false; //judge it exists a circle that all elements for value < 0?
        for(int j=0; j<2*M; ++j)
            if(dest[edge[j].to] > dest[edge[j].from] + edge[j].cost) {
                flag = true;
                break;
            }
        return flag;
    }

    /**
     *  Dijkstra O(V*V) space:O(V)
     */
    boolean[] used = new boolean[MAX_NUM];
    int[][] cost = new int[MAX_NUM][MAX_NUM];
    int V, E;
    void dijkstra(int s) {
        Arrays.fill(dest,Integer.MAX_VALUE);//设置初始距离max
        dest[s] = 0;//搜索起点
        while(true) {
            int v = -1;
            for(int u = 0; u < V; ++u)
                if(!used[u] && (v==-1 || dest[u] < dest[v])) v = u;
            if(v == -1) break;
            used[v] = true;
            for(int u = 0; u < V; ++u)
                dest[u] = min(dest[u], dest[v]+cost[v][u]);
        }
    }

    /**
     *  Dijkstra O(ElogV)
     */
    Vector<?>[] G = new Vector<?>[MAX_NUM];
    void dijkstra2(int s) {
        PriorityQueue<Edge> que = new PriorityQueue<>();
        Arrays.fill(dest,Integer.MAX_VALUE);
        dest[s] = 0;
        que.add(new Edge(s, 0));
        while(!que.isEmpty()) {
            Edge p = que.poll();
            int v = p.to;//-1 -> s -> next ...
            if(dest[v] < p.cost) continue;
            for(int i=0; i<G[v].size(); ++i) {
                Edge e = (Edge) G[v].get(i);
                if(dest[e.to] > dest[v] + e.cost) {
                    dest[e.to] = dest[v] + e.cost;
                    que.add(new Edge(e.to, dest[e.to]));
                }
            }
        }
    }

    /**
     * Floyd O(V*V*V) space:O(V*V)
     * DP
     */
    int[][] d = new int[MAX_NUM][MAX_NUM];
    void floyd() {
        for(int k = 0; k < V; ++k)
            for(int i = 0; i < V; ++i)
                for(int j = 0; j < V; ++j)
                    d[i][j] = min(d[i][j], d[i][k]+d[k][j]);
       return;
    }

    /**
     * SPFA <=O(VE) space:O(E)
     */
    boolean[] visited = new boolean[MAX_NUM];//is the point in queue?
    int[] enqueue_num = new int[MAX_NUM];//the number of the point has enter the queue
    int[] dist = new int[MAX_NUM];//record the distance:source -> i
    int[] path = new int[MAX_NUM];//record the shortest path from source to any point
    int[][] matrix = new int[MAX_NUM][MAX_NUM];//record the distance for each other
    final int INT_MAX = Integer.MAX_VALUE;
    boolean SPFA(int source)
    {
        for (int i = 0; i < MAX_NUM; i++)
        {
            dist[i] = INT_MAX;
            path[i] = source;//记录 source -> dest 路径
        }

        Queue<Integer> queue = new ArrayDeque<>();//点 1 - N 的下标
        queue.add(source);
        dist[source] = 0;
        visited[source] = true;
        enqueue_num[source]++;
        while (!queue.isEmpty())
        {
            int u = queue.poll();
            visited[u] = false;
            for (int v = 0; v < MAX_NUM; v++)
            {
                if (matrix[u][v] != INT_MAX)  //reachable
                {
                    if (dist[u] + matrix[u][v] < dist[v])
                    {
                        dist[v] = dist[u] + matrix[u][v];
                        path[v] = u;//更新前驱点
                        if (!visited[v])
                        {
                            queue.add(v);
                            enqueue_num[v]++;
                            if (enqueue_num[v] >= MAX_NUM)//resovle the circle for all elements; value < 0
                                return false;
                            visited[v] = true;
                        }
                    }
                }
            }
        }
        return true;
    }
}
