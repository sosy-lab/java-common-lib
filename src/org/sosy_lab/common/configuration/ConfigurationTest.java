/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.google.common.truth.Truth.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.ConfigurationBuilderFactory.DefaultConfigurationBuilderFactory;
import org.sosy_lab.common.log.LogManager;

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;

public class ConfigurationTest {

  private static enum TestEnum {
    E1, E2, E3;
  }

  @Options
  private static class TestEnumSetOptions {

    @Option(secure=true, description="Test injection of EnumSet")
    private EnumSet<? extends TestEnum> values = EnumSet.of(TestEnum.E1, TestEnum.E3);
  }

  private Configuration enumTestConfiguration() throws InvalidConfigurationException {
    return Configuration.builder()
                        .setOption("values", "E3, E2")
                        .build();
  }

  @Before
  public void setUp() {
    Configuration.setBuilderFactory(DefaultConfigurationBuilderFactory.INSTANCE);
  }

  @Test
  public void testEnumSet() throws InvalidConfigurationException {
    TestEnumSetOptions options = new TestEnumSetOptions();
    enumTestConfiguration().inject(options);
    assertThat(options.values).iteratesAs(TestEnum.E2, TestEnum.E3);
  }

  @Test
  public void testEnumSetDefault() throws InvalidConfigurationException {
    testDefault(TestEnumSetOptions.class);
  }


  @Options
  private static class TestSetOfEnumsOptions {

    @Option(secure=true, description="Test injection of a set of enum values")
    private Set<TestEnum> values = Sets.immutableEnumSet(TestEnum.E1, TestEnum.E3);
  }

  @Test
  public void testSetOfEnums() throws InvalidConfigurationException {
    TestSetOfEnumsOptions options = new TestSetOfEnumsOptions();
    enumTestConfiguration().inject(options);
    assertThat(options.values).iteratesAs(TestEnum.E2, TestEnum.E3);
  }

  @Test(expected=UnsupportedOperationException.class)
  public void testSetOfEnumsIsImmutable() throws InvalidConfigurationException {
    TestSetOfEnumsOptions options = new TestSetOfEnumsOptions();
    enumTestConfiguration().inject(options);
    options.values.add(TestEnum.E1);
  }

  @Test
  public void testSetOfEnumsIsOptimizedGuavaClass() throws InvalidConfigurationException {
    TestSetOfEnumsOptions options = new TestSetOfEnumsOptions();
    enumTestConfiguration().inject(options);
    assertThat(options.values.getClass().getName()).endsWith("ImmutableEnumSet");
  }

  @Test
  public void testSetOfEnumsDefault() throws InvalidConfigurationException {
    testDefault(TestSetOfEnumsOptions.class);
  }


  @Options
  private static class TestCharsetOptions {

    @Option(secure=true, description="Test injection of Charset instances")
    private Charset charset = Charset.defaultCharset();
  }

  @Test
  public void testCharset() throws InvalidConfigurationException {
    Configuration config = Configuration.builder()
                           .setOption("charset", "utf8")
                           .build();
    TestCharsetOptions options = new TestCharsetOptions();
    config.inject(options);
    assertThat(options.charset).isEqualTo(StandardCharsets.UTF_8);
  }

  @Test(expected=InvalidConfigurationException.class)
  public void testInvalidCharsetName() throws InvalidConfigurationException {
    Configuration config = Configuration.builder()
                           .setOption("charset", "invalid;name")
                           .build();
    TestCharsetOptions options = new TestCharsetOptions();
    config.inject(options);
  }

  @Test(expected=InvalidConfigurationException.class)
  public void testUnsupportedCharset() throws InvalidConfigurationException {
    Configuration config = Configuration.builder()
                           .setOption("charset", "foo-bar")
                           .build();
    TestCharsetOptions options = new TestCharsetOptions();
    config.inject(options);
  }

  @Options
  private static class TestPatternOptions {

    @Option(secure=true, description="Test injection of Pattern instances")
    private Pattern regexp = Pattern.compile(".*");
  }

  @Test
  public void testPattern() throws InvalidConfigurationException {
    Configuration config = Configuration.builder()
                           .setOption("regexp", "foo.*bar")
                           .build();

    TestPatternOptions options = new TestPatternOptions();
    config.inject(options);
    assertThat("fooTESTbar").matches(options.regexp);
    assertThat("barTESTfoo").doesNotMatch(options.regexp);
  }

  @Test(expected=InvalidConfigurationException.class)
  public void testInvalidPattern() throws InvalidConfigurationException {
    Configuration config = Configuration.builder()
                           .setOption("regexp", "*foo.*bar")
                           .build();

    TestPatternOptions options = new TestPatternOptions();
    config.inject(options);
  }

  @Test
  public void testPatternDefault() throws InvalidConfigurationException {
    testDefault(TestPatternOptions.class);
  }

