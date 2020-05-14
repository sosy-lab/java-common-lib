// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.annotations;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for component that are considered unmaintained, and might have inferior quality.
 *
 * <p>Effects of this annotation may include hiding warnings produced by static-analysis tools, and
 * a warning given to the user when this component is used.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {PACKAGE, TYPE})
public @interface Unmaintained {}
