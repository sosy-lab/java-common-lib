// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect.union_find;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.Immutable;
import java.util.Collection;
import java.util.Set;
import org.sosy_lab.common.collect.union_find.ParentPointerTreeUnionFind.UnionType;

/**
 * Abstract builder class which first collects the data in a mutable Union-Find and converts it to
 * an immutable Union-Find when {@code build()} is called. From then onward, previously modifying
 * methods will not cause further modifications to the Union-Find instance inside. See documentation
 * in {@link ParentPointerTreeUnionFind} for explanations on {@code union()}, {@code add()} and
 * {@code addAll()} as the methods in this builder simply pass to their aforementioned namesakes.
 *
 * @param <T> type of elements added to the Union-Find
 */
@Immutable(containerOf = "T")
public abstract class AbstractImmutableParentPointerTreeBuilder<T> {

  // union-find not immutable but only used internally and never mutated passed outward
  // build() returns an immutable union-find that contains a copy of this union-find's map
  @SuppressWarnings("Immutable")
  final ParentPointerTreeUnionFind<T> unionFind;

  // prevents further modifications after build()
  // is never modified once having been switched to false
  @SuppressWarnings("Immutable")
  boolean modificationsAllowed;

  protected AbstractImmutableParentPointerTreeBuilder(UnionType pUnionType) {
    unionFind = new ParentPointerTreeUnionFind<>(pUnionType);
    modificationsAllowed = true;
  }

  @CanIgnoreReturnValue
  public AbstractImmutableParentPointerTreeBuilder<T> union(T pE1, T pE2) {

    if (modificationsAllowed) {

      unionFind.union(pE1, pE2);
    }

    return this;
  }

  @CanIgnoreReturnValue
  public AbstractImmutableParentPointerTreeBuilder<T> add(Set<T> pSet) {

    if (modificationsAllowed) {

      unionFind.add(pSet);
    }

    return this;
  }

  @CanIgnoreReturnValue
  public AbstractImmutableParentPointerTreeBuilder<T> addAll(Collection<Set<T>> pSets) {

    if (modificationsAllowed) {

      unionFind.addAll(pSets);
    }

    return this;
  }

  // get map from mutable Union-Find instance and convert to immutable map, then pass to constructor
  // set modificationsAllowed to false!!
  public abstract AbstractImmutableUnionFind<T> build();
}
