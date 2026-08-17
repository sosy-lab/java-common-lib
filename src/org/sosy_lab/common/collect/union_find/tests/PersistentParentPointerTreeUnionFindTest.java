// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect.union_find.tests;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Set;
import org.junit.Test;
import org.sosy_lab.common.collect.union_find.ParentPointerTreeUnionFind.UnionType;
import org.sosy_lab.common.collect.union_find.PersistentParentPointerTreeUnionFind;
import org.sosy_lab.common.collect.union_find.PersistentSortedParentPointerTreeUnionFind;
import org.sosy_lab.common.collect.union_find.PersistentSortedUnionFind;
import org.sosy_lab.common.collect.union_find.PersistentUnionFind;
import org.sosy_lab.common.collect.union_find.UnionFind;

public class PersistentParentPointerTreeUnionFindTest {

  private final int[] simpleUnionArgs = new int[] {0, 1};

  private static PersistentUnionFind<Integer> emptyPersistentUnionFind(UnionType pUnionType) {
    return PersistentParentPointerTreeUnionFind.of(pUnionType);
  }

  private static PersistentSortedUnionFind<Integer> emptyPersistentSortedUnionFind(
      UnionType pUnionType) {
    return PersistentSortedParentPointerTreeUnionFind.of(pUnionType);
  }

  private static PersistentUnionFind<Integer> applyUnions(UnionType pUnionType, int[]... pUnions) {

    PersistentUnionFind<Integer> unionFind = emptyPersistentUnionFind(pUnionType);

    for (int[] pair : pUnions) {
      unionFind = unionFind.unionAndCopy(pair[0], pair[1]);
    }

    return unionFind;
  }

  private static PersistentSortedUnionFind<Integer> applySortedUnions(
      UnionType pUnionType, int[]... pUnions) {

    PersistentSortedUnionFind<Integer> unionFind = emptyPersistentSortedUnionFind(pUnionType);

    for (int[] pair : pUnions) {
      unionFind = unionFind.unionAndCopy(pair[0], pair[1]);
    }

    return unionFind;
  }

  private static ImmutableList<UnionFind<Integer>> bothVariants(
      UnionType pUnionType, int[]... pUnions) {

    return ImmutableList.of(
        applyUnions(pUnionType, pUnions), applySortedUnions(pUnionType, pUnions));
  }

  @Test
  public void testFind_afterSelfUnion_returnsItself() {

    int[] unionArgs = new int[] {0, 0};

    for (UnionFind<Integer> unionFind : bothVariants(UnionType.UNION_BY_SIZE, unionArgs)) {

      assertThat(unionFind.find(0)).isEqualTo(0);
    }
  }

  @Test
  public void testFind_null_throwsNullPointerException() {

    for (UnionFind<Integer> unionFind : bothVariants(UnionType.UNION_BY_SIZE, simpleUnionArgs)) {

      assertThrows(NullPointerException.class, () -> unionFind.find(null));
    }
  }

  @Test
  public void testFind_elementNotContained_throwsIllegalArgumentException() {

    for (UnionFind<Integer> unionFind : bothVariants(UnionType.UNION_BY_SIZE, simpleUnionArgs)) {

      assertThrows(IllegalArgumentException.class, () -> unionFind.find(10));
    }
  }

  @Test
  public void testUnion_twoNewElements_producesSingleSubsetOfSizeTwo() {

    for (UnionFind<Integer> unionFind : bothVariants(UnionType.UNION_BY_SIZE, simpleUnionArgs)) {

      assertThat(unionFind.find(0)).isEqualTo(unionFind.find(1));
      assertThat(unionFind.getAllSubsets()).hasSize(1);
    }
  }

  @Test
  public void testUnion_disjointPairs_produceDistinctSubsets() {

    for (UnionFind<Integer> unionFind :
        bothVariants(UnionType.UNION_BY_SIZE, new int[] {0, 1}, new int[] {2, 3})) {

      assertThat(unionFind.find(0)).isNotEqualTo(unionFind.find(2));
      assertThat(unionFind.getAllSubsets()).hasSize(2);
    }
  }

