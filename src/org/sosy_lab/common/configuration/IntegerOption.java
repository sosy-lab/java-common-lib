// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.configuration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This is an optional annotation for all configuration options (i.e., elements that are annotated
 * with {@link Option}) whose type is an integer number (i.e., int, long, Integer and Long):
 *
 * <p>It serves to specify minimal and/or maximal values.
 */
@OptionDetailAnnotation(applicableTo = {Integer.class, Long.class})
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface IntegerOption {

  /** An optional minimum value for this option. */
  long min() default Long.MIN_VALUE;

  /** An optional maximum value for this option. */
  long max() default Long.MAX_VALUE;
}
