import static java.lang.Math.min;
import static java.util.Arrays.sort;

/**
 * Prim o(v^2) and Kruskal o(elogv)
 */
public class MinTree {
    final int MAX_V = 100;
    /**
     * Prim
     */
    int[][] cost = new int[MAX_V][MAX_V];
    int[] mincost = new int[MAX_V];
    boolean[] used = new boolean[MAX_V];
    int  prim(int v){
        int res = 0;
        for (int i = 0;i < v;i++) mincost[i] = 0x3f;
        mincost[0] = 0;
        while(true) {
            int index = -1;
            for(int u = 0; u < v; ++u)
                if(!used[u] && ( index==-1 || mincost[u] < mincost[index])) index = u;
            if(index == -1) break;
            used[index] = true;
            res += mincost[index];
            for(int u = 0; u < v; ++u)
                mincost[u] = min(mincost[u], cost[index][u]);
        }
        return res;
    }
    /**
     * Kruskal
     */
    int[] par = new int[MAX_V];
    class Edge implements Comparable<Edge> {
        int u, v, cost;
        @Override
        public int compareTo(Edge o) {
            return o.cost - this.cost;
        }
    }
    Edge[] es = new Edge[MAX_V*(MAX_V-1)/2];
    int kruskal(int v) {
        sort(es);
        init_union_find(v);
        int res = 0;
        for(int i = 0; i < es.length; ++i) {
            Edge e = es[i];
            if(find(e.u) != find(e.v)) {
                unite(e.u, e.v);
                res+=e.cost;
            }
        }
        return res;
    }
    void init_union_find(int N) {
        for(int i = 0; i < N; ++i)
            par[i] = i;
    }
    int find(int x) {
//        int root = x;
//        while(root != par[root])
//            root = par[root];
//        while(x!=root) { // impress path
//            int  t = par[x];
//            par[x] = par[root];
//            x = t;
//        }
        return x==par[x]?x:find(par[x]);
    }
    void unite(int x, int y) {
        x = find(x);
        y = find(y);
        if(x != y)
            par[x] = y;
    }

}
