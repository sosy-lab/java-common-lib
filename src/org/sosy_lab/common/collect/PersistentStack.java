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
import java.util.NoSuchElementException;

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
 * <p>Implementations support standard Java Object Serialization. Serialization succeeds only if
 * each contained value and its serialized object graph are serializable at runtime; otherwise,
 * serialization fails according to the standard rules, for example with {@link
 * java.io.NotSerializableException}.
 *
 * <p>This serialization contract applies to conforming Java SE runtimes. GraalVM in JVM mode uses
 * the same semantics, while GraalVM Native Image may require explicit serialization metadata or
 * configuration. Support in non-Java-SE environments, such as Android or GWT, is not guaranteed.
 * Deserialization may also be rejected by configured {@link java.io.ObjectInputFilter} policies,
 * and portability of serialized data depends on the serialized forms of contained values.
 *
 * <p>After a stack reference has been made visible to other threads through synchronization, a
 * {@code volatile} field, or a concurrency utility, its immutable structure may be accessed
 * concurrently. Such coordination is still required to publish or update a shared reference to a
 * stack version, and compound updates require synchronization or an atomic operation. No
 * thread-safety guarantee is made for iterator instances.
 *
 * <p>Values are stored by reference: they are not copied or made immutable or thread-safe. Changes
 * to mutable values can affect equality and hash codes. Operations that depend on values also
 * depend on their thread safety.
 *
 * @param <T> The type of values.
 */
@Immutable(containerOf = "T")
public interface PersistentStack<T> extends Iterable<T>, Serializable {

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
}
