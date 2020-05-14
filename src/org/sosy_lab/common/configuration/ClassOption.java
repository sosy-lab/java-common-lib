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
 * with {@link Option}) whose type is {@link Class}.
 *
 * <p>It serves to specify additional information which is required for class options.
 */
@OptionDetailAnnotation(applicableTo = {})
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface ClassOption {

  /**
   * This field provides optional package prefixes that can be added to the specified class name.
   * First the specified class name is tried without any prefix, and then with each prefix in the
   * given order. The result is the first class that is found.
   */
  String[] packagePrefix() default "";
}
