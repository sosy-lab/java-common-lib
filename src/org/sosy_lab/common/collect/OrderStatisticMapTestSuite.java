/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.testing.TestStringSortedMapGenerator;
import com.google.common.testing.SerializableTester;
import com.google.errorprone.annotations.Var;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import org.junit.Assert;
import org.junit.Test;

public abstract class OrderStatisticMapTestSuite {

  @SuppressWarnings("unchecked")
  private static final ImmutableList<Entry<String, String>> ELEMS =
      ImmutableList.<Entry<String, String>>builder()
          .add(Maps.immutableEntry("aaa", "Vzza"))
          .add(Maps.immutableEntry("hha", "Vppa"))
          .add(Maps.immutableEntry("ppa", "Vhha"))
          .add(Maps.immutableEntry("zza", "Vaaa"))
          .build();

  @SuppressWarnings("unchecked")
  private static final ImmutableList<Entry<String, String>> ELEMS_ABOVE =
      ImmutableList.<Entry<String, String>>builder()
          .add(Maps.immutableEntry("aab", "Vzzb"))
          .add(Maps.immutableEntry("hhb", "Vppb"))
          .add(Maps.immutableEntry("ppb", "Vhhb"))
          .add(Maps.immutableEntry("zzb", "Vaab"))
          .build();

  @SuppressWarnings("unchecked")
  private static final ImmutableList<Entry<String, String>> ELEMS_BELOW =
      ImmutableList.<Entry<String, String>>builder()
          .add(Maps.immutableEntry("aa", "Vzz"))
          .add(Maps.immutableEntry("hh", "Vpp"))
          .add(Maps.immutableEntry("pp", "Vhh"))
          .add(Maps.immutableEntry("zz", "Vaa"))
          .build();

  public abstract static class OrderStatisticMapFactory extends TestStringSortedMapGenerator {

    @Override
    protected abstract OrderStatisticMap<String, String> create(Entry<String, String>[] pEntries);

    protected abstract OrderStatisticMap<String, String> create(
        List<Entry<String, String>> pEntries);
  }

  private OrderStatisticMapFactory factory;

  public OrderStatisticMapTestSuite(OrderStatisticMapFactory pFactory) {
    factory = pFactory;
  }

  @SuppressWarnings("unchecked")
  private OrderStatisticMap<String, String> createMap() {
    return factory.create(Collections.emptyList());
  }

  @SuppressWarnings("unchecked")
  private OrderStatisticMap<String, String> createMap(List<Entry<String, String>> pEntries) {
    return factory.create(pEntries);
  }

  private static <K, V> boolean containsEntry(Map<K, V> pMap, Entry<K, V> pEntry) {
    return pMap.containsKey(pEntry.getKey()) && pEntry.getValue().equals(pMap.get(pEntry.getKey()));
  }

  private static <K, V> void putEntry(Map<K, V> pMap, Entry<K, V> pEntry) {
    pMap.put(pEntry.getKey(), pEntry.getValue());
  }

  private static <K, V> void removeEntry(Map<K, V> pMap, Entry<K, V> pEntry) {
    Assert.assertEquals(pMap.get(pEntry.getKey()), pMap.remove(pEntry.getKey()));
  }

  @Test
  public void testEquals() {
    OrderStatisticMap<String, String> l1 = createMap();
    @Var OrderStatisticMap<String, String> l2 = createMap();

    // Check that the map sorts its elements
    Assert.assertEquals(l1, l2);
    for (int i = ELEMS.size() - 1; i >= 0; i--) {
      l2.put(ELEMS.get(i).getKey(), ELEMS.get(i).getValue());
    }
    for (int i = 0; i < ELEMS.size(); i++) {
      l1.put(ELEMS.get(i).getKey(), ELEMS.get(i).getValue());
    }
    Assert.assertEquals(l1, l2);

    // Check the map property
    l2 = createMap();
    for (int i = ELEMS.size() - 1; i >= 0; i--) {
      l2.put(ELEMS.get(i).getKey(), ELEMS.get(i).getValue());
    }
    for (int i = 0; i < ELEMS.size(); i++) {
      l2.put(ELEMS.get(i).getKey(), ELEMS.get(i).getValue());
    }
    Assert.assertEquals(l1, l2);
  }

  @Test
  public void testSerialize() {
    OrderStatisticMap<String, String> l = createMap();
    SerializableTester.reserializeAndAssert(l);

    for (int i = 100000; i >= 0; i--) {
      l.put(String.valueOf(i), String.valueOf(i));
    }
    SerializableTester.reserializeAndAssert(l);
  }

