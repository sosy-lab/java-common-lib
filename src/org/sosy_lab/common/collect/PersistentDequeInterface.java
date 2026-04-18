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
public interface PersistentDequeInterface<T> {
  boolean isEmpty();
  T getTop();
  T getBottom();
  PersistentDeque<T> insertTop(T value);
  PersistentDeque<T> insertBottom(T value);
  PersistentDeque<T> deleteTop();
  PersistentDeque<T> deleteBottom();
}
