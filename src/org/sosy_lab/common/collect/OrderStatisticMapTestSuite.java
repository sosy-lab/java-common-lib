// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.testing.TestStringSortedMapGenerator;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.SerializableTester;
import com.google.errorprone.annotations.Var;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import org.junit.Test;

@SuppressWarnings("BadImport") // want to import Map.Entry because this class is about Map
public abstract class OrderStatisticMapTestSuite {

  private static final ImmutableList<Entry<String, String>> ELEMS =
      ImmutableList.of(
          Maps.immutableEntry("aaa", "Vzza"),
          Maps.immutableEntry("hha", "Vppa"),
          Maps.immutableEntry("ppa", "Vhha"),
          Maps.immutableEntry("zza", "Vaaa"));

  private static final ImmutableList<Entry<String, String>> ELEMS_ABOVE =
      ImmutableList.of(
          Maps.immutableEntry("aab", "Vzzb"),
          Maps.immutableEntry("hhb", "Vppb"),
          Maps.immutableEntry("ppb", "Vhhb"),
          Maps.immutableEntry("zzb", "Vaab"));

  private static final ImmutableList<Entry<String, String>> ELEMS_BELOW =
      ImmutableList.of(
          Maps.immutableEntry("aa", "Vzz"),
          Maps.immutableEntry("hh", "Vpp"),
          Maps.immutableEntry("pp", "Vhh"),
          Maps.immutableEntry("zz", "Vaa"));

  protected abstract static class OrderStatisticMapFactory extends TestStringSortedMapGenerator {

    @Override
    protected abstract OrderStatisticMap<String, String> create(Entry<String, String>[] pEntries);

    protected abstract OrderStatisticMap<String, String> create(
        List<Entry<String, String>> pEntries);
  }

  private final OrderStatisticMapFactory factory;

  OrderStatisticMapTestSuite(OrderStatisticMapFactory pFactory) {
    factory = pFactory;
  }

  private OrderStatisticMap<String, String> createMap() {
    return factory.create(ImmutableList.of());
  }

  private OrderStatisticMap<String, String> createMap(List<Entry<String, String>> pEntries) {
    return factory.create(pEntries);
  }

  private static <K, V> void putEntry(Map<K, V> pMap, Entry<K, V> pEntry) {
    pMap.put(pEntry.getKey(), pEntry.getValue());
  }

  private static <K, V> void removeEntry(Map<K, V> pMap, Entry<K, V> pEntry) {
    assertThat(pMap.get(pEntry.getKey())).isEqualTo(pMap.remove(pEntry.getKey()));
  }

  @Test
  public void testEquals() {
    EqualsTester mapEqualsTester = new EqualsTester();
    @Var OrderStatisticMap<String, String> l1 = createMap();
    @Var OrderStatisticMap<String, String> l2 = createMap();

    mapEqualsTester.addEqualityGroup(l1, l2);

    l1 = createMap();
    l2 = createMap();
    // Check that the map sorts its elements
    for (int i = ELEMS.size() - 1; i >= 0; i--) {
      l2.put(ELEMS.get(i).getKey(), ELEMS.get(i).getValue());
    }
    for (int i = 0; i < ELEMS.size(); i++) {
      l1.put(ELEMS.get(i).getKey(), ELEMS.get(i).getValue());
    }

    // Check the map property
    OrderStatisticMap<String, String> l3 = createMap();
    for (int i = ELEMS.size() - 1; i >= 0; i--) {
      l3.put(ELEMS.get(i).getKey(), ELEMS.get(i).getValue());
    }
    for (int i = 0; i < ELEMS.size(); i++) {
      l3.put(ELEMS.get(i).getKey(), ELEMS.get(i).getValue());
    }
    mapEqualsTester.addEqualityGroup(l1, l2, l3);

    mapEqualsTester.testEquals();
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
    assertThat(subMap).containsEntry(toAdd.getKey(), toAdd.getValue());
    assertThat(map).containsEntry(toAdd.getKey(), toAdd.getValue());

    removeEntry(subMap, toAdd);
    assertThat(map).doesNotContainKey(toAdd.getKey());
    assertThat(subMap).doesNotContainKey(toAdd.getKey());

    putEntry(map, toAdd);
    assertThat(subMap).containsEntry(toAdd.getKey(), toAdd.getValue());

    removeEntry(map, toAdd);
    assertThat(subMap).doesNotContainEntry(toAdd.getKey(), toAdd.getValue());
  }

