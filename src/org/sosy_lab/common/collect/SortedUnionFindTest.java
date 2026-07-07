// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Range;
import com.google.errorprone.annotations.Var;
import java.util.Collection;
import org.junit.BeforeClass;
import org.junit.Test;

public class SortedUnionFindTest {

  static final Range<Integer> LOW_NUMS = Range.closed(0, 4);
  static final Range<Integer> HIGH_NUMS = Range.closed(5, 9);

  static SortedUnionFind<Integer> unionFind = new SortedTreeSetUnionFind<>();

  @BeforeClass
  public static void setup() {
    unionFind = new SortedTreeSetUnionFind<>();

    for (int i = 0; i <= 4; i++) {
      unionFind.union(0, i);
    }
    for (int i = 5; i <= 9; i++) {
      unionFind.union(5, i);
    }
  }

  @Test
  public void testFind_ElementNotContained() {
    assertThat(LOW_NUMS.contains(unionFind.find(8))).isFalse();
    assertThat(HIGH_NUMS.contains(unionFind.find(2))).isFalse();
  }

  @Test
  public void testFind_ElementContained() {
    assertThat(LOW_NUMS.contains(unionFind.find(2))).isTrue();
    assertThat(HIGH_NUMS.contains(unionFind.find(8))).isTrue();
  }

  @Test
  public void testUnion_CorrectCanonicalElementAndCorrectSubsetAfterUnionBySize() {
    assertThat(unionFind.getAllSubsets().size() == 2).isTrue();

    for (int i = 0; i <= 4; i++) {
      assertThat(unionFind.find(i).equals(0)).isTrue();
    }
    for (int i = 5; i <= 9; i++) {
      assertThat(unionFind.find(i).equals(5)).isTrue();
    }
  }

  @Test
  public void testUnion_MergeExistingSubsets() {
    unionFind.union(0, 5);

    assertThat(unionFind.getAllSubsets().size() == 1).isTrue();

    @Var boolean canonUnknown = true;
    @Var Integer canon = null;

    for (int i = 0; i <= 9; i++) {
      if (canonUnknown) {
        canon = unionFind.find(i);
        canonUnknown = false;
      }
      assertThat(unionFind.find(i).equals(canon)).isTrue();
    }
  }

  @Test
  public void testUnion_ConstantCanonicalElementDuringNonlinearInsertion() {
    SortedUnionFind<Integer> newUnionFind = new SortedTreeSetUnionFind<>();

    newUnionFind.union(3, 3);
    newUnionFind.union(3, 2);
    newUnionFind.union(3, 5);
    newUnionFind.union(3, 1);
    newUnionFind.union(3, 8);
    newUnionFind.union(3, 6);
    newUnionFind.union(3, 9);
    newUnionFind.union(3, 7);
    newUnionFind.union(3, 4);

    assertThat(newUnionFind.find(3)).isEqualTo(3);
    assertThat(newUnionFind.find(2)).isEqualTo(3);
    assertThat(newUnionFind.find(5)).isEqualTo(3);
    assertThat(newUnionFind.find(1)).isEqualTo(3);
    assertThat(newUnionFind.find(8)).isEqualTo(3);
    assertThat(newUnionFind.find(6)).isEqualTo(3);
    assertThat(newUnionFind.find(9)).isEqualTo(3);
    assertThat(newUnionFind.find(7)).isEqualTo(3);
    assertThat(newUnionFind.find(4)).isEqualTo(3);
  }

  @Test
  public void testUnion_Strings() {
    SortedUnionFind<String> unionFindString = new SortedTreeSetUnionFind<>();
    String expected = ".-1..0..1..2..3..4..5..6..7..8..9.";

    for (int i = 0; i <= 2; i++) {
      unionFindString.union(Integer.toString(0), Integer.toString(i));
    }
    for (int i = 3; i <= 5; i++) {
      unionFindString.union(Integer.toString(3), Integer.toString(i));
    }
    for (int i = 6; i <= 8; i++) {
      unionFindString.union(Integer.toString(6), Integer.toString(i));
    }
    // case: both elements the same; to be added as new subset
    unionFindString.union(Integer.toString(9), Integer.toString(9));
    assertThat(unionFindString.getAllSubsets()).hasSize(4);

    // case: both canonical elements
    unionFindString.union(Integer.toString(0), Integer.toString(6));
    assertThat(unionFindString.getAllSubsets()).hasSize(3);

    // case: one contained but not canonical, one not contained
    unionFindString.union(Integer.toString(7), Integer.toString(-1));
    assertThat(unionFindString.getAllSubsets()).hasSize(3);

    // case: both contained but neither canonical elements
    unionFindString.union(Integer.toString(1), Integer.toString(4));
    assertThat(unionFindString.getAllSubsets()).hasSize(2);

    // case: both canonical elements
    unionFindString.union(Integer.toString(0), Integer.toString(9));
    assertThat(unionFindString.getAllSubsets()).hasSize(1);

    @Var String result = "";

    for (Collection<String> subset : unionFindString.getAllSubsets()) {
      for (String element : subset) {
        result = result.concat("." + Integer.valueOf(element) + ".");
      }
    }
    assertThat(result).isEqualTo(expected);
  }
}
