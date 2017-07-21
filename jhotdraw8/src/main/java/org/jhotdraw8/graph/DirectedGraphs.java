/* @(#)DirectedGraphs.java
 * Copyright (c) 2017 by the authors and contributors of JHotDraw.
 * You may only use this file in compliance with the accompanying license terms.
 */
package org.jhotdraw8.graph;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides algorithms for directed graphs.
 *
 * @author Werner Randelshofer
 * @version $$Id$$
 */
public class DirectedGraphs {

    private DirectedGraphs() {
    }

    /**
     * Sorts the specified directed graph topologically.
     *
     * @param m the graph
     * @return the sorted list of vertices
     */
    public static <V> List<V> sortTopologically(DirectedGraph<V> m) {
        final IntDirectedGraph im;
        if (!(m instanceof IntDirectedGraph)) {
            im = DirectedGraphBuilder.ofDirectedGraph(m);
        } else {
            im = (IntDirectedGraph) m;
        }
        int[] a = sortTopologicallyInt(im);
        List<V> result = new ArrayList<V>(a.length);
        for (int i = 0; i < a.length; i++) {
            result.add(m.getVertex(a[i]));
        }
        return result;
    }

    /**
     * Sorts the specified directed graph topologically.
     *
     * @param model the graph
     * @return the sorted list of vertices
     */
    public static int[] sortTopologicallyInt(IntDirectedGraph model) {
        final int n = model.getVertexCount();
        int[] result = new int[n];// result array
        int[] deg = new int[n]; // number of unprocessed incoming edges on vertex
        int[] queue = new int[n]; // todo queue
        int first = 0, last = 0; // first and last indices in queue

        // Step 1: compute number of incoming edges for each vertex
        for (int i = 0; i < n; i++) {
            final int m = model.getNextCount(i);
            for (int j = 0; j < m; j++) {
                int v = model.getNext(i, j);
                deg[v]++;
            }
        }

        // Step 2: put all vertices with degree zero into queue
        for (int i = 0; i < n; i++) {
            if (deg[i] == 0) {
                queue[last++] = i;
            }
        }

        // Step 3: Repeat until all vertices have been processed or a loop has been detected
        int done = 0;
        BitSet doneSet = null;
        while (done < n) {
            for (; done < n; done++) {
                if (first == last) {
                    // => the graph has a loop!
                    break;
                }
                int v = queue[first++];
                final int m = model.getNextCount(v);
                for (int j = 0; j < m; j++) {
                    int u = model.getNext(v, j);
                    if (--deg[u] == 0) {
                        queue[last++] = u;
                    }
                }
                result[done] = v;
            }

            if (done < n) {
                if (doneSet == null) {
                    doneSet = new BitSet();
                }
                for (int i = doneSet.size(); i < done; i++) {
                    doneSet.set(result[i]);
                }
                for (int i = 0; i < n; i++) {
                    if (!doneSet.get(i)) {
                        deg[i] = 0;
                        queue[last++] = i;
                        break;
                    }
                }
            }
        }

        return result;
    }

    public static <T> String dump(DirectedGraph<T> g) {
        StringBuilder b = new StringBuilder();

        for (int i = 0, n = g.getVertexCount(); i < n; i++) {
            T v = g.getVertex(i);
            if (g.getNextCount(v) == 0) {
                b.append(v)
                        .append('\n');

            } else {
                for (int j = 0, m = g.getNextCount(v); j < m; j++) {
                    b.append(v)
                            .append(" -> ")
                            .append(g.getNext(v, j))
                            .append('\n');
                }
            }
        }
        return b.toString();
    }

    /**
     * Given a directed graph, returns all disjoint sets of vertices.
     * <p>
     * Uses Kruskal's algorithm.
     *
     * @param <V> the vertex type
     * @param g a directed graph
     * @return the disjoint sets.
     */
    public static <V> List<Set<V>> findDisjointSets(DirectedGraph<V> g) {
        Map<V, Set<V>> sets = new LinkedHashMap<>();
        for (int i = 0, n = g.getVertexCount(); i < n; i++) {
            final V v = g.getVertex(i);
            Set<V> initialSet=new LinkedHashSet<>();
            initialSet.add(v);
            sets.put(v, initialSet);
        }
        for (int i = 0, n = g.getVertexCount(); i < n; i++) {
            V u = g.getVertex(i);
            for (int j = 0, m = g.getNextCount(u); j < m; j++) {
                V v = g.getNext(u, j);
                Set<V> uset = sets.get(u);
                Set<V> vset = sets.get(v);
                if (uset != vset) {
                    if (uset.size() < vset.size()) {
                        for (V uu : uset) {
                            sets.put(uu, vset);
                        }
                        vset.addAll(uset);
                    } else {
                        for (V vv : vset) {
                            sets.put(vv, uset);
                        }
                        uset.addAll(vset);
                    }
                }

            }
        }

        Map<Set<V>,Object> forestMap = new IdentityHashMap<Set<V>,Object>();
        List<Set<V>> forest = new ArrayList<>();
        for (Set<V> set : sets.values()) {
            if (null==forestMap.put(set,set)) {
                forest.add(set);
            }
        }
        return forest;
    }
    /**
     * Given an int directed graph, returns all disjoint sets of vertices.
     * <p>
     * Uses Kruskal's algorithm.
     *
     * @param g a directed graph
     * @return the disjoint sets.
     */
    public static List<Set<Integer>> findDisjointSets(IntDirectedGraph  g) {
        List<Set<Integer>> sets = new ArrayList<>(g.getVertexCount());
        for (int v = 0, n = g.getVertexCount(); v < n; v++) {
            final LinkedHashSet<Integer> initialSet = new LinkedHashSet<>();
            initialSet.add(v);
            sets.add(initialSet);
        }
        for (int u = 0, n = g.getVertexCount(); u < n; u++) {
            for (int v = 0, m = g.getNextCount(u); v < m; v++) {
                Set<Integer> uset = sets.get(u);
                Set<Integer> vset = sets.get(v);
                if (uset != vset) {
                    if (uset.size() < vset.size()) {
                        for (Integer uu : uset) {
                            sets.set(uu, vset);
                        }
                        vset.addAll(uset);
                    } else {
                        for (Integer vv : vset) {
                            sets.set(vv, uset);
                        }
                        uset.addAll(vset);
                    }
                }

            }
        }

       Map<Set<Integer>,Object> forestMap = new IdentityHashMap<Set<Integer>,Object>();
        List<Set<Integer>> forest = new ArrayList<>();
        for (Set<Integer> set : sets) {
            if (null==forestMap.put(set,set)) {
                forest.add(set);
            }
        }
        return forest;
     }
}