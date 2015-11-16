/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.sosy_lab.common.collect;

import static com.google.common.truth.Truth.assertThat;
import static org.sosy_lab.common.collect.PersistentSortedMaps.merge;

import com.google.common.base.Equivalence;
import com.google.common.collect.ImmutableMap;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

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
        .iteratesAs(
            MapsDifference.Entry.forRightValueOnly("a", "1"),
            MapsDifference.Entry.forRightValueOnly("b", "2"),
            MapsDifference.Entry.forRightValueOnly("c", "3"),
            MapsDifference.Entry.forRightValueOnly("d", "4"));
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
        .iteratesAs(
            MapsDifference.Entry.forLeftValueOnly("a", "1"),
            MapsDifference.Entry.forLeftValueOnly("b", "2"),
            MapsDifference.Entry.forLeftValueOnly("c", "3"),
            MapsDifference.Entry.forLeftValueOnly("d", "4"));
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
        .iteratesAs(
            MapsDifference.Entry.forRightValueOnly("b", "2"),
            MapsDifference.Entry.forRightValueOnly("d", "4"));
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
        .iteratesAs(
            MapsDifference.Entry.forRightValueOnly("a", "1"),
            MapsDifference.Entry.forRightValueOnly("c", "3"));
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
        .iteratesAs(
            MapsDifference.Entry.forLeftValueOnly("b", "2"),
            MapsDifference.Entry.forLeftValueOnly("d", "4"));
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
        .iteratesAs(
            MapsDifference.Entry.forLeftValueOnly("a", "1"),
            MapsDifference.Entry.forLeftValueOnly("c", "3"));
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
        .iteratesAs(
            MapsDifference.Entry.forLeftValueOnly("a", "1"),
            MapsDifference.Entry.forRightValueOnly("b", "2"),
            MapsDifference.Entry.forLeftValueOnly("c", "3"),
            MapsDifference.Entry.forRightValueOnly("d", "4"));
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
        .iteratesAs(
            MapsDifference.Entry.forRightValueOnly("a", "1"),
            MapsDifference.Entry.forLeftValueOnly("b", "2"),
            MapsDifference.Entry.forRightValueOnly("c", "3"),
            MapsDifference.Entry.forLeftValueOnly("d", "4"));
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
        .iteratesAs(
            MapsDifference.Entry.forDifferingValues("a", "4", "1"),
            MapsDifference.Entry.forRightValueOnly("b", "2"),
            MapsDifference.Entry.forDifferingValues("c", "2", "3"),
            MapsDifference.Entry.forRightValueOnly("d", "4"));
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
        .iteratesAs(
            MapsDifference.Entry.forDifferingValues("a", "1", "4"),
            MapsDifference.Entry.forLeftValueOnly("b", "2"),
            MapsDifference.Entry.forDifferingValues("c", "3", "2"),
            MapsDifference.Entry.forLeftValueOnly("d", "4"));
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
