// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common;

import static com.google.common.base.Preconditions.checkState;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility class for generating unique. This class is fully thread-safe.
 *
 * <p>It gives out at most MAX_INT ids, afterwards it throws an exception.
 */
public final class UniqueIdGenerator {

  private final AtomicInteger nextId = new AtomicInteger();

  public int getFreshId() {
    int id = nextId.getAndIncrement();
    checkState(id >= 0, "Overflow for unique ID");
    return id;
  }
}
