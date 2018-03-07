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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import com.google.common.collect.Ordering;
import com.google.common.collect.testing.NavigableMapTestSuiteBuilder;
import com.google.common.collect.testing.TestStringSortedMapGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.testing.EqualsTester;
import com.google.errorprone.annotations.Var;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import junit.framework.JUnit4TestAdapter;
import junit.framework.TestSuite;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PathCopyingPersistentTreeMapTest {

  private static final TestStringSortedMapGenerator mapGenerator =
      new TestStringSortedMapGenerator() {

        @Override
        protected SortedMap<String, String> create(Entry<String, String>[] pEntries) {
          @Var PersistentSortedMap<String, String> result = PathCopyingPersistentTreeMap.of();
          for (Entry<String, String> entry : pEntries) {
            result = result.putAndCopy(entry.getKey(), entry.getValue());
          }
          return result;
        }
      };

  public static junit.framework.Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(new JUnit4TestAdapter(PathCopyingPersistentTreeMapTest.class));

    suite.addTest(
        NavigableMapTestSuiteBuilder.using(mapGenerator)
            .named("PathCopyingPersistentTreeMap")
            .withFeatures(
                MapFeature.ALLOWS_NULL_VALUES,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.SERIALIZABLE_INCLUDING_VIEWS,
                CollectionSize.ANY)
            .createTestSuite());

    return suite;
  }

  private PersistentSortedMap<String, String> map;

  @Before
  public void setUp() {
    map = PathCopyingPersistentTreeMap.of();
  }

  @After
  public void tearDown() {
    map = null;
  }

  private void put(String key, String value) {
    PersistentSortedMap<String, String> oldMap = map;
    int oldMapSize = oldMap.size();
    String oldMapStr = oldMap.toString();

    map = map.putAndCopy(key, value);
    ((PathCopyingPersistentTreeMap<?, ?>) map).checkAssertions();

    assertThat(oldMap).hasSize(oldMapSize);
    assertThat(oldMap.toString()).isEqualTo(oldMapStr);

    assertThat(map).containsEntry(key, value);

    if (oldMap.containsKey(key)) {
      assertThat(map).hasSize(oldMap.size());

      if (oldMap.get(key).equals(value)) {
        assertThat(map.toString()).isEqualTo(oldMap.toString());
        new EqualsTester().addEqualityGroup(map, oldMap).testEquals();

      } else {
        assertThat(map).isNotEqualTo(oldMap);
      }

    } else {
      assertThat(map).hasSize(oldMap.size() + 1);
      assertThat(map).isNotEqualTo(oldMap);
    }
  }

  private void remove(String key) {
    PersistentSortedMap<String, String> oldMap = map;
    int oldMapSize = oldMap.size();
    String oldMapStr = oldMap.toString();

    map = map.removeAndCopy(key);
    ((PathCopyingPersistentTreeMap<?, ?>) map).checkAssertions();

    assertThat(oldMap).hasSize(oldMapSize);
    assertThat(oldMap.toString()).isEqualTo(oldMapStr);

    assertFalse(map.containsKey(key));

    if (oldMap.containsKey(key)) {
      assertThat(map).hasSize(oldMap.size() - 1);
      assertThat(map).isNotEqualTo(oldMap);

    } else {
      assertThat(map).hasSize(oldMap.size());
      assertThat(map.toString()).isEqualTo(oldMap.toString());
      new EqualsTester().addEqualityGroup(map, oldMap).testEquals();
    }
  }

  @Test
  public void testEmpty() {
    assertThat(map.toString()).isEqualTo("{}");
    assertThat(map).isEmpty();
    assertThat(map).hasSize(0);
    assertThat(map.hashCode()).isEqualTo(0);
  }

  private void putABCD() {
    put("a", "1");
    assertThat(map.toString()).isEqualTo("{a=1}");
    assertEquals("a", map.firstKey());
    assertEquals("a", map.lastKey());

    put("b", "2");
    assertThat(map.toString()).isEqualTo("{a=1, b=2}");
    assertEquals("a", map.firstKey());
    assertEquals("b", map.lastKey());

    put("c", "3");
    assertThat(map.toString()).isEqualTo("{a=1, b=2, c=3}");
    assertEquals("a", map.firstKey());
    assertEquals("c", map.lastKey());

    put("d", "4");
    assertThat(map.toString()).isEqualTo("{a=1, b=2, c=3, d=4}");
    assertEquals("a", map.firstKey());
    assertEquals("d", map.lastKey());
  }

  private void removeDCBA() {
    remove("d");
    remove("c");
    remove("b");
    remove("a");
  }

  private void putDCBA() {
    put("d", "1");
    assertThat(map.toString()).isEqualTo("{d=1}");
    assertThat(map.firstKey()).isEqualTo("d");
    assertThat(map.lastKey()).isEqualTo("d");

    put("c", "2");
    assertThat(map.toString()).isEqualTo("{c=2, d=1}");
    assertThat(map.firstKey()).isEqualTo("c");
    assertThat(map.lastKey()).isEqualTo("d");

    put("b", "3");
    assertThat(map.toString()).isEqualTo("{b=3, c=2, d=1}");
    assertThat(map.firstKey()).isEqualTo("b");
    assertThat(map.lastKey()).isEqualTo("d");

    put("a", "4");
    assertThat(map.toString()).isEqualTo("{a=4, b=3, c=2, d=1}");
    assertThat(map.firstKey()).isEqualTo("a");
    assertThat(map.lastKey()).isEqualTo("d");
  }

  private void removeABCD() {
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
    assertThat(map.toString()).isEqualTo("{a=1}");

    put("z", "2");
    assertThat(map.toString()).isEqualTo("{a=1, z=2}");

    put("b", "3");
    assertThat(map.toString()).isEqualTo("{a=1, b=3, z=2}");

    put("y", "4");
    assertThat(map.toString()).isEqualTo("{a=1, b=3, y=4, z=2}");

    put("c", "5");
    assertThat(map.toString()).isEqualTo("{a=1, b=3, c=5, y=4, z=2}");

    put("x", "6");
    assertThat(map.toString()).isEqualTo("{a=1, b=3, c=5, x=6, y=4, z=2}");

    put("d", "7");
    assertThat(map.toString()).isEqualTo("{a=1, b=3, c=5, d=7, x=6, y=4, z=2}");

    put("w", "8");
    assertThat(map.toString()).isEqualTo("{a=1, b=3, c=5, d=7, w=8, x=6, y=4, z=2}");
  }

  @Test
  public void testOuter() {
    put("d", "1");
    assertThat(map.toString()).isEqualTo("{d=1}");

    put("w", "2");
    assertThat(map.toString()).isEqualTo("{d=1, w=2}");

    put("c", "3");
    assertThat(map.toString()).isEqualTo("{c=3, d=1, w=2}");

    put("x", "4");
    assertThat(map.toString()).isEqualTo("{c=3, d=1, w=2, x=4}");

    put("b", "5");
    assertThat(map.toString()).isEqualTo("{b=5, c=3, d=1, w=2, x=4}");

    put("y", "6");
    assertThat(map.toString()).isEqualTo("{b=5, c=3, d=1, w=2, x=4, y=6}");

    put("a", "7");
    assertThat(map.toString()).isEqualTo("{a=7, b=5, c=3, d=1, w=2, x=4, y=6}");

    put("z", "8");
    assertThat(map.toString()).isEqualTo("{a=7, b=5, c=3, d=1, w=2, x=4, y=6, z=8}");
  }

  @Test
  public void testRandom() {
    int iterations = 50;
    Random rnd = new Random(3987432434L); // static seed for reproducibility
    SortedMap<String, String> comparison = new TreeMap<>();

    // Insert nodes
    for (int i = 0; i < iterations; i++) {
      String key = Integer.toString(rnd.nextInt());
      String value = Integer.toString(rnd.nextInt());

      put(key, value);
      comparison.put(key, value);
      checkEqualTo(comparison);
      checkPartialMaps(comparison, rnd);
    }

    // random put/remove operations
    for (int i = 0; i < iterations; i++) {
      String key = Integer.toString(rnd.nextInt());

      if (rnd.nextBoolean()) {
        String value = Integer.toString(rnd.nextInt());
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
    String key1 = Integer.toString(rnd.nextInt());
    String key2 = Integer.toString(rnd.nextInt());

    checkEqualTo(comparison.tailMap(key1), map.tailMap(key1));
    checkEqualTo(comparison.tailMap(key2), map.tailMap(key2));
    checkEqualTo(comparison.headMap(key1), map.headMap(key1));
    checkEqualTo(comparison.headMap(key2), map.headMap(key2));

    String lowKey = Ordering.natural().min(key1, key2);
    String highKey = Ordering.natural().max(key1, key2);
    checkEqualTo(comparison.subMap(lowKey, highKey), map.subMap(lowKey, highKey));
  }

  private void checkEqualTo(SortedMap<String, String> comparison) {
    checkEqualTo(comparison, map);
  }

  private static void checkEqualTo(
      SortedMap<String, String> comparison, SortedMap<String, String> testMap) {
    assertEquals(comparison, testMap);
    assertEquals(comparison.hashCode(), testMap.hashCode());
    assertThat(testMap.isEmpty()).named("isEmpty").isEqualTo(comparison.isEmpty());
    assertThat(testMap).hasSize(comparison.size());
    checkEqualTo(comparison.entrySet(), testMap.entrySet());
    checkEqualTo(comparison.keySet(), testMap.keySet());
    checkEqualTo(comparison.values(), testMap.values());
    if (!comparison.isEmpty()) {
      assertThat(testMap.firstKey()).named("firstKey").isEqualTo(comparison.firstKey());
      assertThat(testMap.lastKey()).named("lastKey").isEqualTo(comparison.lastKey());
    }
  }

  private static <T> void checkEqualTo(Set<T> comparison, Set<T> set) {
    assertEquals(comparison, set);
    assertEquals(comparison.hashCode(), set.hashCode());
    checkEqualTo((Collection<T>) comparison, (Collection<T>) set);
  }

  private static <T> void checkEqualTo(Collection<T> comparison, Collection<T> set) {
    // equals() and hashCode() is undefined for Collections
    assertThat(set.isEmpty()).named("isEmpty").isEqualTo(comparison.isEmpty());
    assertThat(set).hasSize(comparison.size());
    assertThat(set).containsExactlyElementsIn(comparison).inOrder();
  }

  @Test
  public void testSubmapSubmap() {
    map = map.putAndCopy("a", "a").putAndCopy("b", "b").putAndCopy("c", "c");

    SortedMap<String, String> submap = map.subMap("aa", "c");
    assertThat(submap).containsExactly("b", "b");

    // The bounds of further submap calls may be at most those of the original call
    @Var SortedMap<String, String> subsubmap = submap.subMap("aaa", "c");
    assertThat(subsubmap).containsExactly("b", "b");

    subsubmap = submap.subMap("aa", "bb");
    assertThat(subsubmap).containsExactly("b", "b");

    subsubmap = submap.subMap("aaa", "bb");
    assertThat(subsubmap).containsExactly("b", "b");

    try {
      submap.subMap("a", "c");
      fail();
    } catch (IllegalArgumentException expected) {
    }

    try {
      submap.subMap("aa", "d");
      fail();
    } catch (IllegalArgumentException expected) {
    }

    try {
      submap.subMap("a", "d");
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }
}
