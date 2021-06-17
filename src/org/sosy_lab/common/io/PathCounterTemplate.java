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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Path;
import java.util.IllegalFormatException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A template for {@link Path} objects that uses a counter to produce paths with a fresh new name
 * for every request.
 */
public final class PathCounterTemplate {

  private final String template;
  private final AtomicInteger counter = new AtomicInteger();

  private PathCounterTemplate(String pTemplate) {
    checkArgument(!pTemplate.isEmpty());
    checkPatternValidity(pTemplate);

    template = pTemplate;
  }

  /**
   * Check whether a String is a valid template for inserting one int with {@link
   * String#format(String, Object...)}.
   *
   * @param pTemplate The template to check.
   * @throws IllegalFormatException If the pattern is invalid.
   */
  @SuppressWarnings("ReturnValueIgnored")
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
  private static void checkPatternValidity(String pTemplate) {
    String.format(pTemplate, 0);
  }

  /**
   * Create a new instance.
   *
   * @param pTemplate A non-null non-empty template String in the format for {@link
   *     String#format(String, Object...)} that is suited for exactly one argument of type int.
   * @throws IllegalFormatException If the template is invalid.
   */
  public static PathCounterTemplate ofFormatString(String pTemplate) {
    return new PathCounterTemplate(pTemplate);
  }

  /**
   * Construct a concrete {@link Path} that was not handed out by this instance before.
   *
   * @throws IllegalFormatException If the template is invalid, or the arguments does not match the
   *     template.
   */
  public Path getFreshPath() {
    return Path.of(String.format(template, counter.getAndIncrement()));
  }

  /** Returns the raw template of this instance. */
  public String getTemplate() {
    return template;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("template", template)
        .add("counter", counter.get())
        .toString();
  }
}
