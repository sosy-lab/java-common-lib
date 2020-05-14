// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.annotation.meta.TypeQualifierDefault;

/**
 * An annotation similar to {@link javax.annotation.ParametersAreNonnullByDefault} that defines that
 * all methods inside the annotated element do not return null, unless this is overridden with
 * another annotation.
 *
 * <p>It is defined here because the annotation supplied by FindBugs is deprecated: {@link
 * edu.umd.cs.findbugs.annotations.ReturnValuesAreNonnullByDefault}.
 */
@Documented
@javax.annotation.Nonnull
@org.checkerframework.checker.nullness.qual.NonNull
@TypeQualifierDefault(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReturnValuesAreNonnullByDefault {}
