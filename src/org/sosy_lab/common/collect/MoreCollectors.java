// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/** Additional {@link Collector} implementation, similar to {@link Collectors}. */
public final class MoreCollectors {

  private MoreCollectors() {}

  /**
   * Return a {@link Collector} that produces {@link PersistentLinkedList}s.
   *
   * @deprecated use {@link PersistentLinkedList#toPersistentLinkedList()}
   */
  @Deprecated
  public static <T> Collector<T, ?, PersistentLinkedList<T>> toPersistentLinkedList() {
    return PersistentLinkedList.toPersistentLinkedList();
  }

  /**
   * Return a {@link Collector} that produces {@link ImmutableList}s.
   *
   * <p>Prefer to use this over {@link Collectors#toList()}! The latter does neither guarantee
   * mutability nor immutability, so if you want immutability, use this method, and if you need
   * mutability, use {@code Collectors.toCollection(ArrayList::new)}.
   *
   * @deprecated use {@link ImmutableList#toImmutableList()}
   */
  @Deprecated
  public static <T> Collector<T, ?, ImmutableList<T>> toImmutableList() {
    return ImmutableList.toImmutableList();
  }

  /**
   * Return a {@link Collector} that produces {@link ImmutableSet}s. Just like the usual methods for
   * ImmutableSets, this collector guarantees to keep the order.
   *
   * <p>Prefer to use this over {@link Collectors#toSet()}! The latter does neither guarantee
   * mutability nor immutability, so if you want immutability, use this method, and if you need
   * mutability, use {@code Collectors.toCollection(HashSet::new)}.
   *
   * @deprecated use {@link ImmutableList#toImmutableList()}
   */
  @Deprecated
  public static <T> Collector<T, ?, ImmutableSet<T>> toImmutableSet() {
    return ImmutableSet.toImmutableSet();
  }
}
