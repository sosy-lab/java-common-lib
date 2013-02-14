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

import java.util.Map;


abstract class AbstractImmutableMap<K, V> implements Map<K, V> {

  @Override
  public final void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public final V put(K key, V value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final void putAll(Map<? extends K, ? extends V> pM) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final V remove(Object pKey) {
    throw new UnsupportedOperationException();
  }
}
