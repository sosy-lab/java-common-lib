/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.common.time;

import static com.google.common.truth.Truth.assert_;

import org.junit.Test;

public class TimerTest {

  @Test
  public void initialValue() {
    Timer timer = new Timer();
    assert_().that(timer.getNumberOfIntervals()).isEqualTo(0);
    assert_().that(timer.getSumTime()).isEqualTo(TimeSpan.empty());
    assert_().that(timer.getLengthOfLastInterval()).isEqualTo(TimeSpan.empty());
    assert_().that(timer.getMaxTime()).isEqualTo(TimeSpan.empty());
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

  @Test(expected = IllegalStateException.class)
  public void startTwice() {
    Timer timer = new Timer();
    timer.start();
    timer.start();
  }

  @Test(expected = IllegalStateException.class)
  public void stopTwice() {
    Timer timer = new Timer();
    timer.start();
    timer.stop();
    timer.stop();
  }
}
