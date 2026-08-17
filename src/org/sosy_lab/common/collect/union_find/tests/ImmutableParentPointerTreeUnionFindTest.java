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
import org.sosy_lab.common.collect.union_find.ImmutableParentPointerTreeUnionFind;
import org.sosy_lab.common.collect.union_find.ImmutableSortedParentPointerTreeUnionFind;
import org.sosy_lab.common.collect.union_find.ParentPointerTreeUnionFind.UnionType;
import org.sosy_lab.common.collect.union_find.UnionFind;

public class ImmutableParentPointerTreeUnionFindTest {

  private final int[] simpleUnionArgs = new int[] {0, 1};

  private static ImmutableParentPointerTreeUnionFind<Integer> buildImmutable(
      UnionType pUnionType, int[]... pUnions) {

    ImmutableParentPointerTreeUnionFind.Builder<Integer> builder =
        ImmutableParentPointerTreeUnionFind.Builder.getBuilder(pUnionType);

    for (int[] pair : pUnions) {
      builder.union(pair[0], pair[1]);
    }

    return builder.build();
  }

  private static ImmutableSortedParentPointerTreeUnionFind<Integer> buildImmutableSorted(
      UnionType pUnionType, int[]... pUnions) {

    ImmutableSortedParentPointerTreeUnionFind.Builder<Integer> builder =
        ImmutableSortedParentPointerTreeUnionFind.Builder.getBuilder(pUnionType);

    for (int[] pair : pUnions) {
      builder.union(pair[0], pair[1]);
    }

    return builder.build();
  }

  private static ImmutableList<UnionFind<Integer>> buildUnsortedAndSorted(
      UnionType pUnionType, int[]... pUnions) {

    return ImmutableList.of(
        buildImmutable(pUnionType, pUnions), buildImmutableSorted(pUnionType, pUnions));
  }

  @Test
  public void testFind_afterSelfUnion_returnsItself() {

    int[] unionArgs = new int[] {0, 0};

    for (UnionFind<Integer> unionFind :
        buildUnsortedAndSorted(UnionType.UNION_BY_SIZE, unionArgs)) {

      assertThat(unionFind.find(0)).isEqualTo(0);
    }
  }

  @Test
  public void testFind_null_throwsNullPointerException() {

    for (UnionFind<Integer> unionFind :
        buildUnsortedAndSorted(UnionType.UNION_BY_SIZE, simpleUnionArgs)) {

      assertThrows(NullPointerException.class, () -> unionFind.find(null));
    }
  }

  @Test
  public void testFind_elementNotContained_throwsIllegalArgumentException() {

    for (UnionFind<Integer> unionFind :
        buildUnsortedAndSorted(UnionType.UNION_BY_SIZE, simpleUnionArgs)) {

      assertThrows(IllegalArgumentException.class, () -> unionFind.find(10));
    }
  }

  @Test
  public void testUnion_twoNewElements_producesSingleSubsetOfSizeTwo() {

    for (UnionFind<Integer> unionFind :
        buildUnsortedAndSorted(UnionType.UNION_BY_SIZE, simpleUnionArgs)) {

      assertThat(unionFind.find(0)).isEqualTo(unionFind.find(1));
      assertThat(unionFind.getAllSubsets()).hasSize(1);
    }
  }

  @Test
  public void testUnion_disjointPairs_produceDistinctSubsets() {

    for (UnionFind<Integer> unionFind :
        buildUnsortedAndSorted(UnionType.UNION_BY_SIZE, new int[] {0, 1}, new int[] {2, 3})) {

      assertThat(unionFind.find(0)).isNotEqualTo(unionFind.find(2));
      assertThat(unionFind.getAllSubsets()).hasSize(2);
    }
  }

