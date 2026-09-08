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
   * @param pUnionType type of union to be performed for all unions on this instance
   */
  public ParentPointerTreeUnionFind(UnionType pUnionType) {
    allNodes = new HashMap<>();
    unionType = pUnionType;
  }

  /**
   * Returns the canonical element of the set containing the provided element. Applies path
   * compression where possible.
   *
   * @param pE element for which set is to be found
   * @return canonical element of the found set
   * @throws IllegalArgumentException if element is not contained in any subset
   */
  @Override
  public T find(T pE) {

    Preconditions.checkNotNull(pE);

    List<AbstractTreeNode<T>> toBeCompressed = new ArrayList<>();
    @Var AbstractTreeNode<T> node = allNodes.get(pE);

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
   * <p>USES: Add new element as new set: pass it as both pE1 and pE2. Add new element to existing
   * set: one input value is the new element, the other the canonical element of the set to be added
   * to. Merge two existing sets: pE1, pE2 canonical elements of sets to be merged.
   *
   * @param pE1 first element
   * @param pE2 second element
   */
  @Override
  public void union(T pE1, T pE2) {

    Preconditions.checkNotNull(pE1);
    Preconditions.checkNotNull(pE2);

    if (pE1.equals(pE2)) {
      addElementAsNewSet(pE1);
    } else {
      if (contains(pE1)) {
        if (contains(pE2)) {
          T canon1 = find(pE1);
          T canon2 = find(pE2);

          if (!canon1.equals(canon2)) {
            mergeExistingSets(findNode(canon1), findNode(canon2));
          }
        } else {
          addElementToExistingSet(pE2, findNode(pE1));
        }
      } else if (contains(pE2)) {
        addElementToExistingSet(pE1, findNode(pE2));
      } else {
        addElementAsNewSet(pE1);
        addElementToExistingSet(pE2, findNode(pE1));
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
   * @param pE element to be searched for
   * @return true if contained, false if not
   */
  @Override
  public boolean contains(T pE) {

    return allNodes.containsKey(pE);
  }

  /**
   * Adds the contents of a set of elements to the Union-Find. A random element in the set (the
   * first one accessed) will be used as the canonical element. If the set contains elements already
   * found elsewhere in the Union-Find, these sets will be merged accordingly.
   *
   * @param pSet set to be added to the Union-Find
   */
  public void add(Set<T> pSet) {

    Preconditions.checkNotNull(pSet);

    @Var T canon = null;

    for (T current : pSet) {

      if (canon == null) {
        canon = current;
        addElementAsNewSet(canon);
      }

      addElementToExistingSet(current, findNode(canon));
    }
  }

  /**
   * Adds multiple sets of elements to the Union-Find. For each set, a random element in the set
   * (the first one accessed) will be used as the canonical element. If a set contains elements
   * already found elsewhere in the Union-Find, these sets will be merged accordingly.
   *
   * @param pSets sets to be added to the Union-Find
   */
  public void addAll(Collection<Set<T>> pSets) {

    Preconditions.checkNotNull(pSets);

    for (Set<T> set : pSets) {
      add(set);
    }
  }

  private void addElementAsNewSet(T pE) {

    if (!contains(pE)) {
      RootNode<T> root = new RootNode<>(pE);
      allNodes.put(pE, root);
    }
  }

  // only call with elements that are definitely canonical!
  private void mergeExistingSets(RootNode<T> pCanon1, RootNode<T> pCanon2) {

    Preconditions.checkNotNull(pCanon1);
    Preconditions.checkNotNull(pCanon2);

    if (unionType == UnionType.UNION_BY_SIZE) {
      unionBySize(pCanon1, pCanon2);
    } else {
      unionByRank(pCanon1, pCanon2);
    }
  }

  private void addElementToExistingSet(T pE, RootNode<T> pCanon) {

    NonRootNode<T> newNode = new NonRootNode<>(pCanon, pE);
    pCanon.incrementSizeByOne();

    if (pCanon.getRank() == 0) {
      pCanon.incrementRankByOne();
    }

    allNodes.put(pE, newNode);
  }

  // pCanon1 will be new canonical element only if its set is actually bigger, otherwise pCanon2 new
  // canon
  private void unionBySize(RootNode<T> pCanon1, RootNode<T> pCanon2) {

    int size1 = pCanon1.getSize();
    int size2 = pCanon2.getSize();

    if (size1 > size2) {
      pCanon2.setParent(pCanon1);
      pCanon1.incrementSizeBy(pCanon2.getSize());
    } else {
      pCanon1.setParent(pCanon2);
      pCanon2.incrementSizeBy(pCanon1.getSize());
    }
  }

  // pCanon1 will be new canonical element only if its rank is actually greater, otherwise pCanon2
  // new
  // canon
  private void unionByRank(RootNode<T> pCanon1, RootNode<T> pCanon2) {

    int rank1 = pCanon1.getRank();
    int rank2 = pCanon2.getRank();

    if (rank1 > rank2) {
      pCanon2.setParent(pCanon1);
    } else {
      pCanon1.setParent(pCanon2);

      // as rank only changes if both ranks are the same
      if (rank1 == rank2) {
        pCanon2.incrementRankByOne();
      }
    }
  }

  // like find; returns node of canonical element of the set pE belongs to, not value
  private RootNode<T> findNode(T pE) {

    Preconditions.checkNotNull(pE);

    List<AbstractTreeNode<T>> toBeCompressed = new ArrayList<>();
    @Var AbstractTreeNode<T> node = allNodes.get(pE);

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

      return (RootNode<T>) parent;
    }

    throw new IllegalArgumentException("Element not contained.");
  }
}
