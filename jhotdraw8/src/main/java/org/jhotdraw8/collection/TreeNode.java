/* @(#)TreeNode.java
 * Copyright (c) 2015 by the authors and contributors of JHotDraw.
 * You may only use this file in compliance with the accompanying license terms.
 */
package org.jhotdraw8.collection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents a node of a tree structure.
 * <p>
 * A node has zero or one parents, and zero or more children.
 * <p>
 * All nodes in the same tree structure are of the same type {@literal <T>}.
 * <p>
 * A node may support only a restricted set of parent types {@literal <P extends T>}.
 * <p>
 * A node may only support a restricted set of child types {@literal <C extends T>}.
 * <p>
 * The type {@literal <T>} is checked at compile time using a Java type parameter. 
 * The types {@literal <P>} and {@literal <C>} are checked at runtime.
 *
 * @design.pattern TreeNode Composite, Component.
 * The composite pattern is used to model a tree structure.
 * 
 * @design.pattern TreeNode Iterator, Aggregate.
 * The iterator pattern is used to provide a choice of iteration strategies over an aggregate structure.
 * 
 * @author Werner Randelshofer
 * @version $Id$
 * @param <T> the type of nodes in the tree structure.
 */
public interface TreeNode<T extends TreeNode<T>> {

    /**
     * Returns the children of the tree node.
     * <p>
     * In order to keep the tree structure consistent, the following rules must be followed:
     * <ul>
     * <li>If a child is added to this list, then it must be removed from its former parent,
     * and this this tree node must be set as the parent of the child.</li>
     * <li>
     * If a child is removed from this tree node, then the parent of the child must be set to null.</li>
     * </ul>
     * 
     * @return the children
     */
    List<T> getChildren();

    /**
     * Returns the parent of the tree node.
     * @return the parent. Returns null if the tree node has no parent.
     */
    T getParent();

    /**
     * Returns the nearest ancestor of the specified type.
     *
     * @param <TT> The ancestor type
     * @param ancestorType The ancestor type
     * @return Nearest ancestor of type {@literal <T>} or null if no ancestor of
     * this type is present. Returns {@code this} if this object is of type {@literal <T>}.
     */
    default <TT> TT getAncestor(Class<TT> ancestorType) {
        @SuppressWarnings("unchecked")
        T ancestor = (T) this;
        while (ancestor != null && !ancestorType.isAssignableFrom(ancestor.getClass())) {
            ancestor = ancestor.getParent();
        }
        @SuppressWarnings("unchecked")
        TT temp= (TT) ancestor;
        return temp;
    }

    /**
     * Returns an iterable which can iterate through this figure and all its
     * descendants in preorder sequence.
     *
     * @return the iterable
     */
    default public Iterable<T> preorderIterable() {
        @SuppressWarnings("unchecked")
        Iterable<T> i = () -> new TreeNode.PreorderIterator<>((T) this);
        return i;
    }

    /**
     * Returns a list in preorder sequence.
     * <p>
     * The list is an eager copy of the preorder sequence.
     * 
     * @param <T> the value type
     * @param iterable the iterable
     * @return the list
     */
    static <T> ArrayList<T> toList(Iterable<T> iterable) {
        ArrayList<T> list = new ArrayList<>();
        for (T item:iterable) {
            list.add(item);
        }
        return list;
    }

    /**
     * Returns an iterable which can iterate through this figure and all its
     * descendants in breadth first sequence.
     *
     * @return the iterable
     */
    default public Iterable<T> breadthFirstIterable() {
        @SuppressWarnings("unchecked")
        Iterable<T> i = () -> new TreeNode.BreadthFirstIterator<>((T) this);
        return i;
    }

    /**
     * Returns an iterable which can iterate through this figure and all its
     * ancesters up to the root.
     *
     * @return the iterable
     */
    default Iterable<T> ancestorIterable() {
        @SuppressWarnings("unchecked")
        Iterable<T> i = () -> new TreeNode.AncestorIterator<>((T) this);
        return i;
    }

    /**
     * Dumps the figure and its descendants to system.out.
     */
    default void dumpTree() {
        try {
            dumpTree(System.out, 0);
        } catch (IOException e) {
            throw new InternalError(e);
        }
    }

    /**
     * Dumps the figure and its descendants.
     *
     * @param out an output stream
     * @param depth the indentation depth
     * @throws java.io.IOException from appendable
     */
    default void dumpTree(Appendable out, int depth) throws IOException {
        for (int i = 0; i < depth; i++) {
            out.append('.');
        }
        out.append(toString());
        out.append('\n');
        for (T child : getChildren()) {
            child.dumpTree(out, depth + 1);
        }
    }

    /**
     * @design.pattern TreeNode Iterator, Iterator.
     * 
     * @param <T> the type of the tree nodes
     */
    static class PreorderIterator<T extends TreeNode<T>> implements Iterator<T> {

        private final LinkedList<Iterator<T>> stack = new LinkedList<>();

        private PreorderIterator(T root) {
            LinkedList<T> v = new LinkedList<>();
            v.add(root);
            stack.push(v.iterator());
        }

        @Override
        public boolean hasNext() {
            return (!stack.isEmpty() && stack.peek().hasNext());
        }

        @Override
        public T next() {
            Iterator<T> iter = stack.peek();
            T node = iter.next();
            Iterator<T> children = node.getChildren().iterator();

            if (!iter.hasNext()) {
                stack.pop();
            }
            if (children.hasNext()) {
                stack.push(children);
            }
            return node;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
    /**
     * @design.pattern IterableTree Iterator, Iterator.
     */
    static class BreadthFirstIterator<T extends TreeNode<T>> implements Iterator<T> {

        protected LinkedList<Iterator<T>> queue;

        public BreadthFirstIterator(T root) {
            List<T> l = new LinkedList<>();
            l.add(root);
            queue = new LinkedList<>();
            queue.addLast(l.iterator());
        }

        @Override
        public boolean hasNext() {
            return !queue.isEmpty()
                    && queue.peekFirst().hasNext();
        }

        @Override
        public T next() {
            Iterator<T> iter = queue.peekFirst();
            T node = iter.next();
            Iterator<T> children = node.getChildren().iterator();

            if (!iter.hasNext()) {
                queue.removeFirst();
            }
            if (children.hasNext()) {
                queue.addLast(children);
            }
            return node;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    /**
     * Returns the path to this node.
     *
     * @return path including this node
     */
    @SuppressWarnings("unchecked")
    default List<T> getPath() {
        LinkedList<T> path = new LinkedList<>();
        for (T node = (T) this; node != null; node = node.getParent()) {
            path.addFirst(node);
        }
        return path;
    }
    
    /**
     * @design.pattern TreeNode Iterator, Iterator.
     * 
     * @param <T> the type of the tree nodes
     */
    static class AncestorIterator<T extends TreeNode<T>> implements Iterator<T> {

        private T node;

        private AncestorIterator(T node) {
            this.node = node;
        }

        @Override
        public boolean hasNext() {
            return node != null;
        }

        @Override
        public T next() {
            T next = node;
            node = node.getParent();
            return next;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported.");
        }
    }
}