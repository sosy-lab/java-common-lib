package org.sosy_lab.common.collect;

import static com.google.common.base.Preconditions.*;

import java.io.Serializable;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.AbstractSet;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.google.common.collect.UnmodifiableIterator;

/**
 * This is an implementation of {@link PersistentSortedMap} that is based on
 * left-leaning red-black trees (LLRB) and path copying.
 * Left-leaning red-black trees are similar to red-black trees and 2-3 trees,
 * but are considerably easier to implement than red-black trees. They are
 * described by Robert Sedgewick here:
 * http://www.cs.princeton.edu/~rs/talks/LLRB/RedBlack.pdf
 *
 * The operations insert, lookup, and remove are guaranteed to run in O(log n) time.
 * Insert and remove allocate at most O(log n) memory.
 * Traversal through all entries also allocates up to O(log n) memory.
 * Per entry, this map needs memory for one object with 4 reference fields and 1 boolean.
 * (This is a little bit less than {@link TreeMap} needs.)
 *
 * This implementation does not support <code>null</code> keys (but <code>null</code> values)
 * and always compares according to the natural ordering.
 * All methods may throw {@link ClassCastException} is key objects are passed
 * that do not implement {@link Comparable}.
 *
 * The natural ordering of the keys needs to be consistent with equals.
 *
 * As for all {@link PersistentMap}s, all collection views and all iterators
 * are immutable. They do not reflect changes made to the map and all their
 * modifying operations throw {@link UnsupportedOperationException}.
 *
 * All instances of this class are fully-thread safe.
 * However, note that each modifying operation allocates a new instance
 * whose reference needs to be published safely in order to be usable by other threads.
 * Two concurrent accesses to a modifying operation on the same instance will
 * create two new maps, each reflecting exactly the operation executed by the current thread,
 * and not reflecting the operation executed by the other thread.
 *
 * TODO Missing methods:
 * Currently not supported operations are
 * {{@link #headMap(Comparable)}, {@link #tailMap(Comparable)}, and {{@link #subMap(Comparable, Comparable)}}
 * as well as their counterparts in the collection views returned by methods of this class.
 *
 * @param <K> The type of keys.
 * @param <V> The type of values.
 */
