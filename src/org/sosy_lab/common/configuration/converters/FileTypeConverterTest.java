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
package org.sosy_lab.common.configuration.converters;

import static com.google.common.truth.Truth.assertThat;
import static org.sosy_lab.common.configuration.Configuration.defaultConfiguration;

import com.google.common.base.StandardSystemProperty;
import com.google.common.io.CharSource;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.converters.FileTypeConverterTest.FileTypeConverterSafeModeTest;
import org.sosy_lab.common.configuration.converters.FileTypeConverterTest.FileTypeConverterUnsafeModeTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RunWith(Suite.class)
@SuiteClasses({FileTypeConverterSafeModeTest.class, FileTypeConverterUnsafeModeTest.class})
public class FileTypeConverterTest {

  @Options
  static class FileInjectionTestOptions {
    @FileOption(Type.OPTIONAL_INPUT_FILE)
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
    public static Object[][] testPaths() {
      return new Object[][] {
        // path and whether it is allowed in safe mode and when included from config/file
        {"/etc/passwd", false, false},
        {"relative/dir" + File.pathSeparator + "/etc", false, false},
        {"file::name", true, true},
        {"file::name:illegal", false, false},
        {"file:::illegal", false, false},
        {"file", true, true},
        {"dir/../file", true, true},
        {"./dir/file", true, true},
        {"../dir", false, true},
        {"dir/../../file", false, true},
        {"../../file", false, false},
        {"dir/../../../file", false, false},
        {StandardSystemProperty.JAVA_IO_TMPDIR.value() + "/file", true, true},
        {StandardSystemProperty.JAVA_IO_TMPDIR.value() + "/../file", false, false},
      };
    }

    @Parameter(0)
    public String testPath;

    @Parameter(1)
    public boolean isSafe;

    @Parameter(2)
    public boolean isSafeWhenInConfigFile;

    @Rule public final ExpectedException thrown = ExpectedException.none();

    @Options
    static class FileInjectionTestOptions {
      @FileOption(Type.OPTIONAL_INPUT_FILE)
      @Option(secure = true, description = "none", name = "test.path")
      Path path;
    }

    abstract FileTypeConverter createFileTypeConverter(Configuration config)
        throws InvalidConfigurationException;

    abstract boolean isAllowed(boolean isInFile);

    @Test
    public void testCheckSafePath() throws InvalidConfigurationException {
      FileTypeConverter conv = createFileTypeConverter(defaultConfiguration());

      Path path = Paths.get(testPath);

      if (!isAllowed(false)) {
        thrown.expect(InvalidConfigurationException.class);
        thrown.expectMessage("safe mode");
        thrown.expectMessage("dummy");
        thrown.expectMessage(testPath);
      }

      assertThat((Object) conv.checkSafePath(path, "dummy")).isEqualTo(path);
    }

    @Test
    public void testCreation_RootDirectory() throws InvalidConfigurationException {
      Configuration config = Configuration.builder().setOption("rootDirectory", testPath).build();

      if (!isAllowed(false)) {
        thrown.expect(InvalidConfigurationException.class);
        thrown.expectMessage("safe mode");
        thrown.expectMessage("rootDirectory");
        thrown.expectMessage(testPath);
      }

      FileTypeConverter conv = createFileTypeConverter(config);
      assertThat(conv.getOutputDirectory())
          .isEqualTo(Paths.get(testPath).resolve("output").toString());
    }

    @Test
    public void testCreation_OutputPath() throws InvalidConfigurationException {
      Configuration config = Configuration.builder().setOption("output.path", testPath).build();

      if (!isAllowed(false)) {
        thrown.expect(InvalidConfigurationException.class);
        thrown.expectMessage("output.path");
        thrown.expectMessage(testPath);
      }

      FileTypeConverter conv = createFileTypeConverter(config);
      assertThat(conv.getOutputDirectory()).isEqualTo(Paths.get(".").resolve(testPath).toString());
    }

    @Test
    public void testConvert_InjectPath() throws InvalidConfigurationException {
      Configuration config =
          Configuration.builder()
              .setOption("test.path", testPath)
              .addConverter(FileOption.class, createFileTypeConverter(defaultConfiguration()))
              .build();
      FileInjectionTestOptions options = new FileInjectionTestOptions();

      if (!isAllowed(false)) {
        thrown.expect(InvalidConfigurationException.class);
        thrown.expectMessage("safe mode");
        thrown.expectMessage("test.path");
        thrown.expectMessage(testPath);
      }

      config.inject(options);
      assertThat((Object) options.path).isEqualTo(Paths.get(testPath));
    }

    @Test
    public void testConvert_DefaultPath() throws InvalidConfigurationException {
      Configuration config =
          Configuration.builder()
              .setOption("test.path", testPath)
              .addConverter(FileOption.class, createFileTypeConverter(defaultConfiguration()))
              .build();
      FileInjectionTestOptions options = new FileInjectionTestOptions();
      options.path = Paths.get(testPath);

      if (!isAllowed(false)) {
        thrown.expect(InvalidConfigurationException.class);
        thrown.expectMessage("safe mode");
        thrown.expectMessage("test.path");
        thrown.expectMessage(testPath);
      }

      config.inject(options);
      assertThat((Object) options.path).isEqualTo(Paths.get(testPath));
    }

    @Test
    public void testConvert_InjectPathFromFile() throws InvalidConfigurationException, IOException {
      CharSource configFile = CharSource.wrap("test.path = " + testPath);
      Configuration config =
          Configuration.builder()
              .loadFromSource(configFile, "config", "config/file")
              .addConverter(FileOption.class, createFileTypeConverter(defaultConfiguration()))
              .build();
      FileInjectionTestOptions options = new FileInjectionTestOptions();

      if (!isAllowed(true)) {
        thrown.expect(InvalidConfigurationException.class);
        thrown.expectMessage("safe mode");
        thrown.expectMessage("test.path");
        thrown.expectMessage(testPath);
      }

      config.inject(options);
      assertThat((Object) options.path).isEqualTo(Paths.get("config").resolve(testPath));
    }
  }
}
