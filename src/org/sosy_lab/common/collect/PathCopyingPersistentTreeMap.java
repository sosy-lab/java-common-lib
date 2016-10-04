/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.sosy_lab.common.collect;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Ordering;
import com.google.common.collect.UnmodifiableIterator;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.annotations.concurrent.LazyInit;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.AbstractSet;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import javax.annotation.Nullable;

/**
 * This is an implementation of {@link PersistentSortedMap} that is based on left-leaning red-black
 * trees (LLRB) and path copying. Left-leaning red-black trees are similar to red-black trees and
 * 2-3 trees, but are considerably easier to implement than red-black trees. They are described by
 * Robert Sedgewick here: http://www.cs.princeton.edu/~rs/talks/LLRB/RedBlack.pdf
 *
 * <p>The operations insert, lookup, and remove are guaranteed to run in O(log n) time. Insert and
 * remove allocate at most O(log n) memory. Traversal through all entries also allocates up to O(log
 * n) memory. Per entry, this map needs memory for one object with 4 reference fields and 1 boolean.
 * (This is a little bit less than {@link TreeMap} needs.)
 *
 * <p>This implementation does not support <code>null</code> keys (but <code>null</code> values) and
 * always compares according to the natural ordering. All methods may throw {@link
 * ClassCastException} is key objects are passed that do not implement {@link Comparable}.
 *
 * <p>The natural ordering of the keys needs to be consistent with equals.
 *
 * <p>As for all {@link PersistentMap}s, all collection views and all iterators are immutable. They
 * do not reflect changes made to the map and all their modifying operations throw {@link
 * UnsupportedOperationException}.
 *
 * <p>All instances of this class are fully-thread safe. However, note that each modifying operation
 * allocates a new instance whose reference needs to be published safely in order to be usable by
 * other threads. Two concurrent accesses to a modifying operation on the same instance will create
 * two new maps, each reflecting exactly the operation executed by the current thread, and not
 * reflecting the operation executed by the other thread.
 *
 * @param <K> The type of keys.
 * @param <V> The type of values.
 */
