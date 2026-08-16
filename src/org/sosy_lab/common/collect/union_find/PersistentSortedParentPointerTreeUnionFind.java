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
import java.util.Collection;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.common.collect.union_find.ParentPointerTreeUnionFind.UnionType;

public final class PersistentSortedParentPointerTreeUnionFind<T extends Comparable<T>>
    extends AbstractImmutableSortedUnionFind<T> implements PersistentSortedUnionFind<T> {

  private final PersistentSortedMap<T, T> mapOfNodesToParents;
  private final PersistentSortedMap<T, Integer> mapOfRootsToWeights;
  private final UnionType unionType;

  private PersistentSortedParentPointerTreeUnionFind(UnionType pUnionType) {
    mapOfNodesToParents = PathCopyingPersistentTreeMap.of();
    mapOfRootsToWeights = PathCopyingPersistentTreeMap.of();
    unionType = pUnionType;
  }

  private PersistentSortedParentPointerTreeUnionFind(
      PersistentSortedMap<T, T> mapOfNodesToParents,
      PersistentSortedMap<T, Integer> mapOfRootsToWeights,
      UnionType unionType) {
    this.mapOfNodesToParents = mapOfNodesToParents;
    this.mapOfRootsToWeights = mapOfRootsToWeights;
    this.unionType = unionType;
  }

  public static <T extends Comparable<T>> AbstractImmutableSortedUnionFind<T> of(
      UnionType unionType) {
    return new PersistentSortedParentPointerTreeUnionFind<>(unionType);
  }

  @Override
  public Collection<? extends NavigableSet<T>> getAllSubsets() {

    NavigableMap<T, NavigableSet<T>> allSubsets = new TreeMap<>();

    for (T current : mapOfNodesToParents.keySet()) {

      T root = find(current);

      if (allSubsets.containsKey(root)) {
        allSubsets.get(root).add(current);
      } else {
        NavigableSet<T> set = new TreeSet<>();
        set.add(root);
        set.add(current);
        allSubsets.put(root, set);
      }
    }

    return allSubsets.values();
  }

  @Override
  public boolean contains(T e) {

    Preconditions.checkNotNull(e);

    return mapOfNodesToParents.containsKey(e);
  }

  @Override
  public T find(T e) {

    Preconditions.checkNotNull(e);

    @Var T currentNode = e;
    @Var T parent = mapOfNodesToParents.get(e);

    if (parent != null) {
      while (!currentNode.equals(parent)) {
        currentNode = parent;
        parent = mapOfNodesToParents.get(currentNode);
      }

      return parent;
    }

    throw new IllegalArgumentException("Element not contained.");
  }

  @Override
  public PersistentSortedUnionFind<T> unionAndCopy(T e1, T e2) {

    Preconditions.checkNotNull(e1);
    Preconditions.checkNotNull(e2);

    if (e1.equals(e2)) {
      return addElementAsNewSetAndCopy(e1);
    } else {
      if (contains(e1)) {
        if (contains(e2)) {
          T canon1 = find(e1);
          T canon2 = find(e2);

          if (!canon1.equals(canon2)) {
            return mergeExistingSetsAndCopy(canon1, canon2);
          }
        } else {
          return addElementToExistingSetAndCopy(e2, find(e1));
        }
      } else if (contains(e2)) {
        return addElementToExistingSetAndCopy(e1, find(e2));
      } else {
        return addTwoElementsAsSetAndCopy(e1, e2);
      }
    }

    return this;
  }

  private PersistentSortedUnionFind<T> addElementAsNewSetAndCopy(T e) {

    if (!contains(e)) {
      PersistentSortedMap<T, T> updatedNodesToParents = mapOfNodesToParents.putAndCopy(e, e);

      PersistentSortedMap<T, Integer> updatedRootsToWeights;
      if (unionType == UnionType.UNION_BY_RANK) {
        updatedRootsToWeights = mapOfRootsToWeights.putAndCopy(e, 0); // rank
      } else {
        updatedRootsToWeights = mapOfRootsToWeights.putAndCopy(e, 1); // size
      }

      return new PersistentSortedParentPointerTreeUnionFind<>(
          updatedNodesToParents, updatedRootsToWeights, unionType);
    }

    return this;
  }

  // only call with elements that are definitely canonical!
  private PersistentSortedUnionFind<T> mergeExistingSetsAndCopy(T canon1, T canon2) {

    Preconditions.checkNotNull(canon1);
    Preconditions.checkNotNull(canon2);

    if (unionType == UnionType.UNION_BY_SIZE) {
      return unionBySize(canon1, canon2);
    } else {
      return unionByRank(canon1, canon2);
    }
  }

  private PersistentSortedUnionFind<T> addElementToExistingSetAndCopy(T e, T canon) {

    Preconditions.checkNotNull(canon);

    PersistentSortedMap<T, T> updatedNodesToParents = mapOfNodesToParents.putAndCopy(e, canon);

    PersistentSortedMap<T, Integer> updatedRootsToWeights;
    if (unionType == UnionType.UNION_BY_RANK) {
      @Var int rank = mapOfRootsToWeights.get(canon);

      if (rank == 0) {
        updatedRootsToWeights = mapOfRootsToWeights.putAndCopy(canon, ++rank);
      } else {
        updatedRootsToWeights = mapOfRootsToWeights;
      }
    } else {
      @Var int size = mapOfRootsToWeights.get(canon);
      updatedRootsToWeights = mapOfRootsToWeights.putAndCopy(canon, ++size);
    }

    return new PersistentSortedParentPointerTreeUnionFind<>(
        updatedNodesToParents, updatedRootsToWeights, unionType);
  }

  private PersistentSortedUnionFind<T> addTwoElementsAsSetAndCopy(T e1, T e2) {

    @Var PersistentSortedMap<T, T> updatedNodesToParents;
    updatedNodesToParents = mapOfNodesToParents.putAndCopy(e1, e1);
    updatedNodesToParents = updatedNodesToParents.putAndCopy(e2, e1);

    PersistentSortedMap<T, Integer> updatedRootsToWeights;
    if (unionType == UnionType.UNION_BY_RANK) {
      updatedRootsToWeights = mapOfRootsToWeights.putAndCopy(e1, 1); // rank
    } else {
      updatedRootsToWeights = mapOfRootsToWeights.putAndCopy(e1, 2); // size
    }

    return new PersistentSortedParentPointerTreeUnionFind<>(
        updatedNodesToParents, updatedRootsToWeights, unionType);
  }

  // canon1 will be new canonical element only if its set is actually bigger, otherwise canon2 new
  // canon
  private PersistentSortedUnionFind<T> unionBySize(T canon1, T canon2) {

    int size1 = mapOfRootsToWeights.get(canon1);
    int size2 = mapOfRootsToWeights.get(canon2);

    PersistentSortedMap<T, T> updatedNodesToParents;
    PersistentSortedMap<T, Integer> updatedRootsToWeights;

    if (size1 > size2) {
      updatedNodesToParents = mapOfNodesToParents.putAndCopy(canon2, canon1);

      updatedRootsToWeights = mapOfRootsToWeights.putAndCopy(canon1, size1 + size2);
    } else {
      updatedNodesToParents = mapOfNodesToParents.putAndCopy(canon1, canon2);

      updatedRootsToWeights = mapOfRootsToWeights.putAndCopy(canon2, size2 + size1);
    }

    return new PersistentSortedParentPointerTreeUnionFind<>(
        updatedNodesToParents, updatedRootsToWeights, unionType);
  }

  // canon1 will be new canonical element only if its rank is actually greater, otherwise canon2 new
  // canon
  private PersistentSortedUnionFind<T> unionByRank(T canon1, T canon2) {

    int rank1 = mapOfRootsToWeights.get(canon1);
    @Var int rank2 = mapOfRootsToWeights.get(canon2);

    PersistentSortedMap<T, T> updatedNodesToParents;
    PersistentSortedMap<T, Integer> updatedRootsToWeights;

    if (rank1 > rank2) {
      updatedNodesToParents = mapOfNodesToParents.putAndCopy(canon2, canon1);

      updatedRootsToWeights = mapOfRootsToWeights;
    } else {
      updatedNodesToParents = mapOfNodesToParents.putAndCopy(canon1, canon2);

      if (rank1 == rank2) {
        updatedRootsToWeights = mapOfRootsToWeights.putAndCopy(canon2, ++rank2);
      } else {
        updatedRootsToWeights = mapOfRootsToWeights;
      }
    }

    return new PersistentSortedParentPointerTreeUnionFind<>(
        updatedNodesToParents, updatedRootsToWeights, unionType);
  }
}