  @Test
  public void testSubmapView_outOfBounds_add() {
    NavigableMap<String, String> map = createMap(ELEMS);
    NavigableMap<String, String> subMap =
        map.subMap(
            ELEMS_ABOVE.get(0).getKey(), true,
            ELEMS_ABOVE.get(2).getKey(), true);

    Map<String, String> toAdd =
        ImmutableMap.<String, String>builder()
            .put(ELEMS.get(1))
            .put(ELEMS_BELOW.get(2))
            .put(ELEMS.get(2))
            .put(ELEMS_ABOVE.get(3))
            .build();

    Entry<String, String> firstEntry = ELEMS.get(0);
    Entry<String, String> fourthEntry = ELEMS.get(3);
    assertThrows(IllegalArgumentException.class, () -> putEntry(subMap, firstEntry));
    assertThrows(IllegalArgumentException.class, () -> putEntry(subMap, fourthEntry));

    // the first 3 elements are in the range of the sublist, but the last isn't
    assertThrows(IllegalArgumentException.class, () -> subMap.putAll(toAdd));
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

    assertThat(map).containsEntry(ELEMS.get(1).getKey(), ELEMS.get(1).getValue());
    assertThat(map).containsEntry(ELEMS.get(3).getKey(), ELEMS.get(3).getValue());

    Map<String, String> toRemove =
        ImmutableMap.<String, String>builder()
            .put(ELEMS_BELOW.get(2))
            .put(ELEMS.get(2))
            .put(ELEMS.get(1))
            .build();
    for (Entry<String, String> e : toRemove.entrySet()) {
      removeEntry(subMap, e);
    }

    assertThat(map).containsEntry(ELEMS.get(1).getKey(), ELEMS.get(1).getValue());
    assertThat(map).doesNotContainEntry(ELEMS_BELOW.get(2).getKey(), ELEMS_BELOW.get(2).getValue());
    assertThat(map).doesNotContainEntry(ELEMS.get(2).getKey(), ELEMS.get(2).getValue());
  }

  @Test
  public void testSubmapView_outOfBounds_contains() {
    NavigableMap<String, String> map = createMap(ELEMS);
    @Var
    NavigableMap<String, String> subMap =
        map.subMap(
            ELEMS_ABOVE.get(1).getKey(), true,
            ELEMS_ABOVE.get(2).getKey(), true);

    assertThat(subMap).doesNotContainEntry(ELEMS.get(1).getKey(), ELEMS.get(1).getValue());
    assertThat(subMap).doesNotContainEntry(ELEMS.get(3).getKey(), ELEMS.get(3).getValue());

    subMap = map.subMap(ELEMS_ABOVE.get(1).getKey(), false, ELEMS_ABOVE.get(2).getKey(), false);

    assertThat(subMap)
        .doesNotContainEntry(ELEMS_ABOVE.get(1).getKey(), ELEMS_ABOVE.get(1).getValue());
    assertThat(subMap)
        .doesNotContainEntry(ELEMS_ABOVE.get(2).getKey(), ELEMS_ABOVE.get(2).getValue());

    subMap = map.subMap(ELEMS_ABOVE.get(0).getKey(), true, ELEMS_ABOVE.get(2).getKey(), true);

    assertThat(subMap).containsEntry(ELEMS.get(1).getKey(), ELEMS.get(1).getValue());
    assertThat(subMap).containsEntry(ELEMS.get(2).getKey(), ELEMS.get(2).getValue());
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

    assertThat(ELEMS.get(2)).isEqualTo(subMap.firstEntry());
    assertThat(ELEMS.get(1)).isEqualTo(subMap.lastEntry());

    subMap = subMap.descendingMap();
    assertThat(ELEMS.get(1)).isEqualTo(subMap.firstEntry());
    assertThat(ELEMS.get(2)).isEqualTo(subMap.lastEntry());
  }