  @Test
  public void testUnion_severalElementsToSameSubset() {

    for (UnionFind<Integer> unionFind :
        bothVariants(
            UnionType.UNION_BY_SIZE, new int[] {0, 1}, new int[] {1, 2}, new int[] {2, 3})) {

      int canon = unionFind.find(0);

      assertThat(unionFind.find(1)).isEqualTo(canon);
      assertThat(unionFind.find(2)).isEqualTo(canon);
      assertThat(unionFind.find(3)).isEqualTo(canon);
      assertThat(unionFind.getAllSubsets()).hasSize(1);
    }
  }

  @Test
  public void testUnion_duplicateUnionCall_doesNotLeadToDuplicates() {

    for (UnionFind<Integer> unionFind :
        bothVariants(
            UnionType.UNION_BY_SIZE, new int[] {0, 1}, new int[] {0, 1}, new int[] {1, 0})) {

      assertThat(unionFind.find(0)).isEqualTo(unionFind.find(1));
      assertThat(unionFind.getAllSubsets()).hasSize(1);
      assertThat(unionFind.getAllSubsets().iterator().next()).hasSize(2);
    }
  }

  @Test
  public void testUnion_mergesTwoExistingMultiElementSubsets() {

    for (UnionFind<Integer> unionFind :
        bothVariants(
            UnionType.UNION_BY_SIZE,
            new int[] {0, 1},
            new int[] {0, 2},
            new int[] {3, 4},
            new int[] {3, 5},
            new int[] {0, 3})) {

      assertThat(unionFind.getAllSubsets()).hasSize(1);

      int canon = unionFind.find(0);

      for (int i = 0; i <= 5; i++) {
        assertThat(unionFind.find(i)).isEqualTo(canon);
      }
    }
  }

  @Test
  public void testUnion_constantCanonicalElementDuringNonLinearInsertion() {

    for (UnionFind<Integer> unionFind :
        bothVariants(
            UnionType.UNION_BY_SIZE,
            new int[] {3, 3},
            new int[] {3, 2},
            new int[] {3, 5},
            new int[] {3, 1},
            new int[] {3, 8},
            new int[] {3, 6},
            new int[] {3, 9},
            new int[] {3, 7},
            new int[] {3, 4},
            new int[] {3, 0})) {

      assertThat(unionFind.getAllSubsets()).hasSize(1);

      int canon = unionFind.find(3);

      for (int i = 0; i <= 9; i++) {
        assertThat(unionFind.find(i)).isEqualTo(canon);
      }
    }
  }

  @Test
  public void testUnion_bothUnionTypes_produceSameGrouping() {

    int[][] unions = {{0, 1}, {0, 2}, {3, 4}, {3, 5}, {0, 3}};

    for (UnionType unionType : UnionType.values()) {
      for (UnionFind<Integer> unionFind : bothVariants(unionType, unions)) {

        assertThat(unionFind.getAllSubsets()).hasSize(1);

        int canon = unionFind.find(0);

        for (int i = 0; i <= 5; i++) {
          assertThat(unionFind.find(i)).isEqualTo(canon);
        }
      }
    }
  }

  @Test
  public void testUnion_stringElements() {

    PersistentUnionFind<String> unsortedStringUnionFind =
        PersistentParentPointerTreeUnionFind.of(UnionType.UNION_BY_SIZE);
    unsortedStringUnionFind =
        unsortedStringUnionFind
            .unionAndCopy("0", "1")
            .unionAndCopy("0", "2")
            .unionAndCopy("3", "4");
    PersistentSortedUnionFind<String> sortedStringUnionFind =
        PersistentSortedParentPointerTreeUnionFind.of(UnionType.UNION_BY_SIZE);
    sortedStringUnionFind =
        sortedStringUnionFind.unionAndCopy("0", "1").unionAndCopy("0", "2").unionAndCopy("3", "4");

    assertThat(unsortedStringUnionFind.find("0")).isEqualTo(unsortedStringUnionFind.find("2"));
    assertThat(unsortedStringUnionFind.find("0")).isNotEqualTo(unsortedStringUnionFind.find("3"));
    assertThat(unsortedStringUnionFind.getAllSubsets()).hasSize(2);

    assertThat(sortedStringUnionFind.find("0")).isEqualTo(sortedStringUnionFind.find("2"));
    assertThat(sortedStringUnionFind.find("0")).isNotEqualTo(sortedStringUnionFind.find("3"));
    assertThat(sortedStringUnionFind.getAllSubsets()).hasSize(2);
  }

