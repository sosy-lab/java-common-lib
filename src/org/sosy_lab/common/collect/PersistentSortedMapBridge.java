// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import com.google.errorprone.annotations.Immutable;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Interface that forces generation of bridge methods that return {@link SortedSet} for binary
 * backwards compatibility.
 */
@SuppressWarnings("JdkObsolete")
@Immutable(containerOf = {"K", "V"})
interface PersistentSortedMapBridge<K, V extends @Nullable Object>
    extends PersistentMap<K, V>, SortedMap<K, V> {
  @Override
  SortedSet<K> keySet();

  @Override
  SortedSet<Map.Entry<K, V>> entrySet();
}
