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
package org.sosy_lab.common.io;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.IllegalFormatException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A template for {@link Path} objects that uses a counter
 * to produce paths with a fresh new name for every request.
 */
public final class PathCounterTemplate {

  private final String template;
  private final AtomicInteger counter = new AtomicInteger();

  private PathCounterTemplate(String pTemplate) {
    checkArgument(!pTemplate.isEmpty());

    // check whether the template is valid for inserting ints
    // (will throw exception otherwise)
    String.format(pTemplate, 0);

    template = pTemplate;
  }

  /**
   * Create a new instance.
   * @param pTemplate A non-null non-empty template String in the format for
   * {@link String#format(String, Object...)} that is suited for exactly one argument of type int.
   * @throws IllegalFormatException If the template is invalid.
   */
  public static PathCounterTemplate ofFormatString(String pTemplate) {
    return new PathCounterTemplate(pTemplate);
  }

  /**
   * Construct a concrete {@link Path} that was not handed out by this instance before.
   * @throws IllegalFormatException If the template is invalid, or the arguments does not match the template.
   */
  public Path getFreshPath() {
    return Paths.get(String.format(template, counter.getAndIncrement()));
  }

  /**
   * Returns the raw template of this instance.
   */
  public String getTemplate() {
    return template;
  }
}
