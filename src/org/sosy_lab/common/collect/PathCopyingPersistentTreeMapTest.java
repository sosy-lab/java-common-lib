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

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.google.common.collect.testing.SortedMapTestSuiteBuilder;
import com.google.common.collect.testing.TestStringSortedMapGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.testers.MapEntrySetTester;

public class PathCopyingPersistentTreeMapTest extends TestCase {

  private static final TestStringSortedMapGenerator mapGenerator = new TestStringSortedMapGenerator() {

    @Override
    protected SortedMap<String, String> create(Entry<String, String>[] pEntries) {
      PersistentSortedMap<String, String> result = PathCopyingPersistentTreeMap.of();
      for (Entry<String, String> entry : pEntries) {
        result = result.putAndCopy(entry.getKey(), entry.getValue());
      }
      return result;
    }
  };

  public static junit.framework.Test suite() throws NoSuchMethodException, SecurityException {
    TestSuite suite = new TestSuite();
    suite.addTestSuite(PathCopyingPersistentTreeMapTest.class);

    suite.addTest(SortedMapTestSuiteBuilder.using(mapGenerator)
        .named("PathCopyingPersistentTreeMap")
        .withFeatures(MapFeature.ALLOWS_NULL_VALUES,
                      CollectionFeature.KNOWN_ORDER,
                      CollectionSize.ANY)

        // We throw ClassCastException as allowed by the JavaDoc of SortedMap
        .suppressing(MapEntrySetTester.class.getMethod("testContainsEntryWithIncomparableKey"))

        .createTestSuite());

    return suite;
  }

  private PersistentSortedMap<String, String> map;

  @Override
  @Before
  public void setUp() {
    map = PathCopyingPersistentTreeMap.of();
  }

  @Override
  @After
  public void tearDown() {
    map = null;
  }

  private void put(String key, String value) {
    PersistentSortedMap<String, String> oldMap = map;
    int oldMapSize = oldMap.size();
    String oldMapStr = oldMap.toString();

    map = map.putAndCopy(key, value);
    ((PathCopyingPersistentTreeMap<?, ?>)map).checkAssertions();

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
    ((PathCopyingPersistentTreeMap<?, ?>)map).checkAssertions();

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
    assertEquals("a", map.firstKey());
    assertEquals("a", map.lastKey());

    put("b", "2");
    assertEquals(map.toString(), "[a=1, b=2]");
    assertEquals("a", map.firstKey());
    assertEquals("b", map.lastKey());

    put("c", "3");
    assertEquals(map.toString(), "[a=1, b=2, c=3]");
    assertEquals("a", map.firstKey());
    assertEquals("c", map.lastKey());

    put("d", "4");
    assertEquals(map.toString(), "[a=1, b=2, c=3, d=4]");
    assertEquals("a", map.firstKey());
    assertEquals("d", map.lastKey());
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
    assertEquals("d", map.firstKey());
    assertEquals("d", map.lastKey());

    put("c", "2");
    assertEquals(map.toString(), "[c=2, d=1]");
    assertEquals("c", map.firstKey());
    assertEquals("d", map.lastKey());

    put("b", "3");
    assertEquals(map.toString(), "[b=3, c=2, d=1]");
    assertEquals("b", map.firstKey());
    assertEquals("d", map.lastKey());

    put("a", "4");
    assertEquals(map.toString(), "[a=4, b=3, c=2, d=1]");
    assertEquals("a", map.firstKey());
    assertEquals("d", map.lastKey());
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

    // Insert 500 nodes
    for (int i = 0; i < 500; i++) {
      String key = rnd.nextInt() + "";
      String value = rnd.nextInt() + "";

      put(key, value);
      comparison.put(key, value);
      checkEqualTo(comparison);
      checkPartialMaps(comparison, rnd);
    }

    // 500 random put/remove operations
    for (int i = 0; i < 500; i++) {
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
      checkPartialMaps(comparison, rnd);
    }

    // clear map
    while (!map.isEmpty()) {
      String key = rnd.nextBoolean() ? map.firstKey() : map.lastKey();
      remove(key);
      comparison.remove(key);
      checkEqualTo(comparison);
      checkPartialMaps(comparison, rnd);
    }

    testEmpty();
  }

  private void checkPartialMaps(SortedMap<String, String> comparison, Random rnd) {
    String key1 = rnd.nextInt() + "";
    String key2 = rnd.nextInt() + "";

    checkEqualTo(comparison.tailMap(key1), map.tailMap(key1));
    checkEqualTo(comparison.tailMap(key2), map.tailMap(key2));
    checkEqualTo(comparison.headMap(key1), map.headMap(key1));
    checkEqualTo(comparison.headMap(key2), map.headMap(key2));

    String lowKey  = Ordering.natural().min(key1, key2);
    String highKey = Ordering.natural().max(key1, key2);
    checkEqualTo(comparison.subMap(lowKey, highKey), map.subMap(lowKey, highKey));
  }

  private void checkEqualTo(SortedMap<String, String> comparison) {
    checkEqualTo(comparison, map);
  }

  private void checkEqualTo(SortedMap<String, String> comparison, SortedMap<String, String> map) {
    assertEquals(comparison, map);
    assertEquals(comparison.isEmpty(), map.isEmpty());
    assertEquals(comparison.size(), map.size());
    assertEquals(comparison.hashCode(), map.hashCode());
    checkEqualTo(comparison.entrySet(), map.entrySet());
    checkEqualTo(comparison.keySet(),   map.keySet());
    checkEqualTo(comparison.values(),   map.values());
    if (!comparison.isEmpty()) {
      assertEquals(comparison.firstKey(), map.firstKey());
      assertEquals(comparison.lastKey(), map.lastKey());
    }
  }

  private <T> void checkEqualTo(Set<T> comparison, Set<T> set) {
    assertEquals(comparison, set);
    assertEquals(comparison.hashCode(), set.hashCode());
    checkEqualTo((Collection<T>)comparison, (Collection<T>)set);
  }

  private <T> void checkEqualTo(Collection<T> comparison, Collection<T> set) {
    // equals() and hashCode() is undefined for Collections
    assertEquals(comparison.isEmpty(), set.isEmpty());
    assertEquals(comparison.size(), set.size());
    assertTrue(Iterables.elementsEqual(comparison, set));
  }
}
