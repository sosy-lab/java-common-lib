/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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

import java.io.IOException;

/**
 * An interface for classes that know how to dump themselves into an {@link Appendable}.
 * This can be used for large string outputs, where writing into an Appendable
 * is faster than creating a string and then writing it in one piece.
 */
public interface Appender {

  /**
   * Writes a string representation of this object into the given {@link Appendable}.
   *
   * It is strongly encouraged that this method behaves identically to
   * <code>appendable.append(this.toString())</code>,
   * except for possibly calling append multiple times with bits of the output.
   *
   * @param appendable The appendable, may not be null.
   * @throws IOException If the appendable throws an IOException
   */
  void appendTo(Appendable appendable) throws IOException;
}
