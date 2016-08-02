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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.primitives.Chars;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.stream.Stream;

/**
 * Utility class similar to {@link Collections} and {@link Collections2}.
 */
public final class Collections3 {

  private Collections3() {}

  /**
   * Apply a function to all elements in a collection and return an {@link ImmutableList}
   * with the results. This is an eager version of {@link Lists#transform(List, Function)}
   * and {@link Collections2#transform(Collection, Function)}.
   *
   * This function is more efficient than code doing the same using
   * {@link Stream} or {@link FluentIterable}.
   */
  public static <T1, T2> ImmutableList<T2> transformedImmutableListCopy(
      Collection<T1> input, Function<T1, T2> transformer) {
    return ImmutableList.copyOf(Collections2.transform(input, transformer));
  }

  /**
   * Apply a function to all elements in a collection and return an {@link ImmutableSet}
   * with the results. This is an eager version of
   * {@link Collections2#transform(Collection, Function)}.
   */
  public static <T1, T2> ImmutableSet<T2> transformedImmutableSetCopy(
      Collection<T1> input, Function<T1, T2> transformer) {
    return ImmutableSet.copyOf(Collections2.transform(input, transformer));
  }

  /**
   * Given a {@link SortedMap} with {@link String}s as key,
   * return a partial map (similar to {@link SortedMap#subMap(Object, Object)})
   * of all keys that have a given prefix.
   *
   * @param map The map to filter.
   * @param prefix The prefix that all keys in the result need to have.
   * @return A partial map of the input.
   */
  public static <V> SortedMap<String, V> subMapWithPrefix(SortedMap<String, V> map, String prefix) {
    checkNotNull(map);
    checkArgument(!prefix.isEmpty());

    // As the end marker of the set, create the string that is
    // the next bigger string than all possible strings with the given prefix.
    String end = incrementStringByOne(prefix);
    return map.subMap(prefix, end);
  }

  /**
   * Given a {@link SortedSet} of {@link String},
   * return a set (similar to {@link SortedSet#subSet(Object, Object)})
   * of all entries that have a given prefix.
   *
   * @param set The set to filter.
   * @param prefix The prefix that all keys in the result need to have.
   * @return A subset of the input.
   */
  public static SortedSet<String> subSetWithPrefix(SortedSet<String> set, String prefix) {
    checkNotNull(set);
    checkArgument(!prefix.isEmpty());

    // As the end marker of the set, create the string that is
    // the next bigger string than all possible strings with the given prefix.
    String end = incrementStringByOne(prefix);
    return set.subSet(prefix, end);
  }

  private static String incrementStringByOne(String prefix) {
    // To create the next bigger string than all strings with the same prefix,
    // take the prefix and increment the value of the last character by one.
    StringBuilder end = new StringBuilder(prefix);

    int lastPos = end.length() - 1;
    // This is basically end[lastPos] += 1
    end.setCharAt(lastPos, Chars.checkedCast((end.charAt(lastPos) + 1)));

    return end.toString();
  }
}