  @Test
  public void testSubmapView_mutation() {
    NavigableMap<String, String> map = createMap(ELEMS);
    NavigableMap<String, String> subMap =
        map.subMap(ELEMS.get(1).getKey(), true, ELEMS.get(2).getKey(), true);

    Entry<String, String> toAdd = ELEMS_BELOW.get(2);

    putEntry(subMap, toAdd);
    Assert.assertTrue(containsEntry(subMap, toAdd));
    Assert.assertTrue(containsEntry(map, toAdd));

    removeEntry(subMap, toAdd);
    Assert.assertFalse(map.containsKey(toAdd.getKey()));
    Assert.assertFalse(subMap.containsKey(toAdd.getKey()));

    putEntry(map, toAdd);
    Assert.assertTrue(containsEntry(subMap, toAdd));

    removeEntry(map, toAdd);
    Assert.assertFalse(containsEntry(subMap, toAdd));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSubmapView_outOfBounds_add() {
    NavigableMap<String, String> map = createMap(ELEMS);
    NavigableMap<String, String> subMap =
        map.subMap(
            ELEMS_ABOVE.get(0).getKey(), true,
            ELEMS_ABOVE.get(2).getKey(), true);

    try {
      putEntry(subMap, ELEMS.get(0));
    } catch (IllegalArgumentException e1) {
      try {
        putEntry(subMap, ELEMS.get(3));
      } catch (IllegalArgumentException e2) {
        // the first 3 elements are in the range of the sublist, but the last isn't
        Map<String, String> toAdd =
            ImmutableMap.<String, String>builder()
                .put(ELEMS.get(1))
                .put(ELEMS_BELOW.get(2))
                .put(ELEMS.get(2))
                .put(ELEMS_ABOVE.get(3))
                .build();
        subMap.putAll(toAdd);
      }
    }
  }

  @Test
  public void testSubmapView_outOfBounds_remove() {
    NavigableMap<String, String> map = createMap(ELEMS);
    NavigableMap<String, String> subMap =
        map.subMap(
            ELEMS_ABOVE.get(1).getKey(), true,
            ELEMS_ABOVE.get(2).getKey(), true);

    removeEntry(subMap, ELEMS.get(1));
    removeEntry(subMap, ELEMS.get(3));

    Assert.assertTrue(containsEntry(map, ELEMS.get(1)));
    Assert.assertTrue(containsEntry(map, ELEMS.get(3)));

    Map<String, String> toRemove =
        ImmutableMap.<String, String>builder()
            .put(ELEMS_BELOW.get(2))
            .put(ELEMS.get(2))
            .put(ELEMS.get(1))
            .build();
    for (Entry<String, String> e : toRemove.entrySet()) {
      removeEntry(subMap, e);
    }

    Assert.assertTrue(containsEntry(map, ELEMS.get(1)));
    Assert.assertFalse(containsEntry(map, ELEMS_BELOW.get(2)));
    Assert.assertFalse(containsEntry(map, ELEMS.get(2)));
  }

  @Test
  public void testSubmapView_outOfBounds_contains() {
    NavigableMap<String, String> map = createMap(ELEMS);
    @Var
    NavigableMap<String, String> subMap =
        map.subMap(
            ELEMS_ABOVE.get(1).getKey(), true,
            ELEMS_ABOVE.get(2).getKey(), true);

    Assert.assertFalse(containsEntry(subMap, ELEMS.get(1)));
    Assert.assertFalse(containsEntry(subMap, ELEMS.get(3)));

    subMap = map.subMap(ELEMS_ABOVE.get(1).getKey(), false, ELEMS_ABOVE.get(2).getKey(), false);

    Assert.assertFalse(containsEntry(subMap, ELEMS_ABOVE.get(1)));
    Assert.assertFalse(containsEntry(subMap, ELEMS_ABOVE.get(2)));

    subMap = map.subMap(ELEMS_ABOVE.get(0).getKey(), true, ELEMS_ABOVE.get(2).getKey(), true);

    Assert.assertTrue(containsEntry(subMap, ELEMS.get(1)));
    Assert.assertTrue(containsEntry(subMap, ELEMS.get(2)));
  }

  @Test
  public void testSubmapView_descending() {
    OrderStatisticMap<String, String> map = createMap(ELEMS);
    @Var
    OrderStatisticMap<String, String> subMap =
        map.subMap(
                ELEMS.get(1).getKey(), /* fromInclusive= */ true,
                ELEMS.get(2).getKey(), /* toInclusive= */ true)
            .descendingMap();

    Assert.assertEquals(ELEMS.get(2), subMap.firstEntry());
    Assert.assertEquals(ELEMS.get(1), subMap.lastEntry());

    subMap = subMap.descendingMap();
    Assert.assertEquals(ELEMS.get(1), subMap.firstEntry());
    Assert.assertEquals(ELEMS.get(2), subMap.lastEntry());
  }

  @Test
  public void testKeyset_mutation() {
    OrderStatisticMap<String, String> map = createMap(ELEMS);
    OrderStatisticSet<String> keySet = map.navigableKeySet();

    keySet.removeByRank(0);
    Assert.assertFalse(map.containsKey(ELEMS.get(0).getKey()));
  }

  @Test
  public void testSubmapView_submapOfSubmap() {
    OrderStatisticMap<String, String> map = createMap(ELEMS);
    NavigableMap<String, String> subMap =
        map.subMap(
            ELEMS.get(1).getKey(), /* fromInclusive= */ true,
            ELEMS.get(3).getKey(), /* toInclusive= */ true);
    @Var
    NavigableMap<String, String> subSubMap =
        subMap.subMap(
            ELEMS.get(1).getKey(), /* fromInclusive= */ true,
            ELEMS_BELOW.get(3).getKey(), /* toInclusive= */ true);

    Assert.assertFalse(containsEntry(subSubMap, ELEMS.get(0)));
    Assert.assertTrue(containsEntry(subSubMap, ELEMS.get(1)));
    Assert.assertTrue(containsEntry(subSubMap, ELEMS.get(2)));
    Assert.assertFalse(containsEntry(subSubMap, ELEMS.get(3)));

    // make sure that the inclusive-flags are respected
    subSubMap =
        subMap.subMap(
            ELEMS.get(1).getKey(), /* fromInclusive= */ true,
            ELEMS.get(3).getKey(), /* toInclusive=*/ true);
    Assert.assertTrue(containsEntry(subSubMap, ELEMS.get(1)));
    Assert.assertTrue(containsEntry(subSubMap, ELEMS.get(2)));
    Assert.assertTrue(containsEntry(subSubMap, ELEMS.get(3)));

    subSubMap =
        subSubMap.subMap(
            ELEMS.get(1).getKey(), /* fromInclusive= */ false,
            ELEMS.get(3).getKey(), /* toInclusive= */ false);
    Assert.assertFalse(containsEntry(subSubMap, ELEMS.get(1)));
    Assert.assertTrue(containsEntry(subSubMap, ELEMS.get(2)));
    Assert.assertFalse(containsEntry(subSubMap, ELEMS.get(3)));
  }

  @Test
  public void testGetByRank_valid() {
    OrderStatisticMap<String, String> map = createMap(ELEMS);

    for (int i = 0; i < ELEMS.size(); i++) {
      Assert.assertEquals(ELEMS.get(i), map.getEntryByRank(i));
    }
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void testGetByRank_outOfBounds() {
    OrderStatisticMap<String, String> map = createMap(ELEMS);

    try {
      Entry<?, ?> x = map.getEntryByRank(-1);
      Assert.assertFalse(x != null);
      Assert.fail("Expected " + IndexOutOfBoundsException.class.getSimpleName());
    } catch (IndexOutOfBoundsException e) {
      Entry<?, ?> x = map.getEntryByRank(ELEMS.size());
      Assert.assertFalse(x != null);
      Assert.fail("Expected " + IndexOutOfBoundsException.class.getSimpleName());
    }
  }

  @Test
  public void testRemoveByRank_valid() {
    OrderStatisticMap<String, String> map = createMap(ELEMS);

    map.removeByRank(2);

    Assert.assertFalse(containsEntry(map, ELEMS.get(2)));
    Assert.assertTrue(containsEntry(map, ELEMS.get(0)));
    Assert.assertTrue(containsEntry(map, ELEMS.get(1)));
    Assert.assertTrue(containsEntry(map, ELEMS.get(3)));

    map.removeByRank(0);

    Assert.assertFalse(containsEntry(map, ELEMS.get(0)));
    Assert.assertTrue(containsEntry(map, ELEMS.get(1)));
    Assert.assertTrue(containsEntry(map, ELEMS.get(3)));

    map.removeByRank(map.size() - 1);
    Assert.assertFalse(containsEntry(map, ELEMS.get(3)));
    Assert.assertTrue(containsEntry(map, ELEMS.get(1)));

    map.removeByRank(0);
    Assert.assertTrue(map.isEmpty());
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void testRemoveByRank_invalid() {
    OrderStatisticMap<String, String> map = createMap(ELEMS);

    try {
      map.removeByRank(-1);
    } catch (IndexOutOfBoundsException e1) {
      try {
        map.removeByRank(map.size());

      } catch (IndexOutOfBoundsException e2) {
        OrderStatisticMap<String, String> emptyMap = createMap();
        emptyMap.removeByRank(0);
      }
    }
  }
}
