// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.io;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.MoreObjects;
import com.google.errorprone.annotations.Immutable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.IllegalFormatException;

/**
 * A template for paths, from which a real path can be constructed by passing some values to fill in
 * the blanks.
 */
@Immutable
public final class PathTemplate {

  private final String template;

  private PathTemplate(String pTemplate) {
    checkArgument(!pTemplate.isEmpty());
    template = pTemplate;
  }

  /**
   * Create a new instance.
   *
   * @param pTemplate A non-null non-empty template String in the format for {@link
   *     String#format(String, Object...)}.
   */
  public static PathTemplate ofFormatString(String pTemplate) {
    return new PathTemplate(pTemplate);
  }

  /**
   * Construct a concrete {@link Path} from this template and the given values.
   *
   * @throws IllegalFormatException If the template is invalid, or the arguments does not match the
   *     template.
   */
  public Path getPath(Object... args) {
    checkArgument(!Arrays.asList(args).contains(null), "Values for PathTemplate may not be null");

    return Paths.get(String.format(template, args));
  }

  /** Returns the raw template of this instance. */
  public String getTemplate() {
    return template;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("template", template).toString();
  }
}