  @Test
  public void testKeyset_mutation() {
    OrderStatisticMap<String, String> map = createMap(ELEMS);
    OrderStatisticSet<String> keySet = map.navigableKeySet();

    keySet.removeByRank(0);
    assertThat(map).doesNotContainKey(ELEMS.get(0).getKey());
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

    assertThat(subSubMap).doesNotContainEntry(ELEMS.get(0).getKey(), ELEMS.get(0).getValue());
    assertThat(subSubMap).containsEntry(ELEMS.get(1).getKey(), ELEMS.get(1).getValue());
    assertThat(subSubMap).containsEntry(ELEMS.get(2).getKey(), ELEMS.get(2).getValue());
    assertThat(subSubMap).doesNotContainEntry(ELEMS.get(3).getKey(), ELEMS.get(3).getValue());

    // make sure that the inclusive-flags are respected
    subSubMap =
        subMap.subMap(
            ELEMS.get(1).getKey(), /* fromInclusive= */ true,
            ELEMS.get(3).getKey(), /* toInclusive=*/ true);
    assertThat(subSubMap).containsEntry(ELEMS.get(1).getKey(), ELEMS.get(1).getValue());
    assertThat(subSubMap).containsEntry(ELEMS.get(2).getKey(), ELEMS.get(2).getValue());
    assertThat(subSubMap).containsEntry(ELEMS.get(3).getKey(), ELEMS.get(3).getValue());

    subSubMap =
        subSubMap.subMap(
            ELEMS.get(1).getKey(), /* fromInclusive= */ false,
            ELEMS.get(3).getKey(), /* toInclusive= */ false);
    assertThat(subSubMap).doesNotContainEntry(ELEMS.get(1).getKey(), ELEMS.get(1).getValue());
    assertThat(subSubMap).containsEntry(ELEMS.get(2).getKey(), ELEMS.get(2).getValue());
    assertThat(subSubMap).doesNotContainEntry(ELEMS.get(3).getKey(), ELEMS.get(3).getValue());
  }

  @Test
  public void testGetEntryByRank_valid() {
    OrderStatisticMap<String, String> map = createMap(ELEMS);

    for (int i = 0; i < ELEMS.size(); i++) {
      assertThat(ELEMS.get(i)).isEqualTo(map.getEntryByRank(i));
    }
  }

  @Test
  public void testGetEntryByRank_outOfBounds() {
    OrderStatisticMap<String, String> map = createMap(ELEMS);

    try {
      map.getEntryByRank(-1);
      assertWithMessage("Expected " + IndexOutOfBoundsException.class.getSimpleName()).fail();
    } catch (IndexOutOfBoundsException expected) {
      // expected outcome
    }
    try {
      map.getEntryByRank(ELEMS.size());
      assertWithMessage("Expected " + IndexOutOfBoundsException.class.getSimpleName()).fail();
    } catch (IndexOutOfBoundsException expected) {
      // expected outcome
    }
  }

  @Test
  public void testGetEntryByRank_submapFirst() {
    OrderStatisticMap<String, String> map = createMap(ELEMS);
    Entry<String, String> submapStart = ELEMS.get(1);
    String firstSubmapKey = submapStart.getKey();
    String submapEndKey = ELEMS.get(2).getKey();
    OrderStatisticMap<String, String> subMap =
        map.subMap(
            firstSubmapKey, /* fromInclusive= */ true, submapEndKey, /* toInclusive= */ true);

    Entry<String, String> firstSubmapEntry = subMap.getEntryByRank(0);

    assertThat(firstSubmapEntry).isEqualTo(submapStart);
  }

  @Test
  public void testGetEntryByRank_submapLast() {
    OrderStatisticMap<String, String> map = createMap(ELEMS);
    Entry<String, String> submapStart = ELEMS.get(1);
    String firstSubmapKey = submapStart.getKey();
    String submapEndKey = ELEMS.get(2).getKey();
    OrderStatisticMap<String, String> subMap =
        map.subMap(
            firstSubmapKey, /* fromInclusive= */ true, submapEndKey, /* toInclusive= */ true);

    Entry<String, String> lastSubmapEntry = subMap.getEntryByRank(subMap.size() - 1);

    assertThat(lastSubmapEntry).isEqualTo(ELEMS.get(2));
  }

  @Test
  public void testGetEntryByRank_descendingMapFirstElement() {
    OrderStatisticMap<String, String> map = createMap(ELEMS);
    OrderStatisticMap<String, String> descendingMap = map.descendingMap();

    Entry<String, String> firstEntryDescending = descendingMap.getEntryByRank(0);

    assertThat(firstEntryDescending).isEqualTo(ELEMS.get(ELEMS.size() - 1));
  }

