// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkPositionIndex;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.UnmodifiableListIterator;
import com.google.errorprone.annotations.DoNotCall;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.annotations.InlineMe;
import com.google.errorprone.annotations.Var;
import java.util.AbstractSequentialList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link PersistentList} backed by a {@link PersistentLinkedStack}. List order is top-to-bottom
 * stack order. Null elements are not supported.
 *
 * <p>Prepending, head access, tail access, and size take O(1) time. Updates share unchanged stack
 * nodes.
 *
 * <p>List iterators support forward traversal only; {@link ListIterator#hasPrevious()} and {@link
 * ListIterator#previous()} are unsupported.
 *
 * <p>The list structure is immutable and thread-safe. Elements are not copied; iterator instances
 * have no thread-safety guarantee.
 *
 * @param <T> the type of elements
 */
@Immutable(containerOf = "T")
@SuppressWarnings({
  "deprecation", // javac complains about deprecated methods from PersistentList
  "Immutable", // AbstractList.modCount is mutable but unused
})
public final class PersistentLinkedList<T> extends AbstractSequentialList<T>
    implements PersistentList<T> {

  private static final PersistentLinkedList<?> EMPTY =
      new PersistentLinkedList<>(PersistentLinkedStack.of());

  private final PersistentStack<T> stack;

  private PersistentLinkedList(PersistentStack<T> pStack) {
    stack = checkNotNull(pStack);
  }

  private static <T> PersistentLinkedList<T> fromStack(PersistentStack<T> stack) {
    return stack.isEmpty() ? of() : new PersistentLinkedList<>(stack);
  }

  /** Returns the empty list. */
  @SuppressWarnings("unchecked") // The empty list contains no elements.
  public static <T> PersistentLinkedList<T> of() {
    return (PersistentLinkedList<T>) EMPTY;
  }

  /** Returns a list containing the given element. */
  public static <T> PersistentLinkedList<T> of(T value) {
    return new PersistentLinkedList<>(PersistentLinkedStack.of(value));
  }

  /** Returns a list containing the given elements in argument order. */
  public static <T> PersistentLinkedList<T> of(T v1, T v2) {
    return of(v2).with(v1);
  }

  /** Returns a list containing the given elements in argument order. */
  public static <T> PersistentLinkedList<T> of(T v1, T v2, T v3) {
    return of(v3).with(v2).with(v1);
  }

  /** Returns a list containing the given elements in argument order. */
  @SafeVarargs
  @SuppressWarnings("varargs") // The array is only read and is not retained.
  public static <T> PersistentLinkedList<T> of(T v1, T... values) {
    return copyOf(values).with(v1);
  }

  /** Returns a list containing the given elements in array order. */
  @SafeVarargs
  @SuppressWarnings("varargs") // The array is only read and is not retained.
  public static <T> PersistentLinkedList<T> copyOf(T... values) {
    return copyOf(Arrays.asList(values));
  }

  /**
   * Returns a list in iteration order, reusing {@code values} if it is a {@code
   * PersistentLinkedList}.
   */
  public static <T> PersistentLinkedList<T> copyOf(List<T> values) {
    if (values instanceof PersistentLinkedList<T> list) {
      return list;
    }
    return PersistentLinkedList.<T>of().withAll(values);
  }

  /**
   * Returns the first element.
   *
   * @throws NoSuchElementException if this list is empty
   */
  public T head() {
    return stack.peek();
  }

  /**
   * Returns the list without its first element, sharing the remaining stack nodes.
   *
   * @throws IllegalStateException if this list is empty
   */
  public PersistentLinkedList<T> tail() {
    checkState(!isEmpty());
    return fromStack(stack.popAndCopy());
  }

  /** Returns a list with {@code value} prepended in O(1) time and space. */
  @Override
  public PersistentLinkedList<T> with(T value) {
    return fromStack(stack.pushAndCopy(value));
  }

  /** Returns a list with {@code values} prepended in their iteration order. */
  @Override
  public PersistentLinkedList<T> withAll(List<T> values) {
    if (values.isEmpty()) {
      return this;
    }
    @Var PersistentStack<T> result = stack;
    // A snapshot also supports inputs whose list iterators cannot traverse backwards.
    for (T value : ImmutableList.copyOf(values).reverse()) {
      result = result.pushAndCopy(value);
    }
    return fromStack(result);
  }

  /** Returns a list without the first occurrence of {@code value}, or this list if absent. */
  @Override
  public PersistentLinkedList<T> without(@Nullable T value) {
    int index = indexOf(value);
    if (index < 0) {
      return this;
    }

    List<T> prefix = new ArrayList<>(index);
    @Var PersistentStack<T> result = stack;
    for (int i = 0; i < index; i++) {
      prefix.add(result.peek());
      result = result.popAndCopy();
    }
    result = result.popAndCopy();
    for (T element : Lists.reverse(prefix)) {
      result = result.pushAndCopy(element);
    }
    return fromStack(result);
  }

  @Override
  public PersistentLinkedList<T> empty() {
    return of();
  }

  /** Returns the number of elements in O(1) time. */
  @Override
  public int size() {
    return stack.size();
  }

  @Override
  public boolean isEmpty() {
    return stack.isEmpty();
  }

  /** Returns a list in reverse order in O(n) time and space. */
  @Override
  public PersistentLinkedList<T> reversed() {
    return fromStack(PersistentLinkedStack.copyOf(stack.asTopDownIterable()));
  }

  @Override
  public Iterator<T> iterator() {
    return stack.asTopDownIterable().iterator();
  }

  @Override
  public ListIterator<T> listIterator(int index) {
    checkPositionIndex(index, size());
    ListIterator<T> result = new Iter<>(iterator());
    for (int i = 0; i < index; i++) {
      result.next();
    }
    return result;
  }

  /** Returns a collector that collects elements in reverse encounter order. */
  @SuppressWarnings("NoFunctionalReturnType")
  public static <T> Collector<T, ?, PersistentLinkedList<T>> toPersistentLinkedList() {
    return Collectors.collectingAndThen(
        PersistentLinkedStack.<T>toPersistentLinkedStack(), PersistentLinkedList::fromStack);
  }

  /**
   * Returns a collector that collects elements in reverse encounter order.
   *
   * @deprecated use {@link #toPersistentLinkedList()}
   */
  @Deprecated
  @InlineMe(
      replacement = "PersistentLinkedList.toPersistentLinkedList()",
      imports = "org.sosy_lab.common.collect.PersistentLinkedList")
  public static <T> Collector<T, ?, PersistentLinkedList<T>> collector() {
    return toPersistentLinkedList();
  }

  @Deprecated
  @Override
  @DoNotCall
  public void replaceAll(UnaryOperator<T> pOperator) {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  @DoNotCall
  public void sort(Comparator<? super T> pComparator) {
    throw new UnsupportedOperationException();
  }

  private static final class Iter<T> extends UnmodifiableListIterator<T> {

    private final Iterator<T> delegate;
    private int nextIndex = 0;

    private Iter(Iterator<T> pDelegate) {
      delegate = pDelegate;
    }

    @Override
    public boolean hasNext() {
      return delegate.hasNext();
    }

    @Override
    public T next() {
      T result = delegate.next();
      nextIndex++;
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
}
