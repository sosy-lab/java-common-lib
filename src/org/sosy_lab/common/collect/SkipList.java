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

import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.Var;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nullable;

/**
 * A skip list implementation. Similar to ordered trees, but in list form. Achieves time complexity
 * similar to ordered trees using a probabilistic approach.
 *
 * <p>Value <code>null</code> is not a valid element of this list. Adding it results in a {@link
 * NullPointerException}.
 *
 * <p>Time Complexity:
 *
 * <ul>
 *   <li>Insertion O(log n)
 *   <li>Removal O(log n)
 *   <li>Contains O(log n)
 *   <li>Random-access O(n)
 *   <li>Iteration O(n)
 * </ul>
 *
 * <p>Based on William Pugh: Skip Lists. A Probabilistic Alternative to Balanced Trees, 1990.
 *
 * <p>By design, a skip list is a single linked list. To be able to implement a random-access add
 * operation in a simple manner, we extended the skip list to be double linked.
 *
 * @param <T> type of the elements of this list
 * @see java.util.concurrent.ConcurrentSkipListMap
 */
public class SkipList<T> implements OrderStatisticSet<T>, Serializable {

  private static final long serialVersionUID = 1001345121457565238L;

  private static final int LEVEL_ONE = 0;

  private static class Node<T> implements Serializable {

    private static final long serialVersionUID = 10045121457565238L;

    // These lists are transient because they are manually serialized to prevent stack overflows due
    // to recursive structure
    private transient List<Node<T>> next;
    private transient List<Node<T>> prev;
    private @Nullable T value;

    private List<Integer> inBetweenCount;

    private Node(@Nullable T pValue, int pLevel) {
      value = pValue;
      next = new ArrayList<>(Collections.nCopies(pLevel + 1, null));
      prev = new ArrayList<>(Collections.nCopies(pLevel + 1, null));
      inBetweenCount = new ArrayList<>(Collections.nCopies(pLevel + 1, 0));
    }

    private @Nullable Node<T> getNext(int pLevel) {
      Preconditions.checkElementIndex(pLevel, LEVEL_MAX + 1);
      if (pLevel >= next.size()) {
        return null;
      } else {
        return next.get(pLevel);
      }
    }

    private @Nullable Node<T> getPrevious(int pLevel) {
      Preconditions.checkElementIndex(pLevel, LEVEL_MAX + 1);
      if (pLevel >= prev.size()) {
        return null;
      } else {
        return prev.get(pLevel);
      }
    }

    private @Nullable T getValue() {
      return value;
    }

    private void setNext(@Nullable Node<T> pNext, int pLevel) {
      next.set(pLevel, pNext);
    }

    private void setPrevious(@Nullable Node<T> pPrev, int pLevel) {
      prev.set(pLevel, pPrev);
    }

    private void setInBetweenCount(int pNewValue, int pLevel) {
      if (pLevel > getMaxLvl()) {
        // DO NOTHING
      } else {
        inBetweenCount.set(pLevel, pNewValue);
      }
    }

    void increaseInBetweenCount(int pLevel) {
      inBetweenCount.set(pLevel, inBetweenCount.get(pLevel) + 1);
    }

    int getInBetweenCount(int pLevel) {
      if (pLevel > getMaxLvl()) {
        return 0;
      } else {
        return inBetweenCount.get(pLevel);
      }
    }

    int getMaxLvl() {
      assert next.size() == prev.size();
      assert next.size() == inBetweenCount.size();
      return next.size() - 1;
    }

    private void writeObject(ObjectOutputStream pOut) throws IOException {
      pOut.defaultWriteObject();

      pOut.writeInt(next.size());
    }

    private void readObject(ObjectInputStream pIn) throws IOException, ClassNotFoundException {
      pIn.defaultReadObject();

      int size = pIn.read();
      next = new ArrayList<>(size);
      prev = new ArrayList<>(size);
    }
  }

  private static final int LEVEL_MAX = 31;
  private static final double ONE_HALF_LOG = Math.log(0.5);
  private Random randomGenerator = new Random();

  private final @Nullable Comparator<? super T> comparator;
  private Node<T> head = createHead();
  private Node<T> tail = head;
  private int size = 0;

  @SuppressWarnings("unchecked")
  public SkipList(@Nullable Comparator<? super T> pComparator) {
    comparator = pComparator;
  }

  public SkipList(Collection<? extends T> pCollection) {
    this();
    boolean changed = addAll(pCollection);
    assert changed || pCollection.isEmpty();
  }