  @Test
  public void testGetEntryByRank_descendingMapSecondElement() {
    OrderStatisticMap<String, String> map = createMap(ELEMS);
    OrderStatisticMap<String, String> descendingMap = map.descendingMap();

    Entry<String, String> sndEntryDescending = descendingMap.getEntryByRank(1);

    assertThat(sndEntryDescending).isEqualTo(ELEMS.get(ELEMS.size() - 2));
  }

  @Test
  public void testGetEntryByRank_descendingMapLastElement() {
    OrderStatisticMap<String, String> map = createMap(ELEMS);
    OrderStatisticMap<String, String> descendingMap = map.descendingMap();

    Entry<String, String> lastEntryDescending =
        descendingMap.getEntryByRank(descendingMap.size() - 1);

    assertThat(lastEntryDescending).isEqualTo(ELEMS.get(0));
  }

  @Test
  public void testGetKeyByRank_valid() {
    OrderStatisticMap<String, String> map = createMap(ELEMS);

    for (int i = 0; i < ELEMS.size(); i++) {
      assertThat(ELEMS.get(i).getKey()).isEqualTo(map.getKeyByRank(i));
    }
  }

  @Test
  public void testGetKeyByRank_outOfBounds() {
    OrderStatisticMap<String, String> map = createMap(ELEMS);

    try {
      map.getKeyByRank(-1);
      assertWithMessage("Expected " + IndexOutOfBoundsException.class.getSimpleName()).fail();
    } catch (IndexOutOfBoundsException expected) {
      // expected outcome
    }
    try {
      map.getKeyByRank(ELEMS.size());
      assertWithMessage("Expected " + IndexOutOfBoundsException.class.getSimpleName()).fail();
    } catch (IndexOutOfBoundsException expected) {
      // expected outcome
    }
  }

  @Test
  public void testGetKeyByRank_submapFirst() {
    OrderStatisticMap<String, String> map = createMap(ELEMS);
    Entry<String, String> submapStart = ELEMS.get(1);
    String submapStartKey = submapStart.getKey();
    String submapEnd = ELEMS.get(2).getKey();
    OrderStatisticMap<String, String> subMap =
        map.subMap(submapStartKey, /* fromInclusive= */ true, submapEnd, /* toInclusive= */ true);

    String firstSubmapKey = subMap.getKeyByRank(0);

    assertThat(firstSubmapKey).isEqualTo(submapStartKey);
  }

  @Test
  public void testGetKeyByRank_submapLast() {
    OrderStatisticMap<String, String> map = createMap(ELEMS);
    Entry<String, String> submapStart = ELEMS.get(1);
    String submapStartKey = submapStart.getKey();
    String submapEndKey = ELEMS.get(2).getKey();
    OrderStatisticMap<String, String> subMap =
        map.subMap(
            submapStartKey, /* fromInclusive= */ true, submapEndKey, /* toInclusive= */ true);

    String lastSubmapKey = subMap.getKeyByRank(subMap.size() - 1);

    assertThat(lastSubmapKey).isEqualTo(submapEndKey);
  }

  @Test
  public void testGetKeyByRank_descendingMapFirstElement() {
    OrderStatisticMap<String, String> map = createMap(ELEMS);
    OrderStatisticMap<String, String> descendingMap = map.descendingMap();
    String expectedKey = ELEMS.get(ELEMS.size() - 1).getKey();

    String firstKeyDescending = descendingMap.getKeyByRank(0);

    assertThat(firstKeyDescending).isEqualTo(expectedKey);
  }

  @Test
  public void testGetKeyByRank_descendingMapSecondElement() {
    OrderStatisticMap<String, String> map = createMap(ELEMS);
    OrderStatisticMap<String, String> descendingMap = map.descendingMap();
    String expectedKey = ELEMS.get(ELEMS.size() - 2).getKey();

    String sndKeyDescending = descendingMap.getKeyByRank(1);

    assertThat(sndKeyDescending).isEqualTo(expectedKey);
  }

  @Test
  public void testGetKeyByRank_descendingMapLastElement() {
    OrderStatisticMap<String, String> map = createMap(ELEMS);
    OrderStatisticMap<String, String> descendingMap = map.descendingMap();
    String expectedKey = ELEMS.get(0).getKey();

    String lastKeyDescending = descendingMap.getKeyByRank(descendingMap.size() - 1);

    assertThat(lastKeyDescending).isEqualTo(expectedKey);
  }

