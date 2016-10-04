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
package org.sosy_lab.common;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.Test;

public class ClassesTest {

  private static void assertIsAllowed(
      Iterable<Class<?>> declaredExceptionTypes, Iterable<Class<?>> allowedExceptionTypes) {
    assertThat(Classes.verifyDeclaredExceptions(declaredExceptionTypes, allowedExceptionTypes))
        .isNull();
  }

  private static void assertIsForbidden(
      String forbiddenException,
      Iterable<Class<?>> declaredExceptionTypes,
      Iterable<Class<?>> allowedExceptionTypes) {
    assertThat(Classes.verifyDeclaredExceptions(declaredExceptionTypes, allowedExceptionTypes))
        .isEqualTo(forbiddenException);
  }

  @Test
  public void verifyDeclaredException_Error() {
    assertIsAllowed(ImmutableList.of(Error.class), ImmutableList.of());
  }

  @Test
  public void verifyDeclaredException_RuntimeException() {
    assertIsAllowed(ImmutableList.of(RuntimeException.class), ImmutableList.of());
  }

  @Test
  public void verifyDeclaredException_Throwable() {
    assertIsForbidden("Throwable", ImmutableList.of(Throwable.class), ImmutableList.of());
  }

  @Test
  public void verifyDeclaredException_Exception() {
    assertIsForbidden("Exception", ImmutableList.of(Exception.class), ImmutableList.of());
  }

  @Test
  public void verifyDeclaredException_DeclaredException() {
    assertIsAllowed(ImmutableList.of(Exception.class), ImmutableList.of(Exception.class));
  }

  @Test
  public void verifyDeclaredException_DeclaredSuperException() {
    assertIsAllowed(ImmutableList.of(Exception.class), ImmutableList.of(Throwable.class));
  }

  @Test
  public void verifyDeclaredException_DeclaredSubException() {
    assertIsForbidden(
        "Exception", ImmutableList.of(Exception.class), ImmutableList.of(IOException.class));
  }

  @Test
  public void verifyDeclaredException_DeclaredMultipleExceptions() {
    assertIsAllowed(
        ImmutableList.of(FileNotFoundException.class),
        ImmutableList.of(RuntimeException.class, IOException.class));
  }
}
