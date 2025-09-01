// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.configuration;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.TruthJUnit.assume;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.google.errorprone.annotations.Var;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.Test;
import org.mockito.Mockito;
import org.sosy_lab.common.configuration.converters.FileTypeConverter;
import org.sosy_lab.common.configuration.converters.TypeConverter;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.rationals.Rational;

public class ConfigurationTest {

  @Options
  private static class TestStringOptions {
    TestStringOptions() {}

    @Option(secure = true, description = "basic string option")
    private String s1;

    @Option(secure = true, description = "basic string option with type annotation @Nullable")
    private @Nullable String s2;
  }

  @Test
  public void testString() throws InvalidConfigurationException {
    TestStringOptions options = new TestStringOptions();
    Configuration.builder().setOption("s1", "1").setOption("s2", "2").build().inject(options);
    assertThat(options.s1).isEqualTo("1");
    assertThat(options.s2).isEqualTo("2");
  }

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

  private static Configuration enumTestConfiguration() throws InvalidConfigurationException {
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
  @SuppressWarnings("PreferredInterfaceType")
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

  @Test
  public void testSetOfEnumsIsImmutable() throws InvalidConfigurationException {
    TestSetOfEnumsOptions options = new TestSetOfEnumsOptions();
    enumTestConfiguration().inject(options);
    assertThrows(UnsupportedOperationException.class, () -> options.values.add(TestEnum.E1));
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

  @Test
  public void testInvalidCharsetName() throws InvalidConfigurationException {
    Configuration config = Configuration.builder().setOption("charset", "invalid;name").build();
    TestCharsetOptions options = new TestCharsetOptions();
    assertThrows(InvalidConfigurationException.class, () -> config.inject(options));
  }

  @Test
  public void testUnsupportedCharset() throws InvalidConfigurationException {
    Configuration config = Configuration.builder().setOption("charset", "foo-bar").build();
    TestCharsetOptions options = new TestCharsetOptions();
    assertThrows(InvalidConfigurationException.class, () -> config.inject(options));
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

  @Test
  public void testInvalidPattern() throws InvalidConfigurationException {
    Configuration config = Configuration.builder().setOption("regexp", "*foo.*bar").build();

    TestPatternOptions options = new TestPatternOptions();
    assertThrows(InvalidConfigurationException.class, () -> config.inject(options));
  }

  @Test
  public void testPatternDefault() throws InvalidConfigurationException {
    testDefault(TestPatternOptions.class);
  }

  /**
   * This is parametrized test case that checks whether the injection with a default configuration
   * does not change the value of the fields.
   *
   * @param clsWithOptions A class with some declared options and a default constructor.
   */
  private static void testDefault(Class<?> clsWithOptions) throws InvalidConfigurationException {
    Configuration config = Configuration.defaultConfiguration();

    try {
      Constructor<?> constructor = clsWithOptions.getDeclaredConstructor();
      constructor.setAccessible(true);

      Object injectedInstance = constructor.newInstance();
      config.inject(injectedInstance);

      Object defaultInstance = constructor.newInstance();

      for (Field field : clsWithOptions.getFields()) {
        field.setAccessible(true);
        Object injectedValue = field.get(injectedInstance);
        Object defaultValue = field.get(defaultInstance);

        assertThat(injectedValue).isSameInstanceAs(defaultValue);
      }

    } catch (ReflectiveOperationException e) {
      throw new LinkageError("Reflective access to " + clsWithOptions.getName() + " failed", e);
    }
  }

  @Options(prefix = "prefix", deprecatedPrefix = "deprecated")
  private static class DeprecatedOptions {
    @Option(secure = true, description = "test")
    private String test = "test";
  }

  /** Deprecated options are acceptable, but throw warnings. */
  @Test
  @SuppressWarnings("FormatStringAnnotation")
  public void testDeprecatedOptionsWarning() throws Exception {
    @SuppressWarnings("resource")
    LogManager mockLogger = mock(LogManager.class);
    Configuration c = Configuration.builder().setOption("deprecated.test", "myValue").build();
    c.enableLogging(mockLogger);

    DeprecatedOptions opts = new DeprecatedOptions();
    c.inject(opts);
    verify(mockLogger).logf(eq(Level.WARNING), anyString(), any(Object[].class));
    assertThat(opts.test).isEqualTo("myValue");
  }

  /**
   * When both deprecated and new options are supplied, the new option is used and the warning is
   * logged.
   */
  @Test
  @SuppressWarnings("FormatStringAnnotation")
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
    verify(mockLogger).logf(eq(Level.WARNING), anyString(), any(Object[].class));
    assertThat(opts.test).isEqualTo("myNewValue");
  }

  @Test
  @SuppressWarnings("FormatStringAnnotation")
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
    verify(mockLogger, never()).log(eq(Level.WARNING), (Object[]) any());
    verify(mockLogger, never()).logf(eq(Level.WARNING), anyString(), any());
    assertThat(opts.test).isEqualTo("myValue");
  }

  @SuppressWarnings("deprecation")
  @Test
  public void testCopyWithNewPrefix() throws Exception {
    @Var Configuration c = Configuration.builder().setOption("new-prefix.hello", "world").build();
    c = Configuration.copyWithNewPrefix(c, "new-prefix");
    assertThat(c.getProperty("hello")).isEqualTo("world");
  }

  @Options(prefix = "prefix")
  private static class SecureOptions {
    @SuppressWarnings("unused")
    @Option(secure = false, description = "test")
    private String test = "test";
  }

  @Test
  public void testSecureMode() throws Exception {
    Configuration c = Configuration.builder().setOption("prefix.test", "value").build();
    Configuration.enableSecureModeGlobally();
    SecureOptions opts = new SecureOptions();

    InvalidConfigurationException thrown =
        assertThrows(InvalidConfigurationException.class, () -> c.inject(opts));
    assertThat(thrown).hasMessageThat().contains("not allowed in secure mode");
  }

  @Test
  public void testDefaultConverters() throws Exception {
    Map<Class<?>, TypeConverter> converters = Configuration.getDefaultConverters();
    assertThat(converters).containsKey(IntegerOption.class);
  }

  @SuppressWarnings("deprecation")
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
    assertThat(opts.test).isEqualTo("myDeprecatedValue");
    assertThat(c.getDeprecatedProperties()).contains("prefix.test");
  }

