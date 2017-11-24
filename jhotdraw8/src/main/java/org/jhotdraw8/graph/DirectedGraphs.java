/* @(#)DirectedGraphs.java
 * Copyright © 2017 by the authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.graph;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.jhotdraw8.collection.IntArrayList;

/**
 * Provides algorithms for directed graphs.
 *
 * @author Werner Randelshofer
 * @version $Id$
 */
public class DirectedGraphs {

    
    private DirectedGraphs() {
    }


    private static <V,A> Map<V, List<V>> createForest(DirectedGraph<V,A> g) {
        // Create initial forest.
        Map<V, List<V>> forest = new LinkedHashMap<>(g.getVertexCount());
        for (int i = 0, n = g.getVertexCount(); i < n; i++) {
            final V v = g.getVertex(i);
            List<V> initialSet = new ArrayList<>(1);
            initialSet.add(v);
            forest.put(v, initialSet);
        }
        return forest;
    }

    private static <V> Map<V, List<V>> createForest(Collection<V> vertices) {
        // Create initial forest.
        Map<V, List<V>> forest = new LinkedHashMap<>(vertices.size());
        for (V v : vertices) {
            List<V> initialSet = new ArrayList<>(1);
            initialSet.add(v);
            forest.put(v, initialSet);
        }
        return forest;
    }

