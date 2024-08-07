// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Iterators;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

final class MapValues<E extends @Nullable Object> extends AbstractCollection<E>
    implements Serializable {

  private static final long serialVersionUID = 9122264612662000623L;

  @SuppressWarnings("serial") // This class only needs to be serializable if delegate is.
  private final Map<?, E> delegate;

  MapValues(Map<?, E> pDelegate) {
    delegate = checkNotNull(pDelegate);
  }

  @Override
  public Iterator<E> iterator() {
    return Iterators.transform(delegate.entrySet().iterator(), Map.Entry::getValue);
  }

  @Override
  public void clear() {
    delegate.clear();
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public int size() {
    return delegate.size();
  }
}
