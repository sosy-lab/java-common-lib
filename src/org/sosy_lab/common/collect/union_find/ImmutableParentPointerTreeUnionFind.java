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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.collect.union_find.ParentPointerTreeUnionFind.UnionType;

public class ImmutableParentPointerTreeUnionFind<T> extends AbstractImmutableUnionFind<T> {

  private final ImmutableMap<T, AbstractTreeNode<T>> allNodes;

  protected ImmutableParentPointerTreeUnionFind(ImmutableMap<T, AbstractTreeNode<T>> allNodes) {
    this.allNodes = allNodes;
  }

  @Override
  public T find(T e) {

    Preconditions.checkNotNull(e);

    @Var AbstractTreeNode<T> node = allNodes.get(e);

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
  public Collection<? extends Set<T>> getAllSubsets() {

    Map<T, Set<T>> allSubsets = new HashMap<>();

    for (AbstractTreeNode<T> node : allNodes.values()) {

      T canon = find(node.getValue());

      if (allSubsets.containsKey(canon)) {
        allSubsets.get(canon).add(node.getValue());
      } else {
        Set<T> set = new HashSet<>();
        set.add(node.getValue());
        allSubsets.put(canon, set);
      }
    }

    return allSubsets.values();
  }

  @Override
  public boolean contains(T e) {

    return allNodes.containsKey(e);
  }

  public static final class Builder<T> {

    ParentPointerTreeUnionFind<T> unionFind;

    private Builder(UnionType unionType) {
      unionFind = new ParentPointerTreeUnionFind<>(unionType);
    }

    public static <T> Builder<T> getBuilder(UnionType unionType) {
      return new Builder<>(unionType);
    }

    @CanIgnoreReturnValue
    public Builder<T> union(T value1, T value2) {

      unionFind.union(value1, value2);

      return this;
    }

    public ImmutableParentPointerTreeUnionFind<T> build() {
      return new ImmutableParentPointerTreeUnionFind<>(ImmutableMap.copyOf(unionFind.allNodes));
    }
  }
}
