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

import static java.util.concurrent.TimeUnit.*;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.TimeUnit;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.google.common.base.Ticker;

/**
 * Class providing several convenient {@link Ticker} implementations.
 */
public class Tickers {

  public static abstract class TickerWithUnit extends Ticker {

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

  private static final class CurrentThreadCputime extends TickerWithUnit {
    static final TickerWithUnit INSTANCE = new CurrentThreadCputime();

    private final ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
    {
      if (!threadMxBean.isThreadCpuTimeSupported()) {
        throw new UnsupportedOperationException("JVM does not support measuring per-thread cputime");
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
      } catch (JMException e) {
        throw new UnsupportedOperationException(e);
      }
    }

    @Override
    public TimeUnit unit() {
      return NANOSECONDS;
    }
  }

  /**
   * Return a dummy {@link TickerWithUnit} that always returns 0.
   */
  public static TickerWithUnit getNullTicker() {
    return NullTicker.INSTANCE;
  }

  /**
   * Return a {@link TickerWithUnit} that delegates to {@link System#currentTimeMillis()}.
   * Note that the returned instance validates the contract of Ticker because
   * it provides values in milliseconds, not in nanoseconds.
   */
  public static TickerWithUnit getWalltimeMillis() {
    return WalltimeMillis.INSTANCE;
  }

  /**
   * Return a {@link TickerWithUnit} that delegates to {@link System#nanoTime()}.
   */
  public static TickerWithUnit getWalltimeNanos() {
    return WalltimeNanos.INSTANCE;
  }

  /**
   * Return a {@link TickerWithUnit} that delegates to {@link ThreadMXBean#getCurrentThreadCpuTime()}.
   * @throws UnsupportedOperationException If the JVM does not support measuring per-thread CPU time.
   */
  public static TickerWithUnit getCurrentThreadCputime() {
    return CurrentThreadCputime.INSTANCE;
  }

  /**
   * Return a {@link TickerWithUnit} that delegates to {@link OperatingSystemMXBean#getProcessCputime()}.
   * This is available on Sun/Oracle/OpenJDK JVM for Linux,
   * but not guaranteed on other platforms.
   * @throws UnsupportedOperationException If the JVM does not support measuring process CPU time.
   */
  public static TickerWithUnit getProcessCputime() {
    return ProcessCputime.INSTANCE;
  }
}
