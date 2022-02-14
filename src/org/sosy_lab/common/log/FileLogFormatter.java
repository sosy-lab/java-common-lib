// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.log;

/**
 * @deprecated use {@link TimestampedLogFormatter} instead.
 */
@Deprecated
public class FileLogFormatter extends TimestampedLogFormatter {

  /**
   * @deprecated use {@link TimestampedLogFormatter#withoutColors()}
   */
  @Deprecated
  public FileLogFormatter() {
    super(false);
  }
}
