// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import org.junit.Test;
import org.sosy_lab.common.Appenders.AbstractAppender;

public class AppendersTest {

  @SuppressWarnings("UnnecessaryAnonymousClass") // easier for test
  private final Appender testAppender =
      new AbstractAppender() {
        @Override
        public void appendTo(Appendable out) throws IOException {
          out.append("123");
          out.append("456");
          out.append("7");
          out.append("8");
          out.append("9");
        }
      };

  @Test
  public void testToStringWithTruncation_NoLimit() {
    assertThat(Appenders.toStringWithTruncation(testAppender, 100)).isEqualTo("123456789");
    assertThat(Appenders.toStringWithTruncation(testAppender, 10)).isEqualTo("123456789");
    assertThat(Appenders.toStringWithTruncation(testAppender, 9)).isEqualTo("123456789");
  }

  @Test
  public void testToStringWithTruncation_Limit() {
    assertThat(Appenders.toStringWithTruncation(testAppender, 8)).isEqualTo("12345678");
    assertThat(Appenders.toStringWithTruncation(testAppender, 7)).isEqualTo("1234567");
    assertThat(Appenders.toStringWithTruncation(testAppender, 6)).isEqualTo("123456");
    assertThat(Appenders.toStringWithTruncation(testAppender, 5)).isEqualTo("12345");
    assertThat(Appenders.toStringWithTruncation(testAppender, 4)).isEqualTo("1234");
    assertThat(Appenders.toStringWithTruncation(testAppender, 3)).isEqualTo("123");
    assertThat(Appenders.toStringWithTruncation(testAppender, 2)).isEqualTo("12");
    assertThat(Appenders.toStringWithTruncation(testAppender, 1)).isEqualTo("1");
    assertThat(Appenders.toStringWithTruncation(testAppender, 0)).isEqualTo("");
  }
}
