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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;

import java.util.Collection;
import java.util.Map;

import javax.annotation.Nonnull;

abstract class AbstractImmutableMap<K, V> implements Map<K, V> {

  static <V> Function<Entry<?, V>, V> getValueFunction() {
    return new Function<Map.Entry<?, V>, V>() {
      @Override
      public V apply(@Nonnull Map.Entry<?, V> input) {
        return input.getValue();
      }
    };
  }

  @Deprecated
  @Override
  public final void clear() {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  public final V put(K key, V value) {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  public final void putAll(Map<? extends K, ? extends V> pM) {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  public final V remove(Object pKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsValue(Object pValue) {
    return values().contains(pValue);
  }

  @Override
  public int size() {
    return entrySet().size();
  }

  @Override
  public Collection<V> values() {
    return Collections2.transform(entrySet(), AbstractImmutableMap.<V>getValueFunction());
  }

  @Override
  public boolean equals(Object pObj) {
    if (pObj instanceof Map<?, ?>) {
      return entrySet().equals(((Map<?, ?>) pObj).entrySet());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return entrySet().hashCode();
  }

  @Override
  public String toString() {
    if (isEmpty()) {
      return "{}";
    }
    StringBuilder sb = new StringBuilder();
    sb.append('{');
    Joiner.on(", ").withKeyValueSeparator("=").useForNull("null").appendTo(sb, this);
    sb.append('}');
    return sb.toString();
  }
}
