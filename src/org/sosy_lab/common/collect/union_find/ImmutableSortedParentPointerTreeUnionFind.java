// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect.union_find;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.annotations.Var;
import java.util.Collection;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.sosy_lab.common.collect.union_find.ParentPointerTreeUnionFind.UnionType;

/**
 * A sorted implementation of {@link UnionFind} using a {@link ImmutableMap} of each element to its
 * {@link AbstractTreeNode}. Each node contains a reference to its respective parent node, thus
 * resulting in a parent pointer tree structure for each subset. These are each represented by
 * canonical elements which are the root of each tree. This is always the first element added to the
 * subset, unless it has changed due to union operations. The union can be performed either by size
 * or by rank, * determined by a constructor parameter. The elements are stored in unsorted
 * structures, but {@code getAllSubsets()} returns a sorted view.
 *
 * @param <T> type of elements added to the Union-Find. Must be comparable.
 */
@Immutable(containerOf = "T")
public class ImmutableSortedParentPointerTreeUnionFind<T extends Comparable<T>>
    extends AbstractImmutableSortedUnionFind<T> {

  // tree nodes are not immutable but only used internally and never mutated after creation
  // immutable tree nodes would make conversion during build() difficult and time-consuming
  @SuppressWarnings("Immutable")
  private final ImmutableMap<T, AbstractTreeNode<T>> allNodes;

  /**
   * Only for internal use by the builder.
   *
   * @param pAllNodes finished immutable map storing all nodes contained in this Union-Find
   */
  protected ImmutableSortedParentPointerTreeUnionFind(
      ImmutableMap<T, AbstractTreeNode<T>> pAllNodes) {
    allNodes = pAllNodes;
  }

  /**
   * Returns the canonical element of the set containing the provided element.
   *
   * @param pE element for which set is to be found
   * @return canonical element of the found set
   * @throws IllegalArgumentException if element is not contained in any subset
   */
  @Override
  public T find(T pE) {

    Preconditions.checkNotNull(pE);

    @Var AbstractTreeNode<T> node = allNodes.get(pE);

    if (node != null) {
      @Var AbstractTreeNode<T> parent = node.getParent();

      while (!node.equals(parent)) {
        node = parent;
        parent = node.getParent();
      }

      return parent.getValue();
    }

    throw new IllegalArgumentException("Element not contained.");
  }

  /**
   * Provides a {@link Collection} containing all current subsets. The subsets are sorted by their
   * canonical elements in ascending order. The contents of each subset are equally sorted in
   * ascending order.
   *
   * @return {@link Collection} containing all current subsets
   */
  @Override
  public Collection<? extends NavigableSet<T>> getAllSubsets() {

    NavigableMap<T, NavigableSet<T>> allSubsets = new TreeMap<>();

    for (AbstractTreeNode<T> node : allNodes.values()) {

      T canon = find(node.getValue());

      if (allSubsets.containsKey(canon)) {
        allSubsets.get(canon).add(node.getValue());
      } else {
        NavigableSet<T> set = new TreeSet<>();
        set.add(node.getValue());
        allSubsets.put(canon, set);
      }
    }

    return allSubsets.values();
  }

  /**
   * Checks whether the provided element is contained in any current subset and returns true or
   * false accordingly.
   *
   * @param pE element to be searched for
   * @return true if contained, false if not
   */
  @Override
  public boolean contains(T pE) {

    return allNodes.containsKey(pE);
  }

  /**
   * Builder class which first collects the data in a mutable sorted Union-Find and converts it to
   * an immutable Union-Find when {@code build()} is called. See documentation in {@link
   * ParentPointerTreeUnionFind} for explanations on {@code union()}, {@code add()} and {@code
   * addAll()} as the methods in this builder simply pass to their aforementioned namesakes.
   *
   * @param <T> type of elements added to the Union-Find
   */
  public static final class Builder<T extends Comparable<T>>
      extends AbstractImmutableParentPointerTreeBuilder<T> {

    private Builder(UnionType pUnionType) {
      super(pUnionType);
    }

    public static <T extends Comparable<T>> Builder<T> getBuilder(UnionType pUnionType) {
      return new Builder<>(pUnionType);
    }

    @Override
    public ImmutableSortedParentPointerTreeUnionFind<T> build() {

      modificationsAllowed = false;

      return new ImmutableSortedParentPointerTreeUnionFind<>(
          ImmutableMap.copyOf(unionFind.allNodes));
    }
  }
}
