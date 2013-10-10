/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.common.collect;

import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.Assert;
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

    Assert.assertEquals(resultMap, Collections3.subMapWithPrefix(testMap, "b"));
  }
}
