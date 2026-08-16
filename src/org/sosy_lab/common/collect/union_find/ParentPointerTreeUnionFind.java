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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An implementation of {@link UnionFind} using a {@link Map} of {@link AbstractTreeNode}s. In order
 * to represent subsets by canonical elements, each one is mapped to its representative canonical
 * element. This is always the first element added to the subset, unless it has changed due to union
 * operations. Each subset is stored as a parent pointer tree comprised of {@link NonRootNode}s with
 * exactly one {@link RootNode} as the root. The union can be performed either by size or by rank,
 * determined by a constructor parameter.
 *
 * @param <T> type of elements added to the Union-Find.
 */
public class ParentPointerTreeUnionFind<T> implements UnionFind<T> {

  public enum UnionType {
    UNION_BY_RANK,
    UNION_BY_SIZE
  }

  protected final Map<T, AbstractTreeNode<T>> allNodes;
  private final UnionType unionType;

  /**
   * Creates an empty instance.
   *
   * @param unionType type of union to be performed for all unions on this instance
   */
  public ParentPointerTreeUnionFind(UnionType unionType) {
    allNodes = new HashMap<>();
    this.unionType = unionType;
  }

  /**
   * Returns the canonical element of the set containing the provided element. Applies path
   * compression where possible.
   *
   * @param value element for which set is to be found
   * @return canonical element of the found set
   * @throws IllegalArgumentException if element is not contained in any subset
   */
  @Override
  public T find(T value) {

    Preconditions.checkNotNull(value);

    List<AbstractTreeNode<T>> toBeCompressed = new ArrayList<>();
    @Var AbstractTreeNode<T> node = allNodes.get(value);

    if (node != null) {
      @Var AbstractTreeNode<T> parent = node.getParent();

      while (!node.equals(parent)) {
        toBeCompressed.add(node);
        node = parent;
        parent = node.getParent();
      }

      for (AbstractTreeNode<T> current : toBeCompressed) {

        current.setParent(parent);
      }

      return parent.getValue();
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
            mergeExistingSets(canon1, canon2);
          }
        } else {
          addElementToExistingSet(value2, find(value1));
        }
      } else if (contains(value2)) {
        addElementToExistingSet(value1, find(value2));
      } else {
        addElementAsNewSet(value1);
        addElementToExistingSet(value2, find(value1));
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

    Map<T, Set<T>> allSubsets = new HashMap<>();

    for (AbstractTreeNode<T> node : allNodes.values()) {

      T canon = find(node.getValue());

      if (allSubsets.containsKey(canon)) {
        allSubsets.get(canon).add(node.getValue());
      } else {
        Set<T> set = new HashSet<>();
        set.add(canon);
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
   * @param e element to be searched for
   * @return true if contained, false if not
   */
  @Override
  public boolean contains(T e) {

    return allNodes.containsKey(e);
  }

  private void addElementAsNewSet(T value) {

    if (!contains(value)) {
      RootNode<T> root = new RootNode<>(value);
      allNodes.put(value, root);
    }
  }

  // only call with elements that are definitely canonical!
  private void mergeExistingSets(T canon1, T canon2) {

    Preconditions.checkNotNull(canon1);
    Preconditions.checkNotNull(canon2);

    if (unionType == UnionType.UNION_BY_SIZE) {
      unionBySize(canon1, canon2);
    } else {
      unionByRank(canon1, canon2);
    }
  }

  private void addElementToExistingSet(T value, T canon) {

    RootNode<T> root = (RootNode<T>) allNodes.get(canon);
    NonRootNode<T> newNode = new NonRootNode<>(root, value);
    root.incrementSizeByOne();

    if (root.getRank() == 0) {
      root.incrementRankByOne();
    }

    allNodes.put(value, newNode);
  }

  // canon1 will be new canonical element only if its set is actually bigger, otherwise canon2 new
  // canon
  private void unionBySize(T canon1, T canon2) {

    RootNode<T> rootNode1 = (RootNode<T>) allNodes.get(canon1);
    RootNode<T> rootNode2 = (RootNode<T>) allNodes.get(canon2);

    int size1 = rootNode1.getSize();
    int size2 = rootNode2.getSize();

    if (size1 > size2) {
      rootNode2.setParent(rootNode1);
      rootNode1.incrementSizeBy(rootNode2.getSize());
    } else {
      rootNode1.setParent(rootNode2);
      rootNode2.incrementSizeBy(rootNode1.getSize());
    }
  }

  // canon1 will be new canonical element only if its rank is actually greater, otherwise canon2 new
  // canon
  private void unionByRank(T canon1, T canon2) {

    RootNode<T> rootNode1 = (RootNode<T>) allNodes.get(canon1);
    RootNode<T> rootNode2 = (RootNode<T>) allNodes.get(canon2);

    int rank1 = rootNode1.getRank();
    int rank2 = rootNode2.getRank();

    if (rank1 > rank2) {
      rootNode2.setParent(rootNode1);
    } else {
      rootNode1.setParent(rootNode2);

      // as rank only changes if both ranks are the same
      if (rank1 == rank2) {
        rootNode2.incrementRankByOne();
      }
    }
  }
}
