/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.Test;

public class Collections3Test {

  @Test
  public void testSubMapWithPrefix() {
    SortedMap<String, Void> resultMap = new TreeMap<>();
    resultMap.put("b", null);
    resultMap.put("b" + 0, null);
    resultMap.put("b1", null);
    resultMap.put("b2", null);
    resultMap.put("b" + Character.MAX_VALUE, null);

    SortedMap<String, Void> testMap = new TreeMap<>();
    testMap.putAll(resultMap);
    testMap.put("", null);
    testMap.put("a", null);
    testMap.put("a" + Character.MAX_VALUE, null);
    testMap.put("c", null);
    testMap.put("c" + 0, null);
    testMap.put("ca", null);

    assertThat(Collections3.subMapWithPrefix(testMap, "b")).isEqualTo(resultMap);
  }

  @Test
  public void testSubSetWithPrefix() {
    SortedSet<String> resultSet = new TreeSet<>();
    resultSet.add("b");
    resultSet.add("b" + 0);
    resultSet.add("b1");
    resultSet.add("b2");
    resultSet.add("b" + Character.MAX_VALUE);

    SortedSet<String> testSet = new TreeSet<>();
    testSet.addAll(resultSet);
    testSet.add("");
    testSet.add("a");
    testSet.add("a" + Character.MAX_VALUE);
    testSet.add("c");
    testSet.add("c" + 0);
    testSet.add("ca");

    assertThat(Collections3.subSetWithPrefix(testSet, "b")).isEqualTo(resultSet);
  }
}
