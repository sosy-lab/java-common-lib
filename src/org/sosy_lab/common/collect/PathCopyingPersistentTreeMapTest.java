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

import static org.junit.Assert.*;

import java.util.Random;
import java.util.TreeMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Iterables;


public class PathCopyingPersistentTreeMapTest {

  PersistentSortedMap<String, String> map;

  @Before
  public void setupMap() {
    map = PathCopyingPersistentTreeMap.of();
  }

  @After
  public void deleteMap() {
    map = null;
  }

  private void put(String key, String value) {
    PersistentSortedMap<String, String> oldMap = map;
    int oldMapSize = oldMap.size();
    String oldMapStr = oldMap.toString();

    map = map.putAndCopy(key, value);

    assertEquals(oldMapSize, oldMap.size());
    assertEquals(oldMapStr, oldMap.toString());

    assertEquals(value, map.get(key));

    if (oldMap.containsKey(key)) {
      assertEquals(oldMap.size(), map.size());

      if (oldMap.get(key).equals(value)) {
        assertEquals(oldMap.toString(), map.toString());
        assertEquals(oldMap.hashCode(), map.hashCode());
        assertEquals(oldMap, map);
      } else {
        assertFalse(map.equals(oldMap));
      }

    } else {
      assertEquals(oldMap.size()+1, map.size());
      assertFalse(map.equals(oldMap));
    }
  }

  private void remove(String key) {
    PersistentSortedMap<String, String> oldMap = map;
    int oldMapSize = oldMap.size();
    String oldMapStr = oldMap.toString();

    map = map.removeAndCopy(key);

    assertEquals(oldMapSize, oldMap.size());
    assertEquals(oldMapStr, oldMap.toString());

    assertFalse(map.containsKey(key));

    if (oldMap.containsKey(key)) {
      assertEquals(oldMap.size()-1, map.size());
      assertFalse(map.equals(oldMap));

    } else {
      assertEquals(oldMap.size(), map.size());
      assertEquals(oldMap.toString(), map.toString());
      assertEquals(oldMap.hashCode(), map.hashCode());
      assertEquals(oldMap, map);
    }
  }

  @Test
  public void testEmpty() {
    assertEquals(map.toString(), "[]");
    assertTrue(map.isEmpty());
    assertEquals(0, map.size());
    assertEquals(0, map.hashCode());
  }

  @Test
  public void putABCD() {
    put("a", "1");
    assertEquals(map.toString(), "[a=1]");

    put("b", "2");
    assertEquals(map.toString(), "[a=1, b=2]");

    put("c", "3");
    assertEquals(map.toString(), "[a=1, b=2, c=3]");

    put("d", "4");
    assertEquals(map.toString(), "[a=1, b=2, c=3, d=4]");
  }

  @Test
  public void removeDCBA() {
    remove("d");
    remove("c");
    remove("b");
    remove("a");
  }

  @Test
  public void putDCBA() {
    put("d", "1");
    assertEquals(map.toString(), "[d=1]");

    put("c", "2");
    assertEquals(map.toString(), "[c=2, d=1]");

    put("b", "3");
    assertEquals(map.toString(), "[b=3, c=2, d=1]");

    put("a", "4");
    assertEquals(map.toString(), "[a=4, b=3, c=2, d=1]");
  }

  @Test
  public void removeABCD() {
    remove("a");
    remove("b");
    remove("c");
    remove("d");
  }

  @Test
  public void testRight() {
    putABCD();
    removeDCBA();
    testEmpty();
  }

  @Test
  public void testLeft() {
    putDCBA();
    removeABCD();
    testEmpty();
  }

  @Test
  public void testRightLeft() {
    putABCD();
    removeABCD();
    testEmpty();
  }

  @Test
  public void testLeftRight() {
    putDCBA();
    removeDCBA();
    testEmpty();
  }

  @Test
  public void testInner() {
    put("a", "1");
    assertEquals(map.toString(), "[a=1]");

    put("z", "2");
    assertEquals(map.toString(), "[a=1, z=2]");

    put("b", "3");
    assertEquals(map.toString(), "[a=1, b=3, z=2]");

    put("y", "4");
    assertEquals(map.toString(), "[a=1, b=3, y=4, z=2]");

    put("c", "5");
    assertEquals(map.toString(), "[a=1, b=3, c=5, y=4, z=2]");

    put("x", "6");
    assertEquals(map.toString(), "[a=1, b=3, c=5, x=6, y=4, z=2]");

    put("d", "7");
    assertEquals(map.toString(), "[a=1, b=3, c=5, d=7, x=6, y=4, z=2]");

    put("w", "8");
    assertEquals(map.toString(), "[a=1, b=3, c=5, d=7, w=8, x=6, y=4, z=2]");
  }

  @Test
  public void testOuter() {
    put("d", "1");
    assertEquals(map.toString(), "[d=1]");

    put("w", "2");
    assertEquals(map.toString(), "[d=1, w=2]");

    put("c", "3");
    assertEquals(map.toString(), "[c=3, d=1, w=2]");

    put("x", "4");
    assertEquals(map.toString(), "[c=3, d=1, w=2, x=4]");

    put("b", "5");
    assertEquals(map.toString(), "[b=5, c=3, d=1, w=2, x=4]");

    put("y", "6");
    assertEquals(map.toString(), "[b=5, c=3, d=1, w=2, x=4, y=6]");

    put("a", "7");
    assertEquals(map.toString(), "[a=7, b=5, c=3, d=1, w=2, x=4, y=6]");

    put("z", "8");
    assertEquals(map.toString(), "[a=7, b=5, c=3, d=1, w=2, x=4, y=6, z=8]");
  }

  @Test
  public void testRandom() {
    Random rnd = new Random(3987432434L); // static seed for reproducibility
    TreeMap<String, String> comparison = new TreeMap<>();

    // Insert 1000 nodes
    for (int i = 0; i < 1000; i++) {
      String key = rnd.nextInt() + "";
      String value = rnd.nextInt() + "";

      put(key, value);
      comparison.put(key, value);
      checkEqualTo(comparison);
    }

    // 1000 random put/remove operations
    for (int i = 0; i < 1000; i++) {
      String key = rnd.nextInt() + "";

      if (rnd.nextBoolean()) {
        String value = rnd.nextInt() + "";
        put(key, value);
        comparison.put(key, value);
      } else {
        remove(key);
        comparison.remove(key);
      }

      checkEqualTo(comparison);
    }

    // clear map
    while (!map.isEmpty()) {
      String key = rnd.nextBoolean() ? map.firstKey() : map.lastKey();
      remove(key);
      comparison.remove(key);
      checkEqualTo(comparison);
    }

    testEmpty();
  }

  private void checkEqualTo(TreeMap<String, String> comparison) {
    assertEquals(comparison, map);
    assertEquals(comparison.size(), map.size());
    assertEquals(comparison.hashCode(), map.hashCode());
    assertTrue(Iterables.elementsEqual(comparison.entrySet(), map.entrySet()));
    assertTrue(Iterables.elementsEqual(comparison.keySet(),   map.keySet()));
    assertTrue(Iterables.elementsEqual(comparison.values(),   map.values()));
  }
}
