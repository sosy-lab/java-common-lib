// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.UnmodifiableIterator;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.annotations.Var;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A persistent stack. Pushes structurally share the complete previous stack, and pops return the
 * existing tail without copying, while leaving the original stack unchanged. Thus {@link
 * #pushAndCopy(Object)}, {@link #popAndCopy()}, {@link #peek()}, {@link #empty()}, {@link
 * #isEmpty()}, and {@link #size()} run in O(1) time. Iteration, {@link #equals(Object)}, {@link
 * #hashCode()}, and {@link #toString()} have O(n) worst-case stack-traversal overhead. When two
 * equal-size {@link PersistentLinkedStack} instances share a tail, {@code equals} traverses only
 * the k nodes preceding that tail and thus has O(k) traversal overhead. These bounds exclude work
 * performed by element methods.
 *
 * <p>All structural state is final and correctly constructed, so the immutable stack structure may
 * be accessed concurrently without synchronization. Publishing or updating a shared reference to a
 * stack version still requires coordination, and compound updates require synchronization or an
 * atomic operation. Each traversal has an independent iterator; a single iterator instance has no
 * thread-safety guarantee. Values are stored by reference and are not copied or made immutable or
 * thread-safe.
 *
 * <p>Null values are not supported.
 *
 * <p><b>Serialization:</b> Serialization and deserialization have O(n) stack-traversal overhead and
 * use O(n) temporary memory, excluding the processing of element object graphs. A flattened proxy
 * stores the logical values in top-to-bottom order instead of serializing the linked nodes. This
 * avoids recursive traversal of long stacks and keeps node and cache fields out of the serialized
 * form. Deserialization rejects null proxy data and rebuilds the stack from bottom to top while
 * validating each value, thereby preserving order and restoring the canonical empty instance.
 * Because each stack is flattened independently, distinct, structurally related stacks serialized
 * together have their shared non-empty tails reconstructed independently; {@link #popAndCopy()} on
 * a deserialized stack nevertheless returns its existing tail. Element object graphs must not
 * contain references back to the containing stack because proxy replacement cannot restore such
 * cycles. Persisted data remains readable only while the proxy and element serialized forms remain
 * compatible.
 *
 * @param <T> the type of values
 */
@Immutable(containerOf = "T")
public final class PersistentLinkedStack<T> implements PersistentStack<T> {

  @Serial private static final long serialVersionUID = -4286928240765960519L;

  private static final PersistentLinkedStack<?> EMPTY = new PersistentLinkedStack<>();

  /** The top value, null exactly for the empty singleton. */
  @SuppressWarnings("serial") // writeReplace prevents direct serialization of this field.
  private final @Nullable T top;

  /** The linked tail, null exactly for the empty singleton. */
  private final @Nullable PersistentLinkedStack<T> tail;

  /**
   * The size, cached for O(1) access. It is zero exactly for the empty stack and otherwise equals
   * {@code tail.size + 1}. The cache is one logical 4-byte {@code int}; its actual footprint
   * depends on JVM object layout and alignment. It often fits into padding with compressed
   * references and 8-byte alignment, but may add an alignment unit otherwise.
   */
  private final int size;

  private PersistentLinkedStack() {
    top = null;
    tail = null;
    size = 0;
  }

  private PersistentLinkedStack(T pTop, PersistentLinkedStack<T> pTail) {
    top = checkNotNull(pTop);
    tail = checkNotNull(pTail);
    size = pTail.size + 1;
  }

  /** Returns an empty stack. */
  @SuppressWarnings("unchecked")
  public static <T> PersistentLinkedStack<T> of() {
    return (PersistentLinkedStack<T>) EMPTY;
  }

  /**
   * Returns a stack containing {@code value}.
   *
   * @throws NullPointerException if {@code value} is null
   */
  public static <T> PersistentLinkedStack<T> of(T value) {
    return new PersistentLinkedStack<>(value, PersistentLinkedStack.of());
  }

  @Override
  public PersistentLinkedStack<T> pushAndCopy(T value) {
    return new PersistentLinkedStack<>(value, this);
  }

  @Override
  public PersistentLinkedStack<T> popAndCopy() {
    if (isEmpty()) {
      throw new NoSuchElementException();
    }
    return checkNotNull(tail);
  }

  @Override
  public T peek() {
    if (isEmpty()) {
      throw new NoSuchElementException();
    }
    return checkNotNull(top);
  }

  @Override
  public PersistentLinkedStack<T> empty() {
    return of();
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public Iterator<T> iterator() {
    return new StackIterator<>(this);
  }

  @Override
  @SuppressWarnings("ReferenceEquality") // Node identity detects structurally shared tails.
  public boolean equals(@Nullable Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof PersistentLinkedStack<?> other)) {
      return false;
    }
    if (size != other.size()) {
      return false;
    }

    @Var PersistentLinkedStack<?> thisRemainder = this;
    @Var PersistentLinkedStack<?> otherRemainder = other;
    while (thisRemainder != otherRemainder) {
      if (!Objects.equals(thisRemainder.top, otherRemainder.top)) {
        return false;
      }
      thisRemainder = checkNotNull(thisRemainder.tail);
      otherRemainder = checkNotNull(otherRemainder.tail);
    }
    return true;
  }

  @Override
  public int hashCode() {
    @Var int hashCode = PersistentLinkedStack.class.hashCode();
    for (T value : this) {
      hashCode = 31 * hashCode + value.hashCode();
    }
    return hashCode;
  }

  /**
   * Returns the values in top-to-bottom order, separated by {@code ", "} and enclosed in square
   * brackets: {@code [top, ..., bottom]}. The empty stack is represented as {@code []}.
   */
  @Override
  public String toString() {
    StringBuilder result = new StringBuilder("[");
    Iterator<T> iterator = iterator();
    while (iterator.hasNext()) {
      result.append(iterator.next());
      if (iterator.hasNext()) {
        result.append(", ");
      }
    }
    return result.append(']').toString();
  }

  @Serial
  private Object writeReplace() {
    return new SerializationProxy(this);
  }

  @Serial
  @SuppressWarnings("unused") // Serialization hook prevents bypassing the proxy.
  private void readObject(ObjectInputStream pInputStream) throws InvalidObjectException {
    throw new InvalidObjectException("Serialization proxy required");
  }

  /** Flat serialized form containing the logical values in top-to-bottom order. */
  private static final class SerializationProxy implements Serializable {

    @Serial private static final long serialVersionUID = 2702329958583141147L;

    /** Nullable only to model malformed serialized input, which {@link #readResolve()} rejects. */
    @SuppressWarnings("serial") // ObjectOutputStream checks each element graph at runtime.
    private final @Nullable Object @Nullable [] values;

    private SerializationProxy(PersistentLinkedStack<?> stack) {
      values = new Object[stack.size];
      @Var int index = 0;
      for (Object value : stack) {
        values[index] = value;
        index++;
      }
    }

    @Serial
    private Object readResolve() throws InvalidObjectException {
      @Nullable Object @Nullable [] serializedValues = values;
      if (serializedValues == null) {
        throw new InvalidObjectException("Stack values must not be null");
      }

      @Var PersistentLinkedStack<Object> stack = PersistentLinkedStack.of();
      // Push bottom-to-top to reconstruct the original iteration order.
      for (@Var int index = serializedValues.length - 1; index >= 0; index--) {
        @Nullable Object value = serializedValues[index];
        if (value == null) {
          throw new InvalidObjectException("Stack values must not contain null");
        }
        stack = stack.pushAndCopy(value);
      }
      return stack;
    }
  }

  private static final class StackIterator<T> extends UnmodifiableIterator<T> {

    private @Nullable PersistentLinkedStack<T> stack;

    private StackIterator(PersistentLinkedStack<T> pStack) {
      stack = pStack;
    }

    @Override
    public boolean hasNext() {
      return stack != null && !stack.isEmpty();
    }

    @Override
    public T next() {
      @Nullable PersistentLinkedStack<T> currentStack = stack;
      if (currentStack == null || currentStack.isEmpty()) {
        throw new NoSuchElementException();
      }
      T value = checkNotNull(currentStack.top);
      stack = currentStack.tail;
      return value;
    }
  }
}
