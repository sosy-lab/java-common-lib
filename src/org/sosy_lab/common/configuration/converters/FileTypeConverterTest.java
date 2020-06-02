// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.configuration.converters;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static com.google.common.truth.TruthJUnit.assume;
import static org.junit.Assert.assertThrows;
import static org.sosy_lab.common.configuration.Configuration.defaultConfiguration;
import static org.sosy_lab.common.configuration.converters.FileTypeConverter.stripTrailingSeparator;

import com.google.common.base.Ascii;
import com.google.common.base.StandardSystemProperty;
import com.google.common.collect.Lists;
import com.google.common.io.CharSource;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.converters.FileTypeConverterTest.FileTypeConverterBasicTest;
import org.sosy_lab.common.configuration.converters.FileTypeConverterTest.FileTypeConverterSafeModeTest;
import org.sosy_lab.common.configuration.converters.FileTypeConverterTest.FileTypeConverterUnsafeModeTest;

@RunWith(Suite.class)
@SuiteClasses({
  FileTypeConverterSafeModeTest.class,
  FileTypeConverterUnsafeModeTest.class,
  FileTypeConverterBasicTest.class
})
public class FileTypeConverterTest {

  private FileTypeConverterTest() {}

  public static class FileTypeConverterBasicTest {

    @Test
    public void testgetInstanceForNewConfiguration() throws InvalidConfigurationException {
      Configuration config1 =
          Configuration.builder()
              .setOption("rootDirectory", "root")
              .setOption("output.path", "output")
              .build();
      FileTypeConverter conv1 = FileTypeConverter.createWithSafePathsOnly(config1);

      Configuration config2 = Configuration.builder().setOption("output.path", "output2").build();
      FileTypeConverter conv2 = conv1.getInstanceForNewConfiguration(config2);

      assertThat(conv2.rootPath).isEqualTo(conv1.rootPath);
      assertThat(conv2.safePathsOnly).isEqualTo(conv1.safePathsOnly);
      assertThat(conv2.outputPath).isEqualTo(Paths.get("root", "output2"));
    }
  }

  @Options
  static class FileInjectionTestOptions {
    @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
    @Option(secure = true, description = "none", name = "test.path")
    Path path;
  }

  public static class FileTypeConverterSafeModeTest extends FileTypeConverterTestBase {

    @Override
    FileTypeConverter createFileTypeConverter(Configuration pConfig)
        throws InvalidConfigurationException {
      return FileTypeConverter.createWithSafePathsOnly(pConfig);
    }

    @Override
    boolean isAllowed(boolean isInFile) {
      return isInFile ? isSafeWhenInConfigFile : isSafe;
    }

    @BeforeClass
    public static void skipOnWindows() {
      assume().withMessage("Safe mode not supported on Windows").that(isWindows()).isFalse();
    }
  }

  public static class FileTypeConverterUnsafeModeTest extends FileTypeConverterTestBase {

    @Override
    FileTypeConverter createFileTypeConverter(Configuration pConfig)
        throws InvalidConfigurationException {
      return FileTypeConverter.create(pConfig);
    }

    @Override
    boolean isAllowed(boolean isInFile) {
      return true;
    }
  }

  @RunWith(Parameterized.class)
  public abstract static class FileTypeConverterTestBase {

    @Parameters(name = "{0} (safe={1}, safeInFile={2})")
    public static List<Object[]> testPaths() {
      List<Object[]> tests =
          Lists.newArrayList(
              new Object[][] {
                // path and whether it is allowed in safe mode and when included from config/file
                {"/etc/passwd", false, false},
                {"relative/dir" + File.pathSeparator + "/etc", false, false},
                {"file", true, true},
                {"dir/../file", true, true},
                {"dir/./file", true, true},
                {"dir/./../file", true, true},
                {"dir//../file", true, true},
                {"./dir/file", true, true},
                {"../dir", false, true},
                {"./../dir", false, true},
                {"dir/../../file", false, true},
                {"../../file", false, false},
                {"dir/../../../file", false, false},
                {
                  stripTrailingSeparator(StandardSystemProperty.JAVA_IO_TMPDIR.value()) + "_/file",
                  false,
                  false,
                },
                {StandardSystemProperty.JAVA_IO_TMPDIR.value() + "/file", true, true},
                {StandardSystemProperty.JAVA_IO_TMPDIR.value() + "/../file", false, false},
              });
      if (!isWindows()) {
        tests.add(new Object[] {"file::name", false, false});
        tests.add(new Object[] {"file::name:illegal", false, false});
        tests.add(new Object[] {"file:::illegal", false, false});
      }
      return tests;
    }

    protected static boolean isWindows() {
      return Ascii.toLowerCase(StandardSystemProperty.OS_NAME.value()).contains("windows");
    }

    @Parameter(0)
    public String testPath;

    @Parameter(1)
    public boolean isSafe;

    @Parameter(2)
    public boolean isSafeWhenInConfigFile;

    @Options
    static class FileInjectionTestOptions {
      @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
      @Option(secure = true, description = "none", name = "test.path")
      Path path;
    }

    abstract FileTypeConverter createFileTypeConverter(Configuration config)
        throws InvalidConfigurationException;

    abstract boolean isAllowed(boolean isInFile);

