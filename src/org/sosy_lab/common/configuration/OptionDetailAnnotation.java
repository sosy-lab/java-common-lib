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
 * This is a meta annotation that marks other annotation which may be used in conjunction with
 * {@link Option} to provide more information for a specific option type.
 */
@Documented
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface OptionDetailAnnotation {

  /**
   * The annotation is applicable to configuration of these types. If a wrapper class of a primitive
   * type is added here, the annotation automatically is also applicable to the corresponding
   * primitive type. Otherwise types have to match exactly, i.e., no sub-types and super types.
   */
  Class<?>[] applicableTo();
}
