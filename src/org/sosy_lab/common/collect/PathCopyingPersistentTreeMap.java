package org.sosy_lab.common.collect;

import static com.google.common.base.Preconditions.checkNotNull;

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

import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.Ordering;
import com.google.common.collect.UnmodifiableIterator;

/**
 * This is an implementation of {@link PersistentSortedMap} that is based on
 * red-black trees and path copying.
 *
 * It does not support <code>null</code> keys (but <code>null</code> values)
 * and always compares according to the natural ordering.
 *
 * The natural ordering of the keys needs to be consistent with equals.
 *
 * TODO Missing methods:
 * Currently not supported operations are
 * {{@link #headMap(Comparable)}, {@link #tailMap(Comparable)}, and {{@link #subMap(Comparable, Comparable)}}
 * as well as their counterparts in the collection views returned by methods of this class.
 *
 * TODO: The implementation of {@link #remove(Object)} currently does not shrink the tree,
 * so it may stay larger than necessary.
 *
 * @param <K> The type of keys.
 * @param <V> The type of values.
 */
public final class PathCopyingPersistentTreeMap<K extends Comparable<? super K>, V>
    extends AbstractImmutableMap<K, V>
    implements PersistentSortedMap<K, V>, Serializable {

  private static final long serialVersionUID = 1041711151457528188L;

  private static final class Node<K, V> extends SimpleImmutableEntry<K, V> {

    private static final long serialVersionUID = -7393505826652634501L;

    private final Node<K, V> left;
    private final Node<K, V> right;
    private final boolean isRed;
    private final boolean isDeleted;

    private Node(K pKey, V pValue, Node<K, V> pLeft, Node<K, V> pRight,
        boolean pRed, boolean pDeleted) {
      super(pKey, pValue);
      left = pLeft;
      right = pRight;
      isRed = pRed;
      isDeleted = pDeleted;
    }

    boolean isDeleted() {
      return isDeleted;
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

    @SuppressWarnings("unused")
    boolean isLeaf() {
      return left == null && right == null;
    }

    Node<K, V> withColor(boolean color) {
      if (isRed == color) {
        return this;
      } else {
        return new Node<>(getKey(), getValue(), left, right, color, isDeleted);
      }
    }

    static boolean deepEquals(@Nullable Node<?, ?> a, @Nullable Node<?, ?> b) {
      if (a == b) {
        return true;
      }
      return (a != null)
           && a.equals(b)
           && deepEquals(a.left, b.left)
           && deepEquals(a.right, b.right);
    }

    static int countNodes(@Nullable Node<?, ?> n) {
      if (n == null) {
        return 0;
      }
      return countNodes(n.left)
          + (n.isDeleted() ? 0 : 1)
          + countNodes(n.right);
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

  private static final boolean RED   = true;
  private static final boolean BLACK = false;


  // static creation methods

  private static final PersistentSortedMap<?, ?> emptyMap = new PathCopyingPersistentTreeMap<String, Object>(null);

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
        if (current.isDeleted) {
          return null;
        } else {
          return current;
        }
      }
    }
    return null;
  }

  private static <K, V> int checkColors(Node<K, V> current) {
    if (current == null) {
      return 0;
    }
    if (current.isRed()) {
      assert !Node.isBlack(current.left)  : "Red node with red left child";
      assert !Node.isBlack(current.right) : "Red node with red right child";
    }
    int leftBlackHeight  = checkColors(current.left);
    int rightBlackHeight = checkColors(current.right);
    assert leftBlackHeight == rightBlackHeight : "Black path length on left is " + leftBlackHeight + " and on right is " + rightBlackHeight;

    int blackHeight = leftBlackHeight;
    if (current.isBlack()) {
      blackHeight++;
    }
    return blackHeight;
  }


  // modifying methods

  @Override
  public PersistentSortedMap<K, V> putAndCopy(K key, V value) {
    checkNotNull(key);

    Node<K, V> newRoot = put0(key, value, root);
    if (newRoot == root) {
      return this;
    } else {
      newRoot = newRoot.withColor(BLACK);
      assert checkColors(newRoot) >= 0;
      return new PathCopyingPersistentTreeMap<>(newRoot);
    }
  }

  private static <K extends Comparable<? super K>, V> Node<K, V> put0(K key, V newValue, Node<K, V> current) {
    if (current == null) {
      // key was not in map
      // new nodes are red
      return new Node<>(key, newValue, null, null, RED, false);
    }

    int comp = key.compareTo(current.getKey());

    final Node<K, V> leftChild;
    final Node<K, V> rightChild;
    if (comp < 0) {
      // key < current.data

      leftChild = put0(key, newValue, current.left);
      if (leftChild == current.left) {
        return current;
      }
      rightChild = current.right;

    } else if (comp > 0) {
      // key > current.data

      rightChild = put0(key, newValue, current.right);
      if (rightChild == current.right) {
        return current;
      }
      leftChild = current.left;

    } else {
      // key == current.data

      if (newValue == current.getValue()) {
        return current;
      } else {
        return new Node<>(key, newValue, current.left, current.right, current.getColor(), false);
      }
    }

    // This is implemented according to
    // http://www.ece.uc.edu/~franco/C321/html/RedBlack/redblack.html
    // All numbers relate to the example there.
    if (current.isBlack() && Node.isRed(leftChild) && !Node.isBlack(rightChild)) {
      if (Node.isRed(leftChild.left) || Node.isRed(leftChild.right)) {
        // <1> double-red violation
        // Color children black and this one red
        Node<K, V> newLeftChild = leftChild.withColor(BLACK);
        Node<K, V> newRightChild = Node.isRed(rightChild) ? rightChild.withColor(BLACK) : rightChild;
        new Node<>(current.getKey(), current.getValue(), newLeftChild, newRightChild, RED, current.isDeleted());
      }
    }
    if (current.isBlack() && !Node.isBlack(leftChild) && Node.isRed(rightChild)) {
      if (Node.isRed(rightChild.left) || Node.isRed(rightChild.right)) {
        // <1> double-red violation inverted
        // Color children black and this one red
        Node<K, V> newLeftChild = Node.isRed(leftChild) ? leftChild.withColor(BLACK) : leftChild;
        Node<K, V> newRightChild = rightChild.withColor(BLACK);
        new Node<>(current.getKey(), current.getValue(), newLeftChild, newRightChild, RED, current.isDeleted());
      }
    }

    //  30                 27                      40
    if (current.isRed() && Node.isRed(leftChild) && Node.isBlack(rightChild)) {
      // <2>
      return rotateClockwise(current, leftChild, rightChild);
    }

    if (current.isRed() && Node.isBlack(leftChild) && Node.isRed(rightChild)) {
      // <2>
      return rotateCounterClockwise(current, leftChild, rightChild);
    }

    //  20                   10                        27
    if (current.isBlack() && Node.isBlack(leftChild) && Node.isRed(rightChild)) {
      if (Node.isRed(rightChild.left) || Node.isRed(rightChild.right)) {
        // <3.1>
        current = rotateCounterClockwise(current, leftChild, rightChild);
        // <3.2>
        // swap colors of current and its left child
        Node<K, V> newLeftChild = current.left.withColor(current.getColor());
        return new Node<>(current.getKey(), current.getValue(), newLeftChild, current.right, current.left.getColor(), current.isDeleted());
      }
    }

    if (current.isBlack() && Node.isRed(leftChild) && Node.isBlack(rightChild)) {
      if (Node.isRed(leftChild.left) || Node.isRed(leftChild.right)) {
        // <3.1> inverted
        current = rotateClockwise(current, leftChild, rightChild);
        // <3.2>
        // swap colors of current its right child
        Node<K, V> newRightChild = current.right.withColor(current.getColor());
        return new Node<>(current.getKey(), current.getValue(), current.left, newRightChild, current.right.getColor(), current.isDeleted());
      }
    }

    return new Node<>(current.getKey(), current.getValue(), leftChild, rightChild, current.getColor(), current.isDeleted());
  }

  private static <K, V> Node<K, V> rotateClockwise(final Node<K, V> pCurrent, final Node<K, V> pLeft, final Node<K, V> pRight) {
    final Node<K, V> crossOverNode = pLeft.right; // the node that is moved between subtrees
    final Node<K, V> newRight = new Node<>(pCurrent.getKey(), pCurrent.getValue(), crossOverNode, pRight, pCurrent.getColor(), pCurrent.isDeleted());
    return new Node<>(pLeft.getKey(), pLeft.getValue(), pLeft.left, newRight, pLeft.getColor(), pLeft.isDeleted());
  }

  private static <K, V> Node<K, V> rotateCounterClockwise(final Node<K, V> pCurrent, final Node<K, V> pLeft, final Node<K, V> pRight) {
    final Node<K, V> crossoverNode = pRight.left; // the node that is moved between subtrees
    final Node<K, V> newLeft = new Node<>(pCurrent.getKey(), pCurrent.getValue(), pLeft, crossoverNode, pCurrent.getColor(), pCurrent.isDeleted());
    return new Node<>(pRight.getKey(), pRight.getValue(), newLeft, pRight.right, pRight.getColor(), pRight.isDeleted());
  }


  @Override
  public PersistentSortedMap<K, V> removeAndCopy(K key) {
    checkNotNull(key);

    Node<K, V> newRoot = remove0(key, root);
    if (newRoot == root) {
      return this;
    } else if (newRoot == null) {
      return of();
    } else {
      assert checkColors(newRoot) >= 0;
      return new PathCopyingPersistentTreeMap<>(newRoot);
    }
  }

  private Node<K, V> remove0(K key, Node<K, V> current) {
    if (current == null) {
      // key was not in map
      return null;
    }

    int comp = key.compareTo(current.getKey());

    if (comp < 0) {
      // key < current.data

      final Node<K, V> newLeftChild = remove0(key, current.left);
      if (newLeftChild == current.left) {
        return current;
      } else {
        return new Node<>(current.getKey(), current.getValue(), newLeftChild, current.right, current.getColor(), current.isDeleted());
      }

    } else if (comp > 0) {
      // key > current.data

      final Node<K, V> newRightChild = remove0(key, current.right);
      if (newRightChild == current.right) {
        return current;
      } else {
        return new Node<>(current.getKey(), current.getValue(), current.left, newRightChild, current.getColor(), current.isDeleted());
      }

    } else {
      // key == current.data

      if (current.isRed() && current.isLeaf()) {
        // red leafs can be deleted easily
        return null;
      }

      return new Node<>(key, null, current.left, current.right, current.getColor(), true);
    }
  }

/*
  @Override
  public PersistentMap<K, V> removeAndCopy(K key) {
    checkNotNull(key);

    Node<K, V> newRoot = remove0(key, root);
    if (newRoot == root) {
      return this;
    } else if (newRoot == null) {
      return of();
    } else {
      assert checkColors(newRoot) >= 0;
      return new CopyOnWriteTreeMap<>(newRoot);
    }
  }

  private Node<K, V> remove0(K key, Node<K, V> current) {
    if (current == null) {
      // key was not in map
      return null;
    }

    int comp = key.compareTo(current.getKey());

    Node<K, V> leftChild;
    Node<K, V> rightChild;
    if (comp < 0) {
      // key < current.data

      leftChild = remove0(key, current.left);
      if (leftChild == current.left) {
        return current;
      }
      rightChild = current.right;

    } else if (comp > 0) {
      // key > current.data

      rightChild = remove0(key, current.right);
      if (rightChild == current.right) {
        return current;
      }
      leftChild = current.left;

    } else {
      // key == current.data

      if (current.isRed() && current.isLeaf()) {
        // [1] red leaf, delete it
        return null;
      }
      leftChild = current.left;
      rightChild = current.right;
    }



    if (Node.isBlack(rightChild) && rightChild.isLeaf()) {
      // [5]
      if (leftChild.isRed()) {
        // [5.1.1]
        Node<K, V> newLeftChild = leftChild.withColor(BLACK);
        Node<K, V> newCurrent = current.withColor(RED);
        // [5.1.2]
        newCurrent = rotateClockwise(newCurrent, newLeftChild, rightChild);
        current = newCurrent;
        leftChild = current.left;
        rightChild = current.right;
        assert Node.isBlack(rightChild) && rightChild.isLeaf();
        assert !leftChild.isRed;
      }

      if (leftChild.isBlack() && Node.isBlack(leftChild.left) && Node.isBlack(leftChild.right)) {
        // [5.2.1]
        leftChild = leftChild.withColor(RED);

      }
    }



    if (current.isRed()) {
      if (current.left != null && current.right != null) {
        // red with 2 children
        // TODO
      }

      // [2] red with 1 child -> impossible
      throw new AssertionError();

    } else {
      if (Node.isRed(current.left) && current.right == null) {
        // [4] black with one red child
        assert current.left.left == null && current.left.right == null;
        return current.left.withColor(BLACK);
      }
      if (Node.isRed(current.right) && current.left == null) {
        // [4] black with one red child
        assert current.right.left == null && current.right.right == null;
        return current.right.withColor(BLACK);
      }

      if (current.left == null && current.right == null) {

      }

    }
  }
*/

  // read operations

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
    return root == null || size() == 0;
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
      return root == null || size() == 0;
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
        return Node.deepEquals(this.root, other.root);
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

    private Deque<Node<K, V>> stack;

    private EntryInOrderIterator(Node<K, V> root) {
      stack = new ArrayDeque<>();
      if (root != null) {
        pushLeftMostNodesOnStack(root);
      }
      jumpOverDeletedNodes();
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

    private void jumpOverDeletedNodes() {
      while (hasNext()) {
        Node<K, V> current = stack.peek();
        if (!current.isDeleted()) {
          return;
        }

        forward(); // forward iterator so that the deleted node is not seen
      }
    }

    private Map.Entry<K, V> forward() {
      Node<K, V> current = stack.pop();
      // this is the element to be returned

      // if it has a right subtree,
      // push it on stack so that it will be handled next
      if (current.right != null) {
        pushLeftMostNodesOnStack(current.right);
      }

      return current;
    }

    @Override
    public Map.Entry<K, V> next() {
      Map.Entry<K, V> result = forward();
      jumpOverDeletedNodes();
      return result;
    }
  }
}