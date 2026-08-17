// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect.union_find;

import com.google.errorprone.annotations.DoNotCall;

/**
 * An abstract class for immutable Union-Find implementations.
 *
 * @param <T> type of elements added to the Union-Find
 */
public abstract class AbstractImmutableUnionFind<T> implements UnionFind<T> {
  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  @DoNotCall
  public final void union(T pE1, T pE2) {
    throw new UnsupportedOperationException();
  }
}
