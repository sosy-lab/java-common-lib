// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Immutable;
import java.io.Serializable;
import java.util.List;
import java.util.NoSuchElementException;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Interface for persistent stacks. A persistent data structure is structurally immutable, but
 * provides cheap copy-and-write operations. Operations that conceptually modify the stack return
 * another stack while leaving the current instance unchanged.
 *
 * <p>Implementations are expected to provide {@link #pushAndCopy(Object)}, {@link #popAndCopy()},
 * {@link #peek()}, {@link #empty()}, {@link #isEmpty()}, and {@link #size()} in O(1) time.
 * Iteration proceeds from the top of the stack to the bottom.
 *
 * <p>Null values are not supported.
 *
 * <p>Instances and their views are thread-safe; iterator instances have no thread-safety guarantee.
 * Elements are not copied, and their own thread-safety requirements still apply.
 *
 * <p>Stacks are serializable when their elements are serializable.
 *
 * <p>Values are stored by reference: they are not copied or made immutable or thread-safe.
 *
 * @param <T> The type of values.
 */
@Immutable(containerOf = "T")
public interface PersistentStack<T> extends Serializable {

  /**
   * Returns a stack with {@code value} on top, leaving this stack unchanged.
   *
   * @throws NullPointerException if {@code value} is null
   */
  @CheckReturnValue
  PersistentStack<T> pushAndCopy(T value);

  /**
   * Returns a stack without this stack's top value, leaving this stack unchanged.
   *
   * @throws NoSuchElementException if this stack is empty
   */
  @CheckReturnValue
  PersistentStack<T> popAndCopy();

  /**
   * Returns this stack's top value without modifying the stack.
   *
   * @throws NoSuchElementException if this stack is empty
   */
  T peek();

  /** Returns an empty stack of the same implementation. */
  @CheckReturnValue
  PersistentStack<T> empty();

  /** Returns whether this stack contains no values. */
  boolean isEmpty();

  /** Returns the number of values in this stack. */
  int size();

  /**
   * Returns an unmodifiable top-to-bottom view in O(1) time. Each iterator traverses this stack
   * version independently.
   */
  Iterable<T> asTopDownIterable();

  /** Returns an unmodifiable bottom-to-top list in O(n) time and space. */
  List<T> copyToList();

  /**
   * Returns a stack containing the bottom {@code count} elements.
   *
   * @throws IndexOutOfBoundsException if {@code count} is outside {@code [0, size()]}
   */
  @CheckReturnValue
  PersistentStack<T> takeBottom(int count);

  /**
   * Returns {@code true} if and only if {@code obj} is a {@link PersistentStack} with the same
   * number of elements and equal corresponding elements in top-to-bottom order. Elements are
   * compared using {@link Object#equals(Object)}.
   *
   * <p>Equality is independent of the concrete implementation and structural sharing. All empty
   * stacks are equal.
   *
   * @param obj the object to compare with this stack
   * @return whether the object is equal to this stack
   */
  @Override
  boolean equals(@Nullable Object obj);

  /**
   * Returns the hash code of this stack.
   *
   * <p>The hash code is computed starting with {@code hash = 1} and applying {@code hash = 31 *
   * hash + element.hashCode()} to each element in top-to-bottom order, using Java {@code int}
   * arithmetic. The hash code of an empty stack is {@code 1}.
   *
   * @return the hash code of this stack
   */
  @Override
  int hashCode();
}
