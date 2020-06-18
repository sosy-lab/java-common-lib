// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.time;

import static com.google.common.truth.Truth.assert_;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class TimerTest {

  @Test
  public void initialValue() {
    Timer timer = new Timer();
    assert_().that(timer.getNumberOfIntervals()).isEqualTo(0);
    assert_().that(timer.getSumTime()).isEqualTo(TimeSpan.empty());
    assert_().that(timer.getLengthOfLastInterval()).isEqualTo(TimeSpan.empty());
    assert_().that(timer.getMaxTime()).isEqualTo(TimeSpan.empty());
    assert_().that(timer.getMinTime()).isEqualTo(TimeSpan.empty());
  }

  @Test
  public void interval() {
    Timer timer = new Timer();
    assert_().that(timer.getNumberOfIntervals()).isEqualTo(0);
    timer.start();
    assert_().that(timer.getNumberOfIntervals()).isEqualTo(1);
    timer.stop();
    assert_().that(timer.getNumberOfIntervals()).isEqualTo(1);
  }

  @Test
  public void nterval2() {
    Timer timer = new Timer();

    for (int i = 0; i < 5; i++) {
      assert_().that(timer.getNumberOfIntervals()).isEqualTo(i);
      timer.start();
      assert_().that(timer.getNumberOfIntervals()).isEqualTo(i + 1);
      timer.stop();
      assert_().that(timer.getNumberOfIntervals()).isEqualTo(i + 1);
    }
  }

  @Test
  public void startTwice() {
    Timer timer = new Timer();
    timer.start();
    assertThrows(IllegalStateException.class, () -> timer.start());
  }

  @Test
  public void stopTwice() {
    Timer timer = new Timer();
    timer.start();
    timer.stop();
    assertThrows(IllegalStateException.class, () -> timer.stop());
  }
}
