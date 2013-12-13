/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.common.collect;

import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.FluentIterable.from;

import java.util.AbstractSequentialList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.UnmodifiableListIterator;

/**
* A linked-list implementation of {@link PersistentList}.
*/
@Immutable
public class PersistentLinkedList<T> extends AbstractSequentialList<T> implements PersistentList<T> {

  private PersistentLinkedList(final T head, final PersistentLinkedList<T> tail) {
    this.head = head;
    this.tail = tail;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static PersistentLinkedList makeEmpty() {
    return new PersistentLinkedList(null, null);
  }

  /** Returns the empty list.
   *  @return The empty list */
  @SuppressWarnings("unchecked")
  public static <T> PersistentLinkedList<T> of() {
    return EMPTY;
  }

  /** Returns a list containing the specified value.
   *  @return A list containing the specified value */
  @SuppressWarnings("unchecked")
  public static <T> PersistentLinkedList<T> of(final T value) {
    return new PersistentLinkedList<>(value, EMPTY);
  }

  /** Returns a list containing the specified values.
   *  @return A list containing the specified values */
  @SuppressWarnings("unchecked")
  public static <T> PersistentLinkedList<T> of(final T v1, final T v2) {
    return EMPTY.with(v2).with(v1);
  }

  /** Returns a list containing the specified values.
   *  @return A list containing the specified values */
  @SuppressWarnings("unchecked")
  public static <T> PersistentLinkedList<T> of(final T v1, final T v2, final T v3) {
    return EMPTY.with(v3).with(v2).with(v1);
  }

  /** Returns a list containing the specified values.
   *  @return A list containing the specified values */
  @SuppressWarnings("unchecked")
  public static <T> PersistentLinkedList<T> of(final T v1, final T ... values) {
    PersistentLinkedList<T> result = of(v1);
    for (T value : values) {
      result = result.with(value);
    }
    return result;
  }

  /** Returns a list containing the specified values.
   *  @return A list containing the specified values */
  @SuppressWarnings("unchecked")
  public static <T> PersistentLinkedList<T> copyOf(final T ... values) {
    return copyOf(Arrays.asList(values));
  }

  /** Returns A new list with the values from the Iterable.
   *  @return A new list with the values from the Iterable */
  @SuppressWarnings("unchecked")
  public static <T> PersistentLinkedList<T> copyOf(final Iterable<T> values) {
    PersistentLinkedList<T> result = EMPTY;
    for (T value : values) {
      result = result.with(value);
    }
    return result;
  }

  /** Returns the value at the start of the list.
   *  @return The value at the start of the list */
  public T head() {
    checkState(head != null);
    return head;
  }

  /** Returns the remainder of the list without the first element.
   *  @return The remainder of the list without the first element */
  public PersistentLinkedList<T> tail() {
    checkState(tail != null);
    return tail;
  }

  /** Returns a new list with value as the head and the old list as the tail.
   *  @return A new list with value as the head and the old list as the tail */
  @Override
  public PersistentLinkedList<T> with(final T value) {
    checkNotNull(value);
    return new PersistentLinkedList<>(value, this);
  }

  /** Returns a new list with values as the head and the old list as the tail.
   *  @return A new list with value sas the head and the old list as the tail */
  @Override
  public PersistentLinkedList<T> withAll(final Iterable<T> values) {
    PersistentLinkedList<T> result = this;
    for (T value : values) {
      result = result.with(value);
    }
    return result;
  }

  @Override
  public T get(int i) {
    if (i >= 0) {
      for (PersistentLinkedList<T> list = this; list != EMPTY; list = list.tail()) {
        if (i-- == 0) {
          return list.head;
        }
      }
    }
    throw new IndexOutOfBoundsException();
  }

  /** Returns a new list omitting the specified value.
   *  Note: O(N)
   *  @return A new list omitting the specified value
   */
  @Override
  public PersistentLinkedList<T> without(final T value) {
    PersistentLinkedList<T> suffix = of(); // remainder of list after value

    // find position of value and update suffix
    int pos = 0;
    for (PersistentLinkedList<T> list = this; list != EMPTY; list = list.tail()) {
      if (Objects.equal(value, list.head)) {
        suffix = list.tail;
        break;
      }
      pos++;
    }

    // get start of list until value
    // into a separate list so we can iterate in reverse
    ImmutableList<T> prefix = from(this).limit(pos).toList();

    // concatenate prefix and suffix
    PersistentLinkedList<T> result = suffix;
    for (T v : prefix.reverse()) {
      result = result.with(v);
    }

    return result;
  }

  @Override
  public PersistentLinkedList<T> empty() {
    return of();
  }

  /**
   * Returns the number of elements in the list.
   * Note: O(N)
   * @return The number of elements in the list
   */
  @Override
  public int size() {
    int size = 0;
    for (PersistentLinkedList<T> list = this; list != EMPTY; list = list.tail) {
      ++size;
    }
    return size;
  }

  @Override
  public boolean isEmpty() {
    return this == EMPTY;
  }

  /** Returns a new list with the elements in the reverse order.
   *  @return A new list with the elements in the reverse order */
  @Override
  public PersistentLinkedList<T> reversed() {
    PersistentLinkedList<T> result = empty();
    for (PersistentLinkedList<T> p = this; p != EMPTY; p = p.tail) {
      result = result.with(p.head);
    }
    return result;
  }

  @Override
  public Iterator<T> iterator() {
    return new Iter<>(this);
  }

  @Override
  public ListIterator<T> listIterator(final int index) {
    checkArgument(index >= 0);
    ListIterator<T> it = new Iter<>(this);
    for (int i = 0; i < index; i++) {
      if (!it.hasNext()) {
        throw new IndexOutOfBoundsException();
      }
      it.next();
    }
    return it;
  }

  private static class Iter<T> extends UnmodifiableListIterator<T> {

    private PersistentLinkedList<T> list;
    private int nextIndex = 0;

    private Iter(PersistentLinkedList<T> list) {
      this.list = list;
    }

    @Override
    public boolean hasNext() {
      return list != EMPTY;
    }

    @Override
    public T next() {
      if (list == EMPTY) {
        throw new NoSuchElementException();
      }
      nextIndex++;
      T result = list.head;
      list = list.tail;
      return result;
    }

    @Override
    public int nextIndex() {
      return nextIndex;
    }

    @Override
    public int previousIndex() {
      return nextIndex-1;
    }

    @Override
    public boolean hasPrevious() {
      throw new UnsupportedOperationException();
    }

    @Override
    public T previous() {
      throw new UnsupportedOperationException();
    }
  }

  private final T head;
  private final PersistentLinkedList<T> tail;

  @SuppressWarnings({ "rawtypes" })
  private static final PersistentLinkedList EMPTY = makeEmpty();
}