  public SkipList(SortedSet<T> pSortedSet) {
    this(pSortedSet.comparator());
    boolean changed = addAll(pSortedSet);
    assert changed || pSortedSet.isEmpty();
  }

  @SuppressWarnings("unchecked")
  public SkipList() {
    comparator = null;
  }

  /** Use the given {@link Random} object for future probabilistic computations. */
  public void reinitialize(Random pRandom) {
    Preconditions.checkNotNull(pRandom);
    randomGenerator = pRandom;
  }

  private Node<T> createHead() {
    return new Node<>(null, LEVEL_MAX);
  }

  /**
   * Return a random level between 0 and the max level. The probability distribution is logarithmic
   * (i.e., higher values are less likely).
   */
  private int getRandomLevel() {
    double r = randomGenerator.nextDouble();
    if (r == 0) {
      return LEVEL_MAX;
    } else {
      // change logarithmic base to 0.5
      // to get log_{0.5}(r)
      return ((int) Math.round(Math.log(r) / ONE_HALF_LOG));
    }
  }

  @SuppressWarnings("unchecked")
  private int compare(T pF, T pS) {
    if (comparator == null) {
      return ((Comparable<T>) pF).compareTo(pS);
    } else {
      return comparator.compare(pF, pS);
    }
  }

  @Override
  public boolean add(T pT) {
    Preconditions.checkNotNull(pT);

    if (contains(pT)) {
      return false;
    }

    int level = getRandomLevel();
    Node<T> newNode = new Node<>(pT, level);

    @Var Node<T> currNode = head;
    @Var Node<T> closestGreater;
    for (int currLvl = LEVEL_MAX; currLvl >= LEVEL_ONE; currLvl--) {
      // Get closest node that is less or equal to the new value, and track the number of steps
      // necessary to get there on the current lvl.
      @Var int inBetweenCount = 0;
      @Var Node<T> next = currNode.getNext(currLvl);
      while (next != null && compare(pT, next.getValue()) >= 0) {
        inBetweenCount += next.getInBetweenCount(currLvl);
        currNode = next;
        next = currNode.getNext(currLvl);
      }

      // Intermediate storage, this is not the final value!
      newNode.setInBetweenCount(inBetweenCount, currLvl);

      closestGreater = currNode.getNext(currLvl);

      if (currLvl <= level) {
        if (closestGreater != null) {
          newNode.setNext(closestGreater, currLvl);
          closestGreater.setPrevious(newNode, currLvl);
        }

        newNode.setPrevious(currNode, currLvl);
        currNode.setNext(newNode, currLvl);
      }

      if (closestGreater != null) {
        closestGreater.increaseInBetweenCount(currLvl);
      }
    }

    assert newNode.getPrevious(LEVEL_ONE) == currNode;
    // Adjust tail node
    if (currNode == tail) {
      tail = newNode;
    }

    // Set final interval values
    @Var int inBetweenCount = 1;
    @Var int currCount;
    for (int currLvl = LEVEL_ONE; currLvl <= level; currLvl++) {
      currCount = newNode.getInBetweenCount(currLvl);
      newNode.setInBetweenCount(inBetweenCount, currLvl);

      closestGreater = newNode.getNext(currLvl);
      if (closestGreater != null) {
        closestGreater.setInBetweenCount(
            closestGreater.getInBetweenCount(currLvl) - inBetweenCount, currLvl);
      }

      inBetweenCount += currCount;
    }

    size++;
    return true;
  }

  private Node<T> getClosestLessEqual(Node<T> pStart, T pVal, int pLvl) {
    @Var Node<T> currNode = pStart;
    @Var Node<T> next = currNode.getNext(pLvl);
    while (next != null && compare(pVal, next.getValue()) >= 0) {
      currNode = next;
      next = currNode.getNext(pLvl);
    }
    return currNode;
  }

  private Node<T> getClosestLessEqual(Node<T> pStart, T pVal) {
    @Var Node<T> currNode = pStart;
    for (int currLvl = LEVEL_MAX; currLvl >= LEVEL_ONE; currLvl--) {
      currNode = getClosestLessEqual(currNode, pVal, currLvl);
    }

    return currNode;
  }