  @Test
  public void testGetAllSubsets_reflectsCorrectMembershipAfterMultipleUnions() {

    int[][] unions = {
      {0, 0}, {0, 1}, {0, 2}, {0, 3}, {0, 4}, {5, 5}, {5, 6}, {5, 7}, {5, 8}, {5, 9},
    };

    for (UnionFind<Integer> unionFind : bothVariants(UnionType.UNION_BY_SIZE, unions)) {

      Collection<? extends Set<Integer>> subsets = unionFind.getAllSubsets();

      assertThat(subsets).hasSize(2);

      for (Set<Integer> subset : subsets) {
        assertThat(subset).hasSize(5);
      }
    }
  }

  @Test
  public void testGetAllSubsets_emptyUnionFind_isEmpty() {

    for (UnionFind<Integer> unionFind : bothVariants(UnionType.UNION_BY_SIZE)) {

      assertThat(unionFind.getAllSubsets()).isEmpty();
    }
  }

  @Test
  public void testContains_elementInSubset_returnsTrue() {

    for (UnionFind<Integer> unionFind : bothVariants(UnionType.UNION_BY_SIZE, simpleUnionArgs)) {

      assertThat(unionFind.contains(0)).isTrue();
      assertThat(unionFind.contains(1)).isTrue();
    }
  }

  @Test
  public void testContains_elementNotContained_returnsFalse() {

    for (UnionFind<Integer> unionFind : bothVariants(UnionType.UNION_BY_SIZE, simpleUnionArgs)) {

      assertThat(unionFind.contains(10)).isFalse();
    }
  }

  @Test
  public void testContains_null_throwsNullPointerException() {

    for (UnionFind<Integer> unionFind : bothVariants(UnionType.UNION_BY_SIZE, simpleUnionArgs)) {

      assertThrows(NullPointerException.class, () -> unionFind.contains(null));
    }
  }

  @Test
  public void testUnion_nullElement_throwsNullPointerException() {

    PersistentUnionFind<Integer> unsortedUnionFind =
        emptyPersistentUnionFind(UnionType.UNION_BY_SIZE);
    PersistentUnionFind<Integer> sortedUnionFind =
        emptyPersistentUnionFind(UnionType.UNION_BY_SIZE);

    assertThrows(NullPointerException.class, () -> unsortedUnionFind.unionAndCopy(null, 1));
    assertThrows(NullPointerException.class, () -> sortedUnionFind.unionAndCopy(null, 1));
  }

  @Test
  public void testUnion_doesNotMutateOriginalInstance() {

    PersistentUnionFind<Integer> originalUnsorted =
        emptyPersistentUnionFind(UnionType.UNION_BY_SIZE).unionAndCopy(0, 1);
    PersistentUnionFind<Integer> updatedUnsorted = originalUnsorted.unionAndCopy(2, 3);

    assertThat(originalUnsorted.contains(2)).isFalse();
    assertThat(originalUnsorted.getAllSubsets()).hasSize(1);
    assertThat(updatedUnsorted.contains(2)).isTrue();
    assertThat(updatedUnsorted.getAllSubsets()).hasSize(2);

    PersistentSortedUnionFind<Integer> originalSorted =
        emptyPersistentSortedUnionFind(UnionType.UNION_BY_SIZE).unionAndCopy(0, 1);
    PersistentSortedUnionFind<Integer> updatedSorted = originalSorted.unionAndCopy(2, 3);

    assertThat(originalSorted.contains(2)).isFalse();
    assertThat(originalSorted.getAllSubsets()).hasSize(1);
    assertThat(updatedSorted.contains(2)).isTrue();
    assertThat(updatedSorted.getAllSubsets()).hasSize(2);
  }

