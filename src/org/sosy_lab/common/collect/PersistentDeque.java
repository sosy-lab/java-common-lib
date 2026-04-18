// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import com.google.errorprone.annotations.Immutable;

@Immutable(containerOf = "T")
public final class PersistentDeque<T> implements PersistentDequeInterface<T> {
  final PersistentLinkedList<T> top;
  final PersistentLinkedList<T> bottom;

  private PersistentDeque() {
    top = PersistentLinkedList.of();
    bottom = PersistentLinkedList.of();
  }

  private PersistentDeque(PersistentLinkedList<T> top, PersistentLinkedList<T> bottom) {
    this.top = top;
    this.bottom = bottom;
  }

  @Override
  public boolean isEmpty() {
    return top.isEmpty() && bottom.isEmpty();
  }

  @Override
  public T getTop() {
    if(top.isEmpty()) {
      return null;
      //TODO add exception handling
    }

    return top.head();
  }

  @Override
  public T getBottom() {
    if(bottom.isEmpty()) {
      return null;
      //TODO add exception handling
    }

    return bottom.head();
  }

  @Override
  public PersistentDeque<T> insertTop(T value) {
    return new PersistentDeque<>(top.with(value), bottom);
  }

  @Override
  public PersistentDeque<T> insertBottom(T value) {
    return new PersistentDeque<>(top, bottom.with(value));
  }

  @Override
  public PersistentDeque<T> deleteTop() {
    return new PersistentDeque<>(top.tail(), bottom);
  }
}
