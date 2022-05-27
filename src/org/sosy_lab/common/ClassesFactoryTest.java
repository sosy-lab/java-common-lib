// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.reflect.TypeToken;
import java.io.IOException;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.Test;
import org.sosy_lab.common.Classes.UnsuitedClassException;

/** Tests for {@link Classes#createFactory(TypeToken, Class)}. */
public class ClassesFactoryTest {

  public interface TestFactory {
    Object get(String s) throws Exception;
  }

  public interface TestFactory2 {
    Object get(String s, @Nullable Integer i) throws Exception;
  }

  public static class SimpleTestClass {
    public SimpleTestClass() {}
  }

  public static class SimpleTestClass2 {
    public SimpleTestClass2() {}
  }

  public abstract static class AbstractTestClass {
    protected AbstractTestClass() {}
  }

  public static class ExceptionTestClass {
    @SuppressWarnings({"unused", "CheckedExceptionNotThrown"}) // for tests
    public ExceptionTestClass() throws IOException {}
  }

  public static class ParameterTestClass {
    private final String s;
    private final @Nullable Integer i;

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
  private static final TypeToken<Supplier<Object>> OBJECT_SUPPLIER = new TypeToken<>() {};

  @SuppressWarnings("serial")
  private static final TypeToken<Supplier<SimpleTestClass>> TEST_CLASS_SUPPLIER =
      new TypeToken<>() {};

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

  @Test
  public void genericFactory_nonmatchingType() {
    assertThrows(
        UnsuitedClassException.class,
        () -> Classes.createFactory(TEST_CLASS_SUPPLIER, SimpleTestClass2.class));
  }

  @Test
  public void alwaysFreshInstances() throws UnsuitedClassException {
    Supplier<Object> generatedFactory =
        Classes.createFactory(OBJECT_SUPPLIER, SimpleTestClass.class);
    assertThat(generatedFactory.get()).isNotSameInstanceAs(generatedFactory.get());
  }

  @Test
  public void multipleConstructors() {
    assertThrows(
        UnsuitedClassException.class, () -> Classes.createFactory(OBJECT_SUPPLIER, String.class));
  }

  @Test
  public void abstractClass() {
    assertThrows(
        UnsuitedClassException.class,
        () -> Classes.createFactory(OBJECT_SUPPLIER, AbstractTestClass.class));
  }

  @Test
  public void illegalDeclaredException() {
    assertThrows(
        UnsuitedClassException.class,
        () -> Classes.createFactory(OBJECT_SUPPLIER, ExceptionTestClass.class));
  }

  @Test
  public void legalException_superfluousParameter() throws Exception {
    TestFactory generatedFactory =
        Classes.createFactory(TypeToken.of(TestFactory.class), ExceptionTestClass.class);
    assertThat(generatedFactory.get("")).isInstanceOf(ExceptionTestClass.class);
  }

  @Test
  public void legalException() throws Exception {
    TestFactory2 generatedFactory =
        Classes.createFactory(TypeToken.of(TestFactory2.class), ExceptionTestClass.class);
    assertThat(generatedFactory.get("", null)).isInstanceOf(ExceptionTestClass.class);
  }

  @Test
  public void missingParameter() {
    assertThrows(
        UnsuitedClassException.class,
        () -> Classes.createFactory(OBJECT_SUPPLIER, ParameterTestClass.class));
  }

  @Test
  @SuppressWarnings("ParameterMissingNullable")
  public void nullParameter() throws Exception {
    TestFactory generatedFactory =
        Classes.createFactory(TypeToken.of(TestFactory.class), ParameterTestClass.class);
    assertThrows(NullPointerException.class, () -> generatedFactory.get(null));
  }

  @Test
  @SuppressWarnings("ParameterMissingNullable")
  public void nullParameter2() throws Exception {
    TestFactory2 generatedFactory =
        Classes.createFactory(TypeToken.of(TestFactory2.class), ParameterTestClass.class);
    assertThrows(NullPointerException.class, () -> generatedFactory.get(null, null));
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
  public void withTwoParameters1() throws Exception {
    TestFactory2 generatedFactory =
        Classes.createFactory(TypeToken.of(TestFactory2.class), ParameterTestClass.class);
    ParameterTestClass instance = (ParameterTestClass) generatedFactory.get("TEST", 1);
    assertThat(instance.s).isEqualTo("TEST");
    assertThat(instance.i).isEqualTo(1);
  }

  @Test
  public void withTwoParameters2() throws Exception {
    TestFactory2 generatedFactory =
        Classes.createFactory(TypeToken.of(TestFactory2.class), ParameterTestClass.class);
    ParameterTestClass instance = (ParameterTestClass) generatedFactory.get("TEST", null);
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
  public void factoryMethod2() throws Exception {
    TestFactory2 generatedFactory =
        Classes.createFactory(TypeToken.of(TestFactory2.class), FactoryMethodTestClass.class);
    ParameterTestClass instance = (ParameterTestClass) generatedFactory.get("TEST", null);
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

  @Test
  public void subClassFactoryMethod2() throws Exception {
    TestFactory2 generatedFactory =
        Classes.createFactory(TypeToken.of(TestFactory2.class), FactoryMethodTestClass2.class);
    ParameterTestClass instance = (ParameterTestClass) generatedFactory.get("TEST", null);
    assertThat(instance.getClass()).isEqualTo(FactoryMethodTestClass2.class);
    assertThat(instance.s).isEqualTo("TEST");
    assertThat(instance.i).isNull();
  }
}
