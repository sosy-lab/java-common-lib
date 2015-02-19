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
package org.sosy_lab.common;

import static com.google.common.truth.Truth.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.sosy_lab.common.MoreStrings.WithLongString;

public class MoreStringsTest {

  @Test
  public void startsWithIgnoreCase_Empty() {
    assertThat(MoreStrings.startsWithIgnoreCase("aB", "")).isTrue();
  }

  @Test
  public void startsWithIgnoreCase_Short() {
    assertThat(MoreStrings.startsWithIgnoreCase("aB", "aBc")).isFalse();
  }

  @Test
  public void startsWithIgnoreCase_Matching() {
    assertThat(MoreStrings.startsWithIgnoreCase("AbC", "aB")).isTrue();
  }

  @Test
  public void startsWithIgnoreCase_NonMatching() {
    assertThat(MoreStrings.startsWithIgnoreCase("AbC", "Ac")).isFalse();
  }

  @Test
  public void test_longStringOf() {
    final String testString = "TEST-STRING";
    WithLongString instance = new WithLongString() {
        @Override
        public String toLongString() {
          return testString;
        }
      };

    assertThat(MoreStrings.longStringOf(instance).toString()).isEqualTo(testString);
  }

  @Test
  @SuppressWarnings("unused")
  public void test_longStringOf_lazy() {
    final AtomicBoolean wasCalled = new AtomicBoolean(false);
    WithLongString instance = new WithLongString() {
        @Override
        public String toLongString() {
          wasCalled.set(true);
          return "";
        }
      };

    Object unused = MoreStrings.longStringOf(instance);

    assertThat(wasCalled.get()).named("Whether toLongString method was called").isFalse();
  }
}
