// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import static com.google.common.truth.Truth.assertThat;
import static org.sosy_lab.common.collect.PersistentSortedMaps.merge;

import com.google.common.base.Equivalence;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class PersistentSortedMapsTest {

  private static final PersistentSortedMap<String, String> EMPTY_MAP =
      PathCopyingPersistentTreeMap.<String, String>of();

  private static final PersistentSortedMap<String, String> HALF1_MAP =
      PathCopyingPersistentTreeMap.copyOf(
          ImmutableMap.of(
              "a", "1",
              "c", "3"));

  private static final PersistentSortedMap<String, String> HALF2_MAP =
      PathCopyingPersistentTreeMap.copyOf(
          ImmutableMap.of(
              "b", "2",
              "d", "4"));

  private static final PersistentSortedMap<String, String> FULL_MAP =
      PathCopyingPersistentTreeMap.copyOf(
          ImmutableMap.of(
              "a", "1",
              "b", "2",
              "c", "3",
              "d", "4"));

  private static final PersistentSortedMap<String, String> HALF1_MAP_INVERSE =
      PathCopyingPersistentTreeMap.copyOf(
          ImmutableMap.of(
              "a", "4",
              "c", "2"));

  private static final PersistentSortedMap<String, String> FULL_MAP_INVERSE =
      PathCopyingPersistentTreeMap.copyOf(
          ImmutableMap.of(
              "a", "4",
              "b", "2",
              "c", "3",
              "d", "4"));

  @Test
  public void testMerge_Equal() {

    List<MapsDifference.Entry<String, String>> differences = new ArrayList<>();
    PersistentSortedMap<String, String> result =
        merge(
            FULL_MAP,
            FULL_MAP,
            Equivalence.equals(),
            PersistentSortedMaps.<String, String>getExceptionMergeConflictHandler(),
            MapsDifference.collectMapsDifferenceTo(differences));

    assertThat(result).isEqualTo(FULL_MAP);
    assertThat(differences).isEmpty();
  }

  @Test
  public void testMerge_map1Empty() {

    List<MapsDifference.Entry<String, String>> differences = new ArrayList<>();
    PersistentSortedMap<String, String> result =
        merge(
            EMPTY_MAP,
            FULL_MAP,
            Equivalence.equals(),
            PersistentSortedMaps.<String, String>getExceptionMergeConflictHandler(),
            MapsDifference.collectMapsDifferenceTo(differences));

    assertThat(result).isEqualTo(FULL_MAP);

    assertThat(differences)
        .containsExactly(
            MapsDifference.Entry.forRightValueOnly("a", "1"),
            MapsDifference.Entry.forRightValueOnly("b", "2"),
            MapsDifference.Entry.forRightValueOnly("c", "3"),
            MapsDifference.Entry.forRightValueOnly("d", "4"))
        .inOrder();
  }

  @Test
  public void testMerge_map2Empty() {

    List<MapsDifference.Entry<String, String>> differences = new ArrayList<>();
    PersistentSortedMap<String, String> result =
        merge(
            FULL_MAP,
            EMPTY_MAP,
            Equivalence.equals(),
            PersistentSortedMaps.<String, String>getExceptionMergeConflictHandler(),
            MapsDifference.collectMapsDifferenceTo(differences));

    assertThat(result).isEqualTo(FULL_MAP);

    assertThat(differences)
        .containsExactly(
            MapsDifference.Entry.forLeftValueOnly("a", "1"),
            MapsDifference.Entry.forLeftValueOnly("b", "2"),
            MapsDifference.Entry.forLeftValueOnly("c", "3"),
            MapsDifference.Entry.forLeftValueOnly("d", "4"))
        .inOrder();
  }

  @Test
  public void testMerge_map1Half1() {

    List<MapsDifference.Entry<String, String>> differences = new ArrayList<>();
    PersistentSortedMap<String, String> result =
        merge(
            HALF1_MAP,
            FULL_MAP,
            Equivalence.equals(),
            PersistentSortedMaps.<String, String>getExceptionMergeConflictHandler(),
            MapsDifference.collectMapsDifferenceTo(differences));

    assertThat(result).isEqualTo(FULL_MAP);

    assertThat(differences)
        .containsExactly(
            MapsDifference.Entry.forRightValueOnly("b", "2"),
            MapsDifference.Entry.forRightValueOnly("d", "4"))
        .inOrder();
  }

  @Test
  public void testMerge_map1Half2() {

    List<MapsDifference.Entry<String, String>> differences = new ArrayList<>();
    PersistentSortedMap<String, String> result =
        merge(
            HALF2_MAP,
            FULL_MAP,
            Equivalence.equals(),
            PersistentSortedMaps.<String, String>getExceptionMergeConflictHandler(),
            MapsDifference.collectMapsDifferenceTo(differences));

    assertThat(result).isEqualTo(FULL_MAP);

    assertThat(differences)
        .containsExactly(
            MapsDifference.Entry.forRightValueOnly("a", "1"),
            MapsDifference.Entry.forRightValueOnly("c", "3"))
        .inOrder();
  }

  @Test
  public void testMerge_map2Half1() {

    List<MapsDifference.Entry<String, String>> differences = new ArrayList<>();
    PersistentSortedMap<String, String> result =
        merge(
            FULL_MAP,
            HALF1_MAP,
            Equivalence.equals(),
            PersistentSortedMaps.<String, String>getExceptionMergeConflictHandler(),
            MapsDifference.collectMapsDifferenceTo(differences));

    assertThat(result).isEqualTo(FULL_MAP);

    assertThat(differences)
        .containsExactly(
            MapsDifference.Entry.forLeftValueOnly("b", "2"),
            MapsDifference.Entry.forLeftValueOnly("d", "4"))
        .inOrder();
  }

  @Test
  public void testMerge_map2Half2() {

    List<MapsDifference.Entry<String, String>> differences = new ArrayList<>();
    PersistentSortedMap<String, String> result =
        merge(
            FULL_MAP,
            HALF2_MAP,
            Equivalence.equals(),
            PersistentSortedMaps.<String, String>getExceptionMergeConflictHandler(),
            MapsDifference.collectMapsDifferenceTo(differences));

    assertThat(result).isEqualTo(FULL_MAP);

    assertThat(differences)
        .containsExactly(
            MapsDifference.Entry.forLeftValueOnly("a", "1"),
            MapsDifference.Entry.forLeftValueOnly("c", "3"))
        .inOrder();
  }

  @Test
  public void testMerge_map1Half1_map2Half2() {

    List<MapsDifference.Entry<String, String>> differences = new ArrayList<>();
    PersistentSortedMap<String, String> result =
        merge(
            HALF1_MAP,
            HALF2_MAP,
            Equivalence.equals(),
            PersistentSortedMaps.<String, String>getExceptionMergeConflictHandler(),
            MapsDifference.collectMapsDifferenceTo(differences));

    assertThat(result).isEqualTo(FULL_MAP);

    assertThat(differences)
        .containsExactly(
            MapsDifference.Entry.forLeftValueOnly("a", "1"),
            MapsDifference.Entry.forRightValueOnly("b", "2"),
            MapsDifference.Entry.forLeftValueOnly("c", "3"),
            MapsDifference.Entry.forRightValueOnly("d", "4"))
        .inOrder();
  }

  @Test
  public void testMerge_map1Half2_map2Half1() {

    List<MapsDifference.Entry<String, String>> differences = new ArrayList<>();
    PersistentSortedMap<String, String> result =
        merge(
            HALF2_MAP,
            HALF1_MAP,
            Equivalence.equals(),
            PersistentSortedMaps.<String, String>getExceptionMergeConflictHandler(),
            MapsDifference.collectMapsDifferenceTo(differences));

    assertThat(result).isEqualTo(FULL_MAP);

    assertThat(differences)
        .containsExactly(
            MapsDifference.Entry.forRightValueOnly("a", "1"),
            MapsDifference.Entry.forLeftValueOnly("b", "2"),
            MapsDifference.Entry.forRightValueOnly("c", "3"),
            MapsDifference.Entry.forLeftValueOnly("d", "4"))
        .inOrder();
  }

  @Test
  public void testMerge_map1Differences() {

    List<MapsDifference.Entry<String, String>> differences = new ArrayList<>();
    PersistentSortedMap<String, String> result =
        merge(
            HALF1_MAP_INVERSE,
            FULL_MAP,
            Equivalence.equals(),
            PersistentSortedMaps.<String, String>getMaximumMergeConflictHandler(),
            MapsDifference.collectMapsDifferenceTo(differences));

    assertThat(result).isEqualTo(FULL_MAP_INVERSE);

    assertThat(differences)
        .containsExactly(
            MapsDifference.Entry.forDifferingValues("a", "4", "1"),
            MapsDifference.Entry.forRightValueOnly("b", "2"),
            MapsDifference.Entry.forDifferingValues("c", "2", "3"),
            MapsDifference.Entry.forRightValueOnly("d", "4"))
        .inOrder();
  }

  @Test
  public void testMerge_map2Differences() {

    List<MapsDifference.Entry<String, String>> differences = new ArrayList<>();
    PersistentSortedMap<String, String> result =
        merge(
            FULL_MAP,
            HALF1_MAP_INVERSE,
            Equivalence.equals(),
            PersistentSortedMaps.<String, String>getMaximumMergeConflictHandler(),
            MapsDifference.collectMapsDifferenceTo(differences));

    assertThat(result).isEqualTo(FULL_MAP_INVERSE);

    assertThat(differences)
        .containsExactly(
            MapsDifference.Entry.forDifferingValues("a", "1", "4"),
            MapsDifference.Entry.forLeftValueOnly("b", "2"),
            MapsDifference.Entry.forDifferingValues("c", "3", "2"),
            MapsDifference.Entry.forLeftValueOnly("d", "4"))
        .inOrder();
  }

  @Test
  public void testMerge0_Equal() {

    PersistentSortedMap<String, String> result =
        merge(
            FULL_MAP,
            FULL_MAP,
            PersistentSortedMaps.<String, String>getExceptionMergeConflictHandler());

    assertThat(result).isEqualTo(FULL_MAP);
  }

  @Test
  public void testMerge0_map1Empty() {

    PersistentSortedMap<String, String> result =
        merge(
            EMPTY_MAP,
            FULL_MAP,
            PersistentSortedMaps.<String, String>getExceptionMergeConflictHandler());

    assertThat(result).isEqualTo(FULL_MAP);
  }

  @Test
  public void testMerge0_map2Empty() {

    PersistentSortedMap<String, String> result =
        merge(
            FULL_MAP,
            EMPTY_MAP,
            PersistentSortedMaps.<String, String>getExceptionMergeConflictHandler());

    assertThat(result).isEqualTo(FULL_MAP);
  }

  @Test
  public void testMerge0_map1Half1() {

    PersistentSortedMap<String, String> result =
        merge(
            HALF1_MAP,
            FULL_MAP,
            PersistentSortedMaps.<String, String>getExceptionMergeConflictHandler());

    assertThat(result).isEqualTo(FULL_MAP);
  }

  @Test
  public void testMerge0_map1Half2() {

    PersistentSortedMap<String, String> result =
        merge(
            HALF2_MAP,
            FULL_MAP,
            PersistentSortedMaps.<String, String>getExceptionMergeConflictHandler());

    assertThat(result).isEqualTo(FULL_MAP);
  }

  @Test
  public void testMerge0_map2Half1() {

    PersistentSortedMap<String, String> result =
        merge(
            FULL_MAP,
            HALF1_MAP,
            PersistentSortedMaps.<String, String>getExceptionMergeConflictHandler());

    assertThat(result).isEqualTo(FULL_MAP);
  }

  @Test
  public void testMerge0_map2Half2() {

    PersistentSortedMap<String, String> result =
        merge(
            FULL_MAP,
            HALF2_MAP,
            PersistentSortedMaps.<String, String>getExceptionMergeConflictHandler());

    assertThat(result).isEqualTo(FULL_MAP);
  }

  @Test
  public void testMerge0_map1Half1_map2Half2() {

    PersistentSortedMap<String, String> result =
        merge(
            HALF1_MAP,
            HALF2_MAP,
            PersistentSortedMaps.<String, String>getExceptionMergeConflictHandler());

    assertThat(result).isEqualTo(FULL_MAP);
  }

  @Test
  public void testMerge0_map1Half2_map2Half1() {

    PersistentSortedMap<String, String> result =
        merge(
            HALF2_MAP,
            HALF1_MAP,
            PersistentSortedMaps.<String, String>getExceptionMergeConflictHandler());

    assertThat(result).isEqualTo(FULL_MAP);
  }
}
