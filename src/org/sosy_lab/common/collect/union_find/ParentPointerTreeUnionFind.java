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

/**
 * An implementation of {@link UnionFind} using a {@link Map} of {@link ParentPointerTree}s. In
 * order to represent subsets (the trees) by canonical elements, each one is mapped to its
 * representative canonical element. This is always the first element added to the subset, unless it
 * has changed due to union operations. The union is implemented as union by size.
 *
 * @param <T> type of elements added to the Union-Find.
 */
public class ParentPointerTreeUnionFind<T> implements UnionFind<T> {

  protected final Map<T, ParentPointerTree<T>> forest;

  /** Creates an empty instance. */
  public ParentPointerTreeUnionFind() {
    forest = new HashMap<>();
  }

  /**
   * Returns the canonical element of the set containing the provided element.
   *
   * @param value element for which set is to be found
   * @return canonical element of the found set
   * @throws IllegalArgumentException if element is not contained in any subset
   */
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

  /**
   * Merges the sets represented by the two input values according to standard Union-Find behaviour.
   *
   * <p>USES: Add new element as new set: pass it as both value1 and value2. Add new element to
   * existing set: one input value is the new element, the other the canonical element of the set to
   * be added to. Merge two existing sets: value1, value2 canonical elements of sets to be merged.
   *
   * @param value1 first element
   * @param value2 second element
   */
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

  /**
   * Provides a {@link Collection} containing all current subsets.
   *
   * @return {@link Collection} containing all current subsets
   */
  @Override
  public Collection<? extends Set<T>> getAllSubsets() {

    List<Set<T>> allSubsets = new ArrayList<>();

    for (ParentPointerTree<T> tree : forest.values()) {
      allSubsets.add(tree.getSetOfNodeValues());
    }

    return allSubsets;
  }

  /**
   * Checks whether the provided element is contained in any current subset and returns true or
   * false accordingly.
   *
   * @param e element to be searched for
   * @return true if contained, false if not
   */
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