  @Test
  public void testRemoveByRank_valid() {
    OrderStatisticMap<String, String> map = createMap(ELEMS);

    map.removeByRank(2);

    assertThat(map).doesNotContainEntry(ELEMS.get(2).getKey(), ELEMS.get(2).getValue());
    assertThat(map).containsEntry(ELEMS.get(0).getKey(), ELEMS.get(0).getValue());
    assertThat(map).containsEntry(ELEMS.get(1).getKey(), ELEMS.get(1).getValue());
    assertThat(map).containsEntry(ELEMS.get(3).getKey(), ELEMS.get(3).getValue());

    map.removeByRank(0);

    assertThat(map).doesNotContainEntry(ELEMS.get(0).getKey(), ELEMS.get(0).getValue());
    assertThat(map).containsEntry(ELEMS.get(1).getKey(), ELEMS.get(1).getValue());
    assertThat(map).containsEntry(ELEMS.get(3).getKey(), ELEMS.get(3).getValue());

    map.removeByRank(map.size() - 1);
    assertThat(map).doesNotContainEntry(ELEMS.get(3).getKey(), ELEMS.get(3).getValue());
    assertThat(map).containsEntry(ELEMS.get(1).getKey(), ELEMS.get(1).getValue());

    map.removeByRank(0);
    assertThat(map).isEmpty();
  }

  @Test
  public void testRemoveByRank_invalid() {
    OrderStatisticMap<String, String> map = createMap(ELEMS);
    OrderStatisticMap<String, String> emptyMap = createMap();

    assertThrows(IndexOutOfBoundsException.class, () -> map.removeByRank(-1));
    assertThrows(IndexOutOfBoundsException.class, () -> map.removeByRank(map.size()));
    assertThrows(IndexOutOfBoundsException.class, () -> emptyMap.removeByRank(0));
  }

  @Test
  public void testRemoveByRank_submapFirst() {
    OrderStatisticMap<String, String> map = createMap(ELEMS);
    Entry<String, String> submapStart = ELEMS.get(1);
    String submapStartKey = submapStart.getKey();
    String submapEnd = ELEMS.get(2).getKey();
    OrderStatisticMap<String, String> subMap =
        map.subMap(submapStartKey, /* fromInclusive= */ true, submapEnd, /* toInclusive= */ true);

    String firstSubmapKey = subMap.removeByRank(0);

    assertThat(firstSubmapKey).isEqualTo(submapStartKey);
    assertThat(subMap).doesNotContainEntry(submapStart.getKey(), submapStart.getValue());
    assertThat(map).doesNotContainEntry(submapStart.getKey(), submapStart.getValue());
  }

  @Test
  public void testRemoveByRank_submapLast() {
    OrderStatisticMap<String, String> map = createMap(ELEMS);
    Entry<String, String> submapStart = ELEMS.get(1);
    String submapStartKey = submapStart.getKey();
    Entry<String, String> submapEnd = ELEMS.get(2);
    String submapEndKey = submapEnd.getKey();
    OrderStatisticMap<String, String> subMap =
        map.subMap(
            submapStartKey, /* fromInclusive= */ true, submapEndKey, /* toInclusive= */ true);

    String lastSubmapKey = subMap.removeByRank(subMap.size() - 1);

    assertThat(lastSubmapKey).isEqualTo(submapEndKey);
    assertThat(subMap).doesNotContainEntry(submapEnd.getKey(), submapEnd.getValue());
    assertThat(map).doesNotContainEntry(submapEnd.getKey(), submapEnd.getValue());
  }

  @Test
  public void testRemoveByRank_descendingMapFirstElement() {
    OrderStatisticMap<String, String> map = createMap(ELEMS);
    OrderStatisticMap<String, String> descendingMap = map.descendingMap();
    Entry<String, String> expectedRemove = ELEMS.get(ELEMS.size() - 1);
    String expectedRemoveKey = expectedRemove.getKey();

    String firstKeyDescending = descendingMap.removeByRank(0);

    assertThat(firstKeyDescending).isEqualTo(expectedRemoveKey);
    assertThat(descendingMap)
        .doesNotContainEntry(expectedRemove.getKey(), expectedRemove.getValue());
    assertThat(map).doesNotContainEntry(expectedRemove.getKey(), expectedRemove.getValue());
  }

