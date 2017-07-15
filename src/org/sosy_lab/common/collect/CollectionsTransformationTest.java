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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import org.junit.Test;

/**
 * Tests that ensure that for certain standard techniques for creating immutable transformed copies
 * of a collection the transformation is applied only once (i.e., only one pass is made over the
 * input collection).
 */
public class CollectionsTransformationTest {

  private static void testTransformCalledOnlyOnce(
      Function<List<AtomicInteger>, Collection<AtomicInteger>> inputSupplier,
      BiFunction<Collection<AtomicInteger>, Function<AtomicInteger, String>, Collection<String>>
          transformer) {
    AtomicInteger i1 = new AtomicInteger();
    AtomicInteger i2 = new AtomicInteger(10);
    AtomicInteger i3 = new AtomicInteger(20);
    Collection<AtomicInteger> input = inputSupplier.apply(ImmutableList.of(i1, i2, i3));

    Collection<String> result = transformer.apply(input, e -> "" + e.incrementAndGet());

    // Check twice to ensure transform function is not called again
    assertThat(result).containsAllOf("1", "11", "21");
    assertThat(result).containsAllOf("1", "11", "21");

    // Check how often transform function was called
    assertThat(i1.get()).isEqualTo(1);
    assertThat(i2.get()).isEqualTo(11);
    assertThat(i3.get()).isEqualTo(21);
  }

  @Test
  public void testTransformedListCopy() {
    testTransformCalledOnlyOnce(ImmutableList::copyOf, Collections3::transformedImmutableListCopy);
    testTransformCalledOnlyOnce(Lists::newArrayList, Collections3::transformedImmutableListCopy);
    testTransformCalledOnlyOnce(Sets::newHashSet, Collections3::transformedImmutableListCopy);

    testTransformCalledOnlyOnce(
        ImmutableList::copyOf,
        (input, transformer) -> new ArrayList<>(Collections2.transform(input, transformer)));
    testTransformCalledOnlyOnce(
        ImmutableList::copyOf,
        (input, transformer) -> new LinkedList<>(Collections2.transform(input, transformer)));
  }

  @Test
  public void testTransformedSetCopy() {
    testTransformCalledOnlyOnce(ImmutableList::copyOf, Collections3::transformedImmutableSetCopy);
    testTransformCalledOnlyOnce(Lists::newArrayList, Collections3::transformedImmutableSetCopy);
    testTransformCalledOnlyOnce(Sets::newHashSet, Collections3::transformedImmutableSetCopy);

    testTransformCalledOnlyOnce(
        ImmutableList::copyOf,
        (input, transformer) -> new HashSet<>(Collections2.transform(input, transformer)));
    testTransformCalledOnlyOnce(
        ImmutableList::copyOf,
        (input, transformer) -> new TreeSet<>(Collections2.transform(input, transformer)));
  }

  private static void testMapTransformCalledOnlyOnce(
      Function<Map<String, AtomicInteger>, Map<String, AtomicInteger>> inputSupplier,
      BiFunction<Map<String, AtomicInteger>, Function<AtomicInteger, String>, Map<String, String>>
          transformer) {
    AtomicInteger i1 = new AtomicInteger();
    AtomicInteger i2 = new AtomicInteger(10);
    AtomicInteger i3 = new AtomicInteger(20);
    Map<String, AtomicInteger> input =
        inputSupplier.apply(ImmutableMap.of("a", i1, "b", i2, "c", i3));

    Map<String, String> result = transformer.apply(input, e -> "" + e.incrementAndGet());

    // Check twice to ensure transform function is not called again
    assertThat(result).containsExactly("a", "1", "b", "11", "c", "21");
    assertThat(result).containsExactly("a", "1", "b", "11", "c", "21");

    // Check how often transform function was called
    assertThat(i1.get()).isEqualTo(1);
    assertThat(i2.get()).isEqualTo(11);
    assertThat(i3.get()).isEqualTo(21);
  }

  @Test
  public void testTransformedMapCopy() {
    testMapTransformCalledOnlyOnce(
        ImmutableMap::copyOf,
        (input, transformer) -> ImmutableMap.copyOf(Maps.transformValues(input, transformer)));
    testMapTransformCalledOnlyOnce(
        Maps::newHashMap,
        (input, transformer) -> ImmutableMap.copyOf(Maps.transformValues(input, transformer)));
    testMapTransformCalledOnlyOnce(
        input -> new TreeMap<>(input),
        (input, transformer) -> ImmutableMap.copyOf(Maps.transformValues(input, transformer)));

    testMapTransformCalledOnlyOnce(
        ImmutableMap::copyOf,
        (input, transformer) -> new HashMap<>(Maps.transformValues(input, transformer)));
    testMapTransformCalledOnlyOnce(
        ImmutableMap::copyOf,
        (input, transformer) -> new TreeMap<>(Maps.transformValues(input, transformer)));
  }
}
