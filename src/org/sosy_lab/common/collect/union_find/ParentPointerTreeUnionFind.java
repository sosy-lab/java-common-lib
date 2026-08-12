// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect.union_find;

import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.Var;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ParentPointerTreeUnionFind<T> implements UnionFind<T> {

  private final Map<T, ParentPointerTree<T>> forest;

  public ParentPointerTreeUnionFind() {
    forest = new HashMap<>();
  }

  @Override
  public T find(T value) {

    Preconditions.checkNotNull(value);

    for (Entry<T, ParentPointerTree<T>> entry : forest.entrySet()) {
      if (entry.getValue().contains(value)) {
        return entry.getKey();
      }
    }

    throw new IllegalArgumentException("Element not contained.");
  }

  @Override
  public void union(T value1, T value2) {

    Preconditions.checkNotNull(value1);
    Preconditions.checkNotNull(value2);

    if (value1.equals(value2)) {
      addElementAsNewSet(value1);
    } else {
      if (contains(value1)) {
        if (contains(value2)) {
          T canon1 = find(value1);
          T canon2 = find(value2);

          if (!canon1.equals(canon2)) {
            mergeExistingSets(find(value1), find(value2));
          }
        } else {
          addElementToExistingSet(value2, find(value1));
        }
      } else if (contains(value2)) {
        addElementToExistingSet(value1, find(value2));
      } else {
        addElementAsNewSet(value1);
        addElementToExistingSet(value2, value1);
      }
    }
  }

  @Override
  public Collection<? extends Set<T>> getAllSubsets() {

    List<Set<T>> allSubsets = new ArrayList<>();

    for (ParentPointerTree<T> tree : forest.values()) {
      allSubsets.add(tree.getSetOfNodeValues());
    }

    return allSubsets;
  }

  @Override
  public boolean contains(T e) {

    for (ParentPointerTree<T> tree : forest.values()) {
      if (tree.contains(e)) {
        return true;
      }
    }

    return false;
  }

  private void addElementAsNewSet(T value) {

    if (!contains(value)) {
      ParentPointerTree<T> tree = new ParentPointerTree<>(value);
      forest.put(value, tree);
    }
  }

  // canon1 will be new canonical element only if its set is actually bigger, otherwise canon2 new
  // canon
  private void mergeExistingSets(T canon1, T canon2) {

    @Var ParentPointerTree<T> tree1 = null;
    @Var ParentPointerTree<T> tree2 = null;

    for (Entry<T, ParentPointerTree<T>> entry : forest.entrySet()) {
      if (entry.getKey().equals(canon1)) {
        tree1 = entry.getValue();

        if (tree2 != null) {
          break;
        }
      } else if (entry.getKey().equals(canon2)) {
        tree2 = entry.getValue();

        if (tree1 != null) {
          break;
        }
      }
    }

    Preconditions.checkNotNull(tree1);
    Preconditions.checkNotNull(tree2);

    if (tree1.getSize() > tree2.getSize()) {
      assert tree1.appendTree(tree2);
      assert forest.remove(canon2, tree2);
    } else {
      assert tree2.appendTree(tree1);
      assert forest.remove(canon1, tree1);
    }
  }

  private void addElementToExistingSet(T value, T canon) {
    forest.get(canon).addAsNewNode(value);
  }
}
