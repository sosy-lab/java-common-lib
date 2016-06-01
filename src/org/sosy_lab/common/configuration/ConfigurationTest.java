/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sosy_lab.common.configuration.converters.TypeConverter;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.rationals.Rational;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class ConfigurationTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  private enum TestEnum {
    E1,
    E2,
    E3
  }

  @Options
  private static class TestEnumSetOptions {

    @Option(secure = true, description = "Test injection of EnumSet")
    private EnumSet<? extends TestEnum> values = EnumSet.of(TestEnum.E1, TestEnum.E3);
  }

  private Configuration enumTestConfiguration() throws InvalidConfigurationException {
    return Configuration.builder().setOption("values", "E3, E2").build();
  }

  @Test
  public void testEnumSet() throws InvalidConfigurationException {
    TestEnumSetOptions options = new TestEnumSetOptions();
    enumTestConfiguration().inject(options);
    assertThat(options.values).containsExactly(TestEnum.E2, TestEnum.E3).inOrder();
  }

  @Test
  public void testEnumSetDefault() throws InvalidConfigurationException {
    testDefault(TestEnumSetOptions.class);
  }

  @Options
  private static class TestSetOfEnumsOptions {

    @Option(secure = true, description = "Test injection of a set of enum values")
    private Set<TestEnum> values = Sets.immutableEnumSet(TestEnum.E1, TestEnum.E3);
  }

  @Test
  public void testSetOfEnums() throws InvalidConfigurationException {
    TestSetOfEnumsOptions options = new TestSetOfEnumsOptions();
    enumTestConfiguration().inject(options);
    assertThat(options.values).containsExactly(TestEnum.E2, TestEnum.E3).inOrder();
  }

  @Test(expected = UnsupportedOperationException.class)
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

    @Option(secure = true, description = "Test injection of Charset instances")
    private Charset charset = Charset.defaultCharset();
  }

  @Test
  public void testCharset() throws InvalidConfigurationException {
    Configuration config = Configuration.builder().setOption("charset", "utf8").build();
    TestCharsetOptions options = new TestCharsetOptions();
    config.inject(options);
    assertThat(options.charset).isEqualTo(StandardCharsets.UTF_8);
  }

  @Test(expected = InvalidConfigurationException.class)
  public void testInvalidCharsetName() throws InvalidConfigurationException {
    Configuration config = Configuration.builder().setOption("charset", "invalid;name").build();
    TestCharsetOptions options = new TestCharsetOptions();
    config.inject(options);
  }

  @Test(expected = InvalidConfigurationException.class)
  public void testUnsupportedCharset() throws InvalidConfigurationException {
    Configuration config = Configuration.builder().setOption("charset", "foo-bar").build();
    TestCharsetOptions options = new TestCharsetOptions();
    config.inject(options);
  }

  @Options
  private static class TestPatternOptions {

    @Option(secure = true, description = "Test injection of Pattern instances")
    private Pattern regexp = Pattern.compile(".*");
  }

  @Test
  public void testPattern() throws InvalidConfigurationException {
    Configuration config = Configuration.builder().setOption("regexp", "foo.*bar").build();

    TestPatternOptions options = new TestPatternOptions();
    config.inject(options);
    assertThat("fooTESTbar").matches(options.regexp);
    assertThat("barTESTfoo").doesNotMatch(options.regexp);
  }

  @Test(expected = InvalidConfigurationException.class)
  public void testInvalidPattern() throws InvalidConfigurationException {
    Configuration config = Configuration.builder().setOption("regexp", "*foo.*bar").build();

    TestPatternOptions options = new TestPatternOptions();
    config.inject(options);
  }

  @Test
  public void testPatternDefault() throws InvalidConfigurationException {
    testDefault(TestPatternOptions.class);
  }

  /**
   * This is parametrized test case that checks whether the injection with
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

  @Options(prefix = "prefix", deprecatedPrefix = "deprecated")
  private static class DeprecatedOptions {
    @Option(secure = true, description = "test")
    private String test = "test";
  }

  /**
   * Deprecated options are acceptable, but throw warnings.
   */
  @Test
  public void testDeprecatedOptionsWarning() throws Exception {
    @SuppressWarnings("resource")
    LogManager mockLogger = mock(LogManager.class);
    Configuration c = Configuration.builder().setOption("deprecated.test", "myValue").build();
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
    Configuration c =
        Configuration.builder()
            .setOption("deprecated.test", "myDeprecatedValue")
            .setOption("prefix.test", "myNewValue")
            .build();
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
    Configuration c =
        Configuration.builder()
            .setOption("deprecated.test", "myValue")
            .setOption("prefix.test", "myValue")
            .build();
    c.enableLogging(mockLogger);

    DeprecatedOptions opts = new DeprecatedOptions();
    c.inject(opts);
    verify(mockLogger, never()).log(eq(Level.WARNING), anyVararg());
    verify(mockLogger, never()).logf(eq(Level.WARNING), anyString(), anyVararg());
    assertThat(opts.test).isEqualTo("myValue");
  }

  @Test
  public void testCopyWithNewPrefix() throws Exception {
    Configuration c = Configuration.builder().setOption("new-prefix.hello", "world").build();
    c = Configuration.copyWithNewPrefix(c, "new-prefix");
    assertThat(c.getProperty("hello")).isEqualTo("world");
  }

  @Options(prefix = "prefix")
  private static class SecureOptions {
    @Option(secure = false, description = "test")
    private String test = "test";
  }

  @Test
  public void testSecureMode() throws Exception {
    Configuration c = Configuration.builder().setOption("prefix.test", "value").build();
    Configuration.enableSecureModeGlobally();

    thrown.expect(InvalidConfigurationException.class);
    thrown.expectMessage("not allowed in secure mode");
    SecureOptions opts = new SecureOptions();
    c.inject(opts);
  }

  @Test
  public void testDefaultConverters() throws Exception {
    Map<Class<?>, TypeConverter> converters = Configuration.getDefaultConverters();
    assertThat(converters).containsKey(IntegerOption.class);
  }

  @Test
  public void testHasProperty() throws Exception {
    Configuration c = Configuration.builder().setOption("blah", "blah2").build();
    assertThat(c.hasProperty("blah")).isTrue();
  }

  @Test
  public void testGetUnusedProperties() throws Exception {
    Configuration c =
        Configuration.builder()
            .setOption("start.deprecated.test", "myDeprecatedValue")
            .setOption("start.prefix.test", "myNewValue")
            .setOption("blah", "myNewValue")
            .setPrefix("start")
            .build();
    DeprecatedOptions opts = new DeprecatedOptions();
    c.inject(opts);
    assertThat(c.getUnusedProperties()).contains("blah");
  }

  @Options(prefix = "prefix")
  private static class OptionsWithDeprecatedOption {
    @Deprecated
    @Option(secure = true, description = "test")
    private String test = "test";
  }

  @Test
  public void testGetDeprecatedProperties() throws Exception {
    Configuration c = Configuration.builder().setOption("prefix.test", "myDeprecatedValue").build();
    OptionsWithDeprecatedOption opts = new OptionsWithDeprecatedOption();
    c.inject(opts);
    assertThat(c.getDeprecatedProperties()).contains("prefix.test");
  }

  @Options(prefix = "prefix", deprecatedPrefix = "deprecated")
  private static class DeprecatedOptionNames {
    @Option(secure = true, description = "test", name = "name", deprecatedName = "deprecated")
    private String optionWithDeprecatedName = "default";

    @Option(secure = true, description = "test", name = "name")
    private String optionWithoutDeprecatedName = "default";
  }

  private DeprecatedOptionNames deprecatedInjectionResultOf(String option)
      throws InvalidConfigurationException {
    Configuration c = Configuration.builder().setOption(option, "value").build();
    DeprecatedOptionNames opts = new DeprecatedOptionNames();
    c.inject(opts);
    return opts;
  }

  @Test
  public void testOptionWithDeprecatedName1() throws InvalidConfigurationException {
    assertThat(deprecatedInjectionResultOf("prefix.name").optionWithDeprecatedName)
        .isEqualTo("value");
  }

  @Test
  public void testOptionWithDeprecatedName2() throws InvalidConfigurationException {
    assertThat(deprecatedInjectionResultOf("deprecated.name").optionWithDeprecatedName)
        .isEqualTo("default");
  }

  @Test
  public void testOptionWithDeprecatedName3() throws InvalidConfigurationException {
    assertThat(deprecatedInjectionResultOf("prefix.deprecated").optionWithDeprecatedName)
        .isEqualTo("default");
  }

  @Test
  public void testOptionWithDeprecatedName4() throws InvalidConfigurationException {
    assertThat(deprecatedInjectionResultOf("deprecated.deprecated").optionWithDeprecatedName)
        .isEqualTo("value");
  }

  @Test
  public void testOptionWithDeprecatedName5() throws InvalidConfigurationException {
    assertThat(deprecatedInjectionResultOf("prefix.name").optionWithoutDeprecatedName)
        .isEqualTo("value");
  }

  @Test
  public void testOptionWithDeprecatedName6() throws InvalidConfigurationException {
    assertThat(deprecatedInjectionResultOf("deprecated.name").optionWithoutDeprecatedName)
        .isEqualTo("value");
  }

  @Test
  public void testAsPropertiesString() throws Exception {
    Configuration c = Configuration.builder().setOption("prefix.test", "myDeprecatedValue").build();
    assertThat(c.asPropertiesString()).contains("prefix.test = myDeprecatedValue");
  }

  @Options
  private static class OptionsSuperclass {
    @Option(secure = true, description = "blah")
    protected String test = "oldValue";
  }

  @Options
  private static class OptionsSubclass extends OptionsSuperclass {}

  @Test
  public void testRecursiveInject() throws Exception {
    Configuration c = Configuration.builder().setOption("test", "newValue").build();
    OptionsSubclass opts2 = new OptionsSubclass();
    c.recursiveInject(opts2);
    assertThat(opts2.test).isEqualTo("newValue");
  }

  @Test
  public void testFromCmdLineArgumentsSimple() throws Exception {
    Configuration config =
        Configuration.fromCmdLineArguments(
            new String[] {
              "--option1=value1", "--option2=value2", "--option3=value3",
            });
    assertThat(config.getProperty("option1")).isEqualTo("value1");
    assertThat(config.getProperty("option2")).isEqualTo("value2");
    assertThat(config.getProperty("option3")).isEqualTo("value3");
  }

  @Test
  @SuppressWarnings("CheckReturnValue")
  public void testFromCmdLineArgumentsFailFormat() throws Exception {
    thrown.expect(InvalidConfigurationException.class);

    // No break should be there.
    Configuration.fromCmdLineArguments(
        new String[] {
          "--option1", "value1",
        });
  }

  @Test
  @SuppressWarnings("CheckReturnValue")
  public void testFromCmdLineArgumentsFailFormat2() throws Exception {
    thrown.expect(InvalidConfigurationException.class);

    // No break should be there.
    Configuration.fromCmdLineArguments(
        new String[] {
          "-option1", "value1",
        });
  }

  @Options
  private static class OptionsWithRational {
    @Option(secure = true, description = "")
    Rational x = Rational.ONE;
  }

  @Test
  public void testRationalInput() throws Exception {
    Configuration c = Configuration.builder().setOption("x", "2/3").build();
    OptionsWithRational opts = new OptionsWithRational();
    c.inject(opts);
    assertThat(opts.x).isEqualTo(Rational.of("2/3"));
  }
}
