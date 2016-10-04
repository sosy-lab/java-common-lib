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
package org.sosy_lab.common.time;

import static com.google.common.base.Preconditions.checkState;
import static java.util.concurrent.TimeUnit.SECONDS;

import javax.annotation.Nullable;

/**
 * This class represents a timer similar to {@link Timer}, however it nests two timers.
 *
 * <p>If you have a method A that calls a method B, this class can be used to measure (1) the time
 * spent by A and B together, (2) the time spent by B, and (3) the time spent only in A (without the
 * call to B). The advantage over using two separate timers is that this allows to compute average
 * and maximum of measurements for (3). A simple use of two timers would allow to compute these
 * values only for (1) and (2); for (3) one could only compute the sum time.
 *
 * <p>This class uses the name "total" to refer to measurement (1), the name "inner" to refer to
 * measurement (2), and the name "outer" to refer to measurement (3). So in general "total"
 * represents the sum of "inner" and "outer".
 *
 * <p>The "inner" timer can never be running as long as the "total" timer is not running. During a
 * single "total" interval, the "inner" timer can be started and stopped several times. This will be
 * counted only as one "inner" interval (e.g., when the average "inner" time is computed).
 * Similarly, when the "inner" timer is started and stopped several times in a single "total"
 * interval, this counts as only one "outer" interval. The number of all starts and stops of the
 * "inner" timer summed over all "outer" intervals is never used by this class and is not available
 * to the user.
 *
 * <p>This class is not thread-safe and may be used only from within a single thread.
 */
public final class NestedTimer {

  private final Timer totalTimer = new Timer();

  /** The sum of times of all intervals up to the last call to stopOuter(). */
  private long innerSumTime = 0;

  /** Volatile to make {@link #isRunning()} thread-safe. */
  private volatile @Nullable Timer currentInnerTimer = null;

  /** The maximal time of all intervals. */
  private long innerMaxTime = 0;
  private long outerMaxTime = 0;

  /** The length of the last measured interval. */
  private long lastOuterIntervalLength = 0;

  /** Start the outer timer. May be called only if the timer is currently not running. */
  public void startOuter() {
    checkState(!isRunning());
    assert currentInnerTimer == null;

    totalTimer.start();
    currentInnerTimer = new Timer(totalTimer.clock);
  }

  /**
   * Start both timers. May be called only if the timer is currently not running. Guarantees that
   * both timers are started in the exact same instant of time.
   */
  public void startBoth() {
    checkState(!isRunning());
    assert currentInnerTimer == null;

    long startTime = totalTimer.clock.read();
    totalTimer.start(startTime);
    currentInnerTimer = new Timer(totalTimer.clock);
    currentInnerTimer.start(startTime);
  }

  /**
   * Stop the outer timer. May be called only if the outer timer is currently running and the inner
   * timer is stopped.
   */
  public void stopOuter() {
    checkState(isOnlyOuterRunning());

    stopOuter(totalTimer.clock.read());
  }

  private void stopOuter(final long endTime) {
    assert !currentInnerTimer.isRunning();

    totalTimer.stop(endTime);

    long currentInnerSumTime = currentInnerTimer.sumTime();

    // calculate outer time
    lastOuterIntervalLength = totalTimer.lengthOfLastInterval() - currentInnerSumTime;
    outerMaxTime = Math.max(lastOuterIntervalLength, outerMaxTime);

    // update inner times
    innerSumTime += currentInnerSumTime;
    innerMaxTime = Math.max(currentInnerSumTime, innerMaxTime);

    // reset
    currentInnerTimer = null;
  }

  public void stopBoth() {
    checkState(isRunning());
    checkState(currentInnerTimer.isRunning());

    // stop both timers with same endTime
    long endTime = totalTimer.clock.read();

    currentInnerTimer.stop(endTime);
    stopOuter(endTime);
  }

  /**
   * Check if the timer is running. Contrary to the other methods of this class, this method is
   * thread-safe. This means it can be safely run from another thread.
   */
  public boolean isRunning() {
    return totalTimer.isRunning();
  }

  /**
   * Check if the outer timer is running, i.e., the timer is running but the inner timer is not
   * running. Contrary to the other methods of this class, this method is thread-safe. This means it
   * can be safely run from another thread.
   */
  public boolean isOnlyOuterRunning() {
    return isRunning() && !currentInnerTimer.isRunning();
  }

  /**
   * Get a reference to the current inner timer. This reference can be used to start and stop the
   * inner interval (even multiple times). The returned reference becomes invalid after a call to
   * {@link #stopOuter()} or {@link #stopBoth()} and may not be used anymore afterwards.
   */
  public Timer getCurentInnerTimer() {
    checkState(isRunning());
    return currentInnerTimer;
  }

  private long currentOuterInterval() {
    // TODO This is slightly imprecise if inner timer is running
    // because two clock reads will be made by currentTotalInterval and currentInnerTimer.sumTime().
    return isRunning() ? totalTimer.currentInterval() - currentInnerTimer.sumTime() : 0;
  }