public final class PathCopyingPersistentTreeMap<K extends Comparable<? super K>, V>
    extends AbstractImmutableMap<K, V>
    implements PersistentSortedMap<K, V>, Serializable {

  private static final long serialVersionUID = 1041711151457528188L;

  @SuppressWarnings("unused")
  private static final class Node<K, V> extends SimpleImmutableEntry<K, V> {

    // Constants for isRed field
    private static final boolean RED   = true;
    private static final boolean BLACK = false;

    private static final long serialVersionUID = -7393505826652634501L;

    private final Node<K, V> left;
    private final Node<K, V> right;
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

    Node<K, V> withLeftChild(Node<K, V> newLeft) {
      if (newLeft == left) {
        return this;
      } else {
        return new Node<>(getKey(), getValue(), newLeft, right, isRed);
      }
    }

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

    static <K> Function<Entry<K, ?>, K> getKeyFunction() {
      return new Function<Map.Entry<K, ?>, K>() {
          @Override
          public K apply(Map.Entry<K, ?> input) {
            return input.getKey();
          }
        };
    }

    static <V> Function<Entry<?, V>, V> getValueFunction() {
      return new Function<Map.Entry<?, V>, V>() {
          @Override
          public V apply(Map.Entry<?, V> input) {
            return input.getValue();
          }
        };
    }
  }


  // static creation methods

  private static final PathCopyingPersistentTreeMap<?, ?> emptyMap = new PathCopyingPersistentTreeMap<String, Object>(null);

  @SuppressWarnings("unchecked")
  public static <K extends Comparable<? super K>, V> PersistentSortedMap<K, V> of() {
    return (PersistentSortedMap<K, V>) emptyMap;
  }

  public static <K extends Comparable<? super K>, V> PersistentSortedMap<K, V> copyOf(Map<K, V> map) {
    checkNotNull(map);

    if (map instanceof PathCopyingPersistentTreeMap<?, ?>) {
      return (PathCopyingPersistentTreeMap<K, V>)map;
    }

    PersistentSortedMap<K, V> result = of();
    for (Map.Entry<K, V> entry : map.entrySet()) {
      result = result.putAndCopy(entry.getKey(), entry.getValue());
    }
    return result;
  }


  // state and constructor

  private final Node<K, V> root;

  private transient EntrySet<K, V> entrySet;

  private PathCopyingPersistentTreeMap(Node<K, V> pRoot) {
    root = pRoot;
  }


  // private utility methods

  @SuppressWarnings("unchecked")
  private static <K extends Comparable<? super K>, V> Node<K, V> findNode(Object key, Node<K, V> root) {
    checkNotNull(key);
    return findNode((K)key, root);
  }

  private static <K extends Comparable<? super K>, V> Node<K, V> findNode(K key, Node<K, V> root) {
    Preconditions.checkNotNull(key);

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

  private static <K extends Comparable<? super K>, V> int checkAssertions(Node<K, V> current) throws IllegalStateException {
    if (current == null) {
      return 0;
    }

    // check property of binary search tree
    if (current.left != null) {
      checkState(current.getKey().compareTo(current.left.getKey()) > 0, "Tree has left child that is not smaller.");
    }
    if (current.right != null) {
      checkState(current.getKey().compareTo(current.right.getKey()) < 0, "Tree has right child that is not bigger.");
    }

    // Check LLRB invariants
    // No red right child.
    checkState(!Node.isRed(current.right), "LLRB has red right child");
    // No more than two consecutive red nodes.
    checkState(!Node.isRed(current) || !Node.isRed(current.left) || !Node.isRed(current.left.left), "LLRB has three red nodes in a row.");

    // Check recursively.
    int leftBlackHeight  = checkAssertions(current.left);
    int rightBlackHeight = checkAssertions(current.right);

    // Check black height balancing.
    checkState(leftBlackHeight == rightBlackHeight, "Black path length on left is " + leftBlackHeight + " and on right is " + rightBlackHeight);

    int blackHeight = leftBlackHeight;
    if (current.isBlack()) {
      blackHeight++;
    }
    return blackHeight;
  }

  @VisibleForTesting
  void checkAssertions() throws IllegalStateException {
    checkAssertions(root);
  }

  // modifying methods

  /**
   * Create a map instance with a given root node.
   * @param newRoot A node or null (meaning the empty tree).
   * @return A map instance with the given tree.
   */
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

  private static <K extends Comparable<? super K>, V> Node<K, V> putAndCopy0(final K key, V value, Node<K, V> current) {
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
    return mapFromTree(removeAndCopy0((K)checkNotNull(key), root));
  }

  private static <K extends Comparable<? super K>, V> Node<K, V>  removeAndCopy0(final K key, Node<K, V> current) {
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
        current = new Node<>(successor.getKey(), successor.getValue(), current.left, newRight, current.getColor());

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
   * @return A new subtree reflecting the change.
   */
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
   * Fix the LLRB invariants around a given node
   * (regarding the node, its children, and grand-children).
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
   * Flip the colors of current and its two children.
   * This is an operation that keeps the "black height".
   * @param current A node with two children.
   * @return The same subtree, but with inverted colors for the three top nodes.
   */
  private static <K, V> Node<K, V>  colorFlip(Node<K, V> current) {
    final Node<K, V> newLeft  = current.left.withColor(!current.left.getColor());
    final Node<K, V> newRight = current.right.withColor(!current.right.getColor());
    return new Node<>(current.getKey(), current.getValue(), newLeft, newRight, !current.getColor());
  }

  private static <K, V> Node<K, V> rotateCounterclockwise(Node<K, V> current) {
    final Node<K, V> crossoverNode = current.right.left; // the node that is moved between subtrees
    final Node<K, V> newLeft = new Node<>(current.getKey(), current.getValue(), current.left, crossoverNode, Node.RED);
    return new Node<>(current.right.getKey(), current.right.getValue(), newLeft, current.right.right, current.getColor());
  }

  private static <K, V> Node<K, V> rotateClockwise(Node<K, V> current) {
    final Node<K, V> crossOverNode = current.left.right; // the node that is moved between subtrees
    final Node<K, V> newRight = new Node<>(current.getKey(), current.getValue(), crossOverNode, current.right, Node.RED);
    return new Node<>(current.left.getKey(), current.left.getValue(), current.left.left, newRight, current.getColor());
  }

  private static <K, V> Node<K, V> makeLeftRed(Node<K, V> current) {
    // Make current.left or one of its children red
    // (assuming that current is red and both current.left and current.left.left are black).

    current = colorFlip(current);
    if (Node.isRed(current.right.left)) {
      Node<K, V> newRight = rotateClockwise(current.right);
      current = new Node<>(current.getKey(), current.getValue(), current.left, newRight, current.getColor());

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
  public boolean containsValue(Object pValue) {
    return values().contains(pValue);
  }

  @Override
  public boolean isEmpty() {
    return root == null;
  }

  @Override
  public int size() {
    return entrySet().size();
  }

  @Override
  public String toString() {
    return entrySet().toString();
  }

  @Override
  public Comparator<? super K> comparator() {
    return null;
  }

  @Override
  public K firstKey() {
    return entrySet().first().getKey();
  }

  @Override
  public K lastKey() {
    return entrySet().last().getKey();
  }

  @Override
  public boolean equals(Object pObj) {
    if (pObj instanceof Map<?, ?>) {
      return entrySet().equals(((Map<?, ?>)pObj).entrySet());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return entrySet().hashCode();
  }

  @Override
  public SortedSet<Entry<K, V>> entrySet() {
    if (entrySet == null) {
      entrySet = new EntrySet<>(root);
    }
    return entrySet;
  }

  @Override
  public SortedSet<K> keySet() {
    return new SortedMapKeySet<>(this);
  }

  @Override
  public Collection<V> values() {
    return Collections2.transform(entrySet(), Node.<V>getValueFunction());
  }

  @Override
  public SortedMap<K, V> subMap(K pFromKey, K pToKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public SortedMap<K, V> headMap(K pToKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public SortedMap<K, V> tailMap(K pFromKey) {
    throw new UnsupportedOperationException();
  }

  /**
   * Entry set implementation.
   * All methods are implemented by this class, none are delegated to other collections.
   * @param <K> The type of keys.
   * @param <V> The type of values.
   */
  private static class EntrySet<K extends Comparable<? super K>, V> extends AbstractSet<Map.Entry<K, V>> implements SortedSet<Map.Entry<K, V>> {

    private final Node<K, V> root;

    // Cache size and hashCode
    private transient int size = -1;
    private transient int hashCode = 0;

    private EntrySet(Node<K, V> pRoot) {
      root = pRoot;
    }

    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
      return new EntryInOrderIterator<>(root);
    }

    @Override
    public boolean contains(Object pO) {
      if (!(pO instanceof Map.Entry<?, ?>)) {
        return false;
      }
      // pO is not null here
      Map.Entry<?, ?> other = (Map.Entry<?, ?>)pO;
      Map.Entry<?, ?> thisNode = findNode(other.getKey(), root);

      return (thisNode != null) && Objects.equal(thisNode.getValue(), other.getValue());
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

      Node<K, V> current = root;
      while (current.left != null) {
        current = current.left;
      }
      return current;
    }

    @Override
    public Entry<K, V> last() {
      if (isEmpty()) {
        throw new NoSuchElementException();
      }

      Node<K, V> current = root;
      while (current.right != null) {
        current = current.right;
      }
      return current;
    }

    @Override
    public boolean equals(Object pO) {
      if (pO instanceof EntrySet<?, ?>) {
        EntrySet<?, ?> other = (EntrySet<?, ?>)pO;

        // this is faster than the fallback check because it's O(n)
        return Iterables.elementsEqual(this, other);
      }
      return super.equals(pO); // delegate to AbstractSet
    }

    @Override
    public int hashCode() {
      // cache hashCode
      if (hashCode == 0 && !isEmpty()) {
        hashCode = super.hashCode(); // delegate to AbstractSet
      }
      return hashCode;
    }

    @Override
    public Comparator<? super Entry<K, V>> comparator() {
      return Ordering.natural().onResultOf(Node.<K>getKeyFunction());
    }

    @Override
    public SortedSet<Entry<K, V>> subSet(Entry<K, V> pFromElement,
        Entry<K, V> pToElement) {
      throw new UnsupportedOperationException();
    }

    @Override
    public SortedSet<Entry<K, V>> headSet(Entry<K, V> pToElement) {
      throw new UnsupportedOperationException();
    }

    @Override
    public SortedSet<Entry<K, V>> tailSet(Entry<K, V> pFromElement) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Tree iterator with in-order iteration returning node objects.
   * @param <K> The type of keys.
   * @param <V> The type of values.
   */
  private static class EntryInOrderIterator<K, V> extends UnmodifiableIterator<Map.Entry<K, V>> {

    // invariants:
    // stack.top is always the next element to be returned
    // (i.e., its left subtree has already been handled)

    private final Deque<Node<K, V>> stack;

    private EntryInOrderIterator(Node<K, V> root) {
      stack = new ArrayDeque<>();
      if (root != null) {
        pushLeftMostNodesOnStack(root);
      }
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

    @Override
    public Map.Entry<K, V> next() {
      Node<K, V> current = stack.pop();
      // this is the element to be returned

      // if it has a right subtree,
      // push it on stack so that it will be handled next
      if (current.right != null) {
        pushLeftMostNodesOnStack(current.right);
      }

      return current;
    }
  }
}