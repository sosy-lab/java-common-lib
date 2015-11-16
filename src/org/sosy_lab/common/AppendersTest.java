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
package org.sosy_lab.common;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.sosy_lab.common.Appenders.AbstractAppender;

import java.io.IOException;

public class AppendersTest {

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
