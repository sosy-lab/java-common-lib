// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect.union_find;

import com.google.errorprone.annotations.Immutable;

/**
 * An abstract class for sorted immutable Union-Find implementations.
 *
 * @param <T> type of elements added to the Union-Find. Must be comparable.
 */
@Immutable(containerOf = "T")
public abstract class AbstractImmutableSortedUnionFind<T extends Comparable<T>>
    extends AbstractImmutableUnionFind<T> implements SortedUnionFind<T> {}
