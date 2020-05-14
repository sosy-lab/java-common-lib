// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.testing.NavigableMapTestSuiteBuilder;
import com.google.common.collect.testing.TestSortedMapGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import junit.framework.JUnit4TestAdapter;
import junit.framework.TestSuite;
import org.junit.Test;

public class NaiveOrderStatisticMapTest extends OrderStatisticMapTestSuite {

  private static class OrderStatisticMapProxyFactory extends OrderStatisticMapFactory {

    @Override
    protected OrderStatisticMap<String, String> create(Entry<String, String>[] pEntries) {
      return create(Arrays.asList(pEntries));
    }

    @Override
    protected OrderStatisticMap<String, String> create(List<Entry<String, String>> pEntries) {
      NaiveOrderStatisticMap<String, String> map = createMap();
      for (Entry<String, String> e : pEntries) {
        map.put(e.getKey(), e.getValue());
      }
      return map;
    }

    private static <K, V> NaiveOrderStatisticMap<K, V> createMap() {
      return NaiveOrderStatisticMap.createMap();
    }
  }

  public NaiveOrderStatisticMapTest() {
    super(new OrderStatisticMapProxyFactory());
  }

  public static junit.framework.Test suite() {
    TestSortedMapGenerator<String, String> testSetGenerator = new OrderStatisticMapProxyFactory();

    TestSuite suite =
        NavigableMapTestSuiteBuilder.using(testSetGenerator)
            .named("NaiveOrderStatisticMap")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                MapFeature.GENERAL_PURPOSE,
                MapFeature.ALLOWS_NULL_VALUES)
            .createTestSuite();

    suite.addTest(new JUnit4TestAdapter(NaiveOrderStatisticMapTest.class));

    return suite;
  }

  @Test
  public void testNoReference() {
    NavigableMap<String, String> testMap =
        new TreeMap<>(ImmutableMap.of("a", "Va", "b", "Vb", "bc", "Vbc", "d", "Vd"));
    OrderStatisticMap<String, String> map = NaiveOrderStatisticMap.createMapWithSameOrder(testMap);

    testMap.remove("a");
    assertThat(testMap).doesNotContainKey("a");
    assertThat(map).containsKey("a");
    map.remove("bc");
    assertThat(testMap).containsKey("bc");
    assertThat(map).doesNotContainKey("bc");
  }
}