  @Test
  public void testUnion_severalElementsToSameSubset() {

    for (UnionFind<Integer> unionFind :
        buildUnsortedAndSorted(
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
        buildUnsortedAndSorted(
            UnionType.UNION_BY_SIZE, new int[] {0, 1}, new int[] {0, 1}, new int[] {1, 0})) {

      assertThat(unionFind.find(0)).isEqualTo(unionFind.find(1));
      assertThat(unionFind.getAllSubsets()).hasSize(1);
      assertThat(unionFind.getAllSubsets().iterator().next()).hasSize(2);
    }
  }

  @Test
  public void testUnion_mergesTwoExistingMultiElementSubsets() {

    for (UnionFind<Integer> unionFind :
        buildUnsortedAndSorted(
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
        buildUnsortedAndSorted(
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
      for (UnionFind<Integer> unionFind : buildUnsortedAndSorted(unionType, unions)) {

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

    ImmutableParentPointerTreeUnionFind.Builder<String> unsortedBuilder =
        ImmutableParentPointerTreeUnionFind.Builder.getBuilder(UnionType.UNION_BY_SIZE);
    ImmutableSortedParentPointerTreeUnionFind.Builder<String> sortedBuilder =
        ImmutableSortedParentPointerTreeUnionFind.Builder.getBuilder(UnionType.UNION_BY_SIZE);

    ImmutableParentPointerTreeUnionFind<String> unsortedStringUnionFind =
        unsortedBuilder.union("0", "1").union("0", "2").union("3", "4").build();
    ImmutableSortedParentPointerTreeUnionFind<String> sortedStringUnionFind =
        sortedBuilder.union("0", "1").union("0", "2").union("3", "4").build();

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

    for (UnionFind<Integer> unionFind : buildUnsortedAndSorted(UnionType.UNION_BY_SIZE, unions)) {

      Collection<? extends Set<Integer>> subsets = unionFind.getAllSubsets();

      assertThat(subsets).hasSize(2);

      for (Set<Integer> subset : subsets) {
        assertThat(subset).hasSize(5);
      }
    }
  }

  @Test
  public void testGetAllSubsets_emptyUnionFind_isEmpty() {

    for (UnionFind<Integer> unionFind : buildUnsortedAndSorted(UnionType.UNION_BY_SIZE)) {

      assertThat(unionFind.getAllSubsets()).isEmpty();
    }
  }

  @Test
  public void testContains_elementInSubset_returnsTrue() {

    for (UnionFind<Integer> unionFind :
        buildUnsortedAndSorted(UnionType.UNION_BY_SIZE, simpleUnionArgs)) {

      assertThat(unionFind.contains(0)).isTrue();
      assertThat(unionFind.contains(1)).isTrue();
    }
  }

  @Test
  public void testContains_elementNotContained_returnsFalse() {

    for (UnionFind<Integer> unionFind :
        buildUnsortedAndSorted(UnionType.UNION_BY_SIZE, simpleUnionArgs)) {

      assertThat(unionFind.contains(10)).isFalse();
    }
  }

  @Test
  public void testContains_null_returnsFalse() {

    for (UnionFind<Integer> unionFind :
        buildUnsortedAndSorted(UnionType.UNION_BY_SIZE, simpleUnionArgs)) {

      assertThat(unionFind.contains(null)).isFalse();
    }
  }

  @Test
  public void testBuilder_union_nullElement_throwsNullPointerException() {

    ImmutableParentPointerTreeUnionFind.Builder<Integer> unsortedBuilder =
        ImmutableParentPointerTreeUnionFind.Builder.getBuilder(UnionType.UNION_BY_SIZE);
    ImmutableSortedParentPointerTreeUnionFind.Builder<Integer> sortedBuilder =
        ImmutableSortedParentPointerTreeUnionFind.Builder.getBuilder(UnionType.UNION_BY_SIZE);

    assertThrows(NullPointerException.class, () -> unsortedBuilder.union(null, 1));
    assertThrows(NullPointerException.class, () -> sortedBuilder.union(null, 1));
  }

  @Test
  public void testBuilder_union_returnsSameBuilderInstance() {

    ImmutableParentPointerTreeUnionFind.Builder<Integer> unsortedBuilder =
        ImmutableParentPointerTreeUnionFind.Builder.getBuilder(UnionType.UNION_BY_SIZE);
    ImmutableSortedParentPointerTreeUnionFind.Builder<Integer> sortedBuilder =
        ImmutableSortedParentPointerTreeUnionFind.Builder.getBuilder(UnionType.UNION_BY_SIZE);

    assertThat(unsortedBuilder.union(0, 1)).isSameInstanceAs(unsortedBuilder);
    assertThat(sortedBuilder.union(0, 1)).isSameInstanceAs(sortedBuilder);
  }

  @Test
  public void testBuilder_getBuilder_returnsIndependentBuilders() {

    ImmutableParentPointerTreeUnionFind.Builder<Integer> unsortedBuilder1 =
        ImmutableParentPointerTreeUnionFind.Builder.getBuilder(UnionType.UNION_BY_SIZE);
    ImmutableParentPointerTreeUnionFind.Builder<Integer> unsortedBuilder2 =
        ImmutableParentPointerTreeUnionFind.Builder.getBuilder(UnionType.UNION_BY_SIZE);

    ImmutableSortedParentPointerTreeUnionFind.Builder<Integer> sortedBuilder1 =
        ImmutableSortedParentPointerTreeUnionFind.Builder.getBuilder(UnionType.UNION_BY_SIZE);
    ImmutableSortedParentPointerTreeUnionFind.Builder<Integer> sortedBuilder2 =
        ImmutableSortedParentPointerTreeUnionFind.Builder.getBuilder(UnionType.UNION_BY_SIZE);

    unsortedBuilder1.union(0, 1);
    sortedBuilder1.union(0, 1);

    assertThat(unsortedBuilder1.build().contains(0)).isTrue();
    assertThat(unsortedBuilder2.build().contains(0)).isFalse();

    assertThat(sortedBuilder1.build().contains(0)).isTrue();
    assertThat(sortedBuilder2.build().contains(0)).isFalse();
  }

  @Test
  public void testBuilder_build_laterMutationsDoNotAffectPreviousResult() {

    ImmutableParentPointerTreeUnionFind.Builder<Integer> unsortedBuilder =
        ImmutableParentPointerTreeUnionFind.Builder.getBuilder(UnionType.UNION_BY_SIZE);
    ImmutableSortedParentPointerTreeUnionFind.Builder<Integer> sortedBuilder =
        ImmutableSortedParentPointerTreeUnionFind.Builder.getBuilder(UnionType.UNION_BY_SIZE);

    unsortedBuilder.union(0, 1);
    sortedBuilder.union(0, 1);

    ImmutableParentPointerTreeUnionFind<Integer> firstUnsortedResult = unsortedBuilder.build();
    ImmutableSortedParentPointerTreeUnionFind<Integer> firstSortedResult = sortedBuilder.build();

    unsortedBuilder.union(2, 3);
    sortedBuilder.union(2, 3);

    ImmutableParentPointerTreeUnionFind<Integer> secondUnsortedResult = unsortedBuilder.build();
    ImmutableSortedParentPointerTreeUnionFind<Integer> secondSortedResult = sortedBuilder.build();

    assertThat(firstUnsortedResult.contains(2)).isFalse();
    assertThat(firstUnsortedResult.getAllSubsets()).hasSize(1);

    assertThat(firstSortedResult.contains(2)).isFalse();
    assertThat(firstSortedResult.getAllSubsets()).hasSize(1);

    assertThat(secondUnsortedResult.contains(2)).isTrue();
    assertThat(secondUnsortedResult.getAllSubsets()).hasSize(2);

    assertThat(secondSortedResult.contains(2)).isTrue();
    assertThat(secondSortedResult.getAllSubsets()).hasSize(2);
  }

  @Test
  public void testMutableUnion_isUnsupported() {

    for (UnionFind<Integer> unionFind :
        buildUnsortedAndSorted(UnionType.UNION_BY_SIZE, simpleUnionArgs)) {
      assertThrows(UnsupportedOperationException.class, () -> unionFind.union(0, 1));
    }
  }

  @Test
  public void testGetAllSubsets_mutatingReturnedCollection_doesNotAffectSubsequentCalls() {

    for (UnionFind<Integer> unionFind :
        buildUnsortedAndSorted(UnionType.UNION_BY_SIZE, simpleUnionArgs)) {

      Collection<? extends Set<Integer>> firstCall = unionFind.getAllSubsets();

      firstCall.iterator().next().clear();

      Collection<? extends Set<Integer>> secondCall = unionFind.getAllSubsets();

      assertThat(secondCall).hasSize(1);
      assertThat(secondCall.iterator().next()).containsExactly(0, 1);
    }
  }
}