  @Test
  public void testRemoveByRank_descendingMapLastElement() {
    OrderStatisticMap<String, String> map = createMap(ELEMS);
    OrderStatisticMap<String, String> descendingMap = map.descendingMap();
    Entry<String, String> expectedRemove = ELEMS.get(0);
    String expectedRemoveKey = expectedRemove.getKey();

    String lastKeyDescending = descendingMap.removeByRank(descendingMap.size() - 1);

    assertThat(lastKeyDescending).isEqualTo(expectedRemoveKey);
    assertThat(descendingMap)
        .doesNotContainEntry(expectedRemove.getKey(), expectedRemove.getValue());
    assertThat(map).doesNotContainEntry(expectedRemove.getKey(), expectedRemove.getValue());
  }

  private static <K, V> void assertRankOf(K pKey, int pExpectedRank, OrderStatisticMap<K, V> pMap) {
    int actualRank = pMap.rankOf(pKey);

    assertThat(actualRank).isEqualTo(pExpectedRank);
  }

  @Test
  public void testRankOf_firstElement() {
    OrderStatisticMap<String, String> map = createMap(ELEMS);

    assertRankOf(ELEMS.get(0).getKey(), 0, map);
  }

  @Test
  public void testRankOf_secondElement() {
    OrderStatisticMap<String, String> map = createMap(ELEMS);

    assertRankOf(ELEMS.get(1).getKey(), 1, map);
  }

  @Test
  public void testRankOf_lastElement() {
    OrderStatisticMap<String, String> map = createMap(ELEMS);
    String key = ELEMS.get(ELEMS.size() - 1).getKey();
    int expectedRank = ELEMS.size() - 1;

    assertRankOf(key, expectedRank, map);
  }

  @Test
  public void testRankOf_descendingMapFirstElement() {
    OrderStatisticMap<String, String> descendingMap = createMap(ELEMS).descendingMap();
    String key = ELEMS.get(ELEMS.size() - 1).getKey();
    int expectedRank = 0;

    assertRankOf(key, expectedRank, descendingMap);
  }

  @Test
  public void testRankOf_descendingMapSecondElement() {
    OrderStatisticMap<String, String> descendingMap = createMap(ELEMS).descendingMap();
    String key = ELEMS.get(ELEMS.size() - 2).getKey();
    int expectedRank = 1;

    assertRankOf(key, expectedRank, descendingMap);
  }

  @Test
  public void testRankOf_descendingMapLastElement() {
    OrderStatisticMap<String, String> descendingMap = createMap(ELEMS).descendingMap();
    String key = ELEMS.get(0).getKey();
    int expectedRank = ELEMS.size() - 1;

    assertRankOf(key, expectedRank, descendingMap);
  }

  @Test
  public void testRankOf_subMapFirstElement() {
    String firstSubmapKey = ELEMS.get(1).getKey();
    String lastSubmapKey = ELEMS.get(3).getKey();
    OrderStatisticMap<String, String> subMap =
        createMap(ELEMS)
            .subMap(
                firstSubmapKey, /* fromInclusive= */ true, lastSubmapKey, /* toInclusive= */ true);
    String key = firstSubmapKey;
    int expectedRank = 0;

    assertRankOf(key, expectedRank, subMap);
  }

  @Test
  public void testRankOf_subMapLastElement() {
    String firstSubmapKey = ELEMS.get(1).getKey();
    String lastSubmapKey = ELEMS.get(3).getKey();
    OrderStatisticMap<String, String> subMap =
        createMap(ELEMS)
            .subMap(
                firstSubmapKey, /* fromInclusive= */ true, lastSubmapKey, /* toInclusive= */ true);
    String key = lastSubmapKey;
    int expectedRank = subMap.size() - 1;

    assertRankOf(key, expectedRank, subMap);
  }

  @Test
  public void testRankOf_subMapSecondElement() {
    String firstSubmapKey = ELEMS.get(1).getKey();
    String lastSubmapKey = ELEMS.get(3).getKey();
    OrderStatisticMap<String, String> subMap =
        createMap(ELEMS)
            .subMap(
                firstSubmapKey, /* fromInclusive= */ true, lastSubmapKey, /* toInclusive= */ true);
    String key = ELEMS.get(2).getKey();
    int expectedRank = 1;

    assertRankOf(key, expectedRank, subMap);
  }
}