  @Test
  public void shouldReturnCustomFactory() throws Exception {
    ConfigurationBuilderFactory mockFactory = mock(ConfigurationBuilderFactory.class);
    Configuration.setBuilderFactory(mockFactory);

    assertThat(Configuration.getBuilderFactory()).isSameAs(mockFactory);
  }

  @Test
  public void shouldReturnDefaultBuilder() throws Exception {
    ConfigurationBuilder builder = Configuration.builder();

    assertThat(builder).isInstanceOf(Builder.class);
  }

  @Test
  public void shouldReturnCustomBuilder() throws Exception {
    ConfigurationBuilder mockBuilder = mock(ConfigurationBuilder.class);
    ConfigurationBuilderFactory stubFactory = mock(ConfigurationBuilderFactory.class);
    when(stubFactory.getBuilder()).thenReturn(mockBuilder);

    Configuration.setBuilderFactory(stubFactory);

    assertThat(Configuration.builder()).isSameAs(mockBuilder);
  }

  /**
   * This is parameterized test case that checks whether the injection with
   * a default configuration does not change the value of the fields.
   * @param clsWithOptions A class with some declared options and a default constructor.
   */
  private void testDefault(Class<?> clsWithOptions) throws InvalidConfigurationException {
    Configuration config = Configuration.defaultConfiguration();

    try {
      Constructor<?> constructor = clsWithOptions.getDeclaredConstructor(new Class<?>[0]);
      constructor.setAccessible(true);

      Object injectedInstance = constructor.newInstance();
      config.inject(injectedInstance);

      Object defaultInstance = constructor.newInstance();

      for (Field field : clsWithOptions.getFields()) {
        field.setAccessible(true);
        Object injectedValue = field.get(injectedInstance);
        Object defaultValue = field.get(defaultInstance);

        assertThat(injectedValue).isSameAs(defaultValue);
      }

    } catch (ReflectiveOperationException e) {
      Throwables.propagate(e);
    }
  }

  @Options(prefix="prefix", deprecatedPrefix="deprecated")
  private static class DeprecatedOptions {
    @Option(secure=true, description="test")
    private String test = "test";
  }

  /**
   * Deprecated options are acceptable, but throw warnings.
   */
  @Test
  public void testDeprecatedOptionsWarning() throws Exception {
    @SuppressWarnings("resource")
    LogManager mockLogger = mock(LogManager.class);
    Configuration c = Configuration.builder()
        .setOption("deprecated.test", "myValue").build();
    c.enableLogging(mockLogger);

    DeprecatedOptions opts = new DeprecatedOptions();
    c.inject(opts);
    verify(mockLogger).logf(eq(Level.WARNING), anyString(), anyVararg());
    assertThat(opts.test).isEqualTo("myValue");
  }

  /**
   * When both deprecated and new options are supplied, the new option
   * is used and the warning is logged.
   */
  @Test
  public void testDuplicateOptions() throws Exception {
    @SuppressWarnings("resource")
    LogManager mockLogger = mock(LogManager.class);
    Configuration c = Configuration.builder()
        .setOption("deprecated.test", "myDeprecatedValue")
        .setOption("prefix.test", "myNewValue").build();
    c.enableLogging(mockLogger);
    DeprecatedOptions opts = new DeprecatedOptions();
    c.inject(opts);
    verify(mockLogger).logf(eq(Level.WARNING), anyString(), anyVararg());
    assertThat(opts.test).isEqualTo("myNewValue");
  }

  @Test
  public void testDuplicateOptionsSameValue() throws Exception {
    @SuppressWarnings("resource")
    LogManager mockLogger = mock(LogManager.class);
    Configuration c = Configuration.builder()
        .setOption("deprecated.test", "myValue")
        .setOption("prefix.test", "myValue").build();
    c.enableLogging(mockLogger);

    DeprecatedOptions opts = new DeprecatedOptions();
    c.inject(opts);
    verify(mockLogger, never()).log(eq(Level.WARNING), anyVararg());
    verify(mockLogger, never()).logf(eq(Level.WARNING), anyString(), anyVararg());
    assertThat(opts.test).isEqualTo("myValue");
  }

  @Test
  public void testCopyWithNewPrefix() throws Exception {
    @SuppressWarnings("resource")
    LogManager mockLogger = mock(LogManager.class);
    final Configuration c = Configuration.builder()
        .setOption("start.deprecated.test", "myDeprecatedValue")
        .setOption("start.prefix.test", "myNewValue")
        .setPrefix("start").build();
    c.enableLogging(mockLogger);
    DeprecatedOptions opts = new DeprecatedOptions();
    c.inject(opts);
    verify(mockLogger).logf(eq(Level.WARNING), anyString(), anyVararg());
    assertThat(opts.test).isEqualTo("myNewValue");
  }
}
