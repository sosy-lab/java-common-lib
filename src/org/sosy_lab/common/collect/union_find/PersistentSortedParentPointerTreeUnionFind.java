// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect.union_find;

import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.annotations.Var;
import java.util.Collection;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.common.collect.union_find.ParentPointerTreeUnionFind.UnionType;

/**
 * Implementation of a persistent and sorted union-find. A persistent data structure is immutable,
 * but provides cheap copy-and-write operations. Thus, all write operations ({@link
 * #union(Comparable, Comparable)}) will not modify the current instance, but return a new instance
 * instead. The union can be performed either by size or by rank, determined by a constructor
 * parameter.
 *
 * <p>All modifying operations inherited from {@link SortedUnionFind} are not supported and will
 * always throw {@link UnsupportedOperationException}.
 *
 * @param <T> The type of elements added to the Union-Find. Must be comparable.
 */
@Immutable(containerOf = "T")
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
      PersistentSortedMap<T, T> pMapOfNodesToParents,
      PersistentSortedMap<T, Integer> pMapOfRootsToWeights,
      UnionType pUnionType) {
    mapOfNodesToParents = pMapOfNodesToParents;
    mapOfRootsToWeights = pMapOfRootsToWeights;
    unionType = pUnionType;
  }

  /**
   * Returns a fresh, empty Union-Find instance of the given union type.
   *
   * @param pUnionType specifies whether the union is performed by rank or by size
   * @return empty instance
   * @param <T> type of elements added to the Union-Find. Must be comparable.
   */
  public static <T extends Comparable<T>> PersistentSortedUnionFind<T> of(UnionType pUnionType) {
    return new PersistentSortedParentPointerTreeUnionFind<>(pUnionType);
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

  /**
   * Checks whether the provided element is contained in any current subset and returns true or
   * false accordingly.
   *
   * @param pE element to be searched for
   * @return true if contained, false if not
   */
  @Override
  public boolean contains(T pE) {

    Preconditions.checkNotNull(pE);

    return mapOfNodesToParents.containsKey(pE);
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

    @Var T currentNode = pE;
    @Var T parent = mapOfNodesToParents.get(pE);

    if (parent != null) {
      while (!currentNode.equals(parent)) {
        currentNode = parent;
        parent = mapOfNodesToParents.get(currentNode);
      }

      return parent;
    }

    throw new IllegalArgumentException("Element not contained.");
  }

  /**
   * Merges the sets represented by the two input values according to standard Union-Find behaviour.
   * This operation does not mutate the existing object, but returns a fresh instance to which the
   * changes in question have been applied.
   *
   * <p>USES: Add new element as new set: pass it as both pE1 and pE2. Add new element to existing
   * set: one input value is the new element, the other the canonical element of the set to be added
   * to. Merge two existing sets: pE1, pE2 canonical elements of sets to be merged.
   *
   * @param pE1 first element
   * @param pE2 second element
   */
  @CheckReturnValue
  @Override
  public PersistentSortedUnionFind<T> unionAndCopy(T pE1, T pE2) {

    Preconditions.checkNotNull(pE1);
    Preconditions.checkNotNull(pE2);

    if (pE1.equals(pE2)) {
      return addElementAsNewSetAndCopy(pE1);
    } else {
      if (contains(pE1)) {
        if (contains(pE2)) {
          T canon1 = find(pE1);
          T canon2 = find(pE2);

          if (!canon1.equals(canon2)) {
            return mergeExistingSetsAndCopy(canon1, canon2);
          }
        } else {
          return addElementToExistingSetAndCopy(pE2, find(pE1));
        }
      } else if (contains(pE2)) {
        return addElementToExistingSetAndCopy(pE1, find(pE2));
      } else {
        return addTwoElementsAsSetAndCopy(pE1, pE2);
      }
    }

    return this;
  }

  private PersistentSortedUnionFind<T> addElementAsNewSetAndCopy(T pE) {

    if (!contains(pE)) {
      PersistentSortedMap<T, T> updatedNodesToParents = mapOfNodesToParents.putAndCopy(pE, pE);

      PersistentSortedMap<T, Integer> updatedRootsToWeights;
      if (unionType == UnionType.UNION_BY_RANK) {
        updatedRootsToWeights = mapOfRootsToWeights.putAndCopy(pE, 0); // rank
      } else {
        updatedRootsToWeights = mapOfRootsToWeights.putAndCopy(pE, 1); // size
      }

      return new PersistentSortedParentPointerTreeUnionFind<>(
          updatedNodesToParents, updatedRootsToWeights, unionType);
    }

    return this;
  }

  // only call with elements that are definitely canonical!
  private PersistentSortedUnionFind<T> mergeExistingSetsAndCopy(T pCanon1, T pCanon2) {

    Preconditions.checkNotNull(pCanon1);
    Preconditions.checkNotNull(pCanon2);

    if (unionType == UnionType.UNION_BY_SIZE) {
      return unionBySize(pCanon1, pCanon2);
    } else {
      return unionByRank(pCanon1, pCanon2);
    }
  }

  private PersistentSortedUnionFind<T> addElementToExistingSetAndCopy(T pE, T pCanon) {

    Preconditions.checkNotNull(pCanon);

    PersistentSortedMap<T, T> updatedNodesToParents = mapOfNodesToParents.putAndCopy(pE, pCanon);

    PersistentSortedMap<T, Integer> updatedRootsToWeights;
    if (unionType == UnionType.UNION_BY_RANK) {
      @Var int rank = mapOfRootsToWeights.get(pCanon);

      if (rank == 0) {
        updatedRootsToWeights = mapOfRootsToWeights.putAndCopy(pCanon, ++rank);
      } else {
        updatedRootsToWeights = mapOfRootsToWeights;
      }
    } else {
      @Var int size = mapOfRootsToWeights.get(pCanon);
      updatedRootsToWeights = mapOfRootsToWeights.putAndCopy(pCanon, ++size);
    }

    return new PersistentSortedParentPointerTreeUnionFind<>(
        updatedNodesToParents, updatedRootsToWeights, unionType);
  }

  private PersistentSortedUnionFind<T> addTwoElementsAsSetAndCopy(T pE1, T pE2) {

    @Var PersistentSortedMap<T, T> updatedNodesToParents;
    updatedNodesToParents = mapOfNodesToParents.putAndCopy(pE1, pE1);
    updatedNodesToParents = updatedNodesToParents.putAndCopy(pE2, pE1);

    PersistentSortedMap<T, Integer> updatedRootsToWeights;
    if (unionType == UnionType.UNION_BY_RANK) {
      updatedRootsToWeights = mapOfRootsToWeights.putAndCopy(pE1, 1); // rank
    } else {
      updatedRootsToWeights = mapOfRootsToWeights.putAndCopy(pE1, 2); // size
    }

    return new PersistentSortedParentPointerTreeUnionFind<>(
        updatedNodesToParents, updatedRootsToWeights, unionType);
  }

  // pCanon1 will be new canonical element only if its set is actually bigger, otherwise pCanon2 new
  // canon
  private PersistentSortedUnionFind<T> unionBySize(T pCanon1, T pCanon2) {

    int size1 = mapOfRootsToWeights.get(pCanon1);
    int size2 = mapOfRootsToWeights.get(pCanon2);

    PersistentSortedMap<T, T> updatedNodesToParents;
    PersistentSortedMap<T, Integer> updatedRootsToWeights;

    if (size1 > size2) {
      updatedNodesToParents = mapOfNodesToParents.putAndCopy(pCanon2, pCanon1);

      updatedRootsToWeights = mapOfRootsToWeights.putAndCopy(pCanon1, size1 + size2);
    } else {
      updatedNodesToParents = mapOfNodesToParents.putAndCopy(pCanon1, pCanon2);

      updatedRootsToWeights = mapOfRootsToWeights.putAndCopy(pCanon2, size2 + size1);
    }

    return new PersistentSortedParentPointerTreeUnionFind<>(
        updatedNodesToParents, updatedRootsToWeights, unionType);
  }

  // pCanon1 will be new canonical element only if its rank is actually greater, otherwise pCanon2
  // new
  // canon
  private PersistentSortedUnionFind<T> unionByRank(T pCanon1, T pCanon2) {

    int rank1 = mapOfRootsToWeights.get(pCanon1);
    @Var int rank2 = mapOfRootsToWeights.get(pCanon2);

    PersistentSortedMap<T, T> updatedNodesToParents;
    PersistentSortedMap<T, Integer> updatedRootsToWeights;

    if (rank1 > rank2) {
      updatedNodesToParents = mapOfNodesToParents.putAndCopy(pCanon2, pCanon1);

      updatedRootsToWeights = mapOfRootsToWeights;
    } else {
      updatedNodesToParents = mapOfNodesToParents.putAndCopy(pCanon1, pCanon2);

      if (rank1 == rank2) {
        updatedRootsToWeights = mapOfRootsToWeights.putAndCopy(pCanon2, ++rank2);
      } else {
        updatedRootsToWeights = mapOfRootsToWeights;
      }
    }

    return new PersistentSortedParentPointerTreeUnionFind<>(
        updatedNodesToParents, updatedRootsToWeights, unionType);
  }
}
