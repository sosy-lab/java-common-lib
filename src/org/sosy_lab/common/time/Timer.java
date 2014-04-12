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
package org.sosy_lab.common.time;

import static java.util.concurrent.TimeUnit.SECONDS;

import static com.google.common.base.Preconditions.*;

import org.sosy_lab.common.time.Tickers.TickerWithUnit;

/**
 * This class represents a timer like a stop watch. It can be started and
 * stopped several times. It measures return the sum, the average, the maximum and
 * the number of those intervals.
 * This class is similar to {@link com.google.common.base.Stopwatch} but has more features.
 *
 * This class is not thread-safe and may be used only from within a single thread.
 */
public final class Timer {

  private static final String DEFAULT_CLOCK_PROPERTY_NAME = Timer.class.getCanonicalName() + ".timeSource";

  /**
   * The clock we use for accessing the time.
   */
  static final TickerWithUnit DEFAULT_CLOCK;
  static {
    String clockToUse = System.getProperty(DEFAULT_CLOCK_PROPERTY_NAME, "WALLTIME_MILLIS").toUpperCase().trim();
    switch (clockToUse) {
    case "WALLTIME_MILLIS":
      DEFAULT_CLOCK = Tickers.getWalltimeMillis();
      break;
    case "WALLTIME_NANOS":
      DEFAULT_CLOCK = Tickers.getWalltimeNanos();
      break;
    case "THREAD_CPUTIME":
      DEFAULT_CLOCK = Tickers.getCurrentThreadCputime();
      break;
    case "PROCESS_CPUTIME":
      DEFAULT_CLOCK = Tickers.getProcessCputime();
      break;
    case "NONE":
      DEFAULT_CLOCK = Tickers.getNullTicker();
      break;
    default:
      DEFAULT_CLOCK = null;
    }
  }

  // Visible for NestedTimer
  final TickerWithUnit clock;

  /** Whether the timer is running. */
  private volatile boolean running = false;

  /** The time when the timer was last started. */
  private long startTime             = 0;

  /**
   * The sum of times of all intervals.
   * This field should be accessed through {@link #sumTime()} to account for a currently running interval.
   */
  private long sumTime               = 0;

  /** The maximal time of all intervals. */
  private long maxTime               = 0;

  /**
   * The number of intervals.
   * This field should be accessed through {@link #getNumberOfIntervals()} to account for a currently running interval.
   */
  private int  numberOfIntervals     = 0;

  /** The length of the last measured interval. */
  private long lastIntervalLength    = 0;

  /**
   * Create a fresh timer in the not-running state.
   */
  public Timer() {
    if (DEFAULT_CLOCK == null) {
      throw new IllegalArgumentException("Invalid value \'" + System.getProperty(DEFAULT_CLOCK_PROPERTY_NAME) + "\'"
            + " for property " + DEFAULT_CLOCK_PROPERTY_NAME + ", cannot create Timer without explicitly specified clock.");
    }
    clock = DEFAULT_CLOCK;
  }

  Timer(TickerWithUnit pClock) {
    clock = checkNotNull(pClock);
  }

  /**
   * Start the timer.
   * May be called only if the timer is currently not running.
   */
  public void start() {
    start(clock.read());
  }

  void start(final long newStartTime) {
    checkState(!running);

    startTime = newStartTime;
    // one more interval is started
    numberOfIntervals++;
    running = true;
  }

  /**
   * Stop the timer.
   * May be called only if the timer is currently running.
   */
  public void stop() {
    stop(clock.read());
  }

  public void stopIfRunning() {
    if (isRunning()) {
      stop();
    }
  }

  void stop(final long endTime) {
    checkState(running);

    lastIntervalLength = endTime - startTime;
    sumTime += lastIntervalLength;
    maxTime = Math.max(lastIntervalLength, maxTime);

    // reset
    startTime = 0;
    running = false;
  }

  TimeSpan export(long time) {
    return TimeSpan.of(time, clock.unit());
  }

  /**
   * Check if the timer is running.
   * Contrary to all other methods of this class, this method is thread-safe.
   * This means it can be safely run from another thread.
   */
  public boolean isRunning() {
    return running;
  }

  long currentInterval() {
    return running
          ? clock.read() - startTime
          : 0;
  }

  /**
   * Return the sum of all intervals.
   * If timer is running, the current interval is also counted (up to the current time).
   * If the timer was never started, this method returns 0.
   */
  public TimeSpan getSumTime() {
    return export(sumTime());
  }

  long sumTime() {
    return sumTime + currentInterval();
  }

  /**
   * Return the maximal time of all intervals.
   * If timer is running, the current interval is also counted (up to the current time).
   * If the timer was never started, this method returns 0.
   */
  public TimeSpan getMaxTime() {
    return export(maxTime());
  }

  long maxTime() {
    return Math.max(maxTime, currentInterval());
  }

  /**
   * Return the number of intervals.
   * If timer is running, the current interval is also counted.
   * If the timer was never started, this method returns 0.
   */
  public int getNumberOfIntervals() {
    return numberOfIntervals + (running ? 1 : 0);
  }

  /**
   * Return the length of the last measured interval.
   * If the timer is running, this is the time from the start of the current interval
   * up to now.
   * If the timer was never started, this method returns 0.
   */
  public TimeSpan getLengthOfLastInterval() {
    return export(lengthOfLastInterval());
  }

  long lengthOfLastInterval() {
    return running
          ? currentInterval()
          : lastIntervalLength;
  }

  /**
   * Return the average of all intervals.
   * If timer is running, the current interval is also counted (up to the current time).
   * If the timer was never started, this method returns 0.
   */
  public TimeSpan getAvgTime() {
    int currentNumberOfIntervals = getNumberOfIntervals();
    if (currentNumberOfIntervals == 0) {
      // prevent divide by zero
      return export(0);
    }
    return export(sumTime() / currentNumberOfIntervals);
  }

  /**
   * Return a String with a default representation of the the sum of the times of all intervals.
   * For printing other times, or with a specific unit,
   * use the appropriate getter and call {@link TimeSpan#formatAs(java.util.concurrent.TimeUnit)}.
   * The format and the content of the String returned by this method
   * is not guaranteed to be the same in future versions of this code.
   */
  @Override
  public String toString() {
    return getSumTime().formatAs(SECONDS);
  }
}
