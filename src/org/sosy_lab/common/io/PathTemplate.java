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
package org.sosy_lab.common.io;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.MoreObjects;

import java.util.Arrays;
import java.util.IllegalFormatException;

/**
 * A template for paths, from which a real path can be constructed
 * by passing some values to fill in the blanks.
 */
public final class PathTemplate {

  private final String template;

  private PathTemplate(String pTemplate) {
    checkArgument(!pTemplate.isEmpty());
    template = pTemplate;
  }

  /**
   * Create a new instance.
   * @param pTemplate A non-null non-empty template String in the format for
   * {@link String#format(String, Object...)}.
   */
  public static PathTemplate ofFormatString(String pTemplate) {
    return new PathTemplate(pTemplate);
  }

  /**
   * Construct a concrete {@link Path} from this template and the given values.
   * @throws IllegalFormatException If the template is invalid,
   * or the arguments does not match the template.
   */
  public Path getPath(Object... args) {
    checkArgument(!Arrays.asList(args).contains(null), "Values for PathTemplate may not be null");

    return Paths.get(String.format(template, args));
  }

  /**
   * Returns the raw template of this instance.
   */
  public String getTemplate() {
    return template;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("template", template).toString();
  }
}
