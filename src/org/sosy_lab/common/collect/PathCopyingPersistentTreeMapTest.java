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

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


class PathCopyingPersistentTreeMapTest {

  PersistentMap<String, String> map;

  @Before
  public void setupMap() {
    map = PathCopyingPersistentTreeMap.of();
  }

  @After
  public void deleteMap() {
    map = null;
  }


  @Test
  public void testRightmostInsert() {
    map = map.putAndCopy("a", "1");

    assertEquals(map.get("a"), "1");
    assertEquals(map.toString(), "[a=1]");

    map = map.putAndCopy("b", "2");
    assertEquals(map.get("a"), "1");
    assertEquals(map.get("b"), "2");
    assertEquals(map.toString(), "[a=1, b=2]");

    map = map.putAndCopy("c", "3");
    assertEquals(map.get("a"), "1");
    assertEquals(map.get("b"), "2");
    assertEquals(map.get("c"), "3");
    assertEquals(map.toString(), "[a=1, b=2, c=3]");

    map = map.putAndCopy("d", "4");
    assertEquals(map.get("a"), "1");
    assertEquals(map.get("b"), "2");
    assertEquals(map.get("c"), "3");
    assertEquals(map.get("d"), "4");
    assertEquals(map.toString(), "[a=1, b=2, c=3, d=4]");
  }

  @Test
  public void testLeftmostInsert() {
    map = map.putAndCopy("z", "1");

    assertEquals(map.get("z"), "1");
    assertEquals(map.toString(), "[z=1]");

    map = map.putAndCopy("y", "2");
    assertEquals(map.get("z"), "1");
    assertEquals(map.get("y"), "2");
    assertEquals(map.toString(), "[y=2, z=1]");

    map = map.putAndCopy("x", "3");
    assertEquals(map.get("z"), "1");
    assertEquals(map.get("y"), "2");
    assertEquals(map.get("x"), "3");
    assertEquals(map.toString(), "[x=3, y=2, z=1]");

    map = map.putAndCopy("w", "4");
    assertEquals(map.get("z"), "1");
    assertEquals(map.get("y"), "2");
    assertEquals(map.get("x"), "3");
    assertEquals(map.get("w"), "4");
    assertEquals(map.toString(), "[w=4, x=3, y=2, z=1]");
  }

  @Test
  public void testInnerInsert() {
    map = map.putAndCopy("a", "1");

    assertEquals(map.get("a"), "1");
    assertEquals(map.toString(), "[a=1]");

    map = map.putAndCopy("z", "2");
    assertEquals(map.get("a"), "1");
    assertEquals(map.get("z"), "2");
    assertEquals(map.toString(), "[a=1, z=2]");

    map = map.putAndCopy("b", "3");
    assertEquals(map.get("a"), "1");
    assertEquals(map.get("z"), "2");
    assertEquals(map.get("b"), "3");
    assertEquals(map.toString(), "[a=1, b=3, z=2]");

    map = map.putAndCopy("y", "4");
    assertEquals(map.get("a"), "1");
    assertEquals(map.get("z"), "2");
    assertEquals(map.get("b"), "3");
    assertEquals(map.get("y"), "4");
    assertEquals(map.toString(), "[a=1, b=3, y=4, z=2]");

    map = map.putAndCopy("c", "5");
    assertEquals(map.get("a"), "1");
    assertEquals(map.get("z"), "2");
    assertEquals(map.get("b"), "3");
    assertEquals(map.get("y"), "4");
    assertEquals(map.get("c"), "5");
    assertEquals(map.toString(), "[a=1, b=3, c=5, y=4, z=2]");

    map = map.putAndCopy("x", "6");
    assertEquals(map.get("a"), "1");
    assertEquals(map.get("z"), "2");
    assertEquals(map.get("b"), "3");
    assertEquals(map.get("y"), "4");
    assertEquals(map.get("c"), "5");
    assertEquals(map.get("x"), "6");
    assertEquals(map.toString(), "[a=1, b=3, c=5, x=6, y=4, z=2]");

    map = map.putAndCopy("d", "7");
    assertEquals(map.get("a"), "1");
    assertEquals(map.get("z"), "2");
    assertEquals(map.get("b"), "3");
    assertEquals(map.get("y"), "4");
    assertEquals(map.get("c"), "5");
    assertEquals(map.get("x"), "6");
    assertEquals(map.get("d"), "7");
    assertEquals(map.toString(), "[a=1, b=3, c=5, d=7, x=6, y=4, z=2]");

    map = map.putAndCopy("w", "8");
    assertEquals(map.get("a"), "1");
    assertEquals(map.get("z"), "2");
    assertEquals(map.get("b"), "3");
    assertEquals(map.get("y"), "4");
    assertEquals(map.get("c"), "5");
    assertEquals(map.get("x"), "6");
    assertEquals(map.get("d"), "7");
    assertEquals(map.get("w"), "8");
    assertEquals(map.toString(), "[a=1, b=3, c=5, d=7, w=8, x=6, y=4, z=2]");
  }

