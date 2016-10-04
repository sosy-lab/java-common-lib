/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.common.configuration;

import com.google.auto.value.AutoValue;
import com.google.errorprone.annotations.Immutable;
import java.util.Optional;

/**
 * Immutable container that stores a value and an optional string.
 */
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
