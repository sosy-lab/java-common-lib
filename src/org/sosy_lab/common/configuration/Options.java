// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Annotation for a class which has fields or methods with an {@link Option} annotation. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Options {

  /**
   * An optional prefix for all configuration options of the class annotated with this type. Prefix
   * and name of the option will be separated by a dot.
   */
  String prefix() default "";

  /**
   * When the prefix needs to be renamed, often it is desirable to maintain the backwards
   * compatibility with the previous config. In that case, the previous prefix can be moved to the
   * field {@code deprecatedPrefix}. Both normal and deprecated prefixes would work, with latter
   * printing the deprecation warning.
   */
  String deprecatedPrefix() default Configuration.NO_DEPRECATED_PREFIX;

  /** An optional text, that describes the current options. */
  String description() default "";
}
