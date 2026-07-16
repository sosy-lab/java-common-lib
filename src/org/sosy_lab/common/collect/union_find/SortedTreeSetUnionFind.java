// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect.union_find;

import java.util.HashMap;
import java.util.TreeSet;

/**
 * An implementation of {@link SortedUnionFind} using a {@link HashMap} of {@link TreeSet}s. In
 * order to represent subsets by canonical elements, each one is mapped to its representative
 * canonical element. This is always the first element added to the subset, unless it has changed
 * due to union operations. The union is implemented as union by size.
 *
 * @param <T> type of elements added to the Union-Find. Must be {@link Comparable} to ensure correct
 *     ordering.
 */
public class SortedTreeSetUnionFind<
        T extends Comparable<T>, S extends TreeSet<T>, M extends HashMap<T, S>>
    extends AbstractGenericUnionFind<T, S, M> implements SortedUnionFind<T> {

  /** Generates an empty {@link SortedTreeSetUnionFind}. */
  @SuppressWarnings("unchecked")
  public SortedTreeSetUnionFind() {
    super((M) new HashMap<T, S>());
  }
}