  private void removeNode(Node<T> pNode) {
    if (pNode == tail) {
      tail = pNode.getPrevious(LEVEL_ONE);
      assert tail != null;
    }
    for (int currLvl = pNode.getMaxLvl(); currLvl >= LEVEL_ONE; currLvl--) {
      @Var Node<T> previous = pNode.getPrevious(currLvl);
      assert previous != null;
      @Var Node<T> next = pNode.getNext(currLvl);
      previous.setNext(next, currLvl);
      if (next != null) {
        next.setPrevious(previous, currLvl);
        int inBetweenCount = next.getInBetweenCount(currLvl) + pNode.getInBetweenCount(currLvl);
        next.setInBetweenCount(inBetweenCount, currLvl);
      }
    }
    size--;
  }

  @Override
  public boolean remove(Object pO) {
    Preconditions.checkNotNull(pO);
    if (!contains(pO)) {
      return false;
    }

    @Var Node<T> currNode = head;
    @SuppressWarnings("unchecked")
    T val = (T) pO;
    for (int currLvl = LEVEL_MAX; currLvl >= LEVEL_ONE; currLvl--) {
      @Var Node<T> next = getClosestLessEqual(currNode, val, currLvl);
      currNode = next;
      next = currNode.getNext(currLvl);
      if (next != null) {
        // One predecessor will be removed
        next.setInBetweenCount(next.getInBetweenCount(currLvl) - 1, currLvl);
      }
      // do not store this next node, we stay at the closest less/equal node to find the equal
      // node
    }

    if (currNode != head && compare(currNode.getValue(), val) == 0) {
      removeNode(currNode);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean contains(Object pO) {
    Preconditions.checkNotNull(pO);

    @Var Node<T> currNode = head;
    T val = (T) pO;
    currNode = getClosestLessEqual(currNode, val);

    if (currNode == head) {
      return false;
    } else {
      return compare(currNode.getValue(), val) == 0;
    }
  }

  @Override
  public Object[] toArray() {
    Object[] arr = new Object[size];
    @Var int i = 0;
    for (T v : this) {
      arr[i] = v;
      i++;
    }
    return arr;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T1> T1[] toArray(T1[] a) {
    Preconditions.checkNotNull(a);
    List<T1> newList = new ArrayList<>(size());

    for (T v : this) {
      newList.add((T1) v);
    }

    return newList.toArray(a);
  }

  @Override
  public boolean containsAll(Collection<?> pC) {
    Preconditions.checkNotNull(pC);
    for (Object o : pC) {
      if (!contains(o)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean addAll(Collection<? extends T> pC) {
    Preconditions.checkNotNull(pC);
    @Var boolean changed = false;
    for (T o : pC) {
      changed = add(o) || changed;
    }
    return changed;
  }

  @Override
  public boolean removeAll(Collection<?> pC) {
    Preconditions.checkNotNull(pC);
    @Var boolean changed = false;
    for (Object o : pC) {
      changed = remove(o) || changed;
    }
    return changed;
  }

  @Override
  public boolean retainAll(Collection<?> pC) {
    Preconditions.checkNotNull(pC);
    @Var boolean changed = false;
    @Var Node<T> currNode = head;
    while (currNode != null) {
      @Var Node<T> next = currNode.getNext(LEVEL_ONE);
      if (next != null && !pC.contains(next.getValue())) {
        removeNode(next);
        changed = true;
      } else {
        currNode = next;
      }
    }

    return changed;
  }

  @Override
  public void clear() {
    head = createHead();
    tail = head;
    size = 0;
  }

  private Node<T> getNode(int pIndex) {
    Preconditions.checkElementIndex(pIndex, size);

    @Var int currPos = -1;
    @Var int currLvl = LEVEL_MAX;
    @Var Node<T> currNode = head;
    while (currPos != pIndex) {
      assert currLvl >= LEVEL_ONE;
      @Var Node<T> next = currNode.getNext(currLvl);

      if (next == null) {
        currLvl--;

      } else {
        int nextPos = currPos + next.getInBetweenCount(currLvl);

        if (nextPos <= pIndex) {
          currNode = next;
          currPos = nextPos;
        } else {
          currLvl--;
        }
      }
    }
    return currNode;
  }

  @Override
  public T getByRank(int pIndex) {
    Preconditions.checkElementIndex(pIndex, size);

    return getNode(pIndex).getValue();
  }

  @Override
  public @Nullable T removeByRank(int pIndex) {
    Preconditions.checkElementIndex(pIndex, size);
    @Var int currPos = -1;
    @Var int currLvl = LEVEL_MAX;
    @Var Node<T> currNode = head;
    while (currLvl >= LEVEL_ONE) {
      @Var Node<T> next = currNode.getNext(currLvl);

      if (next == null) {
        currLvl--;

      } else {
        int nextPos = currPos + next.getInBetweenCount(currLvl);

        if (nextPos <= pIndex) {
          // Run to highest less-or-equal node on this level
          currNode = next;
          currPos = nextPos;
        } else {
          // Reduce first bigger node on level by one
          // and descend to next level
          next.setInBetweenCount(next.getInBetweenCount(currLvl) - 1, currLvl);
          currLvl--;
        }
      }
    }

    removeNode(currNode);
    return currNode.getValue();
  }

  @Override
  public int rankOf(Object pO) {
    Preconditions.checkNotNull(pO);

    @SuppressWarnings("unchecked")
    T val = (T) pO;
    @Var Node<T> currNode = head;
    @Var Node<T> next;
    @Var int index = -1;
    for (int currLvl = LEVEL_MAX; currLvl >= LEVEL_ONE; currLvl--) {
      next = currNode.getNext(currLvl);
      while (next != null && compare(val, next.getValue()) >= 0) {
        currNode = next;

        int comp = compare(val, currNode.getValue());
        if (comp >= 0) {
          index += next.getInBetweenCount(currLvl);
        }

        if (comp == 0) {
          break;
        } else {
          next = currNode.getNext(currLvl);
        }
      }
    }

    if (currNode != head && compare(val, currNode.getValue()) == 0) {
      return index;
    } else {
      return -1;
    }
  }

  @Override
  public T first() {
    if (size <= 0) {
      throw new NoSuchElementException();
    }
    return getByRank(0);
  }

  @Override
  public T last() {
    if (size <= 0) {
      throw new NoSuchElementException();
    }
    return tail.getValue();
  }

  @Override
  public SortedSet<T> subSet(T pFromElement, T pToElement) {
    return subSet(pFromElement, true, pToElement, false);
  }

  @Override
  public SortedSet<T> headSet(T pToElement) {
    Preconditions.checkNotNull(pToElement);
    return subSet(first(), pToElement);
  }

  @Override
  public SortedSet<T> tailSet(T pFromElement) {
    Preconditions.checkNotNull(pFromElement);
    return subSet(pFromElement, last());
  }

  @Override
  public Comparator<? super T> comparator() {
    return comparator;
  }

  private @Nullable Node<T> floorNode(T pT) {
    @Var Node<T> candidate = getClosestLessEqual(head, pT);
    if (candidate == head) {
      return null;
    } else {
      return candidate;
    }
  }

  private @Nullable Node<T> ceilingNode(T pT) {
    @Var Node<T> candidate = getClosestLessEqual(head, pT);

    if (candidate == head || compare(candidate.getValue(), pT) != 0) {
      candidate = candidate.getNext(LEVEL_ONE);
    }

    if (candidate != null && candidate != head) {
      return candidate;
    } else {
      return null;
    }
  }

  @Override
  public T lower(T pT) {
    Preconditions.checkNotNull(pT);
    @Var Node<T> candidate = getClosestLessEqual(head, pT);

    if (candidate != head && compare(candidate.getValue(), pT) == 0) {
      candidate = candidate.getPrevious(LEVEL_ONE);
    }

    if (candidate != head) {
      return candidate.getValue();
    } else {
      return null;
    }
  }

  @Override
  public T floor(T pT) {
    Preconditions.checkNotNull(pT);
    Node<T> n = floorNode(pT);
    if (n == null) {
      return null;
    } else {
      return n.getValue();
    }
  }

  @Override
  public T ceiling(T pT) {
    Preconditions.checkNotNull(pT);
    Node<T> n = ceilingNode(pT);
    if (n == null) {
      return null;
    } else {
      return n.getValue();
    }
  }

  @Override
  public T higher(T pT) {
    Preconditions.checkNotNull(pT);
    @Var Node<T> candidate = getClosestLessEqual(head, pT);
    candidate = candidate.getNext(LEVEL_ONE);

    if (candidate != null && candidate != head) {
      return candidate.getValue();
    } else {
      return null;
    }
  }

  @Override
  public T pollFirst() {
    if (size > 0) {
      return removeByRank(0);
    } else {
      return null;
    }
  }

  @Override
  public T pollLast() {
    if (size > 0) {
      return removeByRank(size - 1);
    } else {
      return null;
    }
  }

  @Override
  public Iterator<T> iterator() {
    return new SkipListIterator<>(this);
  }

  @Override
  public NavigableSet<T> descendingSet() {
    return new DescendingList<>(this);
  }

  @Override
  public Iterator<T> descendingIterator() {
    return new SkipListDescendingIterator<>(this);
  }

  @Override
  public NavigableSet<T> subSet(
      T pFromElement, boolean pFromInclusive, T pToElement, boolean pToInclusive) {
    Preconditions.checkNotNull(pFromElement);
    Preconditions.checkNotNull(pToElement);
    return new SubList<>(this, pFromElement, pFromInclusive, pToElement, pToInclusive);
  }

  @Override
  public NavigableSet<T> headSet(T pToElement, boolean pInclusive) {
    Preconditions.checkNotNull(pToElement);
    return subSet(first(), true, pToElement, pInclusive);
  }

  @Override
  public NavigableSet<T> tailSet(T pFromElement, boolean pInclusive) {
    Preconditions.checkNotNull(pFromElement);
    return subSet(pFromElement, pInclusive, last(), true);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("[");
    @Var boolean isFirst = true;
    for (T val : this) {
      if (isFirst) {
        isFirst = false;
      } else {
        sb.append(", ");
      }
      sb.append(val);
    }
    sb.append("]");
    return sb.toString();
  }

  @Override
  public boolean equals(Object pObj) {
    if (pObj == null) {
      return false;
    }

    if (!(pObj instanceof Set)) {
      return false;

    } else {
      @SuppressWarnings("unchecked")
      Set<T> other = (Set<T>) pObj;
      return other.containsAll(this) && containsAll(other);
    }
  }

  @Override
  public int hashCode() {
    @Var int hashCode = 0;
    for (T val : this) {
      hashCode += val.hashCode();
    }

    return hashCode;
  }

  private void writeObject(ObjectOutputStream pOut) throws IOException {
    pOut.defaultWriteObject(); // write all fields that are not transient

    @Var Node<T> currNode = head;
    pOut.writeObject(head);
    while (currNode != null) {
      pOut.writeObject(currNode.next);
      pOut.writeObject(currNode.prev);

      currNode = currNode.getNext(LEVEL_ONE);
    }
  }

  @SuppressWarnings("unchecked")
  private void readObject(ObjectInputStream pIn) throws IOException, ClassNotFoundException {
    pIn.defaultReadObject(); // read all fields that are not transient;

    head = (Node<T>) pIn.readObject();
    @Var Node<T> currNode = head;
    while (currNode != null) {
      currNode.next = (List<Node<T>>) pIn.readObject();
      currNode.prev = (List<Node<T>>) pIn.readObject();
      currNode = currNode.getNext(LEVEL_ONE);
    }
  }

  private static class SkipListIterator<T> implements Iterator<T> {

    private SkipList<T> list;
    Node<T> currentNode;
    private boolean removed = false;

    private SkipListIterator(SkipList<T> pList) {
      list = pList;
      currentNode = list.head;
    }

    @Override
    public boolean hasNext() {
      return currentNode.getNext(LEVEL_ONE) != null;
    }

    @Override
    public T next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      currentNode = currentNode.getNext(LEVEL_ONE);
      assert currentNode != null;
      removed = false;
      return Preconditions.checkNotNull(currentNode.getValue());
    }

    @Override
    public void remove() {
      if (currentNode == list.head || removed) {
        throw new IllegalStateException();
      } else {
        list.removeNode(currentNode);
        currentNode = currentNode.getPrevious(LEVEL_ONE);
        removed = true;
      }
    }
  }

  private static class SkipListDescendingIterator<T> implements Iterator<T> {

    @Nullable Node<T> currentNode = null;
    private boolean removed = false;

    private SkipList<T> list;

    private SkipListDescendingIterator(SkipList<T> pList) {
      list = pList;
    }

    @Override
    public boolean hasNext() {
      assert currentNode != list.head;
      return (currentNode == null && list.head != list.tail)
          || (currentNode != null && currentNode.getPrevious(LEVEL_ONE) != list.head);
    }

    @Override
    public @Nullable T next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      if (currentNode == null) {
        currentNode = list.tail;
      } else {
        currentNode = currentNode.getPrevious(LEVEL_ONE);
      }
      assert currentNode != null;
      removed = false;
      return currentNode.getValue();
    }

    @Override
    public void remove() {
      assert currentNode != list.head;
      if (currentNode == null || removed) {
        throw new IllegalStateException();
      } else {
        Node<T> previous;
        if (currentNode == list.tail) {
          previous = null;
        } else {
          previous = currentNode.getNext(LEVEL_ONE);
        }
        list.removeNode(currentNode);
        removed = true;
        currentNode = previous;
      }
    }
  }


  private static class SubList<T> implements NavigableSet<T>, Serializable {

    private static final long serialVersionUID = -5138563350624662800L;

    private final T bottom;
    private final boolean bottomInclusive;
    private final T top;
    private final boolean topInclusive;

    private final SkipList<T> delegate;

    SubList(
        SkipList<T> pDelegate,
        T pBottomElement,
        boolean pBottomInclusive,
        T pTopElement,
        boolean pTopInclusive) {

      bottom = pBottomElement;
      bottomInclusive = pBottomInclusive;
      top = pTopElement;
      topInclusive = pTopInclusive;

      delegate = pDelegate;
    }

    private boolean tooLow(T pVal) {
      int comp = delegate.compare(pVal, bottom);

      return comp < 0 || (comp == 0 && !bottomInclusive);
    }

    private boolean tooHigh(T pVal) {
      int comp = delegate.compare(pVal, top);

      return comp > 0 || (comp == 0 && !topInclusive);
    }


    private boolean outOfBounds(T pVal) {
      return tooLow(pVal) || tooHigh(pVal);
    }

    @Override
    public T lower(T pT) {
      T l = delegate.lower(pT);
      if (l == null || outOfBounds(l)) {
        return null;
      } else {
        return l;
      }
    }

    @Override
    public T floor(T pT) {
      T f = delegate.floor(pT);
      if (f == null || outOfBounds(f)) {
        return null;
      } else {
        return f;
      }
    }

    @Override
    public T ceiling(T pT) {
      T c = delegate.ceiling(pT);
      if (c == null || outOfBounds(c)) {
        return null;
      } else {
        return c;
      }
    }

    @Override
    public T higher(T pT) {
      T h = delegate.higher(pT);
      if (h == null || outOfBounds(h)) {
        return null;
      } else {
        return h;
      }
    }

    @Override
    public T pollFirst() {
      return ceiling(bottom);
    }

    @Override
    public T pollLast() {
      return floor(top);
    }

    @Override
    public int size() {
      T ceiling = ceiling(bottom);
      if (ceiling == null) {
        return 0;
      }

      int start = delegate.rankOf(ceiling);
      int end = delegate.rankOf(floor(top));

      return end - start + 1;
    }

    @Override
    public boolean isEmpty() {
      return pollFirst() != null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object pO) {
      return delegate.contains(pO) && !outOfBounds((T) pO);
    }

    @Override
    public Iterator<T> iterator() {
      return new SubListIterator<>(this);
    }

    @Override
    public Object[] toArray() {
      Object[] arr = new Object[size()];
      int pos = 0;
      for (T v : this) {
        arr[pos] = v;
      }
      return arr;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T1> T1[] toArray(T1[] pA) {
      Preconditions.checkNotNull(pA);
      List<T1> newList = new ArrayList<>(size());

      for (T v : this) {
        newList.add((T1) v);
      }

      return newList.toArray(pA);
    }

    @Override
    public boolean add(T pT) {
      Preconditions.checkNotNull(pT);
      if (outOfBounds(pT)) {
        throw new IllegalArgumentException("Value out of sub list range: " + pT);
      } else {
        return delegate.add(pT);
      }
    }

    @Override
    public boolean remove(Object pO) {
      Preconditions.checkNotNull(pO);
      if (!contains(pO)) {
        return false;
      } else {
        return delegate.remove(pO);
      }

    }

    @Override
    public boolean containsAll(Collection<?> pCollection) {
      for (Object o : pCollection) {
        if (!contains(o)) {
          return false;
        }
      }
      return true;
    }

    @Override
    public boolean addAll(Collection<? extends T> pCollection) {
      @Var boolean existed = false;
      for (T v : pCollection) {
        existed = add(v) || existed;
      }
      return existed;
    }

    @Override
    public boolean retainAll(Collection<?> pCollection) {
      @Var boolean changed = false;
      Iterator<T> it = iterator();
      while (it.hasNext()) {
        T v = it.next();
        if (!pCollection.contains(v)) {
          it.remove();
          changed = true;
        }
      }

      return changed;
    }

    @Override
    public boolean removeAll(Collection<?> pCollection) {
      @Var boolean changed = false;
      for (Object o : pCollection) {
        changed = remove(o) || changed;
      }

      return changed;
    }

    @Override
    public void clear() {
      Iterator<T> it = iterator();
      while (it.hasNext()) {
        it.next();
        it.remove();
      }
    }

    @Override
    public NavigableSet<T> descendingSet() {
      return new DescendingList<>(this);
    }

    @Override
    public Iterator<T> descendingIterator() {
      return new SubListDescendingIterator<>(this);
    }

    @Override
    public NavigableSet<T> subSet(
        T pBottom, boolean pBottomInclusive, T pTop, boolean pTopInclusive) {

      if (tooHigh(pBottom)) {
        // bottom of new sublist is larger than top of old sublist -> empty
        return Collections.emptyNavigableSet();
      }

      if (tooLow(pTop)) {
        // top of new sublist is smaller than bottom of old sublist -> empty
        return Collections.emptyNavigableSet();
      }

      T newBottom;
      boolean newBottomInclusive;
      @Var int comp = delegate.compare(pBottom, bottom);
      if (comp > 0) {
        newBottom = pBottom;
        newBottomInclusive = pBottomInclusive;
      } else {
        newBottom = bottom;
        if (comp == 0) {
          // if old and new bottom value are the same, we have to respect the inclusive-flag
          newBottomInclusive = bottomInclusive && pBottomInclusive;
        } else {
          // if the old bottom value is larger then the new one, use the inclusive-flag of the
          // old one
          newBottomInclusive = bottomInclusive;
        }
      }

      T newTop;
      boolean newTopInclusive;
      comp = delegate.compare(pTop, top);
      if (comp < 0) {
        newTop = pTop;
        newTopInclusive = pTopInclusive;
      } else {
        newTop = top;
        if (comp == 0) {
          // if old and new bottom value are the same, we have to respect the inclusive-flag
          newTopInclusive = topInclusive && pTopInclusive;
        } else {
          // if the old bottom value is larger then the new one, use the inclusive-flag of the
          // old one
          newTopInclusive = topInclusive;
        }
      }

      return delegate.subSet(newBottom, newBottomInclusive, newTop, newTopInclusive);
    }

    @Override
    public NavigableSet<T> headSet(T pT, boolean pTopInclusive) {
      return subSet(bottom, bottomInclusive, pT, pTopInclusive);
    }

    @Override
    public NavigableSet<T> tailSet(T pT, boolean pBottomInclusive) {
      return subSet(pT, pBottomInclusive, top, topInclusive);
    }

    @Override
    public Comparator<? super T> comparator() {
      return delegate.comparator;
    }

    @Override
    public SortedSet<T> subSet(T pBottom, T pTop) {
      return subSet(pBottom, true, pTop, false);
    }

    @Override
    public SortedSet<T> headSet(T pT) {
      return subSet(bottom, pT);
    }

    @Override
    public SortedSet<T> tailSet(T pT) {
      return subSet(pT, top);
    }

    @Override
    public T first() {
      return pollFirst();
    }

    @Override
    public T last() {
      return pollLast();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object pO) {
      if (pO instanceof Set) {
        Set<T> s = ((Set<T>) pO);
        return s.containsAll(this) && containsAll(s);

      } else {
        return true;
      }
    }

    @Override
    public int hashCode() {
      @Var int hashCode = 0;
      for (T v : this) {
        hashCode += Objects.hashCode(v);
      }
      return hashCode;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder("[");
      @Var boolean isFirst = true;
      for (T val : this) {
        if (isFirst) {
          isFirst = false;
        } else {
          sb.append(", ");
        }
        sb.append(val);
      }
      sb.append("]");
      return sb.toString();
    }

    private static class SubListIterator<T> extends SkipListIterator<T> {

      private SubList<T> subList;
      private SkipList<T> list;

      private SubListIterator(SubList<T> pList) {
        super(pList.delegate);
        list = pList.delegate;
        subList = pList;
        currentNode = getStartNode();
      }

      private Node<T> getStartNode() {
        @Var Node<T> candidate = list.floorNode(subList.bottom);

        if (candidate == null) {
          candidate = list.head;
        }

        return candidate;
      }

      @Override
      public boolean hasNext() {
        return super.hasNext() && !subList.outOfBounds(currentNode.getNext(LEVEL_ONE).getValue());
      }
    }

    private static class SubListDescendingIterator<T> extends SkipListDescendingIterator<T> {

      private SubList<T> subList;
      private SkipList<T> skipList;

      private SubListDescendingIterator(SubList<T> pList) {
        super(pList.delegate);
        subList = pList;
        skipList = subList.delegate;
        currentNode = getStartNode();
      }

      private @Nullable Node<T> getStartNode() {
        return skipList.ceilingNode(subList.top);
      }

      @Override
      public boolean hasNext() {
        return super.hasNext()
            && !subList.outOfBounds(currentNode.getPrevious(LEVEL_ONE).getValue());
      }
    }
  }

  private static class DescendingList<T> implements NavigableSet<T>, Serializable {

    private static final long serialVersionUID = 5419305617773341059L;

    private final NavigableSet<T> delegate;

    private DescendingList(NavigableSet<T> pDelegate) {
      delegate = pDelegate;
    }

    @Override
    public T first() {
      return delegate.last();
    }

    @Override
    public T last() {
      return delegate.first();
    }

    @Override
    public T lower(T pT) {
      return delegate.higher(pT);
    }

    @Override
    public T floor(T pT) {
      return delegate.ceiling(pT);
    }

    @Override
    public T ceiling(T pT) {
      return delegate.floor(pT);
    }

    @Override
    public T higher(T pT) {
      return delegate.lower(pT);
    }

    @Override
    public T pollFirst() {
      return delegate.pollLast();
    }

    @Override
    public T pollLast() {
      return delegate.pollFirst();
    }

    @Override
    public int size() {
      return delegate.size();
    }

    @Override
    public boolean isEmpty() {
      return delegate.isEmpty();
    }

    @Override
    public boolean contains(Object pO) {
      return delegate.contains(pO);
    }

    @Override
    public Iterator<T> iterator() {
      return delegate.descendingIterator();
    }

    @Override
    public Object[] toArray() {
      Object[] arr = new Object[size()];
      @Var int pos = 0;
      for (T v : this) {
        arr[pos] = v;
        pos++;
      }
      return arr;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T1> T1[] toArray(T1[] pA) {
      Preconditions.checkNotNull(pA);
      List<T1> newList = new ArrayList<>(size());

      for (T v : this) {
        newList.add((T1) v);
      }

      return newList.toArray(pA);
    }

    @Override
    public boolean add(T pT) {
      return delegate.add(pT);
    }

    @Override
    public boolean remove(Object pO) {
      return delegate.remove(pO);
    }

    @Override
    public boolean containsAll(Collection<?> pCollection) {
      return delegate.containsAll(pCollection);
    }

    @Override
    public boolean addAll(Collection<? extends T> pCollection) {
      return delegate.addAll(pCollection);
    }

    @Override
    public boolean retainAll(Collection<?> pCollection) {
      return delegate.retainAll(pCollection);
    }

    @Override
    public boolean removeAll(Collection<?> pCollection) {
      return delegate.removeAll(pCollection);
    }

    @Override
    public void clear() {
      delegate.clear();
    }

    @Override
    public NavigableSet<T> descendingSet() {
      return delegate;
    }

    @Override
    public Iterator<T> descendingIterator() {
      return delegate.iterator();
    }

    @Override
    public NavigableSet<T> subSet(
        T pBottom, boolean pBottomInclusive, T pTop, boolean pTopInclusive) {
      return new DescendingList<>(delegate.subSet(pBottom, pBottomInclusive, pTop, pTopInclusive));
    }

    @Override
    public NavigableSet<T> headSet(T pT, boolean pTopInclusive) {
      return new DescendingList<>(delegate.headSet(pT, pTopInclusive));
    }

    @Override
    public NavigableSet<T> tailSet(T pT, boolean pBottomInclusive) {
      return new DescendingList<>(delegate.tailSet(pT, pBottomInclusive));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Comparator<? super T> comparator() {
      @Var Comparator<? super T> reversedComparator = delegate.comparator();

      if (reversedComparator == null) {
        reversedComparator = (Comparator<? super T>) Comparator.naturalOrder();
      }

      return reversedComparator.reversed();
    }

    @Override
    public SortedSet<T> subSet(T pBottom, T pTop) {
      return new DescendingList<>(delegate.subSet(pBottom, true, pTop, false));
    }

    @Override
    public SortedSet<T> headSet(T pT) {
      return new DescendingList<>(delegate.headSet(pT, false));
    }

    @Override
    public SortedSet<T> tailSet(T pT) {
      return new DescendingList<>(delegate.tailSet(pT, true));
    }

    @Override
    public boolean equals(Object pO) {
      return delegate.equals(pO);
    }

    @Override
    public int hashCode() {
      return delegate.hashCode();
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder("[");
      @Var boolean isFirst = true;
      for (T val : this) {
        if (isFirst) {
          isFirst = false;
        } else {
          sb.append(", ");
        }
        sb.append(val);
      }
      sb.append("]");
      return sb.toString();
    }
  }
}
