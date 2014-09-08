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
package org.sosy_lab.common;

/**
 * Utility class for String-related helpers,
 * similar to {@link com.google.common.base.Strings}.
 */
public final class MoreStrings {
  private MoreStrings() { }

  /**
   * Check whether a string starts with a given prefix
   * similar to {@link String#startsWith(String)},
   * but ignoring the case like {@link String#equalsIgnoreCase(String)}.
   * @param s The string to check whether it contains the prefix.
   * @param prefix The prefix.
   * @return Whether {@code s} starts with {@code prefix} in any case.
   */
  public static boolean startsWithIgnoreCase(String s, String prefix) {
    int prefixLength = prefix.length();
    if (prefixLength > s.length()) {
      return false;
    }
    s = s.substring(0, prefixLength);
    return s.equalsIgnoreCase(prefix);
  }
}
