// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

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
    String testString = "TEST-STRING";
    WithLongString instance = () -> testString;

    assertThat(MoreStrings.longStringOf(instance).toString()).isEqualTo(testString);
  }

  @Test
  @SuppressWarnings("unused")
  public void test_longStringOf_lazy() {
    AtomicBoolean wasCalled = new AtomicBoolean(false);
    WithLongString instance =
        () -> {
          wasCalled.set(true);
          return "";
        };

    Object unused = MoreStrings.longStringOf(instance);

    assertWithMessage("toLongString() method was called").that(wasCalled.get()).isFalse();
  }
}
