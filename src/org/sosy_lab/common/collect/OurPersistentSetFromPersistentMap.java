// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.errorprone.annotations.Immutable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.SortedSet;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link PersistentSortedSet}-implementation based on any of our {@link PersistentSortedMap}
 * implementations, e.g. {@link PathCopyingPersistentTreeMap}. We use {@link Boolean} as internal
 * value for the map. {@link PersistentSet} is included in this, and as long as all {@link
 * PersistentMap}s are also {@link PersistentSortedMap}s, this works fine. This set is only
 * serializable iff the used map is serializable as well.
 */
@Immutable(containerOf = "E")
public final class OurPersistentSetFromPersistentMap<E extends Comparable<? super E>>
    extends AbstractImmutableSortedSet<E> implements PersistentSortedSet<E>, Serializable {

  private static final long serialVersionUID = -5645114299446946246L;

  @SuppressWarnings("serial")
  private final PersistentSortedMap<E, Boolean> delegate;

  OurPersistentSetFromPersistentMap(PersistentSortedMap<E, Boolean> pDelegate) {
    checkNotNull(pDelegate);
    if (!pDelegate.isEmpty()) {
      throw new IllegalArgumentException("Map is non-empty");
    } else {
      delegate = pDelegate;
      // TODO: why does Javas version track the key set transiently as well?
      // this.s = map.keySet();
    }
  }

  @Override
  public PersistentSet<E> empty() {
    return newSetFromMap(delegate.empty());
  }

  @SuppressFBWarnings
  @Override
  public PersistentSet<E> addAndCopy(E key) {
    return new OurPersistentSetFromPersistentMap<>(delegate.putAndCopy(key, null));
  }

  @Override
  public PersistentSet<E> removeAndCopy(Object key) {
    return new OurPersistentSetFromPersistentMap<>(delegate.removeAndCopy(key));
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
  public boolean contains(Object o) {
    return delegate.containsKey(o);
  }

  @Override
  public Iterator<E> iterator() {
    return delegate.keySet().iterator();
  }

  @Override
  public Object[] toArray() {
    return delegate.keySet().toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    // TODO
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    for (Object el : c) {
      if (!contains(el)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (other == null) {
      return false;
    }
    return other instanceof OurPersistentSetFromPersistentMap<?>
        && delegate.equals(((OurPersistentSetFromPersistentMap<?>) other).delegate);
  }

  @Override
  public String toString() {
    return delegate.keySet().toString();
  }

  @Override
  public Comparator<? super E> comparator() {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public SortedSet<E> subSet(E pE, E pE1) {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public SortedSet<E> headSet(E pE) {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public SortedSet<E> tailSet(E pE) {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public E first() {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public E last() {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public E lower(E pE) {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public E floor(E pE) {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public E ceiling(E pE) {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public E higher(E pE) {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public E pollFirst() {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public E pollLast() {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public NavigableSet<E> descendingSet() {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterator<E> descendingIterator() {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public NavigableSet<E> subSet(E pE, boolean pB, E pE1, boolean pB1) {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public NavigableSet<E> headSet(E pE, boolean pB) {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public NavigableSet<E> tailSet(E pE, boolean pB) {
    // TODO:
    throw new UnsupportedOperationException();
  }
}
