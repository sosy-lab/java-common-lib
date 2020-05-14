// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.configuration;

import com.google.auto.value.AutoValue;
import com.google.errorprone.annotations.Immutable;
import java.util.Optional;

/** Immutable container that stores a value and an optional string. */
@Immutable(containerOf = "T")
@AutoValue
public abstract class AnnotatedValue<T> {

  AnnotatedValue() {}

  public static <T> AnnotatedValue<T> create(T value) {
    return new AutoValue_AnnotatedValue<>(value, Optional.empty());
  }

  public static <T> AnnotatedValue<T> create(T value, String annotation) {
    return new AutoValue_AnnotatedValue<>(value, Optional.of(annotation));
  }

  public static <T> AnnotatedValue<T> create(T value, Optional<String> annotation) {
    return new AutoValue_AnnotatedValue<>(value, annotation);
  }

  public abstract T value();

  public abstract Optional<String> annotation();
}
