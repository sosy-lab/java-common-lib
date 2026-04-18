// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import com.google.errorprone.annotations.Immutable;
import java.util.Iterator;
import java.util.List;

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

  public static PersistentDeque createEmptyPersistentDeque() {
    return new PersistentDeque<>();
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

  @Override
  public PersistentDeque<T> deleteBottom() {
    return new PersistentDeque<>(top, bottom.tail());
  }

  private PersistentDeque<T> rebalanceDeque() {
    boolean topEmpty = top.isEmpty();
    boolean bottomEmpty = bottom.isEmpty();

    if(topEmpty && bottomEmpty) {
      return this;
    } else if (topEmpty && !bottomEmpty) {
      return split(bottom.reversed());
    } else if(!topEmpty && bottomEmpty) {
      return split(top);
    }

    return this;
  }

  //TODO handle case when Deque only contains one more element
  //TODO integrate deque rebalancing into remove operations

  private PersistentDeque<T> split(PersistentLinkedList<T> list) {
    int size = list.size();
    int halfSize = size/2;

    if(size <= 0) {
      //TODO throw suitable exception
    } else if(size == 1) {
      //TODO throw suitable exception
    }

    PersistentLinkedList<T> newTop = PersistentLinkedList.of();
    PersistentLinkedList<T> newBottom = PersistentLinkedList.of();
    Iterator<T> iterator = list.iterator();

    for(int i = 0; i < size; i++) {
      T element = iterator.next();
      if(i < halfSize) {
        newTop = newTop.with(element);
      } else {
        newBottom = newBottom.with(element);
      }
    }
    newTop = newTop.reversed();
    return new PersistentDeque<>(newTop, newBottom);
  }
}
