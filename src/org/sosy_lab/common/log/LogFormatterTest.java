// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.log;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class LogFormatterTest {

  @Parameters(name = "{0}")
  public static ImmutableList<Formatter> formatters() {
    return ImmutableList.of(
        ConsoleLogFormatter.withColorsIfPossible(),
        ConsoleLogFormatter.withoutColors(),
        TimestampedLogFormatter.withColorsIfPossible(),
        TimestampedLogFormatter.withoutColors());
  }

  @Parameter(0)
  public Formatter formatter;

  @Before
  public void setup() {
    formatter = ConsoleLogFormatter.withoutColors();
  }

  private static LogRecord createTestLogRecord(Level level, String msg) {
    LogRecord record = new LogRecord(level, msg);
    record.setSourceClassName("package.SourceClass");
    record.setSourceMethodName("sourceMethod");
    return record;
  }

  @Test
  public void testMessageContained() {
    LogRecord record = createTestLogRecord(Level.SEVERE, "TEST MESSAGE");
    assertThat(formatter.format(record)).contains("TEST MESSAGE");
  }

  @Test
  public void testLevelContainedSevere() {
    LogRecord record = createTestLogRecord(Level.SEVERE, null);
    String msg = formatter.format(record);
    assertThat(msg).contains("SEVERE");
    assertThat(msg).doesNotContain("WARNING");
  }

  @Test
  public void testLevelContainedWarning() {
    LogRecord record = createTestLogRecord(Level.WARNING, null);
    String msg = formatter.format(record);
    assertThat(msg).contains("WARNING");
    assertThat(msg).doesNotContain("SEVERE");
  }

  @Test
  public void testSourceClassContained() {
    LogRecord record = createTestLogRecord(Level.WARNING, null);
    String msg = formatter.format(record);
    assertThat(msg).contains("SourceClass");
    assertThat(msg).doesNotContain("package");
  }

  @Test
  public void testSourceMethodContained() {
    LogRecord record = createTestLogRecord(Level.WARNING, null);
    assertThat(formatter.format(record)).contains("sourceMethod");
  }

  @Test
  public void testNoSourceInformation() {
    LogRecord record = new LogRecord(Level.WARNING, null);
    record.setSourceClassName(null);
    record.setSourceMethodName(null);
    assertThat(formatter.format(record)).contains("$Unknown$.$unknown$");
  }
}
