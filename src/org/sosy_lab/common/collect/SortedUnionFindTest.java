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

    boolean canonUnknown = true;
    Integer canon = null;

    for (int i = 0; i <= 9; i++) {
      if (canonUnknown) {
        canon = unionFind.find(i);
        canonUnknown = false;
      }
      assertThat(unionFind.find(i).equals(canon)).isTrue();
    }
  }
}
