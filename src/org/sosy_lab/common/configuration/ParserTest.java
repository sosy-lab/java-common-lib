// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.configuration;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.io.CharSource;
import com.google.errorprone.annotations.Var;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.Parser.InvalidConfigurationFileException;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.io.TempFile;
import org.sosy_lab.common.io.TempFile.DeleteOnCloseFile;

@SuppressWarnings("CheckReturnValue")
public class ParserTest {

  private static final String TEST_FILE_SUFFIX = ".properties";
  private static final String TEST_FILE_PREFIX = "SoSy-Lab_Common_ParserTest";

  private Path basePath;

  @Before
  public void resetBasePath() {
    basePath = Paths.get("");
  }

  private Map<String, String> test(String content)
      throws IOException, InvalidConfigurationException {
    return Parser.parse(CharSource.wrap(content), Optional.ofNullable(basePath), basePath)
        .getOptions();
  }

  private void testInvalid(String content) {
    CharSource source = CharSource.wrap(content);
    Optional<Path> path = Optional.ofNullable(basePath);
    assertThrows(
        InvalidConfigurationFileException.class, () -> Parser.parse(source, path, basePath));
  }

  private void testEmpty(String content) {
    try {
      Map<String, String> parsedOptions = test(content);

      assertThat(parsedOptions).isEmpty();

    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private void testSingleOption(String content, String key, String value) {
    try {
      Map<String, String> parsedOptions = test(content);
      assertThat(parsedOptions).containsExactly(key, value);

    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  @Test
  public final void empty() {
    testEmpty("");
    testEmpty("   \t  ");
    testEmpty(" \n \n  \t \n ");
    testEmpty("[foo]");
    testEmpty("# comment\n [foo] \n [] \n \n");
  }

  @Test
  public final void simpleOptions() {
    testSingleOption("foo.bar=ab cde ef", "foo.bar", "ab cde ef");
    testSingleOption(" foo.bar \t= 123 4 5 6 ", "foo.bar", "123 4 5 6");
  }

  @Test
  public final void simpleOptionsWithoutBasePath() {
    basePath = null;
    testSingleOption("foo=bar", "foo", "bar");
  }

  @Test
  public final void specialChar() {
    testSingleOption("foo.bar= äöüß ", "foo.bar", "äöüß");
    testSingleOption("foo.bar= abc\t123 ", "foo.bar", "abc\t123");
    testSingleOption("foo.bar= abc=123 ", "foo.bar", "abc=123");
    testSingleOption("foo.bar====", "foo.bar", "===");
    testSingleOption("foo.bar= \"abc cde\"", "foo.bar", "\"abc cde\"");
    testSingleOption("foo.bar= a=1, b=2, c=3", "foo.bar", "a=1, b=2, c=3");
  }

  @Test
  public final void category() {
    testSingleOption("[foo]\n bar=abc", "foo.bar", "abc");
    testSingleOption("  [  foo  ]  \n bar=abc", "foo.bar", "abc");
    testSingleOption("[foo.bar]\n abc=123", "foo.bar.abc", "123");
    testSingleOption("[]\n foo.bar=123", "foo.bar", "123");
    testSingleOption("[]\n [foo]\n bar=123", "foo.bar", "123");
    testSingleOption("[foo]\n []\n bar=123", "bar", "123");
  }

  @Test
  public final void emptyLine() {
    testSingleOption("\n\n\n foo.bar=abc \n\n\n", "foo.bar", "abc");
    testSingleOption(" \n\t\n \t \n foo.bar=abc \n\n\n", "foo.bar", "abc");
  }

  @Test
  public final void comment() {
    testSingleOption("# comment \n foo.bar=abc", "foo.bar", "abc");
    testSingleOption("// comment \n foo.bar=abc", "foo.bar", "abc");
    testSingleOption("// comment \n foo.bar=abc \n # comment", "foo.bar", "abc");
    testSingleOption("foo.bar=abc # no comment", "foo.bar", "abc # no comment");
    testSingleOption("foo.bar=abc // no comment", "foo.bar", "abc // no comment");
    testSingleOption("#\n foo.bar=abc", "foo.bar", "abc");
    testSingleOption("#    \n foo.bar=abc", "foo.bar", "abc");
  }

  @Test
  public final void lineContinuation() {
    testSingleOption("foo.bar=abc\\\n123", "foo.bar", "abc123");
    testSingleOption("foo.bar=abc\\\n", "foo.bar", "abc");
    testSingleOption("foo.bar=abc\\", "foo.bar", "abc");
    testSingleOption("foo.bar=abc \\\n // no comment", "foo.bar", "abc // no comment");
    testSingleOption("foo.bar=abc \\\n #include no include", "foo.bar", "abc #include no include");
    testSingleOption("foo.bar=abc \\  \n   123", "foo.bar", "abc 123");
    testSingleOption("foo.bar= \\  \n   123", "foo.bar", "123");
    testSingleOption("foo.bar= a=1,\\\n b=2,\\\n c=3", "foo.bar", "a=1,b=2,c=3");
    testSingleOption("foo.bar=abc\\\n \\\n \\\n 123", "foo.bar", "abc123");
  }

  @Test
  public final void illegalLine1() {
    testInvalid("a");
  }

  @Test
  public final void illegalLine2() {
    testInvalid("abc.bar");
  }

  @Test
  public final void illegalLine3() {
    testInvalid("[foo.bar");
  }

  @Test
  public final void illegalKey1() {
    testInvalid("foo bar = abc");
  }

  @Test
  public final void illegalKey2() {
    testInvalid("fooäöüßbar = abc");
  }

  @Test
  public final void illegalKey3() {
    testInvalid("foo\tbar = abc");
  }

  @Test
  public final void illegalKey4() {
    testInvalid("foo\\bar = abc");
  }

  @Test
  public final void illegalCategory1() {
    testInvalid("[foo bar]");
  }

  @Test
  public final void illegalCategory2() {
    testInvalid("[fooäöüßbar]");
  }

  @Test
  public final void illegalCategory3() {
    testInvalid("[foo\tbar]");
  }

  @Test
  public final void illegalCategory4() {
    testInvalid("[foo\\bar]");
  }

  @Test
  public final void illegalDirective() {
    testInvalid("#comment");
  }

  @Test
  public final void illegalInclude1() {
    testInvalid("#include");
  }

  @Test
  public final void illegalInclude2() {
    testInvalid("#include  \t");
  }

  private void testInvalidInclude(String content) {
    CharSource source = CharSource.wrap(content);
    Optional<Path> path = Optional.ofNullable(basePath);
    assertThrows(FileNotFoundException.class, () -> Parser.parse(source, path, basePath));
  }

  @Test
  public final void illegalInclude3() {
    testInvalidInclude("#include .");
  }

  @Test
  public final void illegalInclude4() {
    testInvalidInclude("#include \\");
  }

  @Test
  public final void illegalInclude5() {
    testInvalidInclude("#include /");
  }

  @Test
  public final void illegalInclude6() {
    testInvalidInclude("#include ./SoSy-Lab Common Tests/Non-Existing-File");
  }

  @Test
  public final void duplicateOption1() {
    testInvalid("foo.bar=abc \n foo.bar=abc");
  }

  @Test
  public final void duplicateOption2() {
    testInvalid("foo.bar=abc \n foo.bar=123");
  }

  @Test
  public final void duplicateOption3() {
    testInvalid("foo.bar=abc \n [foo] \n bar=abc");
  }

  @Test
  public final void duplicateOption4() {
    testInvalid("[foo] \n bar=abc \n [foo] \n bar=abc");
  }

  @Test
  public final void duplicateOption5() {
    testInvalid("[foo] \n bar=abc \n [] \n foo.bar=abc");
  }

  private static Path createTempFile(String prefix, String suffix, String content)
      throws IOException {
    return TempFile.builder()
        .prefix(prefix)
        .suffix(suffix)
        .initialContent(content, Charset.defaultCharset())
        .create();
  }

  @Test
  public final void simpleInclude() throws IOException {
    Path included = createTempFile(TEST_FILE_PREFIX, TEST_FILE_SUFFIX, "foo.bar=abc");
    try {
      testSingleOption(" #include  " + included.toAbsolutePath() + "\t", "foo.bar", "abc");
    } finally {
      Files.delete(included);
    }
  }

  @Test
  public final void includeWithoutBasePath() {
    CharSource source = CharSource.wrap("#include test.properties");
    assertThrows(
        IllegalArgumentException.class, () -> Parser.parse(source, Optional.empty(), basePath));
  }

  @Test
  public final void includeWithSpecialCharsFilename() throws IOException {
    Path included =
        createTempFile("SoSy-Lab CommonParserTestÄöüß", TEST_FILE_SUFFIX, "foo.bar=abc");
    try {
      testSingleOption(" #include " + included.toAbsolutePath() + "\t", "foo.bar", "abc");
    } finally {
      Files.delete(included);
    }
  }

  @Test
  public final void includeDepthTwo() throws IOException {
    Path included1 = createTempFile(TEST_FILE_PREFIX, TEST_FILE_SUFFIX, "foo.bar=abc");
    Path included2 =
        createTempFile(
            TEST_FILE_PREFIX, TEST_FILE_SUFFIX, "#include " + included1.toAbsolutePath());
    try {
      testSingleOption("#include " + included2.toAbsolutePath(), "foo.bar", "abc");
    } finally {
      Files.delete(included1);
      Files.delete(included2);
    }
  }

  private static final int MAX_INCLUDE_TEST_DEPTH = 10;

  @Test
  public final void includeDepthN() throws IOException {
    @Var Path included = createTempFile(TEST_FILE_PREFIX, TEST_FILE_SUFFIX, "foo.bar=abc");

    List<Path> allFiles = new ArrayList<>();
    allFiles.add(included);

    for (int i = 0; i < MAX_INCLUDE_TEST_DEPTH; i++) {
      included =
          createTempFile(
              TEST_FILE_PREFIX, TEST_FILE_SUFFIX, "#include " + included.toAbsolutePath());
      allFiles.add(included);
    }

    try {
      testSingleOption("#include " + included.toAbsolutePath(), "foo.bar", "abc");
    } finally {
      for (Path toDelete : allFiles) {
        Files.delete(toDelete);
      }
    }
  }

  @Test
  public final void includeTwice() throws IOException {
    Path included = createTempFile(TEST_FILE_PREFIX, TEST_FILE_SUFFIX, "foo.bar=abc");
    try {
      testSingleOption(
          "#include " + included.toAbsolutePath() + "\n#include " + included.toAbsolutePath(),
          "foo.bar",
          "abc");
    } finally {
      Files.delete(included);
    }
  }

  @Test
  public final void recursiveInclude() throws IOException {
    try (DeleteOnCloseFile included =
        TempFile.builder()
            .prefix(TEST_FILE_PREFIX)
            .suffix(TEST_FILE_SUFFIX)
            .createDeleteOnClose()) {
      IO.writeFile(
          included.toPath(),
          Charset.defaultCharset(),
          "#include " + included.toPath().toAbsolutePath());
      testInvalid("#include " + included.toPath().toAbsolutePath());
    }
  }

  @Test
  public final void recursiveIncludeDepthTwo() throws IOException {
    Path included1 = TempFile.builder().prefix(TEST_FILE_PREFIX).suffix(TEST_FILE_SUFFIX).create();
    Path included2 =
        createTempFile(
            TEST_FILE_PREFIX, TEST_FILE_SUFFIX, "#include " + included1.toAbsolutePath());
    IO.writeFile(included1, Charset.defaultCharset(), "#include " + included2.toAbsolutePath());
    try {
      testInvalid("#include " + included1.toAbsolutePath());
    } finally {
      Files.delete(included1);
      Files.delete(included2);
    }
  }

  private static final int MAX_RECURSIVE_INCLUDE_TEST_DEPTH = 10;

  @Test
  public final void recursiveIncludeDepthN() throws IOException {
    Path firstIncluded =
        TempFile.builder().prefix(TEST_FILE_PREFIX).suffix(TEST_FILE_SUFFIX).create();

    List<Path> allFiles = new ArrayList<>();
    allFiles.add(firstIncluded);
    @Var Path included = firstIncluded;

    for (int i = 0; i < MAX_RECURSIVE_INCLUDE_TEST_DEPTH; i++) {
      included =
          createTempFile(
              TEST_FILE_PREFIX, TEST_FILE_SUFFIX, "#include " + included.toAbsolutePath());
      allFiles.add(included);
    }

    IO.writeFile(firstIncluded, Charset.defaultCharset(), "#include " + included.toAbsolutePath());

    try {
      testInvalid("#include " + included.toAbsolutePath());
    } finally {
      for (Path toDelete : allFiles) {
        Files.delete(toDelete);
      }
    }
  }

  @Test
  public final void overwriteIncludedOptionBefore() throws IOException {
    Path included = createTempFile(TEST_FILE_PREFIX, TEST_FILE_SUFFIX, "foo.bar=abc");
    try {
      testSingleOption("foo.bar=123 \n#include " + included.toAbsolutePath(), "foo.bar", "123");
    } finally {
      Files.delete(included);
    }
  }

  @Test
  public final void overwriteIncludedOptionAfter() throws IOException {
    Path included = createTempFile(TEST_FILE_PREFIX, TEST_FILE_SUFFIX, "foo.bar=abc");
    try {
      testSingleOption(
          "#include " + included.toAbsolutePath() + " \n foo.bar=123", "foo.bar", "123");
    } finally {
      Files.delete(included);
    }
  }

  @Test
  public final void overwriteIncludedDepthTwo1() throws IOException {
    Path included1 = createTempFile(TEST_FILE_PREFIX, TEST_FILE_SUFFIX, "foo.bar=abc");
    Path included2 =
        createTempFile(
            TEST_FILE_PREFIX,
            TEST_FILE_SUFFIX,
            "#include " + included1.toAbsolutePath() + "\n foo.bar=xyz");
    try {
      testSingleOption("#include " + included2.toAbsolutePath(), "foo.bar", "xyz");
    } finally {
      Files.delete(included1);
      Files.delete(included2);
    }
  }

  @Test
  public final void overwriteIncludedDepthTwo2() throws IOException {
    Path included1 = createTempFile(TEST_FILE_PREFIX, TEST_FILE_SUFFIX, "foo.bar=abc");
    Path included2 =
        createTempFile(
            TEST_FILE_PREFIX,
            TEST_FILE_SUFFIX,
            "#include " + included1.toAbsolutePath() + "\n foo.bar=xyz");
    try {
      testSingleOption("foo.bar=123 \n#include " + included2.toAbsolutePath(), "foo.bar", "123");
    } finally {
      Files.delete(included1);
      Files.delete(included2);
    }
  }

  @Test
  public final void contradictoryIncludes() throws IOException {
    Path included1 = createTempFile(TEST_FILE_PREFIX, TEST_FILE_SUFFIX, "foo.bar=abc");
    Path included2 = createTempFile(TEST_FILE_PREFIX, TEST_FILE_SUFFIX, "foo.bar=xyz");
    try {
      testSingleOption(
          "#include " + included1.toAbsolutePath() + "\n#include " + included2.toAbsolutePath(),
          "foo.bar",
          "xyz");
    } finally {
      Files.delete(included1);
      Files.delete(included2);
    }
  }

  @Test
  public final void relativePath1() throws IOException {
    Path included = createTempFile(TEST_FILE_PREFIX, TEST_FILE_SUFFIX, "foo.bar=abc");
    basePath = included;

    try {
      testSingleOption("#include " + included.getFileName(), "foo.bar", "abc");
    } finally {
      Files.delete(included);
    }
  }

  @Test
  public final void relativePath2() throws IOException {
    Path included1 = createTempFile(TEST_FILE_PREFIX, TEST_FILE_SUFFIX, "foo.bar=abc");
    Path included2 =
        createTempFile(TEST_FILE_PREFIX, TEST_FILE_SUFFIX, "#include " + included1.getFileName());

    try {
      testSingleOption("#include " + included2.toAbsolutePath(), "foo.bar", "abc");
    } finally {
      Files.delete(included1);
      Files.delete(included2);
    }
  }
}
