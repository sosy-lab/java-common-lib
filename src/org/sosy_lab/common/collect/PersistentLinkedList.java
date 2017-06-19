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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.UnmodifiableListIterator;
import java.util.AbstractSequentialList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import javax.annotation.Nullable;

/**
 * A single-linked-list implementation of {@link PersistentList}. Null values are not supported
 * (similarly to {@link ImmutableList}).
 *
 * <p>Adding to the front of the list needs only O(1) time and memory.
 *
 * <p>This implementation supports almost all operations, except for the {@link
 * ListIterator#hasPrevious()} and {@link ListIterator#previous()} methods of its list iterator.
 * This means you cannot traverse this list in reverse order.
 *
 * <p>All instances of this class are fully-thread safe. However, note that each modifying operation
 * allocates a new instance whose reference needs to be published safely in order to be usable by
 * other threads. Two concurrent accesses to a modifying operation on the same instance will create
 * two new maps, each reflecting exactly the operation executed by the current thread, and not
 * reflecting the operation executed by the other thread.
 */
@javax.annotation.concurrent.Immutable
@SuppressWarnings("deprecation") // javac complains about deprecated methods from PersistentList
public final class PersistentLinkedList<T> extends AbstractSequentialList<T>
    implements PersistentList<T> {

  private final @Nullable T head; // only null for the empty list
  private final @Nullable PersistentLinkedList<T> tail; // only null for the empty list

  private PersistentLinkedList(
      final @Nullable T head, final @Nullable PersistentLinkedList<T> tail) {
    this.head = head;
    this.tail = tail;
  }

  @SuppressWarnings("rawtypes")
  private static final PersistentLinkedList EMPTY = makeEmpty();

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static PersistentLinkedList makeEmpty() {
    return new PersistentLinkedList(null, null);
  }

  /**
   * Returns the empty list.
   *
   * @return The empty list
   */
  @SuppressWarnings("unchecked")
  public static <T> PersistentLinkedList<T> of() {
    return EMPTY;
  }

  /**
   * Returns a list containing the specified value.
   *
   * @return A list containing the specified value
   */
  public static <T> PersistentLinkedList<T> of(final T value) {
    checkNotNull(value);
    return new PersistentLinkedList<>(value, PersistentLinkedList.<T>of());
  }

  /**
   * Returns a list containing the specified values.
   *
   * @return A list containing the specified values
   */
  public static <T> PersistentLinkedList<T> of(final T v1, final T v2) {
    return of(v2).with(v1);
  }

  /**
   * Returns a list containing the specified values.
   *
   * @return A list containing the specified values
   */
  public static <T> PersistentLinkedList<T> of(final T v1, final T v2, final T v3) {
    return of(v3).with(v2).with(v1);
  }

  /**
   * Returns a list containing the specified values.
   *
   * @return A list containing the specified values
   */
  @SuppressWarnings("unchecked")
  public static <T> PersistentLinkedList<T> of(final T v1, final T... values) {
    return copyOf(values).with(v1);
  }

  /**
   * Returns a list containing the specified values.
   *
   * @return A list containing the specified values
   */
  @SuppressWarnings("unchecked")
  public static <T> PersistentLinkedList<T> copyOf(final T... values) {
    return copyOf(Arrays.asList(values));
  }

  /**
   * Returns A new list with the values from the Iterable.
   *
   * @return A new list with the values from the Iterable
   */
  public static <T> PersistentLinkedList<T> copyOf(final List<T> values) {
    if (values instanceof PersistentLinkedList<?>) {
      return (PersistentLinkedList<T>) values;
    }
    PersistentLinkedList<T> result = PersistentLinkedList.<T>of();
    for (T value : Lists.reverse(values)) {
      result = result.with(value);
    }
    return result;
  }

  /**
   * Returns the value at the start of the list.
   *
   * @return The value at the start of the list
   */
  public T head() {
    checkState(!isEmpty());
    return head;
  }

  /**
   * Returns the remainder of the list without the first element.
   *
   * @return The remainder of the list without the first element
   */
  public PersistentLinkedList<T> tail() {
    checkState(!isEmpty());
    return tail;
  }

  /**
   * Returns a new list with value as the head and the old list as the tail.
   *
   * @return A new list with value as the head and the old list as the tail
   */
  @Override
  public PersistentLinkedList<T> with(final T value) {
    checkNotNull(value);
    return new PersistentLinkedList<>(value, this);
  }

  /**
   * Returns a new list with values as the head and the old list as the tail.
   *
   * @return A new list with value sas the head and the old list as the tail
   */
  @Override
  public PersistentLinkedList<T> withAll(List<T> values) {
    PersistentLinkedList<T> result = this;
    if (values instanceof PersistentLinkedList<?>) {
      // does not support listIterator() and thus fails on Lists.reverse()
      values = ImmutableList.copyOf(values);
    }
    for (T value : Lists.reverse(values)) {
      result = result.with(value);
    }
    return result;
  }

  /**
   * Returns a new list omitting the specified value. Note: O(N)
   *
   * @return A new list omitting the specified value
   */
  @Override
  public PersistentLinkedList<T> without(final @Nullable T value) {
    PersistentLinkedList<T> suffix = of(); // remainder of list after value

    // find position of value and update suffix
    int pos = 0;
    for (PersistentLinkedList<T> list = this; !list.isEmpty(); list = list.tail) {
      if (Objects.equals(value, list.head)) {
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
   * Returns the number of elements in the list. Note: O(N)
   *
   * @return The number of elements in the list
   */
  @Override
  public int size() {
    int size = 0;
    for (PersistentLinkedList<T> list = this; !list.isEmpty(); list = list.tail) {
      ++size;
    }
    return size;
  }

  @Override
  @SuppressWarnings("ReferenceEquality") // singleton instance
  public boolean isEmpty() {
    return this == EMPTY;
  }

  /**
   * Returns a new list with the elements in the reverse order. This operation runs in O(n).
   *
   * @return A new list with the elements in the reverse order
   */
  @Override
  public PersistentLinkedList<T> reversed() {
    PersistentLinkedList<T> result = empty();
    for (PersistentLinkedList<T> p = this; !p.isEmpty(); p = p.tail) {
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
    if (index < 0) {
      throw new IndexOutOfBoundsException();
    }
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
      return !list.isEmpty();
    }

    @Override
    public T next() {
      if (list.isEmpty()) {
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
      return nextIndex - 1;
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

  /**
   * Return a {@link Collector} that creates PersistentLinkedLists and can be used in {@link
   * java.util.stream.Stream#collect(Collector)}. The returned collector does not support parallel
   * streams.
   */
  public static <T> Collector<T, ?, PersistentLinkedList<T>> toPersistentLinkedList() {
    return new Collector<T, PersistentLinkedListBuilder<T>, PersistentLinkedList<T>>() {

      @Override
      public Supplier<PersistentLinkedListBuilder<T>> supplier() {
        return PersistentLinkedListBuilder::new;
      }

      @Override
      public BiConsumer<PersistentLinkedListBuilder<T>, T> accumulator() {
        return PersistentLinkedListBuilder::add;
      }

      @Override
      public BinaryOperator<PersistentLinkedListBuilder<T>> combiner() {
        return (a, b) -> {
          throw new UnsupportedOperationException("Should be used sequentially");
        };
      }

      @Override
      public Function<PersistentLinkedListBuilder<T>, PersistentLinkedList<T>> finisher() {
        return PersistentLinkedListBuilder::build;
      }

      @Override
      public Set<Characteristics> characteristics() {
        return EnumSet.noneOf(Characteristics.class);
      }
    };
  }

  /**
   * Return a {@link Collector} that creates PersistentLinkedLists and can be used in {@link
   * java.util.stream.Stream#collect(Collector)}. The returned collector does not support parallel
   * streams.
   *
   * @deprecated renamed to {@link #toPersistentLinkedList()} to conform with Guava's naming
   */
  @Deprecated
  public static <T> Collector<T, ?, PersistentLinkedList<T>> collector() {
    return toPersistentLinkedList();
  }

  private static class PersistentLinkedListBuilder<T> {
    private PersistentLinkedList<T> list = PersistentLinkedList.of();

    void add(final T e) {
      list = list.with(e);
    }

    PersistentLinkedList<T> build() {
      return list;
    }
  }

  @Deprecated
  @Override
  public void replaceAll(UnaryOperator<T> pOperator) {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  public void sort(Comparator<? super T> pC) {
    throw new UnsupportedOperationException();
  }
}
