// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.time;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Ascii;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.time.Tickers.TickerWithUnit;

/**
 * This class represents a timer like a stop watch. It can be started and stopped several times. It
 * measures the sum, the average, the minimum, the maximum and the number of those intervals. This
 * class is similar to {@link com.google.common.base.Stopwatch} but has more features.
 *
 * <p>This class is not thread-safe and may be used only from within a single thread.
 */
@SuppressFBWarnings(
    value = {"AT_NONATOMIC_OPERATIONS_ON_SHARED_VARIABLE", "AT_NONATOMIC_64BIT_PRIMITIVE"},
    justification = "Class is not supposed to be thread-safe.")
public final class Timer {

  private static final String DEFAULT_CLOCK_PROPERTY_NAME =
      Timer.class.getCanonicalName() + ".timeSource";

  /** The clock we use for accessing the time. */
  static final @Nullable TickerWithUnit DEFAULT_CLOCK;

  static {
    String clockToUse =
        Ascii.toUpperCase(
            System.getProperty(DEFAULT_CLOCK_PROPERTY_NAME, "WALLTIME_MILLIS").trim());
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
  private long startTime = 0;

  /**
   * The sum of times of all intervals. This field should be accessed through {@link #sumTime()} to
   * account for a currently running interval.
   */
  private long sumTime = 0;

  /** The maximal time of all intervals. */
  private long maxTime = 0;

  /**
   * The minimal time of all intervals. If no interval exists, this value is {@link Long#MAX_VALUE},
   * but will be interpreted by method {@link #minTime()} as 0. Thus, method {@link #minTime()}
   * should always be used to access this value in a meaningful way.
   */
  private long minTime = Long.MAX_VALUE;

  /**
   * The number of intervals. This field should be accessed through {@link #getNumberOfIntervals()}
   * to account for a currently running interval.
   */
  private int numberOfIntervals = 0;

  /** The length of the last measured interval. */
  private long lastIntervalLength = 0;

  /** Create a fresh timer in the not-running state. */
  public Timer() {
    if (DEFAULT_CLOCK == null) {
      throw new IllegalArgumentException(
          String.format(
              "Invalid value \'%s\' for property %s,"
                  + "cannot create Timer without explicitly specified clock.",
              System.getProperty(DEFAULT_CLOCK_PROPERTY_NAME), DEFAULT_CLOCK_PROPERTY_NAME));
    }
    clock = DEFAULT_CLOCK;
  }

  Timer(TickerWithUnit pClock) {
    clock = checkNotNull(pClock);
  }

  /** Start the timer. May be called only if the timer is currently not running. */
  public void start() {
    start(clock.read());
  }

  void start(long newStartTime) {
    checkState(!running);

    startTime = newStartTime;
    // one more interval is started
    numberOfIntervals++;
    running = true;
  }

  /** Stop the timer. May be called only if the timer is currently running. */
  public void stop() {
    stop(clock.read());
  }

  public void stopIfRunning() {
    if (isRunning()) {
      stop();
    }
  }

  void stop(long endTime) {
    checkState(running);

    lastIntervalLength = endTime - startTime;
    sumTime += lastIntervalLength;
    maxTime = Math.max(lastIntervalLength, maxTime);
    minTime = Math.min(lastIntervalLength, minTime);

    // reset
    startTime = 0;
    running = false;
  }

  TimeSpan export(long time) {
    return TimeSpan.of(time, clock.unit());
  }

  /**
   * Check if the timer is running. Contrary to all other methods of this class, this method is
   * thread-safe. This means it can be safely run from another thread.
   */
  public boolean isRunning() {
    return running;
  }

  long currentInterval() {
    return running ? clock.read() - startTime : 0;
  }

  /**
   * Return the sum of all intervals. If timer is running, the current interval is also counted (up
   * to the current time). If the timer was never started, this method returns 0.
   */
  public TimeSpan getSumTime() {
    return export(sumTime());
  }

  long sumTime() {
    return sumTime + currentInterval();
  }

  /**
   * Return the maximal time of all intervals. If timer is running, the current interval is also
   * counted (up to the current time). If the timer was never started, this method returns 0.
   */
  public TimeSpan getMaxTime() {
    return export(maxTime());
  }

  /**
   * Return the minimal time of all intervals. If the timer is running, the current interval is not
   * considered. If the timer was never started, this method returns 0.
   */
  public TimeSpan getMinTime() {
    return export(minTime());
  }

  long maxTime() {
    return Math.max(maxTime, currentInterval());
  }

  long minTime() {
    if (minTime == Long.MAX_VALUE) {
      return 0;

    } else {
      return minTime;
    }
  }

  /**
   * Return the number of intervals. If timer is running, the current interval is also counted. If
   * the timer was never started, this method returns 0.
   */
  public int getNumberOfIntervals() {
    return numberOfIntervals;
  }

  /**
   * Return the length of the last measured interval. If the timer is running, this is the time from
   * the start of the current interval up to now. If the timer was never started, this method
   * returns 0.
   */
  public TimeSpan getLengthOfLastInterval() {
    return export(lengthOfLastInterval());
  }

  long lengthOfLastInterval() {
    return running ? currentInterval() : lastIntervalLength;
  }

  /**
   * Return the average of all intervals. If timer is running, the current interval is also counted
   * (up to the current time). If the timer was never started, this method returns 0.
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
   * Return a String with a default representation of the the sum of the times of all intervals. For
   * printing other times, or with a specific unit, use the appropriate getter and call {@link
   * TimeSpan#formatAs(java.util.concurrent.TimeUnit)}. The format and the content of the String
   * returned by this method is not guaranteed to be the same in future versions of this code.
   */
  @Override
  public String toString() {
    return getSumTime().formatAs(TimeUnit.SECONDS);
  }

  /** Syntax sugar method: pretty-format the timer output into a string in seconds. */
  public String prettyFormat() {
    TimeUnit t = TimeUnit.SECONDS;
    return String.format(
        "%s (Max: %s), (Avg: %s), (#intervals = %s)",
        getSumTime().formatAs(t),
        getMaxTime().formatAs(t),
        getAvgTime().formatAs(t),
        getNumberOfIntervals());
  }
}
