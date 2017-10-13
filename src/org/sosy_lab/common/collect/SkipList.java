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
import java.util.NoSuchElementException;
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
    private final int maxLvl;

    private List<Integer> inBetweenCount;

    private Node(@Nullable T pValue, int pLevel) {
      value = pValue;
      next = new ArrayList<>(Collections.nCopies(pLevel + 1, null));
      prev = new ArrayList<>(Collections.nCopies(pLevel + 1, null));
      inBetweenCount = new ArrayList<>(Collections.nCopies(pLevel + 1, 0));
      maxLvl = pLevel;
    }

    @Nullable
    Node<T> getNext(int pLevel) {
      Preconditions.checkElementIndex(pLevel, MAX_LEVEL + 1);
      if (pLevel >= next.size()) {
        return null;
      } else {
        return next.get(pLevel);
      }
    }

    @Nullable
    Node<T> getPrevious(int pLevel) {
      Preconditions.checkElementIndex(pLevel, MAX_LEVEL + 1);
      if (pLevel >= prev.size()) {
        return null;
      } else {
        return prev.get(pLevel);
      }
    }

    @Nullable
    T getValue() {
      return value;
    }

    void setNext(@Nullable Node<T> pNext, int pLevel) {
      next.set(pLevel, pNext);
    }

    void setPrevious(@Nullable Node<T> pPrev, int pLevel) {
      prev.set(pLevel, pPrev);
    }

    void setInBetweenCount(int pNewValue, int pLevel) {
      if (pLevel > maxLvl) {
        // DO NOTHING
      } else {
        inBetweenCount.set(pLevel, pNewValue);
      }
    }

    void increaseInBetweenCount(int pLevel) {
      inBetweenCount.set(pLevel, inBetweenCount.get(pLevel) + 1);
    }

    int getInBetweenCount(int pLevel) {
      if (pLevel > maxLvl) {
        return 0;
      } else {
        return inBetweenCount.get(pLevel);
      }
    }

    int getMaxLvl() {
      return maxLvl;
    }

    private void readObject(ObjectInputStream pIn) throws IOException, ClassNotFoundException {
      pIn.defaultReadObject();

      next = new ArrayList<>(getMaxLvl());
      prev = new ArrayList<>(getMaxLvl());
    }
  }

  private static final int MAX_LEVEL = 31;
  private static final double ONE_HALF_LOG = Math.log(0.5);
  private Random randomGenerator = new Random();

  private final Comparator<? super T> comparator;
  private Node<T> head = createHead();
  private Node<T> tail = head;
  private int size = 0;

  @SuppressWarnings("unchecked")
  public SkipList(@Nullable Comparator<? super T> pComparator) {
    if (pComparator == null) {
      comparator = (Comparator<? super T>) Comparator.naturalOrder();
    } else {
      comparator = pComparator;
    }
  }

  public SkipList(Collection<? extends T> pCollection) {
    this();
    boolean changed = addAll(pCollection);
    assert changed;
  }

  public SkipList(SortedSet<T> pSortedSet) {
    this(pSortedSet.comparator());
    boolean changed = addAll(pSortedSet);
    assert changed;
  }

  @SuppressWarnings("unchecked")
  public SkipList() {
    comparator = (Comparator<? super T>) Comparator.naturalOrder();
  }

  /** Use the given {@link Random} object for future probabilistic computations. */
  public void reinitialize(Random pRandom) {
    Preconditions.checkNotNull(pRandom);
    randomGenerator = pRandom;
  }

  private Node<T> createHead() {
    return new Node<>(null, MAX_LEVEL);
  }

  /**
   * Return a random level between 0 and the max level. The probability distribution is logarithmic
   * (i.e., higher values are less likely).
   */
  private int getRandomLevel() {
    double r = randomGenerator.nextDouble();
    if (r == 0) {
      return MAX_LEVEL;
    } else {
      // change logarithmic base to 0.5
      // to get log_{0.5}(r)
      return ((int) Math.round(Math.log(r) / ONE_HALF_LOG));
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
    for (int currLvl = MAX_LEVEL; currLvl >= LEVEL_ONE; currLvl--) {
      // Get closest node that is less or equal to the new value, and track the number of steps
      // necessary to get there on the current lvl.
      @Var int inBetweenCount = 0;
      @Var Node<T> next = currNode.getNext(currLvl);
      while (next != null && comparator.compare(pT, next.getValue()) >= 0) {
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

      // We want to compare by identity here
      if (currNode == tail) {
        tail = newNode;
      }
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

  private Node<T> getClosestLessEqual(Node<T> pStart, int pLvl, T pVal) {
    @Var Node<T> currNode = pStart;
    @Var Node<T> next = currNode.getNext(pLvl);
    while (next != null && comparator.compare(pVal, next.getValue()) >= 0) {
      currNode = next;
      next = currNode.getNext(pLvl);
    }
    return currNode;
  }

  private void removeNode(Node<T> pNode) {
    for (int currLvl = pNode.getMaxLvl(); currLvl >= LEVEL_ONE; currLvl--) {
      @Var Node<T> previous = pNode.getPrevious(currLvl);
      assert previous != null;
      @Var Node<T> next = pNode.getNext(currLvl);
      previous.setNext(next, currLvl);
      if (next != null) {
        next.setPrevious(previous, currLvl);
        int inBetweenCount = next.getInBetweenCount(currLvl) + pNode.getInBetweenCount(currLvl);
        next.setInBetweenCount(inBetweenCount, currLvl);
      } else if (pNode == tail) {
        tail = previous;
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
    for (int currLvl = MAX_LEVEL; currLvl >= LEVEL_ONE; currLvl--) {
      @Var Node<T> next = getClosestLessEqual(currNode, currLvl, val);
      currNode = next;
      next = currNode.getNext(currLvl);
      if (next != null) {
        // One predecessor will be removed
        next.setInBetweenCount(next.getInBetweenCount(currLvl) - 1, currLvl);
      }
      // do not store this next node, we stay at the closest less/equal node to find the equal
      // node
    }

    while (currNode != head && comparator.compare(currNode.getValue(), val) == 0) {
      if (currNode.getValue().equals(pO)) {
        removeNode(currNode);
        return true;
      } else {
        currNode = currNode.getNext(LEVEL_ONE);
      }
    }
    return false;
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
    for (int currLvl = MAX_LEVEL; currLvl >= LEVEL_ONE; currLvl--) {
      currNode = getClosestLessEqual(currNode, currLvl, val);
    }

    while (currNode != head
        && currNode != null
        && comparator.compare(currNode.getValue(), val) == 0) {
      if (currNode.getValue().equals(pO)) {
        return true;
      } else {
        currNode = currNode.getNext(LEVEL_ONE);
      }
    }
    return false;
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
    size = 0;
  }

  private Node<T> getNode(int pIndex) {
    Preconditions.checkElementIndex(pIndex, size);

    @Var int currPos = -1;
    @Var int currLvl = MAX_LEVEL;
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
    @Var int currLvl = MAX_LEVEL;
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
    for (int currLvl = MAX_LEVEL; currLvl >= LEVEL_ONE; currLvl--) {
      next = currNode.getNext(currLvl);
      while (next != null && comparator.compare(val, next.getValue()) >= 0) {
        currNode = next;

        int comp = comparator.compare(val, currNode.getValue());
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

    while (currNode != head && comparator.compare(val, currNode.getValue()) == 0) {
      if (currNode.getValue().equals(pO)) {
        return index;
      } else {
        currNode = currNode.getNext(LEVEL_ONE);
        index++;
      }
    }

    return -1;
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
    Preconditions.checkNotNull(pFromElement);
    Preconditions.checkNotNull(pToElement);
    SkipList<T> subList = new SkipList<>(comparator);

    int start = rankOf(pFromElement);
    if (start < 0) {
      throw new IllegalStateException("From-element doesn't exist in list: " + pFromElement);
    }

    int end = rankOf(pToElement);
    if (end < 0) {
      throw new IllegalStateException("To-element doesn't exist in list: " + pToElement);
    }

    Node<T> startNode = getNode(start);
    Node<T> endNode = getNode(end);

    for (Node<T> currNode = startNode; currNode != endNode; ) {
      boolean existed = subList.add(currNode.getValue());
      assert !existed;
      currNode = currNode.getNext(LEVEL_ONE);
    }

    return subList;
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

  @Override
  public Iterator<T> iterator() {
    return new Iterator<T>() {

      private Node<T> currentNode = head;
      boolean removed = false;

      @Override
      public boolean hasNext() {
        return currentNode.getNext(LEVEL_ONE) != null;
      }

      @Override
      public @Nullable T next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }

        currentNode = currentNode.getNext(LEVEL_ONE);
        assert currentNode != null;
        removed = false;
        return currentNode.getValue();
      }

      @Override
      public void remove() {
        if (currentNode == head || removed) {
          throw new IllegalStateException();
        } else {
          removeNode(currentNode);
          currentNode = currentNode.getPrevious(LEVEL_ONE);
          removed = true;
        }
      }
    };
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
}
