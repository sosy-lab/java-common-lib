// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.time;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.google.common.base.Ticker;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.TimeUnit;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.RuntimeErrorException;

/** Class providing several convenient {@link Ticker} implementations. */
public final class Tickers {

  public abstract static class TickerWithUnit extends Ticker {

    public abstract TimeUnit unit();
  }

  private Tickers() {}

  // Use Initialization on Demand Holder pattern for all instances
  // so they get initialized only if they are needed
  // (not all JVMs support all of them).

  private static final class NullTicker extends TickerWithUnit {
    static final TickerWithUnit INSTANCE = new NullTicker();

    @Override
    public long read() {
      return 0;
    }

    @Override
    public TimeUnit unit() {
      return NANOSECONDS;
    }
  }

  private static final class WalltimeMillis extends TickerWithUnit {
    static final TickerWithUnit INSTANCE = new WalltimeMillis();

    @Override
    public long read() {
      return System.currentTimeMillis();
    }

    @Override
    public TimeUnit unit() {
      return MILLISECONDS;
    }
  }

  private static final class WalltimeNanos extends TickerWithUnit {
    static final TickerWithUnit INSTANCE = new WalltimeNanos();

    @Override
    public long read() {
      return System.nanoTime();
    }

    @Override
    public TimeUnit unit() {
      return NANOSECONDS;
    }
  }

  @SuppressWarnings("CheckReturnValue")
  private static final class CurrentThreadCputime extends TickerWithUnit {
    static final TickerWithUnit INSTANCE = new CurrentThreadCputime();

    private final ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();

    {
      if (!threadMxBean.isThreadCpuTimeSupported()) {
        throw new UnsupportedOperationException(
            "JVM does not support measuring per-thread cputime");
      }
      threadMxBean.setThreadCpuTimeEnabled(true);
      read(); // read once to throw fail-fast exception if its not supported
    }

    @Override
    public long read() {
      return threadMxBean.getCurrentThreadCpuTime();
    }

    @Override
    public TimeUnit unit() {
      return NANOSECONDS;
    }
  }

  @SuppressWarnings("CheckReturnValue")
  private static final class ProcessCputime extends TickerWithUnit {
    static final TickerWithUnit INSTANCE = new ProcessCputime();

    private final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
    private final ObjectName osMbean;
    private static final String PROCESS_CPU_TIME = "ProcessCpuTime";

    {
      try {
        osMbean = new ObjectName(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
      } catch (MalformedObjectNameException e) {
        throw new UnsupportedOperationException(e);
      }
      read(); // read once to throw fail-fast exception if its not supported
    }

    @Override
    public long read() {
      try {
        return (Long) mbeanServer.getAttribute(osMbean, PROCESS_CPU_TIME);
      } catch (RuntimeErrorException e) {
        // Errors are better not hidden in an exception.
        if (e.getTargetError() != null) {
          throw e.getTargetError();
        }
        throw e;
      } catch (JMException e) {
        throw new UnsupportedOperationException(e);
      }
    }

    @Override
    public TimeUnit unit() {
      return NANOSECONDS;
    }
  }

  /** Return a dummy {@link TickerWithUnit} that always returns 0. */
  public static TickerWithUnit getNullTicker() {
    return NullTicker.INSTANCE;
  }

  /**
   * Return a {@link TickerWithUnit} that delegates to {@link System#currentTimeMillis()}. Note that
   * the returned instance validates the contract of Ticker because it provides values in
   * milliseconds, not in nanoseconds.
   */
  public static TickerWithUnit getWalltimeMillis() {
    return WalltimeMillis.INSTANCE;
  }

  /** Return a {@link TickerWithUnit} that delegates to {@link System#nanoTime()}. */
  public static TickerWithUnit getWalltimeNanos() {
    return WalltimeNanos.INSTANCE;
  }

  /**
   * Return a {@link TickerWithUnit} that delegates to {@link
   * ThreadMXBean#getCurrentThreadCpuTime()}.
   *
   * @throws UnsupportedOperationException If the JVM does not support measuring per-thread CPU
   *     time.
   */
  public static TickerWithUnit getCurrentThreadCputime() {
    return CurrentThreadCputime.INSTANCE;
  }

  /**
   * Return a {@link TickerWithUnit} that delegates to <code>
   * com.sun.management.OperatingSystemMXBean.getProcessCpuTime()</code>. This is available on
   * Sun/Oracle/OpenJDK JVM for Linux, but not guaranteed on other platforms.
   *
   * @throws UnsupportedOperationException If the JVM does not support measuring process CPU time.
   */
  public static TickerWithUnit getProcessCputime() {
    return ProcessCputime.INSTANCE;
  }
}
