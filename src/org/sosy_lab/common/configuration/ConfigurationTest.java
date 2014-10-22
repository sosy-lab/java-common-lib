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

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.ConfigurationBuilderFactory.DefaultConfigurationBuilderFactory;

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
    assertEquals(EnumSet.of(TestEnum.E2, TestEnum.E3), options.values);
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
    assertEquals(EnumSet.of(TestEnum.E2, TestEnum.E3), options.values);
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
    assertThat(options.values.getClass().getName(), containsString("ImmutableEnumSet"));
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
    assertEquals(StandardCharsets.UTF_8, options.charset);
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
    assertTrue(options.regexp.matcher("fooTESTbar").matches());
    assertFalse(options.regexp.matcher("barTESTfoo").matches());
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

    assertEquals(mockFactory, Configuration.getBuilderFactory());
  }

  @Test
  public void shouldReturnDefaultBuilder() throws Exception {
    ConfigurationBuilder builder = Configuration.builder();

    assertTrue(builder instanceof Builder);
  }

  @Test
  public void shouldReturnCustomBuilder() throws Exception {
    ConfigurationBuilder mockBuilder = mock(ConfigurationBuilder.class);
    ConfigurationBuilderFactory stubFactory = mock(ConfigurationBuilderFactory.class);
    when(stubFactory.getBuilder()).thenReturn(mockBuilder);

    Configuration.setBuilderFactory(stubFactory);

    assertEquals(mockBuilder, Configuration.builder());
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

        assertEquals(defaultValue, injectedValue);
      }

    } catch (ReflectiveOperationException e) {
      Throwables.propagate(e);
    }
  }
}