@Immutable(containerOf = {"K", "V"})
public final class PathCopyingPersistentTreeMap<K extends Comparable<? super K>, V>
    extends AbstractImmutableSortedMap<K, V> implements PersistentSortedMap<K, V>, Serializable {

  private static final long serialVersionUID = 1041711151457528188L;

  @SuppressWarnings("unused")
  @SuppressFBWarnings(
    value = "EQ_DOESNT_OVERRIDE_EQUALS",
    justification = "Inherits equals() according to specification."
  )
  @Immutable(containerOf = {"K", "V"})
  private static final class Node<K, V> extends SimpleImmutableEntry<K, V> {

    // Constants for isRed field
    private static final boolean RED = true;
    private static final boolean BLACK = false;

    private static final long serialVersionUID = -7393505826652634501L;

    private @Nullable final Node<K, V> left;
    private @Nullable final Node<K, V> right;
    private final boolean isRed;

    // Leaf node
    Node(K pKey, V pValue) {
      super(pKey, pValue);
      left = null;
      right = null;
      isRed = RED;
    }

    // Any node
    Node(K pKey, V pValue, Node<K, V> pLeft, Node<K, V> pRight, boolean pRed) {
      super(pKey, pValue);
      left = pLeft;
      right = pRight;
      isRed = pRed;
    }

    boolean isLeaf() {
      return left == null && right == null;
    }

    boolean isRed() {
      return isRed;
    }

    boolean isBlack() {
      return !isRed;
    }

    boolean getColor() {
      return isRed;
    }

    static boolean isRed(@Nullable Node<?, ?> n) {
      return n != null && n.isRed;
    }

    static boolean isBlack(@Nullable Node<?, ?> n) {
      return n != null && n.isBlack();
    }

    // Methods for creating new nodes based on current node.

    Node<K, V> withColor(boolean color) {
      if (isRed == color) {
        return this;
      } else {
        return new Node<>(getKey(), getValue(), left, right, color);
      }
    }

    @SuppressWarnings("ReferenceEquality") // cannot use equals() for check whether tree is the same
    Node<K, V> withLeftChild(Node<K, V> newLeft) {
      if (newLeft == left) {
        return this;
      } else {
        return new Node<>(getKey(), getValue(), newLeft, right, isRed);
      }
    }

    @SuppressWarnings("ReferenceEquality") // cannot use equals() for check whether tree is the same
    Node<K, V> withRightChild(Node<K, V> newRight) {
      if (newRight == right) {
        return this;
      } else {
        return new Node<>(getKey(), getValue(), left, newRight, isRed);
      }
    }

    static int countNodes(@Nullable Node<?, ?> n) {
      if (n == null) {
        return 0;
      }
      return countNodes(n.left) + 1 + countNodes(n.right);
    }
  }

  // static creation methods

  private static final PathCopyingPersistentTreeMap<?, ?> EMPTY_MAP =
      new PathCopyingPersistentTreeMap<String, Object>(null);

  @SuppressWarnings("unchecked")
  public static <K extends Comparable<? super K>, V> PersistentSortedMap<K, V> of() {
    return (PersistentSortedMap<K, V>) EMPTY_MAP;
  }

  public static <K extends Comparable<? super K>, V> PersistentSortedMap<K, V> copyOf(
      Map<K, V> map) {
    checkNotNull(map);

    if (map instanceof PathCopyingPersistentTreeMap<?, ?>) {
      return (PathCopyingPersistentTreeMap<K, V>) map;
    }

    PersistentSortedMap<K, V> result = of();
    for (Map.Entry<K, V> entry : map.entrySet()) {
      result = result.putAndCopy(entry.getKey(), entry.getValue());
    }
    return result;
  }

  // state and constructor

  private final @Nullable Node<K, V> root;

  private transient @LazyInit @Nullable EntrySet<K, V> entrySet;

  private PathCopyingPersistentTreeMap(@Nullable Node<K, V> pRoot) {
    root = pRoot;
  }

  // private utility methods

  @SuppressWarnings("unchecked")
  @Nullable
  private static <K extends Comparable<? super K>, V> Node<K, V> findNode(
      Object key, Node<K, V> root) {
    checkNotNull(key);
    return findNode((K) key, root);
  }

  @Nullable
  private static <K extends Comparable<? super K>, V> Node<K, V> findNode(K key, Node<K, V> root) {
    checkNotNull(key);

    Node<K, V> current = root;
    while (current != null) {
      int comp = key.compareTo(current.getKey());

      if (comp < 0) {
        // key < current.data

        current = current.left;

      } else if (comp > 0) {
        // key > current.data

        current = current.right;

      } else {
        // key == current.data

        return current;
      }
    }
    return null;
  }

  /**
   * Find the node with the smallest key in a given non-empty subtree.
   *
   * @param root The subtree to search in.
   * @return The node with the smallest key.
   * @throws NullPointerException If tree is empty.
   */
  private static <K extends Comparable<? super K>, V> Node<K, V> findSmallestNode(Node<K, V> root) {
    Node<K, V> current = root;
    while (current.left != null) {
      current = current.left;
    }
    return current;
  }

  /**
   * Find the node with the largest key in a given non-empty subtree.
   *
   * @param root The subtree to search in.
   * @return The node with the largest key.
   * @throws NullPointerException If tree is empty.
   */
  private static <K extends Comparable<? super K>, V> Node<K, V> findLargestNode(Node<K, V> root) {
    Node<K, V> current = root;
    while (current.right != null) {
      current = current.right;
    }
    return current;
  }

  /**
   * Given a key and a tree, find the node in the tree with the given key, or (if there is no such
   * node) the node with the smallest key that is still greater than the key to look for. In terms
   * of SortedMap operations, this returns the node for map.tailMap(key).first() (tailMap() has an
   * inclusive bound). Returns null if the tree is empty or there is no node that matches (i.e., key
   * is larger than the largest key in the map).
   *
   * @param key The key to search for.
   * @param root The tree to look in.
   * @return A node or null.
   */
  private static @Nullable <K extends Comparable<? super K>, V>
      Node<K, V> findNextGreaterOrEqualNode(K key, Node<K, V> root) {
    checkNotNull(key);

    Node<K, V> result = null; // this is always greater than or equal to key

    Node<K, V> current = root;
    while (current != null) {
      int comp = key.compareTo(current.getKey());

      if (comp < 0) {
        // key < current.data
        // All nodes to the right of current are irrelevant because they are too big.
        // current is the best candidate we have found so far, so it becomes the new result
        // (current is always smaller than the previous result and still bigger than key).

        result = current;
        current = current.left;

      } else if (comp > 0) {
        // key > current.data
        // All nodes to the left of current are irrelevant because they are too small.
        // current itself is too small, too.

        current = current.right;

      } else {
        // key == current.data

        return current;
      }

      if (current == null) {
        // We have reached a leaf without finding the element.
        return result;
      }
    }
    return null;
  }

  /**
   * Given a key and a tree, find the node with the largest key that is still strictly smaller than
   * the key to look for. In terms of SortedMap operations, this returns the node for
   * map.headMap(key).last() (heapMap() has an exclusive bound). Returns null if the tree is empty
   * or there is no node that matches (i.e., key is smaller than or equal to the smallest key in the
   * map).
   *
   * @param key The key to search for.
   * @param root The tree to look in.
   * @return A node or null.
   */
  private static @Nullable <K extends Comparable<? super K>, V>
      Node<K, V> findNextStrictlySmallerNode(K key, Node<K, V> root) {
    checkNotNull(key);

    Node<K, V> result = null; // this is always smaller than key

    Node<K, V> current = root;
    while (current != null) {
      int comp = key.compareTo(current.getKey());

      if (comp < 0) {
        // key < current.data
        // All nodes to the right of current are irrelevant because they are too big.
        // current itself is too big, too.

        current = current.left;

      } else if (comp > 0) {
        // key > current.data
        // All nodes to the left of current are irrelevant because they are too small.
        // current is the best candidate we have found so far, so it becomes the new result
        // (current is always bigger than the previous result and still smaller than key).

        result = current;
        current = current.right;

      } else {
        // key == current.data
        // All nodes to the right of current are irrelevant because they are too big.
        // current itself is too big, too.
        // The right-most node in the left subtree of child is the result.

        if (current.left == null) {
          // no node smaller than key in this subtree
          return result;
        } else {
          return findLargestNode(current.left);
        }
      }

      if (current == null) {
        // We have reached a leaf without finding the element.
        return result;
      }
    }
    return null;
  }

  private static <K extends Comparable<? super K>, V> int checkAssertions(Node<K, V> current)
      throws IllegalStateException {
    if (current == null) {
      return 0;
    }

    // check property of binary search tree
    if (current.left != null) {
      checkState(
          current.getKey().compareTo(current.left.getKey()) > 0,
          "Tree has left child that is not smaller.");
    }
    if (current.right != null) {
      checkState(
          current.getKey().compareTo(current.right.getKey()) < 0,
          "Tree has right child that is not bigger.");
    }

    // Check LLRB invariants
    // No red right child.
    checkState(!Node.isRed(current.right), "LLRB has red right child");
    // No more than two consecutive red nodes.
    checkState(
        !Node.isRed(current) || !Node.isRed(current.left) || !Node.isRed(current.left.left),
        "LLRB has three red nodes in a row.");

    // Check recursively.
    int leftBlackHeight = checkAssertions(current.left);
    int rightBlackHeight = checkAssertions(current.right);

    // Check black height balancing.
    checkState(
        leftBlackHeight == rightBlackHeight,
        "Black path length on left is " + leftBlackHeight + " and on right is " + rightBlackHeight);

    int blackHeight = leftBlackHeight;
    if (current.isBlack()) {
      blackHeight++;
    }
    return blackHeight;
  }

  @VisibleForTesting
  @SuppressWarnings("CheckReturnValue")
  void checkAssertions() throws IllegalStateException {
    checkAssertions(root);
  }

  // modifying methods

  /**
   * Create a map instance with a given root node.
   *
   * @param newRoot A node or null (meaning the empty tree).
   * @return A map instance with the given tree.
   */
  @SuppressWarnings("ReferenceEquality") // cannot use equals() for check whether tree is the same
  private PersistentSortedMap<K, V> mapFromTree(Node<K, V> newRoot) {
    if (newRoot == root) {
      return this;
    } else if (newRoot == null) {
      return of();
    } else {
      // Root is always black.
      newRoot = newRoot.withColor(Node.BLACK);
      return new PathCopyingPersistentTreeMap<>(newRoot);
    }
  }

  @Override
  public PersistentSortedMap<K, V> putAndCopy(K key, V value) {
    return mapFromTree(putAndCopy0(checkNotNull(key), value, root));
  }

  private static <K extends Comparable<? super K>, V> Node<K, V> putAndCopy0(
      final K key, V value, Node<K, V> current) {
    // Inserting is easy:
    // We find the place where to insert,
    // and afterwards fix the invariants by some rotations or re-colorings.

    if (current == null) {
      return new Node<>(key, value);
    }

    int comp = key.compareTo(current.getKey());
    if (comp < 0) {
      // key < current.data
      final Node<K, V> newLeft = putAndCopy0(key, value, current.left);
      current = current.withLeftChild(newLeft);

    } else if (comp > 0) {
      // key > current.data
      final Node<K, V> newRight = putAndCopy0(key, value, current.right);
      current = current.withRightChild(newRight);

    } else {
      current = new Node<>(key, value, current.left, current.right, current.getColor());
    }

    // restore invariants
    return restoreInvariants(current);
  }

  @SuppressWarnings("unchecked")
  @Override
  public PersistentSortedMap<K, V> removeAndCopy(final Object key) {
    if (isEmpty()) {
      return this;
    }
    return mapFromTree(removeAndCopy0((K) checkNotNull(key), root));
  }

  @Nullable
  private static <K extends Comparable<? super K>, V> Node<K, V> removeAndCopy0(
      final K key, Node<K, V> current) {
    // Removing a node is more difficult.
    // We can remove a leaf if it is red.
    // So we try to always have a red node while going downwards.
    // This is accomplished by calling moveRedLeft() when going downwards to the left
    // or by calling moveRedRight() otherwise.
    // If we found the node and it is a leaf, we can then delete it
    // and do the usual adjustments for re-establishing the invariants (just like for insertion).
    // If we found the node and it is not a leaf node,
    // we can use a trick. We replace the node with the next greater node
    // (the left-most node in the right subtree),
    // and afterwards delete that node from the right subtree (otherwise it would be duplicate).

    int comp = key.compareTo(current.getKey());

    if (comp < 0) {
      // key < current.data
      if (current.left == null) {
        // Target key is not in map.
        return current;
      }

      // Go down leftwards, keeping a red node.

      if (!Node.isRed(current.left) && !Node.isRed(current.left.left)) {
        // Push red to left if necessary.
        current = makeLeftRed(current);
      }

      // recursive descent
      final Node<K, V> newLeft = removeAndCopy0(key, current.left);
      current = current.withLeftChild(newLeft);

    } else {
      // key >= current.data
      if ((comp > 0) && (current.right == null)) {
        // Target key is not in map.
        return current;
      }

      if (Node.isRed(current.left)) {
        // First chance to push red to right.
        current = rotateClockwise(current);

        // re-update comp
        comp = key.compareTo(current.getKey());
        assert comp >= 0;
      }

      if ((comp == 0) && (current.right == null)) {
        assert current.left == null;
        // We can delete the node easily, it's a leaf.
        return null;
      }

      if (!Node.isRed(current.right) && !Node.isRed(current.right.left)) {
        // Push red to right.
        current = makeRightRed(current);

        // re-update comp
        comp = key.compareTo(current.getKey());
        assert comp >= 0;
      }

      if (comp == 0) {
        // We have to delete current, but is has children.
        // We replace current with the smallest node in the right subtree (the "successor"),
        // and delete that (leaf) node there.

        Node<K, V> successor = current.right;
        while (successor.left != null) {
          successor = successor.left;
        }

        // Delete the successor
        Node<K, V> newRight = removeMininumNodeInTree(current.right);
        // and replace current with it
        current =
            new Node<>(
                successor.getKey(),
                successor.getValue(),
                current.left,
                newRight,
                current.getColor());

      } else {
        // key > current.data
        // Go down rightwards.

        final Node<K, V> newRight = removeAndCopy0(key, current.right);
        current = current.withRightChild(newRight);
      }
    }

    return restoreInvariants(current);
  }

  /**
   * Unconditionally delete the node with the smallest key in a given subtree.
   *
   * @return A new subtree reflecting the change.
   */
  @Nullable
  private static <K, V> Node<K, V> removeMininumNodeInTree(Node<K, V> current) {
    if (current.left == null) {
      // This is the minium node to delete
      return null;
    }

    if (!Node.isRed(current.left) && !Node.isRed(current.left.left)) {
      // Push red to left if necessary (similar to general removal strategy).
      current = makeLeftRed(current);
    }

    // recursive descent
    Node<K, V> newLeft = removeMininumNodeInTree(current.left);
    current = current.withLeftChild(newLeft);

    return restoreInvariants(current);
  }

  /**
   * Fix the LLRB invariants around a given node (regarding the node, its children, and
   * grand-children).
   *
   * @return A new subtree with the same content that is a legal LLRB.
   */
  private static <K, V> Node<K, V> restoreInvariants(Node<K, V> current) {
    if (Node.isRed(current.right)) {
      // Right should not be red in a left-leaning red-black tree.
      current = rotateCounterclockwise(current);
    }

    if (Node.isRed(current.left) && Node.isRed(current.left.left)) {
      // Don't have consecutive red nodes.
      current = rotateClockwise(current);
    }

    if (Node.isRed(current.left) && Node.isRed(current.right)) {
      // Again, don't have red right children.
      // We make both children black and this one red,
      // so we pass the potential problem of having a red right upwards in the tree.
      current = colorFlip(current);
    }

    return current;
  }

  /**
   * Flip the colors of current and its two children. This is an operation that keeps the "black
   * height".
   *
   * @param current A node with two children.
   * @return The same subtree, but with inverted colors for the three top nodes.
   */
  private static <K, V> Node<K, V> colorFlip(Node<K, V> current) {
    final Node<K, V> newLeft = current.left.withColor(!current.left.getColor());
    final Node<K, V> newRight = current.right.withColor(!current.right.getColor());
    return new Node<>(current.getKey(), current.getValue(), newLeft, newRight, !current.getColor());
  }

  private static <K, V> Node<K, V> rotateCounterclockwise(Node<K, V> current) {
    // the node that is moved between subtrees:
    final Node<K, V> crossoverNode = current.right.left;
    final Node<K, V> newLeft =
        new Node<>(current.getKey(), current.getValue(), current.left, crossoverNode, Node.RED);
    return new Node<>(
        current.right.getKey(),
        current.right.getValue(),
        newLeft,
        current.right.right,
        current.getColor());
  }

  private static <K, V> Node<K, V> rotateClockwise(Node<K, V> current) {
    // the node that is moved between subtrees:
    final Node<K, V> crossOverNode = current.left.right;
    final Node<K, V> newRight =
        new Node<>(current.getKey(), current.getValue(), crossOverNode, current.right, Node.RED);
    return new Node<>(
        current.left.getKey(),
        current.left.getValue(),
        current.left.left,
        newRight,
        current.getColor());
  }

  private static <K, V> Node<K, V> makeLeftRed(Node<K, V> current) {
    // Make current.left or one of its children red
    // (assuming that current is red and both current.left and current.left.left are black).

    current = colorFlip(current);
    if (Node.isRed(current.right.left)) {
      Node<K, V> newRight = rotateClockwise(current.right);
      current =
          new Node<>(
              current.getKey(), current.getValue(), current.left, newRight, current.getColor());

      current = rotateCounterclockwise(current);
      current = colorFlip(current);
    }
    return current;
  }

  private static <K, V> Node<K, V> makeRightRed(Node<K, V> current) {
    // Make current.right or one of its children red
    // (assuming that current is red and both current.right and current.right.left are black).

    current = colorFlip(current);
    if (Node.isRed(current.left.left)) {
      current = rotateClockwise(current);
      current = colorFlip(current);
    }
    return current;
  }

  // read operations

  @Override
  public PersistentSortedMap<K, V> empty() {
    return of();
  }

  @Override
  public boolean containsKey(Object pObj) {
    return findNode(pObj, root) != null;
  }

  @Override
  public V get(Object pObj) {
    Node<K, V> node = findNode(pObj, root);
    return node == null ? null : node.getValue();
  }

  @Override
  public V getOrDefault(Object pKey, V pDefaultValue) {
    Node<K, V> node = findNode(pKey, root);
    return node == null ? pDefaultValue : node.getValue();
  }

  @Override
  public boolean isEmpty() {
    return root == null;
  }

  @Override
  public SortedSet<Entry<K, V>> entrySet() {
    if (entrySet == null) {
      entrySet = new EntrySet<>(root);
    }
    return entrySet;
  }

  @Override
  public SortedMap<K, V> subMap(K pFromKey, K pToKey) {
    checkNotNull(pFromKey);
    checkNotNull(pToKey);

    return PartialSortedMap.create(root, pFromKey, pToKey);
  }

  @Override
  public SortedMap<K, V> headMap(K pToKey) {
    checkNotNull(pToKey);

    return PartialSortedMap.create(root, null, pToKey);
  }

  @Override
  public SortedMap<K, V> tailMap(K pFromKey) {
    checkNotNull(pFromKey);

    return PartialSortedMap.create(root, pFromKey, null);
  }

  /**
   * Entry set implementation. All methods are implemented by this class, none are delegated to
   * other collections.
   *
   * @param <K> The type of keys.
   * @param <V> The type of values.
   */
  @Immutable(containerOf = {"K", "V"})
  private static final class EntrySet<K extends Comparable<? super K>, V>
      extends AbstractSet<Map.Entry<K, V>> implements SortedSet<Map.Entry<K, V>> {

    private final @Nullable Node<K, V> root;

    // Cache size
    private transient @LazyInit int size = -1;

    private EntrySet(Node<K, V> pRoot) {
      root = pRoot;
    }

    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
      return EntryInOrderIterator.create(root);
    }

    @Override
    public boolean contains(Object pO) {
      if (!(pO instanceof Map.Entry<?, ?>)) {
        return false;
      }
      // pO is not null here
      Map.Entry<?, ?> other = (Map.Entry<?, ?>) pO;
      Map.Entry<?, ?> thisNode = findNode(other.getKey(), root);

      return (thisNode != null) && Objects.equals(thisNode.getValue(), other.getValue());
    }

    @Override
    @SuppressWarnings("ReferenceEquality") // cannot use equals() for check whether tree is the same
    public boolean containsAll(Collection<?> pC) {
      if (pC instanceof EntrySet<?, ?>) {
        // We can rely on sorted-set semantics here and optimize.
        EntrySet<?, ?> other = (EntrySet<?, ?>) pC;
        if (this.root == other.root) {
          return true;
        }
      }

      if (pC.size() > this.size()) {
        return false;
      }

      // Arbitrary collection, delegate to AbstractSet
      return super.containsAll(pC);
    }

    // This method is unused because we should benchmark whether it is actually
    // faster.
    @SuppressWarnings("unused")
    private boolean containsAll(EntrySet<?, ?> other) {
      // There are two strategies for containsAll:
      // 1) iterate through both sets simultaneously
      // 2) iterate through the other set and check for containment each time
      // Assuming this set has n elements and the other has k, the time is as follows:
      // 1) O(n)              (because k < n)
      // 2) O(k * log(n))     (lookup is logarithmic)
      // Thus 2) is better if (n >= k*log(n))
      // <===> 2^n >= 2^(k*log(n))
      // <===> 2^n >= n^k
      if (Math.pow(2, this.size()) >= Math.pow(this.size(), other.size())) {
        // AbstractSet implements method 2)
        return super.containsAll(other);
      }

      // The rest of this method implements method 1)
      Iterator<Map.Entry<K, V>> thisIt = this.iterator();
      Iterator<? extends Map.Entry<?, ?>> otherIt = other.iterator();

      // We iterate synchronously through the sets.
      // otherEntry is always the next entry we have to find in this set.
      // If its not there, we can return false.
      Map.Entry<?, ?> otherEntry = null;

      while (thisIt.hasNext() && otherIt.hasNext()) {
        Map.Entry<K, V> thisEntry = thisIt.next();

        if (otherEntry == null) {
          otherEntry = otherIt.next();
        }

        @SuppressWarnings("unchecked")
        int comp = thisEntry.getKey().compareTo((K) otherEntry.getKey());

        if (comp < 0) {
          // thisEntry < otherEntry, just continue

        } else if (comp > 0) {
          // thisEntry > otherEntry
          // There is no matching entry of otherEntry in this set.
          return false;

        } else {
          // thisEntry == otherEntry
          if (!Objects.equals(thisEntry.getKey(), otherEntry.getKey())) {
            // value mis-match
            return false;
          }

          // setting this to null forwards both iterators by one in the next loop iteration
          otherEntry = null;
        }
      }

      // If otherIt still has elements, they are unequal.
      return !otherIt.hasNext();
    }

    @Override
    public int size() {
      if (size < 0) {
        size = Node.countNodes(root);
      }
      return size;
    }

    @Override
    public boolean isEmpty() {
      return root == null;
    }

    @Override
    public Entry<K, V> first() {
      if (isEmpty()) {
        throw new NoSuchElementException();
      }

      return findSmallestNode(root);
    }

    @Override
    public Entry<K, V> last() {
      if (isEmpty()) {
        throw new NoSuchElementException();
      }

      return findLargestNode(root);
    }

    @Override
    @SuppressWarnings("ReferenceEquality") // cannot use equals() for check whether tree is the same
    public boolean equals(Object pO) {
      if (pO instanceof EntrySet<?, ?>) {
        EntrySet<?, ?> other = (EntrySet<?, ?>) pO;
        if (this.root == other.root) {
          return true;
        }

        if (this.size() != other.size()) {
          return false;
        }

        // this is faster than the fallback check because it's O(n)
        return Iterables.elementsEqual(this, other);
      }
      return super.equals(pO); // delegate to AbstractSet
    }

    @Override
    public int hashCode() {
      // hashCode() delegates to AbstractSet
      return super.hashCode();
    }

    @Override
    public Comparator<? super Entry<K, V>> comparator() {
      return Ordering.natural().onResultOf(Entry::getKey);
    }

    @Override
    public SortedSet<Entry<K, V>> subSet(Entry<K, V> pFromElement, Entry<K, V> pToElement) {
      K fromKey = pFromElement.getKey();
      K toKey = pToElement.getKey();

      checkNotNull(fromKey);
      checkNotNull(toKey);

      return PartialSortedMap.create(root, fromKey, toKey).entrySet();
    }

    @Override
    public SortedSet<Entry<K, V>> headSet(Entry<K, V> pToElement) {
      K toKey = pToElement.getKey();

      checkNotNull(toKey);

      return PartialSortedMap.create(root, null, toKey).entrySet();
    }

    @Override
    public SortedSet<Entry<K, V>> tailSet(Entry<K, V> pFromElement) {
      K fromKey = pFromElement.getKey();

      checkNotNull(fromKey);

      return PartialSortedMap.create(root, fromKey, null).entrySet();
    }
  }

  /**
   * Tree iterator with in-order iteration returning node objects, with possibility for lower and
   * upper bound. The lower bound (if present) needs to exist in the set and is inclusive, the upper
   * bound is exclusive.
   *
   * @param <K> The type of keys.
   * @param <V> The type of values.
   */
  private static class EntryInOrderIterator<K extends Comparable<? super K>, V>
      extends UnmodifiableIterator<Map.Entry<K, V>> {

    // invariants:
    // stack.top is always the next element to be returned
    // (i.e., its left subtree has already been handled)

    private final Deque<Node<K, V>> stack;

    // If not null, iteration stops at this key.
    private final @Nullable K highKey; // exclusive

    static <K extends Comparable<? super K>, V> Iterator<Map.Entry<K, V>> create(
        @Nullable Node<K, V> root) {
      if (root == null) {
        return Collections.emptyIterator();
      } else {
        return new EntryInOrderIterator<>(root, null, null);
      }
    }

    /**
     * Create a new iterator with an optional lower and upper bound.
     *
     * @param pFromKey null or inclusive lower bound that needs to exist in the map
     * @param pToKey null or exclusive lower bound
     */
    static <K extends Comparable<? super K>, V> Iterator<Map.Entry<K, V>> createWithBounds(
        @Nullable Node<K, V> root, @Nullable K pFromKey, @Nullable K pToKey) {
      if (root == null) {
        return Collections.emptyIterator();
      } else {
        return new EntryInOrderIterator<>(root, pFromKey, pToKey);
      }
    }

    private EntryInOrderIterator(Node<K, V> root, @Nullable K pLowKey, @Nullable K pHighKey) {
      stack = new ArrayDeque<>();
      highKey = pHighKey;

      if (pLowKey == null) {
        pushLeftMostNodesOnStack(root);
      } else {
        pushNodesToKeyOnStack(root, pLowKey);
      }
      stopFurtherIterationIfOutOfRange();
    }

    @Override
    public boolean hasNext() {
      return !stack.isEmpty();
    }

    private void pushLeftMostNodesOnStack(Node<K, V> current) {
      while (current.left != null) {
        stack.push(current);
        current = current.left;
      }
      stack.push(current);
    }

    private void pushNodesToKeyOnStack(Node<K, V> current, K key) {
      while (current != null) {
        int comp = key.compareTo(current.getKey());

        if (comp < 0) {
          stack.push(current);
          current = current.left;

        } else if (comp > 0) {
          // This node and it's left subtree can be ignored completely.
          current = current.right;

        } else {
          stack.push(current);
          return;
        }
      }
      throw new AssertionError(
          "PartialEntryInOrderIterator created with lower bound that is not in map");
    }

    private void stopFurtherIterationIfOutOfRange() {
      if (highKey != null && !stack.isEmpty() && stack.peek().getKey().compareTo(highKey) >= 0) {
        // We have reached the end, next element would already be too large
        stack.clear();
      }
    }

    @Override
    public Map.Entry<K, V> next() {
      Node<K, V> current = stack.pop();
      // this is the element to be returned

      // if it has a right subtree,
      // push it on stack so that it will be handled next
      if (current.right != null) {
        pushLeftMostNodesOnStack(current.right);
      }

      stopFurtherIterationIfOutOfRange();

      return current;
    }
  }

  /**
   * Partial map implementation for {@link SortedMap#subMap(Object, Object)} etc. At least one bound
   * (upper/lower) needs to be present. The lower bound (if present) needs to exist in the map and
   * is inclusive, the upper bound is exclusive. The range needs to contain at least one mapping.
   *
   * @param <K> The type of keys.
   * @param <V> The type of values.
   */
  @Immutable(containerOf = {"K", "V"})
  private static class PartialSortedMap<K extends Comparable<? super K>, V>
      extends AbstractImmutableSortedMap<K, V> implements OurSortedMap<K, V> {

    static <K extends Comparable<? super K>, V> OurSortedMap<K, V> create(
        Node<K, V> pRoot, @Nullable K pFromKey, @Nullable K pToKey) {
      checkArgument(pFromKey != null || pToKey != null);

      if (pFromKey != null && pToKey != null) {
        int comp = pFromKey.compareTo(pToKey);
        if (comp == 0) {
          return EmptyImmutableOurSortedMap.<K, V>of();
        }
        checkArgument(comp < 0, "fromKey > toKey");
      }

      Node<K, V> root = findBestRoot(pRoot, pFromKey, pToKey);
      if (root == null) {
        return EmptyImmutableOurSortedMap.<K, V>of();
      }

      K fromKey = pFromKey;
      Node<K, V> lowestNode = null;
      if (pFromKey != null) {
        lowestNode = findNextGreaterOrEqualNode(pFromKey, root);
        if (lowestNode == null) {
          return EmptyImmutableOurSortedMap.<K, V>of();
        }
        fromKey = lowestNode.getKey();
      }

      Node<K, V> highestNode = null;
      if (pToKey != null) {
        highestNode = findNextStrictlySmallerNode(pToKey, root);
        if (highestNode == null) {
          return EmptyImmutableOurSortedMap.<K, V>of();
        }
      }

      if (pFromKey != null && pToKey != null) {
        assert lowestNode != null && highestNode != null;
        // [pFromKey; pToKey[ == [lowestNode; pToKey[ == [lowestNode; highestNode]

        if (lowestNode.getKey().compareTo(highestNode.getKey()) > 0) {
          // no mappings in [pFromKey; pToKey[
          return EmptyImmutableOurSortedMap.<K, V>of();
        }
      }

      return new PartialSortedMap<>(root, fromKey, pToKey);
    }

    // Find the best root for a given set of bounds
    // (the lowest node in the tree that represents the complete range).
    // Not using root directly but potentially only a subtree is more efficient.
    private static @Nullable <K extends Comparable<? super K>, V> Node<K, V> findBestRoot(
        @Nullable Node<K, V> pRoot, @Nullable K pFromKey, @Nullable K pToKey) {

      Node<K, V> current = pRoot;
      while (current != null) {

        if (pFromKey != null && current.getKey().compareTo(pFromKey) < 0) {
          // current < fromKey -> current and left subtree can be ignored
          current = current.right;
        } else if (pToKey != null && current.getKey().compareTo(pToKey) >= 0) {
          // current -> toKey -> current and right subtree can be ignored
          current = current.left;
        } else {
          // current is in range
          return current;
        }
      }

      return null; // no mapping in range
    }

    // Invariant: This map is never empty.

    private final Node<K, V> root;

    // null if there is no according bound
    private final @Nullable K fromKey; // inclusive
    private final @Nullable K toKey; // exclusive

    private transient @LazyInit @Nullable SortedSet<Map.Entry<K, V>> entrySet;

    private PartialSortedMap(Node<K, V> pRoot, @Nullable K pLowKey, @Nullable K pHighKey) {
      root = pRoot;
      fromKey = pLowKey;
      toKey = pHighKey;

      // check non-emptiness invariant
      assert root != null;
      assert pLowKey == null || containsKey(pLowKey);
      assert pHighKey == null || containsKey(findNextStrictlySmallerNode(pHighKey, pRoot).getKey());
    }

    private boolean inRange(K key) {
      return !tooLow(key) && !tooHigh(key);
    }

    private boolean tooLow(K key) {
      return fromKey != null && key.compareTo(fromKey) < 0;
    }

    private boolean tooHigh(K key) {
      return toKey != null && key.compareTo(toKey) >= 0;
    }

    @Override
    public boolean containsKey(Object pKey) {
      @SuppressWarnings("unchecked")
      K key = (K) checkNotNull(pKey);
      return inRange(key) && findNode(key, root) != null;
    }

    @Override
    public V get(Object pKey) {
      @SuppressWarnings("unchecked")
      K key = (K) checkNotNull(pKey);
      if (!inRange(key)) {
        return null;
      }
      Node<K, V> node = findNode(key, root);
      return node == null ? null : node.getValue();
    }

    @Override
    public boolean isEmpty() {
      return false;
    }

    @Override
    public SortedSet<Entry<K, V>> entrySet() {
      if (entrySet == null) {
        entrySet = new PartialEntrySet();
      }
      return entrySet;
    }

    @Override
    public OurSortedMap<K, V> subMap(K pFromKey, K pToKey) {
      checkNotNull(pFromKey);
      checkNotNull(pToKey);

      checkArgument(inRange(pFromKey));
      checkArgument(inRange(pToKey));

      return PartialSortedMap.create(root, pFromKey, pToKey);
    }

    @Override
    public OurSortedMap<K, V> headMap(K pToKey) {
      checkNotNull(pToKey);
      checkArgument(inRange(pToKey));

      return PartialSortedMap.create(root, fromKey, pToKey);
    }

    @Override
    public OurSortedMap<K, V> tailMap(K pFromKey) {
      checkNotNull(pFromKey);
      checkArgument(inRange(pFromKey));

      return PartialSortedMap.create(root, pFromKey, toKey);
    }

    /**
     * Entry set implementation. The lower bound (if present) needs to exist in the map and is
     * inclusive, the upper bound is exclusive. The range needs to contain at least one mapping.
     */
    private class PartialEntrySet extends AbstractSet<Map.Entry<K, V>>
        implements SortedSet<Map.Entry<K, V>> {

      private transient int size;

      @Override
      public Iterator<Map.Entry<K, V>> iterator() {
        return EntryInOrderIterator.createWithBounds(root, fromKey, toKey);
      }

      @SuppressWarnings("unchecked")
      @Override
      public boolean contains(Object pO) {
        if (!(pO instanceof Map.Entry<?, ?>)) {
          return false;
        }
        // pO is not null here
        Map.Entry<?, ?> other = (Map.Entry<?, ?>) pO;
        if (!inRange((K) other.getKey())) {
          return false;
        }
        Map.Entry<?, ?> thisNode = findNode(other.getKey(), root);

        return (thisNode != null) && Objects.equals(thisNode.getValue(), other.getValue());
      }

      @Override
      public boolean containsAll(Collection<?> pC) {
        if (pC.size() > this.size()) {
          return false;
        }

        return super.containsAll(pC);
      }

      @Override
      public boolean isEmpty() {
        return false;
      }

      @Override
      public int size() {
        if (size == 0) {
          size = Iterators.size(iterator());
        }
        return size;
      }

      @Override
      public Map.Entry<K, V> first() {
        if (fromKey == null) {
          return findSmallestNode(root);
        } else {
          return findNextGreaterOrEqualNode(fromKey, root);
        }
      }

      @Override
      public Map.Entry<K, V> last() {
        if (toKey == null) {
          return findLargestNode(root);
        } else {
          return findNextStrictlySmallerNode(toKey, root);
        }
      }

      @Override
      public Comparator<? super Entry<K, V>> comparator() {
        return Ordering.natural().onResultOf(Entry::getKey);
      }

      @Override
      public SortedSet<Entry<K, V>> subSet(Entry<K, V> pFromElement, Entry<K, V> pToElement) {
        return subMap(pFromElement.getKey(), pToElement.getKey()).entrySet();
      }

      @Override
      public SortedSet<Entry<K, V>> headSet(Entry<K, V> pToElement) {
        return headMap(pToElement.getKey()).entrySet();
      }

      @Override
      public SortedSet<Entry<K, V>> tailSet(Entry<K, V> pFromElement) {
        return tailMap(pFromElement.getKey()).entrySet();
      }
    }
  }
}