  @Test
  public void testUnion_eachVersionKeepsItsOwnSnapshot() {

    PersistentUnionFind<Integer> version0 = emptyPersistentUnionFind(UnionType.UNION_BY_SIZE);
    PersistentUnionFind<Integer> version1 = version0.unionAndCopy(0, 1);
    PersistentUnionFind<Integer> version2 = version1.unionAndCopy(0, 2);
    PersistentUnionFind<Integer> version3 = version2.unionAndCopy(3, 4);

    assertThat(version0.getAllSubsets()).isEmpty();

    assertThat(version1.getAllSubsets()).hasSize(1);
    assertThat(version1.getAllSubsets().iterator().next()).containsExactly(0, 1);

    assertThat(version2.getAllSubsets()).hasSize(1);
    assertThat(version2.getAllSubsets().iterator().next()).containsExactly(0, 1, 2);

    assertThat(version3.getAllSubsets()).hasSize(2);
    assertThat(version3.contains(3)).isTrue();
    assertThat(version2.contains(3)).isFalse();
  }

  @Test
  public void testUnion_selfUnionOnExistingElement_returnsSameInstance() {

    PersistentUnionFind<Integer> unsortedUnionFind =
        emptyPersistentUnionFind(UnionType.UNION_BY_SIZE).unionAndCopy(0, 1);
    PersistentUnionFind<Integer> sortedUnionFind =
        emptyPersistentUnionFind(UnionType.UNION_BY_SIZE).unionAndCopy(0, 1);

    assertThat(unsortedUnionFind.unionAndCopy(0, 0)).isSameInstanceAs(unsortedUnionFind);
    assertThat(sortedUnionFind.unionAndCopy(0, 0)).isSameInstanceAs(sortedUnionFind);
  }

  @Test
  public void testUnion_bothElementsAlreadyInSameSubset_returnsSameInstance() {

    PersistentUnionFind<Integer> unsortedUnionFind =
        emptyPersistentUnionFind(UnionType.UNION_BY_SIZE).unionAndCopy(0, 1).unionAndCopy(0, 2);
    PersistentUnionFind<Integer> sortedUnionFind =
        emptyPersistentUnionFind(UnionType.UNION_BY_SIZE).unionAndCopy(0, 1).unionAndCopy(0, 2);

    assertThat(unsortedUnionFind.unionAndCopy(1, 2)).isSameInstanceAs(unsortedUnionFind);
    assertThat(sortedUnionFind.unionAndCopy(1, 2)).isSameInstanceAs(sortedUnionFind);
  }

  @Test
  public void testOf_returnsIndependentEmptyInstance() {

    PersistentUnionFind<Integer> firstUnsortedUnionFind =
        emptyPersistentUnionFind(UnionType.UNION_BY_SIZE);
    PersistentUnionFind<Integer> secondUnsortedUnionFind =
        emptyPersistentUnionFind(UnionType.UNION_BY_SIZE);

    firstUnsortedUnionFind = firstUnsortedUnionFind.unionAndCopy(0, 1);

    assertThat(firstUnsortedUnionFind.contains(0)).isTrue();
    assertThat(secondUnsortedUnionFind.contains(0)).isFalse();

    PersistentUnionFind<Integer> firstSortedUnionFind =
        emptyPersistentUnionFind(UnionType.UNION_BY_SIZE);
    PersistentUnionFind<Integer> secondSortedUnionFind =
        emptyPersistentUnionFind(UnionType.UNION_BY_SIZE);

    firstSortedUnionFind = firstSortedUnionFind.unionAndCopy(0, 1);

    assertThat(firstSortedUnionFind.contains(0)).isTrue();
    assertThat(secondSortedUnionFind.contains(0)).isFalse();
  }

  @Test
  public void testMutableUnion_isUnsupported() {

    for (UnionFind<Integer> unionFind : bothVariants(UnionType.UNION_BY_SIZE, simpleUnionArgs)) {
      assertThrows(UnsupportedOperationException.class, () -> unionFind.union(0, 1));
    }
  }

  @Test
  public void testGetAllSubsets_mutatingReturnedCollections_doesNotAffectSubsequentCalls() {

    for (UnionFind<Integer> unionFind : bothVariants(UnionType.UNION_BY_SIZE, simpleUnionArgs)) {

      Collection<? extends Set<Integer>> firstCall = unionFind.getAllSubsets();
      firstCall.iterator().next().clear();

      Collection<? extends Set<Integer>> secondCall = unionFind.getAllSubsets();

      assertThat(secondCall).hasSize(1);
      assertThat(secondCall.iterator().next()).containsExactly(0, 1);
    }
  }
}
