// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common;

import java.io.IOException;

/**
 * An interface for classes that know how to dump themselves into an {@link Appendable}. This can be
 * used for large string outputs, where writing into an Appendable is faster than creating a string
 * and then writing it in one piece.
 */
public interface Appender {

  /**
   * Writes a string representation of this object into the given {@link Appendable}.
   *
   * <p>It is strongly encouraged that this method behaves identically to <code>
   * appendable.append(this.toString())</code>, except for possibly calling append multiple times
   * with bits of the output.
   *
   * @param appendable The appendable, may not be null.
   * @throws IOException If the appendable throws an IOException
   */
  void appendTo(Appendable appendable) throws IOException;
}
