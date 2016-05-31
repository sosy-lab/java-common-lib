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

import com.google.common.reflect.TypeToken;

import org.junit.Test;
import org.sosy_lab.common.Classes.UnsuitedClassException;

import java.io.IOException;
import java.util.function.Supplier;

import javax.annotation.Nullable;

/**
 * Tests for {@link Classes#createFactory(TypeToken, Class)}.
 */
public class ClassesFactoryTest {

  public interface TestFactory {
    Object get(String s) throws Exception;
  }

  public static class SimpleTestClass {
    public SimpleTestClass() {}
  }

  public static class SimpleTestClass2 {
    public SimpleTestClass2() {}
  }

  public abstract class AbstractTestClass {
    public AbstractTestClass() {}
  }

  public static class ExceptionTestClass {
    @SuppressWarnings("unused")
    public ExceptionTestClass() throws IOException {}
  }

  public static class ParameterTestClass {
    private final String s;
    private final Integer i;

    public ParameterTestClass(String s, @Nullable Integer i) {
      this.s = s;
      this.i = i;
    }
  }

  public static class FactoryMethodTestClass extends ParameterTestClass {

    private FactoryMethodTestClass(String s, @Nullable Integer i) {
      super(s, i);
    }

    public static ParameterTestClass create(String s) {
      return new FactoryMethodTestClass(s, null);
    }
  }

  public static class FactoryMethodTestClass2 extends FactoryMethodTestClass {

    private FactoryMethodTestClass2(String s, @Nullable Integer i) {
      super(s, i);
    }

    public static ParameterTestClass create(String s) {
      return new FactoryMethodTestClass2(s, null);
    }
  }

  @SuppressWarnings("serial")
  private static final TypeToken<Supplier<Object>> OBJECT_SUPPLIER =
      new TypeToken<Supplier<Object>>() {};

  @SuppressWarnings("serial")
  private static final TypeToken<Supplier<SimpleTestClass>> TEST_CLASS_SUPPLIER =
      new TypeToken<Supplier<SimpleTestClass>>() {};

  @Test
  public void genericFactory() throws UnsuitedClassException {
    Supplier<Object> generatedFactory =
        Classes.createFactory(OBJECT_SUPPLIER, SimpleTestClass.class);
    assertThat(generatedFactory.get()).isInstanceOf(SimpleTestClass.class);
  }

  @Test
  public void genericFactory2() throws UnsuitedClassException {
    Supplier<SimpleTestClass> generatedFactory =
        Classes.createFactory(TEST_CLASS_SUPPLIER, SimpleTestClass.class);
    assertThat(generatedFactory.get()).isInstanceOf(SimpleTestClass.class);
  }

  @Test(expected = UnsuitedClassException.class)
  @SuppressWarnings("CheckReturnValue")
  public void genericFactory_nonmatchingType() throws UnsuitedClassException {
    Classes.createFactory(TEST_CLASS_SUPPLIER, SimpleTestClass2.class);
  }

  @Test
  public void alwaysFreshInstances() throws UnsuitedClassException {
    Supplier<Object> generatedFactory =
        Classes.createFactory(OBJECT_SUPPLIER, SimpleTestClass.class);
    assertThat(generatedFactory.get()).isNotSameAs(generatedFactory.get());
  }

  @Test(expected = UnsuitedClassException.class)
  @SuppressWarnings("CheckReturnValue")
  public void multipleConstructors() throws UnsuitedClassException {
    Classes.createFactory(OBJECT_SUPPLIER, String.class);
  }

  @Test(expected = UnsuitedClassException.class)
  @SuppressWarnings("CheckReturnValue")
  public void abstractClass() throws UnsuitedClassException {
    Classes.createFactory(OBJECT_SUPPLIER, AbstractTestClass.class);
  }

  @Test(expected = UnsuitedClassException.class)
  @SuppressWarnings("CheckReturnValue")
  public void illegalDeclaredException() throws UnsuitedClassException {
    Classes.createFactory(OBJECT_SUPPLIER, ExceptionTestClass.class);
  }

  @Test
  public void legalException_superfluousParameter() throws Exception {
    TestFactory generatedFactory =
        Classes.createFactory(TypeToken.of(TestFactory.class), ExceptionTestClass.class);
    assertThat(generatedFactory.get("")).isInstanceOf(ExceptionTestClass.class);
  }

  @Test(expected = UnsuitedClassException.class)
  @SuppressWarnings("CheckReturnValue")
  public void missingParameter() throws UnsuitedClassException {
    Classes.createFactory(OBJECT_SUPPLIER, ParameterTestClass.class);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("CheckReturnValue")
  public void nullParameter() throws Exception {
    TestFactory generatedFactory =
        Classes.createFactory(TypeToken.of(TestFactory.class), ParameterTestClass.class);
    generatedFactory.get(null);
  }

  @Test
  public void withParameter() throws Exception {
    TestFactory generatedFactory =
        Classes.createFactory(TypeToken.of(TestFactory.class), ParameterTestClass.class);
    ParameterTestClass instance = (ParameterTestClass) generatedFactory.get("TEST");
    assertThat(instance.s).isEqualTo("TEST");
    assertThat(instance.i).isNull();
  }

  @Test
  public void factoryMethod() throws Exception {
    TestFactory generatedFactory =
        Classes.createFactory(TypeToken.of(TestFactory.class), FactoryMethodTestClass.class);
    ParameterTestClass instance = (ParameterTestClass) generatedFactory.get("TEST");
    assertThat(instance.getClass()).isEqualTo(FactoryMethodTestClass.class);
    assertThat(instance.s).isEqualTo("TEST");
    assertThat(instance.i).isNull();
  }

  @Test
  public void subClassFactoryMethod() throws Exception {
    TestFactory generatedFactory =
        Classes.createFactory(TypeToken.of(TestFactory.class), FactoryMethodTestClass2.class);
    ParameterTestClass instance = (ParameterTestClass) generatedFactory.get("TEST");
    assertThat(instance.getClass()).isEqualTo(FactoryMethodTestClass2.class);
    assertThat(instance.s).isEqualTo("TEST");
    assertThat(instance.i).isNull();
  }
}