  @Test
  public void testOuterInsert() {
    map = map.putAndCopy("d", "1");

    assertEquals(map.get("d"), "1");
    assertEquals(map.toString(), "[d=1]");

    map = map.putAndCopy("w", "2");
    assertEquals(map.get("d"), "1");
    assertEquals(map.get("w"), "2");
    assertEquals(map.toString(), "[d=1, w=2]");

    map = map.putAndCopy("c", "3");
    assertEquals(map.get("d"), "1");
    assertEquals(map.get("w"), "2");
    assertEquals(map.get("c"), "3");
    assertEquals(map.toString(), "[c=3, d=1, w=2]");

    map = map.putAndCopy("x", "4");
    assertEquals(map.get("d"), "1");
    assertEquals(map.get("w"), "2");
    assertEquals(map.get("c"), "3");
    assertEquals(map.get("x"), "4");
    assertEquals(map.toString(), "[c=3, d=1, w=2, x=4]");

    map = map.putAndCopy("b", "5");
    assertEquals(map.get("d"), "1");
    assertEquals(map.get("w"), "2");
    assertEquals(map.get("c"), "3");
    assertEquals(map.get("x"), "4");
    assertEquals(map.get("b"), "5");
    assertEquals(map.toString(), "[b=5, c=3, d=1, w=2, x=4]");

    map = map.putAndCopy("y", "6");
    assertEquals(map.get("d"), "1");
    assertEquals(map.get("w"), "2");
    assertEquals(map.get("c"), "3");
    assertEquals(map.get("x"), "4");
    assertEquals(map.get("b"), "5");
    assertEquals(map.get("y"), "6");
    assertEquals(map.toString(), "[b=5, c=3, d=1, w=2, x=4, y=6]");

    map = map.putAndCopy("a", "7");
    assertEquals(map.get("d"), "1");
    assertEquals(map.get("w"), "2");
    assertEquals(map.get("c"), "3");
    assertEquals(map.get("x"), "4");
    assertEquals(map.get("b"), "5");
    assertEquals(map.get("y"), "6");
    assertEquals(map.get("a"), "7");
    assertEquals(map.toString(), "[a=7, b=5, c=3, d=1, w=2, x=4, y=6]");

    map = map.putAndCopy("z", "8");
    assertEquals(map.get("d"), "1");
    assertEquals(map.get("w"), "2");
    assertEquals(map.get("c"), "3");
    assertEquals(map.get("x"), "4");
    assertEquals(map.get("b"), "5");
    assertEquals(map.get("y"), "6");
    assertEquals(map.get("a"), "7");
    assertEquals(map.get("z"), "8");
    assertEquals(map.toString(), "[a=7, b=5, c=3, d=1, w=2, x=4, y=6, z=8]");
  }
}
