// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import com.google.errorprone.annotations.DoNotCall;
import com.google.errorprone.annotations.Immutable;
import java.util.Collection;
import java.util.Set;

@Immutable(containerOf = {"E"})
public abstract class AbstractImmutableSet<E extends Comparable<? super E>> implements Set<E> {

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  @DoNotCall
  public final boolean add(E var1) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  @DoNotCall
  public final boolean remove(Object var1) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  @DoNotCall
  public final boolean addAll(Collection<? extends E> var1) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  @DoNotCall
  public final boolean retainAll(Collection<?> var1) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  @DoNotCall
  public final boolean removeAll(Collection<?> var1) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  @DoNotCall
  public final void clear() {
    throw new UnsupportedOperationException();
  }
}