    /**
     * Dumps a directed graph into a String which can be rendered with the "dot"
     * tool.
     *
     * @param <V> the vertex type
     * @param <A> the arrow type
     * @param g the graph
     * @return a "dot" String.
     */
    public static <V,A> String dump(DirectedGraph<V,A> g) {
        StringBuilder b = new StringBuilder();

        for (int i = 0, n = g.getVertexCount(); i < n; i++) {
            V v = g.getVertex(i);
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
     * @param <A> the arrow type
     * @param g a directed graph
     * @return the disjoint sets.
     */
    public static <V,A> List<Set<V>> findDisjointSets(DirectedGraph<V,A> g) {
        // Create initial forest
        Map<V, List<V>> forest = createForest(g);
        // Merge sets.
        for (int i = 0, n = g.getVertexCount(); i < n; i++) {
            V u = g.getVertex(i);
            for (int j = 0, m = g.getNextCount(u); j < m; j++) {
                V v = g.getNext(u, j);
                List<V> uset = forest.get(u);
                List<V> vset = forest.get(v);
                if (uset != vset) {
                    union(uset, vset, forest);
                }
            }
        }

        // Create final forest.
        Set<List<V>> visited = new HashSet<List<V>>(forest.size());
        List<Set<V>> disjointSets = new ArrayList<>(forest.size());
        for (List<V> set : forest.values()) {
            if (visited.add(set)) {
                disjointSets.add(new LinkedHashSet<>(set));
            }
        }
        return disjointSets;
    }

    /**
     * Given an int directed graph, returns all disjoint sets of vertices.
     * <p>
     * Uses Kruskal's algorithm.
     *
     * @param <A> the arrow type
     * @param g a directed graph
     * @return the disjoint sets.
     */
    public static <A> List<Set<Integer>> findDisjointSets(IntDirectedGraph<A> g) {
        // Create initial forest.
        final List<IntArrayList> sets = new ArrayList<>(g.getVertexCount());
        for (int v = 0, n = g.getVertexCount(); v < n; v++) {
            final IntArrayList initialSet = new IntArrayList(1);
            initialSet.add(v);
            sets.add(initialSet);
        }
        // Merge sets.
        for (int u = 0, n = g.getVertexCount(); u < n; u++) {
            for (int v = 0, m = g.getNextCount(u); v < m; v++) {
                final IntArrayList uset = sets.get(u);
                final IntArrayList vset = sets.get(v);
                if (uset != vset) {
                    if (uset.size() < vset.size()) {
                        for (int i = 0, usize = uset.size(); i < usize; i++) {
                            int uu = uset.get(i);
                            sets.set(uu, vset);
                        }
                        vset.addAll(uset);
                    } else {
                        for (int i = 0, vsize = vset.size(); i < vsize; i++) {
                            int vv = vset.get(i);
                            sets.set(vv, uset);
                        }
                        uset.addAll(vset);
                    }
                }
            }
        }
        // Create final forest.
        final Map<IntArrayList, Object> setMap = new HashMap<IntArrayList, Object>();
        final List<Set<Integer>> disjointSets = new ArrayList<>();
        for (IntArrayList set : sets) {
            if (!setMap.containsKey(set)) {
                setMap.put(set, set);
                disjointSets.add(set.addAllInto(new LinkedHashSet<Integer>()));
            }
        }
        return disjointSets;
    }

    /**
     * Given a set of vertices and a list of arrows ordered by cost, returns the
     * minimum spanning tree.
     * <p>
     * Uses Kruskal's algorithm.
     *
     * @param <V> the vertex type
     * @param <A> the arrow type
     * @param vertices a directed graph
     * @param orderedArrows list of arrows sorted by cost in ascending order
     * (lowest cost first, highest cost last).
     * @param rejectedArrows optional, all excluded arrows are added to this list,
     * if it is provided.
     * @return the arrows that are part of the minimum spanning tree.
     */
    public static <V, A extends Pair<V>> List<A> findMinimumSpanningTree(Collection<V> vertices, List<A> orderedArrows, List<A> rejectedArrows) {
        List<A> minimumSpanningTree = new ArrayList<>(orderedArrows.size());
        if (rejectedArrows == null) {
            rejectedArrows = new ArrayList<>(orderedArrows.size());
        }

        // Create initial forest
        Map<V, List<V>> forest = createForest(vertices);

        // Process arrows from lowest cost to highest cost
        for (A arrow : orderedArrows) {
            List<V> uset = forest.get(arrow.getStart());
            List<V> vset = forest.get(arrow.getEnd());
            if (uset != vset) {
                union(uset, vset, forest);
                minimumSpanningTree.add(arrow);
            } else {
                rejectedArrows.add(arrow);
            }
        }

        return minimumSpanningTree;
    }

    /**
     * Given a set of vertices and a list of arrows ordered by cost, returns a
     * builder with the minimum spanning tree. This is an undirected graph with
     * an arrow in each direction.
     * <p>
     *
     * @param <V> the vertex type
     * @param <A> the arrow type
     * @param vertices the list of vertices
     * @param orderedArrows list of arrows sorted by cost in ascending order
     * (lowest cost first, highest cost last)
     * @param includedArrows optional, all included arrows are added to this list, if it is provided.
     * @param rejectedArrows optional, all excluded arrows are added to this list,
     * if it is provided.
     * @return the graph builder
     */
    public static <V, A extends Pair<V>> DirectedGraphBuilder<V,A> findMinimumSpanningTreeGraph(Collection<V> vertices, List<A> orderedArrows, List<A> includedArrows, List<A> rejectedArrows) {
        List<A> includedArrowList = findMinimumSpanningTree(vertices, orderedArrows, rejectedArrows);
        if (includedArrows != null) {
            includedArrows.addAll(includedArrowList);
        }
        DirectedGraphBuilder<V,A> builder = new DirectedGraphBuilder<>();
        for (V v : vertices) {
            builder.addVertex(v);
        }
        for (A e : includedArrowList) {
            builder.addArrow(e.getStart(), e.getEnd(),e);
            builder.addArrow(e.getEnd(), e.getStart(),e);
        }
        return builder;
    }

    /**
     * Sorts the specified directed graph topologically.
     *
     * @param <V> the vertex type
     * @param <A> the arrow type
     * @param m the graph
     * @return the sorted list of vertices
     */
    @SuppressWarnings("unchecked")
    public static <V,A> List<V> sortTopologically(DirectedGraph<V,A> m) {
        final IntDirectedGraph<A> im;
        if (!(m instanceof IntDirectedGraph)) {
            im =new DirectedGraphBuilder<>(m);
        } else {
            im = (IntDirectedGraph<A>) m;
        }
        int[] a = sortTopologicallyInt(im);
        List<V> result = new ArrayList<>(a.length);
        for (int i = 0; i < a.length; i++) {
            result.add(m.getVertex(a[i]));
        }
        return result;
    }

    /**
     * Sorts the specified directed graph topologically.
     *
     * @param <A> the arrow type
     * @param model the graph
     * @return the sorted list of vertices
     */
    public static<A> int[] sortTopologicallyInt(IntDirectedGraph<A> model) {
        final int n = model.getVertexCount();

        // Step 1: compute number of incoming arrows for each vertex
        final int[] deg = new int[n]; // number of unprocessed incoming arrows on vertex
        for (int i = 0; i < n; i++) {
            final int m = model.getNextCount(i);
            for (int j = 0; j < m; j++) {
                int v = model.getNext(i, j);
                deg[v]++;
            }
        }

        // Step 2: put all vertices with degree zero into queue
        final int[] queue = new int[n]; // todo queue
        int first = 0, last = 0; // first and last indices in queue
        for (int i = 0; i < n; i++) {
            if (deg[i] == 0) {
                queue[last++] = i;
            }
        }

        // Step 3: Repeat until all vertices have been processed or a loop has been detected
        final int[] result = new int[n];// result array
        int done = 0;
        Random random=null;
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
                // Break loop in graph by removing an arbitrary arrow.
                if (random == null) {
                    random=new Random(0);
                }
                int i;
                do {
                    i=random.nextInt(n);
                }
                while (deg[i]<=0);
                        deg[i] = 0;// this can actually remove more than one arrow
                        queue[last++]=i;
            }
        }

        return result;
    }

    private static <V> void union(List<V> uset, List<V> vset, Map<V, List<V>> forest) {
        if (uset != vset) {
            if (uset.size() < vset.size()) {
                for (V uu : uset) {
                    forest.put(uu, vset);
                }
                vset.addAll(uset);
            } else {
                for (V vv : vset) {
                    forest.put(vv, uset);
                }
                uset.addAll(vset);
            }
        }
    }

}