  /**
   * Return the sum of all outer intervals. If the outer timer is running, the current interval is
   * also counted (up to the current time). If the timer was never started, this method returns 0.
   */
  public TimeSpan getOuterSumTime() {
    return totalTimer.export(outerSumTime());
  }

  private long outerSumTime() {
    // TODO This is slightly imprecise if inner timer is running
    // because two clock reads will be made by totalTimer.sumTime() and innerSumTime().
    return totalTimer.sumTime() - innerSumTime();
  }

  /**
   * Return the sum of all inner intervals. If the inner timer is running, the current interval is
   * also counted (up to the current time). If the timer was never started, this method returns 0.
   * To get only the sum time of the inner timer for the interval since the last time the outer
   * timer was started, call <code>getInnerTimer().getSumTime()</code>.
   */
  public TimeSpan getInnerSumTime() {
    return totalTimer.export(innerSumTime());
  }

  private long innerSumTime() {
    long result = innerSumTime;
    if (isRunning()) {
      result += currentInnerTimer.sumTime();
    }
    return result;
  }

  /**
   * Return the sum of all total intervals. If the outer timer is running, the current interval is
   * also counted (up to the current time). If the timer was never started, this method returns 0.
   */
  public TimeSpan getTotalSumTime() {
    return totalTimer.getSumTime();
  }

  /**
   * Return the maximal time of all outer intervals. If the outer timer is running, the current
   * interval is also counted (up to the current time). If the timer was never started, this method
   * returns 0.
   */
  public TimeSpan getOuterMaxTime() {
    return totalTimer.export(Math.max(outerMaxTime, currentOuterInterval()));
  }

  /**
   * Return the maximal time of all inner intervals. Note that this is not the same as the maximum
   * of the intervals of all inner timers. If the inner timer is running, the current interval is
   * also counted (up to the current time). If the timer was never started, this method returns 0.
   */
  public TimeSpan getInnerMaxTime() {
    long result = innerMaxTime;
    if (isRunning()) {
      result = Math.max(result, currentInnerTimer.maxTime());
    }
    return totalTimer.export(result);
  }

  /**
   * Return the maximal time of all total intervals. If the outer timer is running, the current
   * interval is also counted (up to the current time). If the timer was never started, this method
   * returns 0.
   */
  public TimeSpan getTotalMaxTime() {
    return totalTimer.getMaxTime();
  }

  /**
   * Return the number of total intervals. Note that this is the same as the number of outer
   * intervals. If timer is running, the current interval is also counted. If the timer was never
   * started, this method returns 0.
   */
  public int getNumberOfIntervals() {
    return totalTimer.getNumberOfIntervals();
  }

  /**
   * Return the length of the last measured outer interval. If the outer timer is running, this is
   * the time from the start of the current interval up to now. If the timer was never started, this
   * method returns 0.
   */
  public TimeSpan getLengthOfLastOuterInterval() {
    return totalTimer.export(isRunning() ? currentOuterInterval() : lastOuterIntervalLength);
  }

  /**
   * Return the length of the last measured total interval. If the timer is running, this is the
   * time from the start of the current interval up to now. If the timer was never started, this
   * method returns 0.
   */
  public TimeSpan getLengthOfLastTotalInterval() {
    return totalTimer.getLengthOfLastInterval();
  }

  /**
   * Return the average of all outer intervals. If the outer timer is running, the current interval
   * is also counted (up to the current time). If the timer was never started, this method returns
   * 0.
   */
  public TimeSpan getOuterAvgTime() {
    int currentNumberOfIntervals = getNumberOfIntervals();
    if (currentNumberOfIntervals == 0) {
      // prevent divide by zero
      return totalTimer.export(0);
    }
    return totalTimer.export(outerSumTime() / currentNumberOfIntervals);
  }

  /**
   * Return the average of all inner times. Note that this is not the average of all intervals of
   * the inner timers, but basically the same as <code>getInnerSumTime() / getNumberOfIntervals()
   * </code>. If the inner timer is running, the current interval is also counted (up to the current
   * time). If the timer was never started, this method returns 0.
   */
  public TimeSpan getInnerAvgSumTime() {
    int currentNumberOfIntervals = getNumberOfIntervals();
    if (currentNumberOfIntervals == 0) {
      // prevent divide by zero
      return totalTimer.export(0);
    }
    return totalTimer.export(innerSumTime() / currentNumberOfIntervals);
  }

  /**
   * Return the average of all total intervals. If the timer is running, the current interval is
   * also counted (up to the current time). If the timer was never started, this method returns 0.
   */
  public TimeSpan getTotalAvgTime() {
    return totalTimer.getAvgTime();
  }

  /**
   * Return a String with a default representation of the the sum of the times of total intervals.
   * For printing other times, or with a specific unit, use the appropriate getter and call {@link
   * TimeSpan#formatAs(java.util.concurrent.TimeUnit)}. The format and the content of the String
   * returned by this method is not guaranteed to be the same in future versions of this code.
   */
  @Override
  public String toString() {
    return getTotalSumTime().formatAs(SECONDS);
  }
}
