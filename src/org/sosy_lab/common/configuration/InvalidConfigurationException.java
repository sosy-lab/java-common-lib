/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.common.configuration;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Exception class to signal that something is wrong in the user-specified
 * configuration.
 */
public class InvalidConfigurationException extends Exception {

  private static final long serialVersionUID = -2482555561027049741L;

  public InvalidConfigurationException(String msg) {
    super(checkNotNull(msg));
  }

  public InvalidConfigurationException(String msg, Throwable source) {
    super(checkNotNull(msg), checkNotNull(source));
  }
}
