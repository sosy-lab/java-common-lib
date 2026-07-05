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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
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
    // TODO

    Random random = new Random(1357111317L);
    AuxiliarySortedUnionFind<String> expected = new AuxiliarySortedUnionFind<>();
    SortedUnionFind<String> unionFind = new SortedTreeSetUnionFind<>();

    int noOfSubsets = 5;
    int sizeOfSubsets = 10;

    for (int i = 0; i < noOfSubsets; i++) {
      String canon = Integer.toString(random.nextInt());

      unionFind.union(canon, canon);
      expected.union(canon, canon);

      for (int j = 1; j < sizeOfSubsets; j++) {
        String elem = Integer.toString(random.nextInt());
        unionFind.union(canon, elem);
        expected.union(canon, elem);
      }
    }

    // TODO now check they're the same
  }

  protected class AuxiliarySortedUnionFind<T extends Comparable<T>> {
    ArrayList<SubsetOfAuxiliarySortedUnionFind<T>> subsets;

    AuxiliarySortedUnionFind() {
      subsets = new ArrayList<>();
    }

    // TODO
    /*
    String getContentsAsInt() {
      int contents;
      ArrayList<T> subsetsCopy = (ArrayList<T>) subsets.clone();
    }*/

    SubsetOfAuxiliarySortedUnionFind<T> find(T e) {
      for (SubsetOfAuxiliarySortedUnionFind<T> current : subsets) {
        if (current.contains(e)) {
          return current;
        }
      }

      throw new NoSuchElementException();
    }

    void union(T e1, T e2) {
      if (contains(e1)) {
        if (contains(e2)) {
          mergeExistingSubsets(e1, e2);
        } else {
          addToExistingSubset(e1, e2);
        }
      } else if (contains(e2)) {
        addToExistingSubset(e2, e1);
      } else {
        addAsNewSubset(e1, e2);
      }
    }

    boolean contains(T e) {
      for (SubsetOfAuxiliarySortedUnionFind<T> current : subsets) {
        if (current.contains(e)) {
          return true;
        }
      }

      return false;
    }

    private void mergeExistingSubsets(T e1, T e2) {
      SubsetOfAuxiliarySortedUnionFind<T> subset1 = find(e1);
      SubsetOfAuxiliarySortedUnionFind<T> subset2 = find(e2);

      subsets.remove(subset1);
      subsets.remove(subset2);

      if (subset1.size() >= subset2.size()) {
        Iterator<T> iterator = subset2.iterator();

        while (iterator.hasNext()) {
          T current = iterator.next();
          subset1.add(current);
        }

        subsets.add(subset1);
      } else {
        Iterator<T> iterator = subset1.iterator();

        while (iterator.hasNext()) {
          T current = iterator.next();
          subset2.add(current);
        }

        subsets.add(subset2);
      }
    }

    private void addToExistingSubset(T alreadyContained, T newElement) {
      SubsetOfAuxiliarySortedUnionFind<T> subset = find(alreadyContained);

      subsets.remove(subset);
      subset.add(newElement);
      subsets.add(subset);
    }

    private void addAsNewSubset(T e1, T e2) {
      SubsetOfAuxiliarySortedUnionFind<T> newSubset = new SubsetOfAuxiliarySortedUnionFind<>(e1);
      newSubset.add(e2);
      subsets.add(newSubset);
    }
  }

  protected class SubsetOfAuxiliarySortedUnionFind<T extends Comparable<T>> {
    final T canon;
    Set<T> set =
        new HashSet<>(); // TODO potentially change to data structure that is already sorted to
                         // avoid work later on

    protected SubsetOfAuxiliarySortedUnionFind(T firstElement) {
      this.canon = firstElement;
      this.set.add(firstElement);
    }

    protected void add(T e) {
      set.add(e);
    }

    protected boolean contains(T e) {
      return set.contains(e);
    }

    protected int size() {
      return set.size();
    }

    protected Iterator<T> iterator() {
      return set.iterator();
    }
  }
}
