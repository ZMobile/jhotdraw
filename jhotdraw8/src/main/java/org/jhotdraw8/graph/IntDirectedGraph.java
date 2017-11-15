/* @(#)DirectedGraphWithArrows.java
 * Copyright (c) 2017 by the authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.graph;

import javax.annotation.Nonnull;

/**
 * DirectedGraphWithArrows.
 *
 * @author Werner Randelshofer
 * @version $$Id$$
 * @param <A> the arrow type
 */
public interface IntDirectedGraph< A>  {
  /**
     * Returns the number of arrows.
     *
     * @return arrow count
     */
    int getArrowCount();

    /**
     * Returns the i-th next vertex of v.
     *
     * @param v a vertex
     * @param i the index of the desired next vertex
     * @return i the index
     */
    int getNext(int v, int i);

    /**
     * Returns the number of next vertices of v.
     *
     * @param v a vertex
     * @return the number of next vertices of v.
     */
    int getNextCount(int v);

    /**
     * Returns the number of vertices {@code V}.
     *
     * @return vertex count
     */
    int getVertexCount();

    
        /**
     * Returns the index of vertex b.
     *
     * @param a a vertex
     * @param b another vertex
     * @return index of vertex b. Returns -1 if b is not next index of a.
     */
    default int findIndexOfNext(int a, int b) {
        for (int i = 0, n = getNextCount(a); i < n; i++) {
            if (b==getNext(a, i)) {
                return i;
            }
        }
        return -1;
    }
    /**
     * Returns the specified arrow.
     *
     * @param index index of arrow
     * @return arrow
     */
    @Nonnull
    A getArrow(int index);

    /**
     * Returns the specified successor (next) arrow of the specified vertex.
     *
     * @param vertex a vertex
     * @param index index of next arrow
     * @return the specified arrow
     */
    @Nonnull
    A getArrow(int vertex, int index);

    /**
     * Returns the arrow if b is next of a.
     *
     * @param a a vertex
     * @param b a vertex
     * @return the arrow or null if b is not next of a
     */
    @Nonnull
    default A findArrow(int a, int b) {
        int index = findIndexOfNext(a, b);
        return index == -1 ? null : getArrow(a, index);
    }

}
