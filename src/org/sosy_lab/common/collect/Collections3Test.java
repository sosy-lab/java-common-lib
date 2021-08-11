// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.Test;

public class Collections3Test {

  private static final ImmutableList<Object> MIXED_OBJECTS_LIST = ImmutableList.of("1", 1, 1L, 1);

  @Test
  public void testAllElementsEqual_emptyArray() {
    assertThrows(
        IllegalArgumentException.class, () -> Collections3.allElementsEqual(ImmutableList.of()));
  }

  @Test
  public void testAllElementsEqual_emptyList() {
    assertThrows(
        IllegalArgumentException.class, () -> Collections3.allElementsEqual(new String[0]));
  }

  @Test
  public void testAllElementsEqual_emptyStream() {
    assertThrows(IllegalArgumentException.class, () -> Collections3.allElementsEqual(Stream.of()));
  }

  @Test
  public void testFilterByClass() {
    assertThat(Collections3.filterByClass(MIXED_OBJECTS_LIST, Integer.class)).containsExactly(1, 1);
  }

  @Test
  public void testFilterByClass_NoMatch() {
    assertThat(Collections3.filterByClass(MIXED_OBJECTS_LIST, IOException.class)).isEmpty();
  }

  @Test
  public void testFilterByClass_AllMatch() {
    assertThat(Collections3.filterByClass(MIXED_OBJECTS_LIST, Object.class))
        .containsExactlyElementsIn(MIXED_OBJECTS_LIST)
        .inOrder();
  }

  @Test
  @SuppressWarnings("JdkObsolete") // we want to test that method
  public void testSubMapWithPrefix() {
    NavigableMap<String, Void> resultMap = new TreeMap<>();
    resultMap.put("b", null);
    resultMap.put("b" + 0, null);
    resultMap.put("b1", null);
    resultMap.put("b2", null);
    resultMap.put("b" + Character.MAX_VALUE, null);

    NavigableMap<String, Void> testMap = new TreeMap<>();
    testMap.putAll(resultMap);
    testMap.put("", null);
    testMap.put("a", null);
    testMap.put("a" + Character.MAX_VALUE, null);
    testMap.put("c", null);
    testMap.put("c" + 0, null);
    testMap.put("ca", null);

    assertThat(Collections3.subMapWithPrefix(testMap, "b")).isEqualTo(resultMap);
    assertThat(Collections3.subMapWithPrefix((SortedMap<String, Void>) testMap, "b"))
        .isEqualTo(resultMap);
  }

  @Test
  @SuppressWarnings("JdkObsolete") // we want to test that method
  public void testSubSetWithPrefix() {
    NavigableSet<String> resultSet = new TreeSet<>();
    resultSet.add("b");
    resultSet.add("b" + 0);
    resultSet.add("b1");
    resultSet.add("b2");
    resultSet.add("b" + Character.MAX_VALUE);

    NavigableSet<String> testSet = new TreeSet<>();
    testSet.addAll(resultSet);
    testSet.add("");
    testSet.add("a");
    testSet.add("a" + Character.MAX_VALUE);
    testSet.add("c");
    testSet.add("c" + 0);
    testSet.add("ca");

    assertThat(Collections3.subSetWithPrefix(testSet, "b")).isEqualTo(resultSet);
    assertThat(Collections3.subSetWithPrefix((SortedSet<String>) testSet, "b"))
        .isEqualTo(resultSet);
  }
}