    private void assertThrowsICE(ThrowingRunnable code, String... msgParts) {
      InvalidConfigurationException thrown =
          assertThrows(InvalidConfigurationException.class, code);
      assertThat(thrown).hasMessageThat().contains(testPath.replace('/', File.separatorChar));
      for (String part : msgParts) {
        assertThat(thrown).hasMessageThat().contains(part);
      }
    }

    @Test
    public void testCheckSafePath() throws Exception {
      FileTypeConverter conv = createFileTypeConverter(defaultConfiguration());
      Path path = Paths.get(testPath);

      Callable<Path> code = () -> conv.checkSafePath(path, "dummy");

      if (isAllowed(false)) {
        assertThat(code.call()).isEqualTo(path);
      } else {
        assertThrowsICE(code::call, "safe mode", "dummy");
      }
    }

    @Test
    public void testCreation_RootDirectory() throws Exception {
      Configuration config = Configuration.builder().setOption("rootDirectory", testPath).build();

      Callable<FileTypeConverter> code = () -> createFileTypeConverter(config);

      if (isAllowed(false)) {
        assertThat(code.call().getOutputDirectory())
            .isEqualTo(Paths.get(testPath).resolve("output").toString());
      } else {
        assertThrowsICE(code::call, "safe mode", "rootDirectory");
      }
    }

    @Test
    public void testCreation_OutputPath() throws Exception {
      Configuration config = Configuration.builder().setOption("output.path", testPath).build();

      Callable<FileTypeConverter> code = () -> createFileTypeConverter(config);

      if (isAllowed(false)) {
        assertThat(code.call().getOutputDirectory())
            .isEqualTo(Paths.get(".").resolve(testPath).toString());
      } else {
        assertThrowsICE(code::call, "output.path");
      }
    }

    @Test
    public void testConvert_InjectPath() throws Throwable {
      Configuration config =
          Configuration.builder()
              .setOption("test.path", testPath)
              .addConverter(FileOption.class, createFileTypeConverter(defaultConfiguration()))
              .build();
      FileInjectionTestOptions options = new FileInjectionTestOptions();

      ThrowingRunnable code = () -> config.inject(options);

      if (isAllowed(false)) {
        code.run();
        assertThat(options.path).isEqualTo(Paths.get(testPath));
      } else {
        assertThrowsICE(code, "safe mode", "test.path");
      }
    }

    @Test
    public void testConvert_DefaultPath() throws Throwable {
      Configuration config =
          Configuration.builder()
              .addConverter(FileOption.class, createFileTypeConverter(defaultConfiguration()))
              .build();
      FileInjectionTestOptions options = new FileInjectionTestOptions();
      options.path = Paths.get(testPath);

      ThrowingRunnable code = () -> config.inject(options);

      if (isAllowed(false)) {
        code.run();
        assertThat(options.path).isEqualTo(Paths.get(".").resolve(testPath));
      } else {
        assertThrowsICE(code, "safe mode", "test.path");
      }
    }

    @Test
    public void testConvert_DefaultPathWithRootDirectory() throws Throwable {
      Configuration configForConverter =
          Configuration.builder()
              .setOption("rootDirectory", "root")
              .setOption("output.path", "output")
              .build();

      Configuration config =
          Configuration.builder()
              .addConverter(FileOption.class, createFileTypeConverter(configForConverter))
              .build();
      FileInjectionTestOptions options = new FileInjectionTestOptions();
      options.path = Paths.get(testPath);

      ThrowingRunnable code = () -> config.inject(options);

      if (isAllowed(true)) {
        code.run();
        assertThat(options.path).isEqualTo(Paths.get("root").resolve(testPath));
      } else {
        assertThrowsICE(code, "safe mode", "test.path");
      }
    }

    @Test
    public void testConvert_InjectPathFromFile() throws Throwable {
      CharSource configFile = CharSource.wrap("test.path = " + testPath);
      Configuration config =
          Configuration.builder()
              .loadFromSource(configFile, "config", "config/file")
              .addConverter(FileOption.class, createFileTypeConverter(defaultConfiguration()))
              .build();
      FileInjectionTestOptions options = new FileInjectionTestOptions();

      ThrowingRunnable code = () -> config.inject(options);

      if (isAllowed(true)) {
        code.run();
        assertThat((Comparable<?>) options.path).isEqualTo(Paths.get("config").resolve(testPath));
      } else {
        assertThrowsICE(code, "safe mode", "test.path");
      }
    }

    @Test
    public void testConvertDefaultValueFromOtherInstance() throws Throwable {
      Configuration configForConverter =
          Configuration.builder()
              .setOption("rootDirectory", "root")
              .setOption("output.path", "output")
              .build();

      Configuration config =
          Configuration.builder()
              .addConverter(FileOption.class, createFileTypeConverter(configForConverter))
              .build();
      FileInjectionTestOptions options = new FileInjectionTestOptions();
      options.path = Paths.get(testPath);
      FileInjectionTestOptions options2 = new FileInjectionTestOptions();

      ThrowingRunnable code =
          () -> config.injectWithDefaults(options2, FileInjectionTestOptions.class, options);

      if (isAllowed(false)) {
        code.run();
        assertThat(options2.path).isEqualTo(Paths.get(testPath));
      } else {
        assertThrowsICE(code, "safe mode", "test.path");
      }
    }
  }
}
