// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Immutable;
import java.util.NavigableSet;

@Immutable(containerOf = {"E"})
public interface PersistentSortedSet<E extends Comparable<? super E>>
    extends PersistentSet<E>, NavigableSet<E> {

  /**
   * Replacement for Constructor that returns a fresh instance based on the given {@link
   * PersistentSortedMap}. The characteristics of the returned {@link PersistentSortedSet} are based
   * on the used {@link PersistentSortedMap}. If the given {@link PersistentSortedMap} is
   * serializable, the returned {@link PersistentSortedSet} is also serializable.
   */
  @CheckReturnValue
  default PersistentSortedSet<E> newSetFromMap(PersistentSortedMap<E, Boolean> mapForSet) {
    return new OurPersistentSetFromPersistentMap<>(mapForSet);
  }
}
