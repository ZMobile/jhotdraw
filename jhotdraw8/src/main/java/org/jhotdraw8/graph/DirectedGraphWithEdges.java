/* @(#)DirectedGraphWithEdges.java
 * Copyright (c) 2017 by the authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.graph;

import javax.annotation.Nonnull;

/**
 * DirectedGraphWithEdges.
 *
 * @author Werner Randelshofer
 * @version $$Id$$
 * @param <V> the vertex type
 * @param <E> the edge type
 */
public interface DirectedGraphWithEdges<V, E> extends DirectedGraph<V> {

    /**
     * Returns the specified edge.
     *
     * @param indexOfEdge index of edge
     * @return edge
     */
    @Nonnull
    E getEdge(int indexOfEdge);

    /**
     * Returns the specified outgoing (next) edge of the specified vertex.
     *
     * @param vertex a vertex
     * @param index index of next edge
     * @return the specified edge
     */
    @Nonnull
    E getNextEdge(@Nonnull V vertex, int index);

    /**
     * Returns the edge if b is next of a.
     *
     * @param a a vertex
     * @param b a vertex
     * @return the edge or null if b is not next of a
     */
    @Nonnull
    default E findEdge(@Nonnull V a, @Nonnull V b) {
        int index = findIndexOfNext(a, b);
        return index == -1 ? null : getNextEdge(a, index);
    }

}
