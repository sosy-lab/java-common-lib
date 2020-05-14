// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.configuration;

import static com.google.common.base.Preconditions.checkNotNull;

/** Exception class to signal that something is wrong in the user-specified configuration. */
public class InvalidConfigurationException extends Exception {

  private static final long serialVersionUID = -2482555561027049741L;

  public InvalidConfigurationException(String msg) {
    super(checkNotNull(msg));
  }

  public InvalidConfigurationException(String msg, Throwable source) {
    super(checkNotNull(msg), checkNotNull(source));
  }
}
