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
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.Var;
import java.util.Collection;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.sosy_lab.common.collect.union_find.ParentPointerTreeUnionFind.UnionType;

public class ImmutableSortedParentPointerTreeUnionFind<T extends Comparable<T>>
    extends AbstractImmutableSortedUnionFind<T> {

  private final ImmutableMap<T, AbstractTreeNode<T>> allNodes;

  protected ImmutableSortedParentPointerTreeUnionFind(
      ImmutableMap<T, AbstractTreeNode<T>> pAllNodes) {
    allNodes = pAllNodes;
  }

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

  @Override
  public boolean contains(T pE) {

    return allNodes.containsKey(pE);
  }

  public static final class Builder<T extends Comparable<T>> {

    SortedParentPointerTreeUnionFind<T> unionFind;

    private Builder(UnionType pUnionType) {
      unionFind = new SortedParentPointerTreeUnionFind<>(pUnionType);
    }

    public static <T extends Comparable<T>> Builder<T> getBuilder(UnionType pUnionType) {
      return new Builder<>(pUnionType);
    }

    @CanIgnoreReturnValue
    public ImmutableSortedParentPointerTreeUnionFind.Builder<T> union(T pE1, T pE2) {

      unionFind.union(pE1, pE2);

      return this;
    }

    public ImmutableParentPointerTreeUnionFind<T> build() {
      return new ImmutableParentPointerTreeUnionFind<>(ImmutableMap.copyOf(unionFind.allNodes));
    }
  }
}
