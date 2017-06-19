/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