  @Options(prefix = "prefix", deprecatedPrefix = "deprecated")
  private static class DeprecatedOptionNames {
    @Option(secure = true, description = "test", name = "name", deprecatedName = "deprecated")
    private String optionWithDeprecatedName = "default";

    @Option(secure = true, description = "test", name = "name")
    private String optionWithoutDeprecatedName = "default";
  }

  private static DeprecatedOptionNames deprecatedInjectionResultOf(String option)
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
    String test = "oldValue";

    void callInjectSuperclassBug(Configuration config) throws InvalidConfigurationException {
      config.inject(this);
    }

    void callInjectSuperclassCorrect(Configuration config) throws InvalidConfigurationException {
      config.inject(this, OptionsSuperclass.class);
    }
  }

  @Options
  private static class OptionsSubclass extends OptionsSuperclass {

    void callInjectSubclass(Configuration config) throws InvalidConfigurationException {
      config.inject(this);
    }
  }

  @Test
  public void testDetectionOfSubclass() throws Exception {
    OptionsSubclass opts = new OptionsSubclass();
    Configuration c = Configuration.builder().setOption("test", "newValue").build();

    // expected to succeed but do nothing
    opts.callInjectSubclass(c);
    assertThat(opts.test).isEqualTo("oldValue");

    // expected to succeed
    opts.callInjectSuperclassCorrect(c);
    assertThat(opts.test).isEqualTo("newValue");

    // expected to fail with assertion (but only if assertions are enabled)
    assume().that(ConfigurationTest.class.desiredAssertionStatus()).isTrue();
    AssertionError error =
        assertThrows(AssertionError.class, () -> opts.callInjectSuperclassBug(c));
    assertThat(error).hasMessageThat().contains("injected by calls from a superclass");
  }

  @Test
  public void testRecursiveInject() throws Exception {
    Configuration c = Configuration.builder().setOption("test", "newValue").build();
    OptionsSubclass opts2 = new OptionsSubclass();
    c.recursiveInject(opts2);
    assertThat(opts2.test).isEqualTo("newValue");
  }

  @Test
  public void testWithDefaultsUsed() throws Exception {
    String value = "newValue";
    OptionsSubclass opts = new OptionsSubclass();
    opts.test = value;

    Configuration c = Configuration.defaultConfiguration();

    OptionsSubclass opts2 = new OptionsSubclass();
    c.injectWithDefaults(opts2, OptionsSuperclass.class, opts);
    assertThat(opts2.test).isEqualTo(value);
  }

  @Test
  public void testWithDefaultsUnused() throws Exception {
    String value = "newValue";
    OptionsSubclass opts = new OptionsSubclass();
    opts.test = value;

    Configuration c = Configuration.builder().setOption("test", value + "2").build();

    OptionsSubclass opts2 = new OptionsSubclass();
    c.injectWithDefaults(opts2, OptionsSuperclass.class, opts);
    assertThat(opts2.test).isEqualTo(value + "2");
    assertThat(opts.test).isEqualTo(value); // opts should not be changed
  }

  @Test
  public void testCallTypeConverterConvert() throws Exception {
    TypeConverter conv = mock(TypeConverter.class);
    when(conv.convert(any(), any(), any(), any(), any(), any())).thenReturn("newValue");

    Configuration c =
        Configuration.builder()
            .setOption("test", "otherValue")
            .addConverter(String.class, conv)
            .build();

    OptionsSubclass opts = new OptionsSubclass();
    c.inject(opts, OptionsSuperclass.class);

    assertThat(opts.test).isEqualTo("newValue");

    verify(conv)
        .convert(
            eq("test"), eq("otherValue"), eq(TypeToken.of(String.class)), eq(null), any(), any());
    verifyNoMoreInteractions(conv);
  }

  @Test
  public void testCallTypeConverterConvertDefaultValue() throws Exception {
    TypeConverter conv = mock(TypeConverter.class);
    when(conv.convertDefaultValue(any(), any(), any(), any())).thenReturn("newValue");

    Configuration c = Configuration.builder().addConverter(String.class, conv).build();

    OptionsSubclass opts = new OptionsSubclass();
    c.inject(opts, OptionsSuperclass.class);

    assertThat(opts.test).isEqualTo("newValue");

    verify(conv).convertDefaultValue("test", "oldValue", TypeToken.of(String.class), null);
    verifyNoMoreInteractions(conv);
  }

  @Test
  public void testCallTypeConverterConvertDefaultValueFromOtherInstance() throws Exception {
    TypeConverter conv = mock(TypeConverter.class);
    when(conv.convertDefaultValueFromOtherInstance(any(), any(), any(), any()))
        .thenReturn("newValue");

    Configuration c = Configuration.builder().addConverter(String.class, conv).build();

    OptionsSubclass opts = new OptionsSubclass();
    OptionsSubclass opts2 = new OptionsSubclass();
    opts.test = "otherValue";
    c.injectWithDefaults(opts2, OptionsSuperclass.class, opts);

    assertThat(opts2.test).isEqualTo("newValue");
    assertThat(opts.test).isEqualTo("otherValue"); // opts should not be changed

    verify(conv)
        .convertDefaultValueFromOtherInstance(
            "test", "otherValue", TypeToken.of(String.class), null);
    verifyNoMoreInteractions(conv);
  }

  @SuppressWarnings("deprecation")
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
  public void testFromCmdLineArgumentsFailFormat() throws Exception {
    String[] args = {"--option1", "value1"};
    assertThrows(
        InvalidConfigurationException.class, () -> Configuration.fromCmdLineArguments(args));
  }

  @Test
  public void testFromCmdLineArgumentsFailFormat2() throws Exception {
    String[] args = {"-option1", "value1"};
    assertThrows(
        InvalidConfigurationException.class, () -> Configuration.fromCmdLineArguments(args));
  }

  @Options
  private static class OptionsWithRational {
    @Option(secure = true, description = "description")
    Rational x = Rational.ONE;
  }

  @Test
  public void testRationalInput() throws Exception {
    Configuration c = Configuration.builder().setOption("x", "2/3").build();
    OptionsWithRational opts = new OptionsWithRational();
    c.inject(opts);
    assertThat(opts.x).isEqualTo(Rational.of("2/3"));
  }

  @Options
  private static class AnnotatedOptions {
    @Option(secure = true, description = "test")
    AnnotatedValue<String> string;

    @Option(secure = true, description = "test")
    AnnotatedValue<Integer> integer;

    @Option(secure = true, description = "test")
    List<AnnotatedValue<Integer>> integerList;

    @Option(secure = true, description = "test")
    @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
    AnnotatedValue<Path> path;
  }

  @Test
  public void testAnnotatedOptions_noAnnotation() throws InvalidConfigurationException {
    FileTypeConverter fileConverter =
        FileTypeConverter.createWithSafePathsOnly(Configuration.defaultConfiguration());
    Configuration c =
        Configuration.builder()
            .setOption("string", "test")
            .setOption("integer", "1")
            .setOption("integerList", "1, 2, 3")
            .setOption("path", "test.txt")
            .addConverter(FileOption.class, fileConverter)
            .build();
    AnnotatedOptions opts = new AnnotatedOptions();
    c.inject(opts);
    assertThat(opts.string).isEqualTo(AnnotatedValue.create("test"));
    assertThat(opts.integer).isEqualTo(AnnotatedValue.create(1));
    assertThat(opts.integerList)
        .containsExactly(
            AnnotatedValue.create(1), AnnotatedValue.create(2), AnnotatedValue.create(3))
        .inOrder();
    assertThat(opts.path).isEqualTo(AnnotatedValue.create(Path.of("test.txt")));
  }

  @Test
  public void testAnnotatedOptions_annotation() throws InvalidConfigurationException {
    FileTypeConverter fileConverter =
        FileTypeConverter.createWithSafePathsOnly(Configuration.defaultConfiguration());
    Configuration c =
        Configuration.builder()
            .setOption("string", "test::a1")
            .setOption("integer", "1::a2")
            .setOption("integerList", "1::a1, 2, 3::a3")
            .setOption("path", "test.txt::a")
            .addConverter(FileOption.class, fileConverter)
            .build();
    AnnotatedOptions opts = new AnnotatedOptions();
    c.inject(opts);
    assertThat(opts.string).isEqualTo(AnnotatedValue.create("test", "a1"));
    assertThat(opts.integer).isEqualTo(AnnotatedValue.create(1, "a2"));
    assertThat(opts.integerList)
        .containsExactly(
            AnnotatedValue.create(1, "a1"),
            AnnotatedValue.create(2),
            AnnotatedValue.create(3, "a3"))
        .inOrder();
    assertThat(opts.path).isEqualTo(AnnotatedValue.create(Path.of("test.txt"), "a"));
  }

  @Options
  private abstract static class SetterOptions {
    @Option(secure = true, description = "test")
    abstract void pub(String s);

    @Option(secure = true, description = "test")
    private void priv(String s) {
      // private methods cannot be mocked, so we forward to a mockable method
      privForMock(s);
    }

    abstract void privForMock(String s);

    @Option(secure = true, description = "test")
    abstract void none(@Nullable String s);
  }

  @Test
  public void testSetter() throws InvalidConfigurationException {
    Configuration c =
        Configuration.builder().setOption("pub", "public").setOption("priv", "private").build();

    SetterOptions opts = Mockito.mock(SetterOptions.class);
    c.inject(opts, SetterOptions.class);
    Mockito.verify(opts).priv("private");
    Mockito.verify(opts).pub("public");
    Mockito.verify(opts).none(null);
    Mockito.verifyNoMoreInteractions(opts);
  }
}
